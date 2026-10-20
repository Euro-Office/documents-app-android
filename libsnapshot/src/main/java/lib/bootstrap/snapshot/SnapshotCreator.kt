package lib.bootstrap.snapshot

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import lib.toolkit.base.managers.utils.FileUtils

class SnapshotCreator {
    companion object {
        // The native library ships with the editors; without it every call is a no-op
        private val isLibraryLoaded: Boolean = try {
            Log.d("log", "loading library bootstrap_jni...")
            System.loadLibrary("bootstrap_jni")
            true
        } catch (error: UnsatisfiedLinkError) {
            Log.w("log", "bootstrap_jni is not available, snapshots are disabled", error)
            false
        }

        private val assetsPaths: List<String> by lazy {
            arrayListOf(
                "documents",
                "presentations",
                "spreadsheets",
                "fonts",
                "themes",
                "icu",
                "dictionaries",
                "sdk.version"
            )
        }

        @SuppressLint("MissingPermission")
        @JvmStatic
        fun unpackAssets(context: Context): String {
            if (!isLibraryLoaded) return ""
            return FileUtils.assetUnpack(context, assetsPaths, isExternal = false) ?: ""
        }

        @JvmStatic
        fun start(snapshotsPath: String, assetsPath: String) {
            if (isLibraryLoaded) nativeStart(snapshotsPath, assetsPath)
        }

        @JvmStatic
        fun stop() {
            if (isLibraryLoaded) nativeStop()
        }

        @JvmStatic
        private external fun nativeStart(snapshotsPath: String, assetsPath: String)

        @JvmStatic
        private external fun nativeStop()
    }
}
