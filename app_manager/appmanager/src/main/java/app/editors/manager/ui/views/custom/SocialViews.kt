package app.editors.manager.ui.views.custom

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import app.documents.core.network.common.contracts.ApiContract
import app.editors.manager.BuildConfig
import app.editors.manager.databinding.IncludeSocialNetworksLayoutBinding
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.FacebookSdkNotInitializedException
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton

class SocialViews(
    private val activity: FragmentActivity, view: View?,
    private val facebookId: String?
) {

    interface OnSocialNetworkCallbacks {
        fun onTwitterSuccess(token: String)
        fun onTwitterFailed()
        fun onFacebookSuccess(token: String)
        fun onFacebookLogin(message: String)
        fun onFacebookCancel()
        fun onFacebookFailed()
        fun onSocialClick(social: String)
    }

    private var onSocialNetworkCallbacks: OnSocialNetworkCallbacks? = null
    private var facebookCallbackManager: CallbackManager? = null
    private var loginPersonalSocialFacebookNativeButton: LoginButton? = null
    private var viewBinding: IncludeSocialNetworksLayoutBinding? = null

    init {
        viewBinding = view?.let { IncludeSocialNetworksLayoutBinding.bind(it) }
        initListeners()
        initFacebook()
    }

    fun setProviders(providers: List<String>?) {
        if (providers == null) return
        viewBinding?.let {
            it.loginSocialGoogleButton.isVisible = false
            it.loginSocialFacebookButton.isVisible = providers.contains(ApiContract.Social.FACEBOOK)
            it.loginSocialAppleidButton.isVisible = false
            it.loginSocialZoomButton.isVisible = providers.contains(ApiContract.Social.ZOOM)
            it.loginSocialLinkedinButton.isVisible = providers.contains(ApiContract.Social.LINKEDIN)
            it.loginSocialTwitterButton.isVisible = false
        }
    }

    /*
    * Facebook initWithPreferences
    * */
    private fun initFacebook() {
        Log.d(TAG, "initFacebook() - app ID: " + BuildConfig.FACEBOOK_APP_ID)
        if (FacebookSdk.isInitialized()) {
            facebookId?.let { FacebookSdk.setApplicationId(it) }
            loginPersonalSocialFacebookNativeButton = LoginButton(activity)
            loginPersonalSocialFacebookNativeButton?.loginBehavior = LoginBehavior.WEB_ONLY
            facebookCallbackManager = CallbackManager.Factory.create()
            LoginManager.getInstance().registerCallback(facebookCallbackManager, FacebookAuthCallback())
        } else {
            viewBinding?.loginSocialFacebookButton?.isVisible = false
        }

    }

    private fun initListeners() {
        viewBinding?.let {
            it.loginSocialFacebookButton.setOnClickListener { onFacebookClick() }
            it.loginSocialAppleidButton.setOnClickListener {
                onSocialNetworkCallbacks?.onSocialClick(ApiContract.Social.APPLE_ID)
            }
            it.loginSocialZoomButton.setOnClickListener {
                onSocialNetworkCallbacks?.onSocialClick(ApiContract.Social.ZOOM)
            }
            it.loginSocialLinkedinButton.setOnClickListener {
                onSocialNetworkCallbacks?.onSocialClick(ApiContract.Social.LINKEDIN)
            }
        }
    }

    /*
    * Actions
    * */
    /*
    * Facebook click. Get previous token or get new with button click.
    * */
    private fun onFacebookClick() {
        try {
            val accessToken: AccessToken? = AccessToken.getCurrentAccessToken()
            onSocialNetworkCallbacks?.let { callbacks ->
                accessToken?.let { token ->
                    callbacks.onFacebookLogin(token.userId)
                } ?: run {
                    loginPersonalSocialFacebookNativeButton?.performClick()
                }
            }
        } catch (error: FacebookSdkNotInitializedException) {
            Log.e(TAG, "onFacebookClick: ", error)
            onSocialNetworkCallbacks?.onFacebookFailed()
        }
    }

    fun onFacebookContinue() {
        onSocialNetworkCallbacks?.onFacebookSuccess(AccessToken.getCurrentAccessToken()?.token ?: "")
    }

    fun onFacebookLogout() {
        LoginManager.getInstance().logOut()
        loginPersonalSocialFacebookNativeButton?.performClick()
    }

    /*
    * Lifecycle methods
    * */
    fun onDestroyView() {
        if (FacebookSdk.isInitialized()) {
            LoginManager.getInstance().unregisterCallback(facebookCallbackManager)
        }
        setOnSocialNetworkCallbacks(null)
        viewBinding = null
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        facebookCallbackManager?.onActivityResult(requestCode, resultCode, data)
    }

    /*
    * Getters/Setters
    * */
    fun setOnSocialNetworkCallbacks(onSocialNetworkCallbacks: OnSocialNetworkCallbacks?) {
        this.onSocialNetworkCallbacks = onSocialNetworkCallbacks
    }

    /*
    * Facebook callback
    * */
    private inner class FacebookAuthCallback : FacebookCallback<LoginResult> {
        override fun onSuccess(result: LoginResult) {
            onSocialNetworkCallbacks?.onFacebookSuccess(result.accessToken.token)
        }

        override fun onCancel() {
            onSocialNetworkCallbacks?.onFacebookCancel()
        }

        override fun onError(error: FacebookException) {
            LoginManager.getInstance().logOut()
            onSocialNetworkCallbacks?.onFacebookFailed()
        }
    }

    companion object {
        val TAG = SocialViews::class.java.simpleName
    }

}