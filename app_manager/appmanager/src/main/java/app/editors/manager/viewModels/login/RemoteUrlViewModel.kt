package app.editors.manager.viewModels.login

import android.text.Spanned
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import app.editors.manager.R
import app.editors.manager.app.App
import app.editors.manager.managers.utils.AppConfig
import lib.toolkit.base.managers.tools.ResourcesProvider

class RemoteUrlViewModel: ViewModel() {
    private val resourcesProvider: ResourcesProvider
        get() = App.getApp().appComponent.resourcesProvider

    val remoteUrls: LiveData<Spanned?> = MutableLiveData(
        AppConfig.servicesUrls(resourcesProvider.context).let { urls ->
            resourcesProvider.getHtmlSpanned(R.string.sign_in_terms_and_policy_text, urls[1], urls[0])
        }
    )

}