package app.editors.manager.mvp.presenters.login

import app.editors.manager.app.App
import app.editors.manager.mvp.presenters.base.BasePresenter
import app.editors.manager.mvp.views.login.CloudStartView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState

@InjectViewState
class CloudStartPresenter : BasePresenter<CloudStartView>() {

    init {
        App.getApp().appComponent.inject(this)
    }

    fun checkHasAccounts() {
        CoroutineScope(Dispatchers.Default).launch {
            val accounts = cloudDataSource.getAccounts()
            withContext(Dispatchers.Main) {
                viewState.showAccountsButton(accounts.isNotEmpty())
            }
        }
    }

    fun getAccounts() {
        CoroutineScope(Dispatchers.Default).launch {
            val accounts = cloudDataSource.getAccounts()
            withContext(Dispatchers.Main) {
                viewState.checkAccounts(accounts.isEmpty())
            }
        }
    }

}