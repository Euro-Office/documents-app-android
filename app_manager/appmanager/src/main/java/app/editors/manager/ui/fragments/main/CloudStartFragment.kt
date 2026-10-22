package app.editors.manager.ui.fragments.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import app.documents.core.model.cloud.WebdavProvider
import app.editors.manager.R
import app.editors.manager.databinding.FragmentCloudLayoutBinding
import app.editors.manager.managers.utils.AppConfig
import app.editors.manager.mvp.presenters.login.CloudStartPresenter
import app.editors.manager.mvp.views.login.CloudStartView
import app.editors.manager.ui.activities.login.SignInActivity
import app.editors.manager.ui.activities.login.WebDavLoginActivity
import app.editors.manager.ui.activities.main.CloudsActivity
import app.editors.manager.ui.activities.main.IMainActivity
import app.editors.manager.ui.fragments.base.BaseAppFragment
import moxy.presenter.InjectPresenter

class CloudStartFragment : BaseAppFragment(), CloudStartView {

    @InjectPresenter
    lateinit var presenter: CloudStartPresenter

    private var viewBinding: FragmentCloudLayoutBinding? = null
    private var mainActivity: IMainActivity? = null
    private var isAccounts = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IMainActivity) {
            mainActivity = context
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        viewBinding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentCloudLayoutBinding.inflate(layoutInflater, container, false)
        return viewBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkArguments()
        init()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.cloud_settings_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun checkArguments() {
        if (arguments?.containsKey(KEY_PROFILE) == true) {
            arguments?.getBoolean(KEY_PROFILE)?.let {
                isAccounts = it
                setHasOptionsMenu(isAccounts)
            }
        }
    }

    private fun init() {
        setActionBarTitle(getString(if (isAccounts)
            R.string.cloud_accounts_title else R.string.fragment_clouds_title))
        mainActivity?.showNavigationButton(false)
        mainActivity?.showActionButton(false)
        if (AppConfig.isNextcloudOnly) initNextcloud()
        initListeners()
    }

    private fun initNextcloud() {
        viewBinding?.let { binding ->
            binding.titleTextView.setText(R.string.storage_select_next_cloud)
            binding.titleTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_storage_nextcloud, 0, 0, 0
            )
            binding.descriptionTextView.setText(R.string.cloud_nextcloud_description)
            binding.startButton.setText(R.string.cloud_connect_nextcloud)
            binding.otherStorageButton.isVisible = false
        }
        presenter.checkHasAccounts()
    }

    private fun initListeners() {
        viewBinding?.let {
            it.startButton.setOnClickListener {
                if (AppConfig.isNextcloudOnly) {
                    WebDavLoginActivity.show(requireActivity(), WebdavProvider.NextCloud, null)
                } else {
                    SignInActivity.showPortalCreate(requireContext())
                }
            }
            it.otherStorageButton.setOnClickListener {
                presenter.getAccounts()
            }
        }
    }

    override fun checkAccounts(isEmpty: Boolean) {
        if (isEmpty) {
            CloudsActivity.show(requireContext())
        } else {
            mainActivity?.showAccountsActivity()
        }
    }

    override fun showAccountsButton(isVisible: Boolean) {
        viewBinding?.otherStorageButton?.isVisible = isVisible
    }

    override fun onError(message: String?) {}

    companion object {
        val TAG = CloudStartFragment::class.java.simpleName
        private const val KEY_PROFILE = "KEY_PROFILE"

        fun newInstance(isProfile: Boolean): CloudStartFragment =
            CloudStartFragment().apply {
                arguments = Bundle(1).apply {
                    putBoolean(KEY_PROFILE, isProfile)
                }
            }
    }
}