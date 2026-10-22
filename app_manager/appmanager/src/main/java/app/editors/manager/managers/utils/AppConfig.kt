package app.editors.manager.managers.utils

import android.content.Context
import app.editors.manager.R
import lib.toolkit.base.managers.utils.FileUtils

/**
 * Settings that came from Firebase Remote Config. The app uses no Firebase, so they have the
 * values that were its defaults.
 */
object AppConfig {

    /** Google Drive as a storage. */
    const val isGoogleDriveEnabled = false

    /**
     * Nextcloud is the only cloud the app offers. The other clouds (portals, ownCloud, kDrive,
     * OneDrive, Dropbox, Google Drive, WebDAV) stay in the code but are not shown.
     */
    const val isNextcloudOnly = true

    /** The terms of service and the privacy policy. */
    fun servicesUrls(context: Context): Array<String> =
        arrayOf(context.getString(R.string.app_url_terms), context.getString(R.string.app_url_policy))

    /**
     * Whether files of a portal are co-edited with its editors, version [webSdk]: when their major
     * and minor version are the app's editors' ones.
     */
    fun isCoauthoring(context: Context, webSdk: String): Boolean {
        val portal = webSdk.replace(".", "")
        val local = FileUtils.readSdkVersion(context).replace(".", "")
        if (portal.length < 2 || local.length < 2) return false
        return portal[0] == local[0] && portal[1] == local[1]
    }
}
