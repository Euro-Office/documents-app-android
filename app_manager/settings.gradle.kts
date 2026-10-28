rootProject.name = "app_manager"

val withEditors: String? by settings  // parameter -PincludeEditors=true/false

fun shouldIncludeEditors(): Boolean? {
    return withEditors?.toBoolean() ?: true
}

include(
    ":appmanager",
    ":core",
    ":core:network",
    ":libcompose",
    ":libtoolkit",
    ":core:model",
    ":core:database",
    ":libshared"
)

//  Check the existence of editor modules before connecting
fun includeEditorModule(path: String, projectPath: String) {
    val projectDir = File(settingsDir, path)
    if (projectDir.exists()) {
        include(projectPath)
        project(projectPath).projectDir = projectDir
    } else {
        println("⚠️ The $projectPath editor module is missing (path: ${projectDir.absolutePath})")
    }
}

// Native core libraries for Android (built by AndroidEditors/build); libx2t only links once they exist
val withNativeCore: String? by settings  // parameter -PwithNativeCore=true

if (shouldIncludeEditors() == true) {
    if (withNativeCore?.toBoolean() == true) {
        includeEditorModule("../../core/X2tConverter/build/Android/libx2t", ":libx2t")
    }
    includeEditorModule("../../editors-android/libeditors", ":libeditors")
    includeEditorModule("../../editors-android/libcells", ":libcells")
    includeEditorModule("../../editors-android/libdocs", ":libdocs")
    includeEditorModule("../../editors-android/libslides", ":libslides")
    includeEditorModule("../../editors-android/libgeditors", ":libgeditors")
    includeEditorModule("../../editors-android/libresources", ":libresources")
    includeEditorModule("../../editors-android/libgcells", ":libgcells")
    includeEditorModule("../../editors-android/libgdocs", ":libgdocs")
    includeEditorModule("../../editors-android/libgslides", ":libgslides")
    includeEditorModule("../libsnapshot", ":libsnapshot")
}

project(":libtoolkit").projectDir = File(settingsDir, "../toolkit/libtoolkit")