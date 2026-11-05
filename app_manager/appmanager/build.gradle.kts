@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileInputStream
import java.io.FileWriter
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlinx-serialization")
    kotlin("android")
    kotlin("kapt")
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.compose)
//    id("org.owasp.dependencycheck")
}

val withEditors: Boolean = project.findProperty("withEditors")?.toString()?.toBoolean() ?: true
val isBuildingBundle = gradle.startParameter.taskNames.any { it.lowercase().contains("bundle") }

val appId = "eu.eurooffice.documents"
// Names the APK files
val appName = "euro-office-documents"

// The same app as Nextcloud Office, the product flavor nextcloud. Its own application ID lets both
// be installed next to each other
val nextcloudAppId = "com.nextcloud.office"
val nextcloudAppName = "nextcloud-office"
val nextcloudProductName = "Nextcloud Office"

/*
* Create a variable called keystorePropertiesFile, and initialize it to your
* keystore.properties file, in the rootProject folder.
* Example of file content:
*   storePassword=password
*   keyPassword=password
*   keyAlias=AliasInKeyStore
*   storeFile=C:/example/MyAndroidKeys.jks
*/

fun getKeystore(filePath: String): Properties {
    // Initialize a new Properties() object called keystoreProperties.
    val keystoreProperties = Properties()

    // You can place here passwords and path to keystore instead of file properties
    keystoreProperties["keyAlias"] = "<YOUR_ALIAS>"
    keystoreProperties["keyPassword"] = "<YOUR_PASSWORD>"
    keystoreProperties["storeFile"] = "<PATH_TO_KEYSTORE_FILE>"
    keystoreProperties["storePassword"] = "<KEYSTORE_PASSWORD>"

    // Get file with properties
    val keystorePropertiesFile = rootProject.file(filePath)
    // File check to exist for success script building
    if (keystorePropertiesFile.exists()) {
        // Load your keystore.properties file into the keystoreProperties object.
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    } else {
        val writer = FileWriter(keystorePropertiesFile, false)
        keystoreProperties.store(writer, "Google keystore file")
    }

    return keystoreProperties
}

// The resources of the nextcloud flavor, made at build time from the Euro-Office resources of all
// modules: the Euro-Office name (not in URLs) and the application ID replaced, in all languages,
// so new strings and translations need no copy here. The launcher's name becomes the product's
abstract class BrandResources : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resources: ConfigurableFileCollection

    @get:Input
    abstract val productName: Property<String>

    @get:Input
    abstract val applicationIds: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val output = outputDir.get().asFile.apply { deleteRecursively() }
        val (euroOfficeId, brandId) = applicationIds.get().entries.single().toPair()
        val rebrand = { text: String ->
            text.replace(Regex("(?<!/)Euro-Office"), productName.get()).replace(euroOfficeId, brandId)
        }
        val comment = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
        val element = Regex("""<(string|plurals|string-array)\b[^>]*[^/]>.*?</\1>""", RegexOption.DOT_MATCHES_ALL)
        val name = Regex("""name="([^"]+)"""")
        // As in the app, the first module's string of a name wins: the app's own come first
        val seen = mutableSetOf<String>()
        val strings = sortedMapOf<String, MutableList<String>>()

        resources.files.forEach { res ->
            res.listFiles().orEmpty().filter { it.name.startsWith("values") }.forEach { values ->
                values.listFiles().orEmpty().filter { it.extension == "xml" }.forEach { file ->
                    element.findAll(file.readText().replace(comment, "")).forEach { match ->
                        val stringName = name.find(match.value)!!.groupValues[1]
                        val branded = if (stringName == "app_name") {
                            match.value.replace(Regex(">.*<"), ">${productName.get()}<")
                        } else {
                            rebrand(match.value)
                        }
                        if (seen.add("${values.name}/$stringName") && branded != match.value) {
                            strings.getOrPut(values.name) { mutableListOf() } += branded
                        }
                    }
                }
            }
            res.resolve("xml").listFiles().orEmpty().filter { euroOfficeId in it.readText() }.forEach { file ->
                output.resolve("xml/${file.name}").apply { parentFile.mkdirs() }.writeText(rebrand(file.readText()))
            }
        }

        strings.forEach { (values, elements) ->
            output.resolve("$values/brand.xml").apply { parentFile.mkdirs() }.writeText(
                elements.joinToString(
                    separator = "\n",
                    prefix = "<resources xmlns:tools=\"http://schemas.android.com/tools\" " +
                        "xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n",
                    postfix = "\n</resources>\n"
                )
            )
        }
    }
}

val nextcloudResources = tasks.register<BrandResources>("generateNextcloudResources") {
    resources.from(
        (listOf(project) + (rootProject.subprojects - project)).flatMap { module ->
            module.file("src").listFiles().orEmpty().map { it.resolve("res") }.filter { it.isDirectory }
        }
    )
    productName.set(nextcloudProductName)
    applicationIds.put(appId, nextcloudAppId)
    outputDir.set(layout.buildDirectory.dir("generated/brand/res"))
}

androidComponents {
    onVariants(selector().withFlavor("brand" to "nextcloud")) { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(nextcloudResources, BrandResources::outputDir)
    }
}

android {

    namespace = "app.editors.manager"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        manifestPlaceholders += mapOf()
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        // Releases pass the version: -PappVersionName=0.1.0-alpha.1 -PappVersionCode=10001
        versionCode = providers.gradleProperty("appVersionCode").orNull?.toInt() ?: 707
        versionName = providers.gradleProperty("appVersionName").orNull ?: "9.3.1"
        multiDexEnabled = true
        applicationId = "eu.eurooffice.documents"

        vectorDrawables {
            useSupportLibrary = true
        }

        val keystoreProperties = getKeystore("eurooffice-keystore.properties")

        manifestPlaceholders["facebookId"] = keystoreProperties["FACEBOOK_APP_ID"] as String? ?: ""
        manifestPlaceholders["dropboxKey"] = keystoreProperties["DROP_BOX_COM_CLIENT_ID"] as? String ?: ""

        buildConfigField("boolean", "IS_BETA", "false")
        buildConfigField("String", "RELEASE_ID", "\"" + appId + "\"")
        buildConfigField("String", "APP_NAME", "\"" + appName + "\"")
        buildConfigField("String", "COMMUNITY_ID", "\"" + keystoreProperties["COMMUNITY_ID"] + "\"")

        //Twitter
        buildConfigField(
            "String",
            "TWITTER_CONSUMER_SECRET",
            "\"" + keystoreProperties["TWITTER_CONSUMER_SECRET"] + "\""
        )
        buildConfigField("String", "TWITTER_CONSUMER_KEY", "\"" + keystoreProperties["TWITTER_CONSUMER_KEY"] + "\"")

        //Captcha
        buildConfigField(
            "String",
            "CAPTCHA_PUBLIC_KEY_INFO",
            "\"" + keystoreProperties["CAPTCHA_PUBLIC_KEY_INFO"] + "\""
        )
        buildConfigField("String", "CAPTCHA_PUBLIC_KEY_COM", "\"" + keystoreProperties["CAPTCHA_PUBLIC_KEY_COM"] + "\"")

        //Facebook
        buildConfigField("String", "FACEBOOK_APP_ID_INFO", "\"" + keystoreProperties["FACEBOOK_APP_ID_INFO"] + "\"")
        buildConfigField("String", "FACEBOOK_APP_ID", "\"" + keystoreProperties["FACEBOOK_APP_ID"] + "\"")

        //Tasks
        manifestPlaceholders["tasks"] = keystoreProperties["CUSTOM_TASKS"] as String? ?: ""

        buildConfigField("String", "PUSH_SCHEME", "\"" + "oodocuments" + "\"")
    }

    flavorDimensions += "brand"

    productFlavors {
        create("eurooffice") {
            dimension = "brand"
            isDefault = true
        }
        create("nextcloud") {
            dimension = "brand"
            applicationId = nextcloudAppId
            buildConfigField("String", "RELEASE_ID", "\"" + nextcloudAppId + "\"")
            buildConfigField("String", "APP_NAME", "\"" + nextcloudAppName + "\"")
        }
    }

    splits {
        abi {
            isEnable = !isBuildingBundle
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64") //comment to armv7
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("eurooffice") {
            // The release key: from the environment (CI: EO_KEYSTORE_FILE, EO_KEYSTORE_PASSWORD,
            // EO_KEY_ALIAS, EO_KEY_PASSWORD), else from eurooffice-keystore.properties
            val keystoreFile = System.getenv("EO_KEYSTORE_FILE")
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("EO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("EO_KEY_ALIAS")
                keyPassword = System.getenv("EO_KEY_PASSWORD")
            } else {
                val keystoreProperties = getKeystore("eurooffice-keystore.properties")
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] ?: "")
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }
    buildTypes {

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

            signingConfig = signingConfigs.getByName("eurooffice")
            ndk {
                abiFilters.addAll(arrayOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
            }
        }

        debug {
            isMinifyEnabled = false
            ndk {
                if (System.getProperty("os.arch") == "aarch64") {
                    abiFilters.add("arm64-v8a")
                } else {
                    abiFilters.addAll(arrayOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
                }
            }
        }

        applicationVariants.all {
            val variant = this
            variant.outputs
                .map { it as BaseVariantOutputImpl }
                .forEach { output ->
                    // The same name for each build of a variant, so a new build replaces the last one
                    val buildAbi = output.filters.find { it.filterType == "ABI" }?.identifier ?: "universal"
                    val buildType = if (buildType.isDebuggable) "debug" else "release"
                    val name = if (flavorName == "nextcloud") nextcloudAppName else appName
                    output.outputFileName = "${name}-${versionName}-${buildAbi}-${buildType}.apk"
                }
        }

    }

    tasks.preBuild {
        doFirst {
            delete(fileTree(mapOf("dir" to "build", "include" to listOf("**/*.apk"))))
        }
    }


    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    bundle {
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
        language {
            enableSplit = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        packaging {
            jniLibs {
                pickFirsts.add("lib/armeabi-v7a/libc++_shared.so")
                pickFirsts.add("lib/x86/libc++_shared.so")
                pickFirsts.add("lib/arm64-v8a/libc++_shared.so")
                pickFirsts.add("lib/x86_64/libc++_shared.so")
            }
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":core"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":libcompose"))
    implementation(project(":libtoolkit"))
    implementation(project(":libshared"))
    // Dynamic connection of editors
    if (withEditors) {
        val editorModules = listOf(
            ":libx2t",
            ":libeditors",
            ":libcells",
            ":libdocs",
            ":libslides",
            ":libgeditors",
            ":libgcells",
            ":libgdocs",
            ":libgslides",
            ":libsnapshot"
        )

        editorModules.forEach { modulePath ->
            try {
                implementation(project(modulePath))
                println("✅ The $modulePath editor module is enabled")
            } catch (e: UnknownProjectException) {
                println("⚠️ The $modulePath editor module is missing and will be skipped.")
            }
        }
    } else {
        println("ℹ️ Build mode without editors")
    }

    // Google libs
    implementation(libs.google.material)
    implementation(libs.google.gson)

    // Androidx
    implementation(libs.appcompat)
    implementation(libs.biometric)
    implementation(libs.fragmentKtx)

    // RecyclerView
    implementation(libs.recyclerView)
    implementation(libs.recyclerViewSelection)

    implementation(libs.cardView)
    implementation(libs.constraint)

    // Dagger
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.xml) { exclude(group="xpp3", module= "xpp3" )}
    implementation(libs.retrofit.rx)
    implementation(libs.retrofit.kotlin.serialization)

    // Moxy
    implementation(libs.moxy)
    implementation(libs.moxy.material)
    implementation(libs.moxy.ktx)
    kapt(libs.moxy.compiler)

    // Kotlin
    implementation(libs.kotlin.core)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.kotlin.serialization.json)

    // RX
    implementation(libs.rx.java)
    implementation(libs.rx.relay)

    // Room
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // Other
    implementation(libs.phoneNumber)
    implementation(libs.facebookLogin)
    implementation(libs.pageIndicator)
    implementation(libs.glide)
    implementation(libs.glideCompose)
    implementation(libs.photoView)
    implementation(libs.androidWorkManager)
    implementation(libs.androidCustomTabs)
    implementation(libs.hCaptcha)

    //TODO add to base module
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    //Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.navigation)
    implementation(libs.compose.livedata)
    implementation(libs.compose.constraint.layout)
    debugImplementation(libs.compose.uiTooling)

    //Jackson
    implementation(libs.jackson)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
}

//tasks.register("copySamples") {
//    println("Copy samples")
//
//    val documentSamplesPath = "../../../document-templates/sample"
//    val documentNewPath = "../../../document-templates/new"
//
//    val assetsSamplePath = projectDir.absolutePath + "/src/main/assets/samples"
//    val assetsNewPath = projectDir.absolutePath + "/src/main/assets/templates"
//
//    if (!File(assetsSamplePath).exists()) {
//        File(assetsSamplePath).mkdirs()
//    }
//
//    if (!File(assetsNewPath).exists()) {
//        File(assetsNewPath).mkdirs()
//    }
//
//    copy {
//        from(documentSamplesPath)
//        into(assetsSamplePath)
//    }
//
//    copy {
//        from(documentNewPath)
//        into(assetsNewPath)
//    }
//}

