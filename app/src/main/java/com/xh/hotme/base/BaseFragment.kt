package com.xh.hotme.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.xh.hotme.R
import com.xh.hotme.bluetooth.listener.IBleScanRequestListener
import com.xh.hotme.utils.AppTrace
import com.xh.hotme.utils.ClickGuard
import com.xh.hotme.utils.Constants
import com.xh.hotme.utils.PermissionsUtil
import com.xh.hotme.utils.ToastUtil
import com.xh.hotme.widget.ModalDialog
import org.greenrobot.eventbus.EventBus
import pub.devrel.easypermissions.EasyPermissions
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

@Keep
abstract class BaseFragment<T : ViewBinding?> : Fragment() {
    val TAG: String = javaClass.simpleName

    @JvmField
    var bleScanRequestListener: IBleScanRequestListener? = null

    // Don't judge the state of the parent fragment,
    // because if the parent fragment visible state changes,
    // you must take the initiative to change the state of the sub fragment
    var isFragmentVisible: Boolean = false
        private set

    @JvmField
    protected var _handler: Handler? = null

    @JvmField
    protected var _viewBinding: T? = null

    @JvmField
    protected var _rootView: View? = null

    private var _disconnectNetworkDialog: ModalDialog? = null

    protected open val switchType: SwitchType?
        get() = SwitchType.SHOW_HIDE

    protected val dialogStack: DialogStack?
        get() {
            if (activity is BaseActivity<*>) {
                return (activity as BaseActivity<*>?)!!.dialogStack
            }
            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _handler = Handler()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AppTrace.d("BaseFragment onCreateView " + TAG + " , viewBinding is null ? " + (_viewBinding == null))
        if (_viewBinding == null) {
            initViewBinding(inflater, container)
            if (_viewBinding != null) {
                _rootView = _viewBinding!!.getRoot()
                initView()
            } else {
                _rootView = getErrLayout(inflater, container)
            }
        }
        return _rootView
    }

    override fun onStart() {
        super.onStart()
        if (_viewBinding != null) {
            initData()
        }
    }

    protected abstract fun initView()

    protected abstract fun initData()

    private fun getErrLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.activity_inititial_err, container, false)
    }

    protected open fun initViewBinding(inflater: LayoutInflater?, container: ViewGroup?) {
        val type = (javaClass.genericSuperclass as ParameterizedType)
        val cls = type.actualTypeArguments[0] as Class<*>
        try {
            val inflate = cls.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.javaPrimitiveType
            )
            _viewBinding = inflate.invoke(null, inflater, container, false) as T
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }

    protected fun <K : View?> findViewById(@IdRes id: Int): K {
        return _rootView!!.findViewById(id)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _handler!!.removeCallbacksAndMessages(null)
        _handler = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (javaClass.isAnnotationPresent(IEventBus::class.java)) {
            try {
                EventBus.getDefault().register(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (javaClass.isAnnotationPresent(IEventBus::class.java)) {
            try {
                EventBus.getDefault().unregister(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        when (switchType) {
            SwitchType.ATTACH_DETACH -> notifyOnFragmentVisible()
            SwitchType.SHOW_HIDE -> if (!isHidden) {
                notifyOnFragmentVisible()
            }
            else ->
                //If the parent fragment exist and hidden when activity destroy,
                //when the activity restore, The parent Fragment  will be restore to hidden state.
                //And the sub Fragment which in ViewPager is also be restored, and the onResumed() method will callback.
                //And The sub Fragment's getUserVisibleHint() method will return true  if it is in active position.
                //So we need to judge the parent Fragment visible state.
                if (userVisibleHint && isParentFragmentVisible) {
                    notifyOnFragmentVisible()
                }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        when (switchType) {
            SwitchType.SHOW_HIDE -> if (hidden) {
                notifyOnFragmentInvisible()
            } else {
                //Just judge for safe
                if (isResumed) {
                    notifyOnFragmentVisible()
                }
            }

            else -> {}
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        when (switchType) {
            SwitchType.VIEW_PAGER -> if (isVisibleToUser) {
                //If ViewPager in ViewPager , the sub ViewPager will call setUserVisibleHint before onResume
                if (isResumed) {
                    notifyOnFragmentVisible()
                }
            } else {
                notifyOnFragmentInvisible()
            }

            else -> {}
        }
    }

    override fun onPause() {
        super.onPause()
        notifyOnFragmentInvisible()
    }

    private val isParentFragmentVisible: Boolean
        get() {
            val parent = parentFragment ?: return true
            return if (parent is BaseFragment<*>) {
                parent.isFragmentVisible
            } else {
                //TODO May be can't get the correct visible state if parent Fragment is not SmartFragment
                parent.isVisible
            }
        }

    private fun notifyOnFragmentVisible() {
        if (!isFragmentVisible) {
            notifyVisibleChanged(true)
            isFragmentVisible = true
        }
    }

    private fun notifyOnFragmentInvisible() {
        if (isFragmentVisible) {
            notifyVisibleChanged(false)
            isFragmentVisible = false
        }
    }

    private fun notifyVisibleChanged(isVisible: Boolean) {
        // check permission
        if (isVisible) {
            if (javaClass.isAnnotationPresent(RequireStoragePermission::class.java)) {
                if (activity is BaseActivity<*>) {
                    val perm = javaClass.getAnnotation(
                        RequireStoragePermission::class.java
                    )
                    (activity as BaseActivity<*>?)!!.listenStoragePermissionGranted(
                        this
                    )
                    (activity as BaseActivity<*>?)!!.checkStoragePermission(perm != null && perm.mandatory)
                }
            }
        }

        // to subclass
        onVisibleChanged(isVisible)
    }

    protected open fun onVisibleChanged(isVisible: Boolean) {
    }

    open fun onStoragePermissionGranted() {
    }

    public fun requestBluetoothPermission() {
        activity?.let {
            EasyPermissions.requestPermissions(
                it,
                "热我APP需要定位和蓝牙设备权限, 否则无法搜索附近设备",
                PermissionsUtil.REQUEST_PERMISSION_BLUE,
                *PermissionsUtil.getBlePermissions()
            )
        }
    }

    @JvmOverloads
    fun showConfirmDialog(
        title: String?,
        msg: String?,
        positiveBtnText: String? = null,
        negativeBtnText: String? = null,
        positiveCb: ClickGuard.GuardedOnClickListener? = null,
        negativeCb: ClickGuard.GuardedOnClickListener? = null
    ) {
        val d = ModalDialog(context)
        d.setTitle(title)
        d.setMessage(msg)
        d.setNegativeButton(negativeBtnText ?: getString(R.string.cancel), negativeCb)
        d.setPositiveButton(positiveBtnText ?: getString(R.string.ok), positiveCb)
        d.show()
    }

    fun showDisconnectToast(){
        if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            ToastUtil.s(getString(R.string.info_bluetooth_offline))
        }else{
            ToastUtil.s(getString(R.string.info_network_disconnect))
        }
    }


    fun showNetworkDisconnectDialog() {
        showNetworkDisconnectDialog(getString(R.string.phone_no_network_title), getString(R.string.phone_no_network_message))
    }

    fun showNetworkDisconnectDialog(
        title: String?,
        msg: String?) {
        if (_disconnectNetworkDialog == null) {
            _disconnectNetworkDialog = ModalDialog(activity)
            _disconnectNetworkDialog!!.setTitle(title)
            _disconnectNetworkDialog!!.setMessage(msg)
            _disconnectNetworkDialog!!.setNegativeButton(
                getString(R.string.cancel),
                object : ClickGuard.GuardedOnClickListener() {
                    override fun onClicked(): Boolean {
                        _disconnectNetworkDialog!!.dismiss()
                        return true
                    }
                })
            _disconnectNetworkDialog!!.setPositiveButton(
                getString(R.string.setting),
                object : ClickGuard.GuardedOnClickListener() {
                    override fun onClicked(): Boolean {
                        _disconnectNetworkDialog!!.dismiss()

                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                        return true
                    }
                })
            _disconnectNetworkDialog!!.show()
        } else {
            if (!_disconnectNetworkDialog!!.isShowing) {
                _disconnectNetworkDialog!!.show()
            }
        }
    }


    fun showDeviceOfflineDialog() {
        if (_disconnectNetworkDialog == null) {
            _disconnectNetworkDialog = ModalDialog(activity)
            _disconnectNetworkDialog!!.setTitle(getString(R.string.device_offline_remote))
            _disconnectNetworkDialog!!.setMessage(getString(R.string.remote_model_offline_message))
            _disconnectNetworkDialog!!.setNegativeButton(
                getString(R.string.cancel),
                object : ClickGuard.GuardedOnClickListener() {
                    override fun onClicked(): Boolean {
                        _disconnectNetworkDialog!!.dismiss()
                        return true
                    }
                })
            _disconnectNetworkDialog!!.setPositiveButton(
                getString(R.string.setting),
                object : ClickGuard.GuardedOnClickListener() {
                    override fun onClicked(): Boolean {
                        _disconnectNetworkDialog!!.dismiss()

                        return true
                    }
                })
            _disconnectNetworkDialog!!.show()
        } else {
            if (!_disconnectNetworkDialog!!.isShowing) {
                _disconnectNetworkDialog!!.show()
            }
        }
    }
    fun dismissDeviceOfflineDialog() {
        if (_disconnectNetworkDialog != null && _disconnectNetworkDialog!!.isShowing) {
            _disconnectNetworkDialog!!.dismiss()
        }
    }
}
