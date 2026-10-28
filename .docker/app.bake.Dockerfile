# ==============================================================================
# MODULE DOCKERFILE
# This file is not meant to be built standalone. It is consumed by the
# docker-bake.hcl files in the parent monorepos (MobileEditors).
#
# The app (Gradle project app_manager), with or without the native editors.
#
# REQUIRED CONTEXTS:
# - android-ndk: an image with the NDK in ANDROID_NDK_ROOT (its version in
#   ANDROID_NDK_VERSION), the one core is built with
# apk-editors and editors-unit-tests additionally:
# - core-android-arm64, core-android-x64: core's libraries per ABI
#   (core/.docker/core-android.bake.Dockerfile)
# - sdkjs-native: the files the editor engine loads (sdkjs/.docker/sdkjs.bake.Dockerfile)
# - dictionaries: the bundled spell checking dictionary and the index of the others
# Files from the main context (the monorepo, whose layout the Gradle projects
# expect): documents-app-android; for the editors also editors-android,
# editors-engine, core (the libx2t JNI wrapper), core-fonts and document-templates.
#
# Stages:
# - apk: the app without the editors (FROM scratch: its APKs)
# - apk-editors: the app with the native editors
# - editors-unit-tests: unit tests and Android Lint of the editors' modules (they
#   build only within the app's Gradle project): reports, gradle.log and result
#   (0 for success, kept also when they fail)
# - android-toolchain: the JDK and the Android SDK the app's Gradle build needs
# ==============================================================================

#### TOOLCHAIN ####
FROM android-ndk AS android-toolchain

    ARG CMDLINE_TOOLS_VERSION=16111833
    # Keep in sync with app_manager/gradle/libs.versions.toml (ndk: the android-ndk context's)
    ARG ANDROID_PLATFORM=android-36
    ARG ANDROID_BUILD_TOOLS=36.0.0
    ARG ANDROID_CMAKE=3.22.1

    ENV ANDROID_HOME=/opt/android-sdk
    ENV ANDROID_SDK_ROOT=${ANDROID_HOME}
    ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

    RUN apt-get update && \
        apt-get install -y --no-install-recommends openjdk-17-jdk-headless && \
        rm -rf /var/lib/apt/lists/*

    RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
        curl -fsSL -o /tmp/cmdline-tools.zip \
            https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip && \
        unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
        mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
        rm /tmp/cmdline-tools.zip

    # Gradle finds the NDK by its version in the SDK: the context's NDK is linked there
    RUN yes | sdkmanager --licenses > /dev/null && \
        sdkmanager --install \
            "platform-tools" \
            "platforms;${ANDROID_PLATFORM}" \
            "build-tools;${ANDROID_BUILD_TOOLS}" \
            "cmake;${ANDROID_CMAKE}" > /dev/null && \
        mkdir -p ${ANDROID_HOME}/ndk && \
        ln -s ${ANDROID_NDK_ROOT} ${ANDROID_HOME}/ndk/${ANDROID_NDK_VERSION} && \
        chmod -R a+rX ${ANDROID_HOME}  # builds may run as a non-root user

    ENV ANDROID_NDK_HOME=${ANDROID_NDK_ROOT}

    WORKDIR /src

# Gradle's outputs (build directories, CMake's .cxx, the project's .gradle) are deleted in
# the step that made them, once its results are in /out: they would stay in its layer,
# several GB each time the sources change and never used again (Gradle's own caches are
# cache mounts)

#### APP ####
FROM android-toolchain AS apk-build

    ARG GRADLE_TASK=:appmanager:assembleDebug

    COPY documents-app-android /src/documents-app-android

    # ~/.android holds the debug keystore; keeping it gives all debug APKs the same signature
    RUN --mount=type=cache,target=/root/.gradle \
        --mount=type=cache,target=/root/.android \
        cd /src/documents-app-android/app_manager && \
        ./gradlew --no-daemon ${GRADLE_TASK} && \
        mkdir -p /out && \
        find appmanager/build/outputs/apk -name '*.apk' -exec cp {} /out/ \; && \
        { find /src/documents-app-android /src/editors-android /src/core/X2tConverter/build/Android/libx2t \
            -type d \( -name build -o -name .cxx -o -name .gradle \) -prune -exec rm -rf {} + 2>/dev/null || true; }

FROM scratch AS apk
    COPY --from=apk-build /out/ /

#### APP WITH THE EDITORS ####
# The monorepo's layout: settings.gradle.kts includes ../../editors-android, libeditors
# reads build/out (core-android, sdkjs, dictionaries)
FROM android-toolchain AS editors-sources

    COPY documents-app-android /src/documents-app-android
    COPY editors-android /src/editors-android
    COPY editors-engine /src/editors-engine
    COPY core /src/core
    COPY core-fonts /src/core-fonts
    COPY document-templates/new/en-US/new.docx /src/document-templates/new/en-US/new.docx
    COPY --from=core-android-arm64 / /src/build/out/core-android/arm64-v8a
    COPY --from=core-android-x64 / /src/build/out/core-android/x86_64
    COPY --from=sdkjs-native / /src/build/out/sdkjs
    COPY --from=dictionaries / /src/build/out/dictionaries

FROM editors-sources AS apk-editors-build

    ARG GRADLE_TASK=:appmanager:assembleDebug

    RUN --mount=type=cache,target=/root/.gradle \
        --mount=type=cache,target=/root/.android \
        cd /src/documents-app-android/app_manager && \
        ./gradlew --no-daemon -PwithNativeCore=true ${GRADLE_TASK} && \
        mkdir -p /out && \
        find appmanager/build/outputs/apk -name '*.apk' -exec cp {} /out/ \; && \
        { find /src/documents-app-android /src/editors-android /src/core/X2tConverter/build/Android/libx2t \
            -type d \( -name build -o -name .cxx -o -name .gradle \) -prune -exec rm -rf {} + 2>/dev/null || true; }

FROM scratch AS apk-editors
    COPY --from=apk-editors-build /out/ /

#### UNIT TESTS AND LINT OF THE EDITORS ####
FROM editors-sources AS editors-unit-tests-run

    RUN --mount=type=cache,target=/root/.gradle \
        cd /src/documents-app-android/app_manager && \
        mkdir -p /out && \
        { ./gradlew --no-daemon -PwithNativeCore=true --continue \
            :libeditors:testDebugUnitTest :libgeditors:testDebugUnitTest \
            :libeditors:lintDebug :libgeditors:lintDebug > /out/gradle.log 2>&1; \
          echo $? > /out/result; } && \
        for module in libeditors libgeditors; do \
            cp -r /src/editors-android/$module/build/test-results /out/$module-results 2>/dev/null; \
            cp -r /src/editors-android/$module/build/reports /out/$module-reports 2>/dev/null; \
        done; \
        find /src/documents-app-android /src/editors-android /src/core/X2tConverter/build/Android/libx2t \
            -type d \( -name build -o -name .cxx -o -name .gradle \) -prune -exec rm -rf {} + 2>/dev/null; true

FROM scratch AS editors-unit-tests
    COPY --from=editors-unit-tests-run /out/ /
