package app.editors.manager.di.module

import android.content.Context
import app.documents.core.model.cloud.CloudAccount
import app.documents.core.providers.DropboxFileProvider
import app.documents.core.providers.GoogleDriveFileProvider
import app.documents.core.providers.OneDriveFileProvider
import app.documents.core.utils.CoauthoringChecker
import app.editors.manager.managers.providers.DropboxStorageHelper
import app.editors.manager.managers.providers.GoogleDriveStorageHelper
import app.editors.manager.managers.providers.OneDriveStorageHelper
import app.editors.manager.managers.tools.FilterManager
import app.editors.manager.managers.utils.AppConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppModule {

    @Provides
    fun provideDropboxFileProvider(context: Context): DropboxFileProvider {
        return DropboxFileProvider(
            context = context,
            helper = DropboxStorageHelper()
        )
    }

    @Provides
    fun provideGoogleDriveFileProvider(context: Context): GoogleDriveFileProvider {
        return GoogleDriveFileProvider(
            context = context,
            helper = GoogleDriveStorageHelper()
        )
    }

    @Provides
    fun provideOneDriveFileProvider(context: Context): OneDriveFileProvider {
        return OneDriveFileProvider(
            context = context,
            helper = OneDriveStorageHelper()
        )
    }

    @Provides
    @Singleton
    fun provideCoauthoringChecker(
        context: Context,
        cloudAccount: CloudAccount?
    ): CoauthoringChecker {
        return object : CoauthoringChecker {

            private var _isCoauthoring: Boolean? = null

            override suspend fun isCoauthoring(): Boolean {
                _isCoauthoring?.let { enabled -> return enabled }

                return checkSdkVersion(cloudAccount?.portalSdkVersion)
                    .also { _isCoauthoring = it }
            }

            override suspend fun checkCoauthoring(sdkVersion: String?): Boolean {
                return checkSdkVersion(sdkVersion)
            }

            private fun checkSdkVersion(webSdkVersion: String?): Boolean {
                if (webSdkVersion == null) return false
                return AppConfig.isCoauthoring(context, webSdkVersion).also { _isCoauthoring = it }
            }
        }
    }

    @Provides
    @Singleton
    fun provideFilterManager(): FilterManager = FilterManager()
}