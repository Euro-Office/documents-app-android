package app.documents.core.utils

/** Whether files of a portal are co-edited with its editors (their version matches the app's editors). */
interface CoauthoringChecker {

    suspend fun isCoauthoring(): Boolean

    suspend fun checkCoauthoring(sdkVersion: String?): Boolean
}