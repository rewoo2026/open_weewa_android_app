package com.xh.hotme.base

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.xh.hotme.R
import com.xh.hotme.bluetooth.BluetoothHandle
import com.xh.hotme.bluetooth.BluetoothManager
import com.xh.hotme.bluetooth.Device
import com.xh.hotme.bluetooth.bean.WeewaConnectedRequestBean
import com.xh.hotme.bluetooth.listener.IBleConnectListener
import com.xh.hotme.thrid.UMengManager
import com.xh.hotme.utils.AppTrace
import com.xh.hotme.utils.BaseAppUtil
import com.xh.hotme.utils.ClickGuard.GuardedOnClickListener
import com.xh.hotme.utils.Constants
import com.xh.hotme.utils.DialogUtil
import com.xh.hotme.utils.NetUtil
import com.xh.hotme.utils.PermissionsUtil
import com.xh.hotme.utils.StatusBarUtil
import com.xh.hotme.utils.ToastUtil
import com.xh.hotme.widget.BluetoothDialog
import com.xh.hotme.widget.LoadingDialog
import com.xh.hotme.widget.ModalDialog
import org.greenrobot.eventbus.EventBus
import pub.devrel.easypermissions.EasyPermissions
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

/**
 * Created by liu hong liang on 2017/4/27.
 */
@Keep
abstract class BaseActivity<T : ViewBinding?> : FragmentActivity() {
    @JvmField
    protected var viewBinding: T? = null

    @JvmField
    val dialogStack: DialogStack = DialogStack()

    @JvmField
    protected var isPortrait: Boolean = true

    // 当前activity是否可见
    protected var isActive: Boolean = false
        private set

    // BLEAware
    private var _connectNotifyListener: IBleConnectListener? = null
    @JvmField
    protected var _handler: Handler? = null
    @JvmField
    protected var _device: Device? = null
    private var _disconnectBTDialog: ModalDialog? = null
    private var _disconnectNetworkDialog: ModalDialog? = null

    private var _connectDevice: Device? = null

    private var _storagePermLauncher: ActivityResultLauncher<Intent>? = null
    private var _storagePermLegacyLauncher: ActivityResultLauncher<String>? = null
    private var _storagePermForwardFragment: WeakReference<BaseFragment<*>?>? = null

    // for other permission
    private var _pendingPerm: String? = null
    private var _singlePermLauncher: ActivityResultLauncher<String>? = null

    private val _loadingTimeoutCleanerRunnable = Runnable { dismissLoading() }

    private val _openBtLauncher: ActivityResultLauncher<Intent>

    init {
        _openBtLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { o: ActivityResult? ->
            onBluetoothMayBeOpened()
            if (javaClass.isAnnotationPresent(BLEAware::class.java)) {
                if (_device != null) {
                    if (BluetoothManager.getImpl().isConnected) {
                        BluetoothManager.getImpl().connectToBle(_device)
                    }
                } else {
                    AppTrace.d(TAG, "no device")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // detect orientation
        isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        // create handler
        _handler = Handler()

        // activity launcher must be created before started
        _singlePermLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
                if (granted) {
                    onOnePermissionGranted(_pendingPerm)
                } else {
                    onOnePermissionDenied(_pendingPerm)
                }
            }
        _storagePermLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { o: ActivityResult? ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        onStoragePermissionGranted()
                        if (_storagePermForwardFragment != null && _storagePermForwardFragment!!.get() != null) {
                            _storagePermForwardFragment!!.get()!!.onStoragePermissionGranted()
                        }
                    } else {
                        val perm = javaClass.getAnnotation(RequireStoragePermission::class.java)
                        if (perm != null && perm.mandatory) {
                            showMandatoryStoragePermissionDialog()
                        }
                    }
                    _storagePermForwardFragment = null
                }
            }
        _storagePermLegacyLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
                if (granted) {
                    onStoragePermissionGranted()
                    if (_storagePermForwardFragment != null && _storagePermForwardFragment!!.get() != null) {
                        _storagePermForwardFragment!!.get()!!.onStoragePermissionGranted()
                    }
                } else {
                    val perm = javaClass.getAnnotation(RequireStoragePermission::class.java)
                    if (perm != null && perm.mandatory) {
                        showMandatoryStoragePermissionDialog()
                    }
                }
                _storagePermForwardFragment = null
            }

        // init view
        val superclass = javaClass.genericSuperclass
        val aClass =
            (superclass as ParameterizedType).actualTypeArguments[0] as Class<*>
        try {
            val method = aClass.getDeclaredMethod("inflate", LayoutInflater::class.java)
            viewBinding = method.invoke(null, layoutInflater) as T
            if (viewBinding == null) {
                onViewInitErr()
            } else {
                setContentView(viewBinding!!.root)
                initView()
                initData()
            }
            if (javaClass.isAnnotationPresent(IEventBus::class.java)) {
                if (!EventBus.getDefault().isRegistered(this)) {
                    EventBus.getDefault().register(this)
                }
            }
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }

        installSystemBarInsets()

        // 如果有BLEAware标注, 则监听蓝牙事件
        if (javaClass.isAnnotationPresent(BLEAware::class.java)) {
            _device = BluetoothManager.getConnectDevice()
            installBleConnectionListener()
        }
    }

    protected fun onViewInitErr() {
        setContentView(R.layout.activity_inititial_err)
        findViewById<View>(R.id.button).setOnClickListener { v: View? -> finish() }
    }

    /**
     * 初始化布局View
     */
    protected abstract fun initView()

    /**
     * 初始化数据
     */
    protected abstract fun initData()

    @JvmOverloads
    protected fun requestOpenBluetooth(intent: Intent? = null) {
        if (!PermissionsUtil.isLocServiceEnable(this@BaseActivity)) {
            PermissionsUtil.openLocationService(this@BaseActivity)
            return
        }
        val hasPermission = PermissionsUtil.hasBlePermission(this@BaseActivity)
        if (!hasPermission) {
            EasyPermissions.requestPermissions(
                this@BaseActivity,
                getString(R.string.permission_title_location_and_device),
                PermissionsUtil.REQUEST_PERMISSION_BLUE,
                *PermissionsUtil.getBlePermissions()
            )
            return
        }

        _openBtLauncher.launch(intent ?: Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    protected open fun onBluetoothMayBeOpened() {
    }

    protected fun setDevice(device: Device?) {
        _device = device
    }

    protected fun applySystemBarColor() {
        StatusBarUtil.setStatusBarColor(this, statusBarColor)
        StatusBarUtil.setStatusBarStyle(this, !isDarkStatusBar)
        window.navigationBarColor = navigationBarColor
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
            androidx.core.graphics.ColorUtils.calculateLuminance(navigationBarColor) > 0.5
    }

    protected open val isDarkStatusBar: Boolean
        get() = false

    protected open val statusBarColor: Int
        get() = getColor(android.R.color.white)

    protected open val navigationBarColor: Int
        get() = getColor(android.R.color.white)

    protected val isFullscreenInPortrait: Boolean
        get() = javaClass.isAnnotationPresent(FullscreenPortrait::class.java)

    protected val isFullscreenInLandscape: Boolean
        get() = javaClass.isAnnotationPresent(FullscreenLandscape::class.java)

    protected val isImmersiveInPortrait: Boolean
        get() {
            val fp = javaClass.getAnnotation(
                FullscreenPortrait::class.java
            )
            return fp != null && fp.immersive
        }

    protected val isImmersiveInLandscape: Boolean
        get() {
            val fp = javaClass.getAnnotation(
                FullscreenLandscape::class.java
            )
            return fp != null && fp.immersive
        }

    protected open val topInsetAwarableView: View?
        get() = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val prevIsPortrait = isPortrait
        isPortrait = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE

        if (prevIsPortrait != isPortrait) {
            if (isPortrait) {
                exitLandscape()
            } else {
                enterLandscape()
            }
        }

        applySystemUi()
        applySystemBarColor()
    }

    protected open fun enterLandscape() {
    }

    protected open fun exitLandscape() {
    }

    private fun installSystemBarInsets() {
        val root = viewBinding?.root ?: return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        val insetView = topInsetAwarableView
        val initialTopMargin = (insetView?.layoutParams as? MarginLayoutParams)?.topMargin ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val fullscreen = if (isPortrait) isFullscreenInPortrait else isFullscreenInLandscape
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            root.setPadding(
                left + if (fullscreen) 0 else bars.left,
                top + if (fullscreen) 0 else bars.top,
                right + if (fullscreen) 0 else bars.right,
                bottom + if (fullscreen) 0 else maxOf(bars.bottom, ime.bottom)
            )
            (insetView?.layoutParams as? MarginLayoutParams)?.let { params ->
                params.topMargin = initialTopMargin + if (fullscreen) bars.top else 0
                insetView.layoutParams = params
            }
            // Keep IME and other insets available to child views.
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    protected fun applySystemUi() {
        val window = window
        var flags = window.decorView.systemUiVisibility
        flags = flags and (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY).inv()
        flags = flags or (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        if (isPortrait) {
            if (isFullscreenInPortrait) {
                flags = flags or (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
                if (isImmersiveInPortrait) {
                    flags = flags or (View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }
            }
        } else {
            if (isFullscreenInLandscape) {
                flags = flags or (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
                if (isImmersiveInLandscape) {
                    flags = flags or (View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }
            }
        }
        window.decorView.systemUiVisibility = flags
        viewBinding?.root?.let { ViewCompat.requestApplyInsets(it) }
    }

    protected fun requestOrientation(landscape: Boolean) {
        if (landscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            applySystemUi()
            applySystemBarColor()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // check permission
        if (javaClass.isAnnotationPresent(RequireStoragePermission::class.java)) {
            checkStoragePermission()
        }
    }

    public override fun onResume() {
        super.onResume()
        isActive = true
        UMengManager.onResume(this)
    }

    public override fun onPause() {
        super.onPause()
        isActive = false
        UMengManager.onPause(this)
    }

    @JvmOverloads
    fun showLoading(message: String? = "") {
        showLoading(false, message)
    }

    @JvmOverloads
    fun showLoading(cancelable: Boolean?, message: String?, timeout: Long = 10000) {
        try {
            if (!isDestroyed && !isFinishing) {
                val d = LoadingDialog(this)
                d.zOrder = ZORDER_LOADING
                d.setCancelable(cancelable!!)
                d.setCanceledOnTouchOutside(cancelable)
                d.setMessage(message)
                if (timeout > 0) {
                    _handler!!.removeCallbacks(_loadingTimeoutCleanerRunnable)
                    _handler!!.postDelayed(_loadingTimeoutCleanerRunnable, timeout)
                }
                dialogStack.push(d)
            }
        } catch (e: Throwable) {
        }
    }

    fun dismissLoading() {
        try {
            if (!isDestroyed && !isFinishing) {
                dialogStack.removeByZOrder(ZORDER_LOADING)
                _handler!!.removeCallbacks(_loadingTimeoutCleanerRunnable)
            }
        } catch (e: Throwable) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (javaClass.isAnnotationPresent(IEventBus::class.java)) {
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this)
            }
        }

        if (javaClass.isAnnotationPresent(BLEAware::class.java)) {
            uninstallBleConnectionListener()
        }

        _handler!!.removeCallbacksAndMessages(null)
        _handler = null
        dialogStack.clear()
        dismissLoading()
    }

    override fun addMenuProvider(provider: MenuProvider, owner: LifecycleOwner, state: Lifecycle.State) {
    }

    private fun installBleConnectionListener() {
        _connectNotifyListener = object : IBleConnectListener {
            override fun onConnecting(device: Device) {
                AppTrace.d(TAG, "onConnectStart....")
                _connectDevice = device
                DialogUtil.showDialog(BluetoothManager.getDisplayDeviceName(device.getName()))
            }

            override fun onConnectFailure(address: String, error: String) {
                AppTrace.d(TAG, "onConnectFailure....$address, error = $error")
                _handler!!.post {
                    DialogUtil.dismissDialog()
                    if (_connectDevice != null && _connectDevice!!.getAddress()
                            .equals(address, ignoreCase = true)
                    ) {
                        ToastUtil.s(R.string.info_ble_connect_fail)
                        _connectDevice = null
                    }
                }
            }

            override fun onDisConnecting(address: String) {
                AppTrace.d(TAG, "onDisConnecting $address")
            }

            override fun onDisConnectSuccess(address: String, status: Int) {
                AppTrace.d(TAG, "onDisConnectSuccess $address")
                if (_device != null && !TextUtils.isEmpty(_device!!.address) && !TextUtils.isEmpty(
                        address
                    ) && !address.equals(
                        _device!!.address, ignoreCase = true
                    )
                ) {
                    return
                }
                if (!BaseAppUtil.isDestroy(this@BaseActivity)) {
                    _handler!!.post {
                        if (!BaseAppUtil.isDestroy(this@BaseActivity) && BaseAppUtil.isForegroundActivity(
                                this@BaseActivity, this@BaseActivity.javaClass.name
                            )
                        ) {
                            showBlueToothDisconnectDialog()
                        } else {
                            _connectDevice = null
                        }
                    }
                }
            }

            override fun onConnectTimeOut(address: String) {
                AppTrace.d(TAG, "onConnectTimeOut $address")
                _handler!!.post {
                    if (_connectDevice != null && _connectDevice!!.getAddress()
                            .equals(address, ignoreCase = true)
                    ) {
                        DialogUtil.dismissDialog()
                        ToastUtil.s(R.string.info_ble_connect_timeout)
                    }
                }
            }

            override fun onServiceDiscoverySucceed(address: String, status: Int) {
                AppTrace.d(TAG, "onServiceDiscoverySucceed $address")
                _handler!!.postDelayed({
                    DialogUtil.dismissDialog()
                    //测试发送一个weewa连接成功的消息
                    BluetoothManager.getImpl().sendCmdSimpleObj(WeewaConnectedRequestBean())
                    ToastUtil.s(getString(R.string.device_connected))
                }, 100)
            }

            override fun onServiceDiscoveryFailed(address: String, message: String) {
                AppTrace.d(TAG, "onServiceDiscoveryFailed $address, message=$message")
                _handler!!.post {
                    if (_connectDevice != null && _connectDevice!!.getAddress()
                            .equals(address, ignoreCase = true)
                    ) {
                        ToastUtil.s(message)
                    }
                }
            }

            override fun onConnectSuccess(device: Device, status: Int) {  //
                AppTrace.d(TAG, "onConnect status=$status")
            }
        }
        BluetoothHandle.addConnectNotifyListener(_connectNotifyListener)
    }

    fun showBlueToothDisconnectDialog() {
        if (_disconnectBTDialog == null) {
            _disconnectBTDialog = ModalDialog(this)
            _disconnectBTDialog!!.setMessage(getString(R.string.info_bluetooth_disconnected))
            _disconnectBTDialog!!.setNegativeButton(
                getString(R.string.cancel),
                object : GuardedOnClickListener() {
                    override fun onClicked(): Boolean {
                        _disconnectBTDialog!!.dismiss()
                        return true
                    }
                })
            _disconnectBTDialog!!.setPositiveButton(
                getString(R.string.retry),
                object : GuardedOnClickListener() {
                    override fun onClicked(): Boolean {
                        _disconnectBTDialog!!.dismiss()

                        if (!BluetoothManager.getImpl().enableBlueTooth()) {
                            val dialog = BluetoothDialog(this@BaseActivity)
                            dialog.setOnClickListener { dialog, which ->
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    requestOpenBluetooth()
                                }
                                dialog.dismiss()
                            }
                            dialog.show()
                        } else {
                            if (!PermissionsUtil.isLocServiceEnable(this@BaseActivity)) {
                                PermissionsUtil.openLocationService(this@BaseActivity)
                                return true
                            }
                            val hasPermission = PermissionsUtil.hasBlePermission(this@BaseActivity)
                            if (!hasPermission) {
                                EasyPermissions.requestPermissions(
                                    this@BaseActivity,
                                    getString(R.string.permission_title_location_and_device),
                                    PermissionsUtil.REQUEST_PERMISSION_BLUE,
                                    *PermissionsUtil.getBlePermissions()
                                )
                                return true
                            }

                            if (_device != null) {
                                BluetoothManager.getImpl().connectToBle(_device)
                            }
                        }

                        return true
                    }
                })
            _disconnectBTDialog!!.show()
        } else {
            if (!_disconnectBTDialog!!.isShowing) {
                _disconnectBTDialog!!.show()
            }
        }
    }

    private fun uninstallBleConnectionListener() {
        BluetoothHandle.removeConnectNotifyListener(_connectNotifyListener)
        _connectNotifyListener = null
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.setData(Uri.parse(String.format("package:%s", packageName)))
                    _storagePermLauncher!!.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    _storagePermLauncher!!.launch(intent)
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                _storagePermLegacyLauncher!!.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                _storagePermLegacyLauncher!!.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun showMandatoryStoragePermissionDialog() {
        val d = ModalDialog(this)
        d.zOrder = ZORDER_STORAGE_PERM_MANDATORY_DIALOG
        d.setTitle(getString(R.string.permission_mandatory_storage_title))
        d.setMessage(getString(R.string.permission_mandatory_storage_message))
        d.setNegativeButton(getString(R.string.cancel), object : GuardedOnClickListener() {
            override fun onClicked(): Boolean {
                finish()
                return true
            }
        })
        d.setPositiveButton(getString(R.string.ok), object : GuardedOnClickListener() {
            override fun onClicked(): Boolean {
                requestStoragePermission()
                return true
            }
        })
        dialogStack.push(d)
    }

    private fun showMandatoryCameraPermissionDialog() {
        val d = ModalDialog(this)
        d.zOrder = ZORDER_STORAGE_PERM_MANDATORY_DIALOG
        d.setTitle(getString(R.string.permission_mandatory_camera_title))
        d.setMessage(getString(R.string.permission_mandatory_camera_message))
        d.setNegativeButton(getString(R.string.cancel), object : GuardedOnClickListener() {
            override fun onClicked(): Boolean {
                finish()
                return true
            }
        })
        d.setPositiveButton(getString(R.string.ok), object : GuardedOnClickListener() {
            override fun onClicked(): Boolean {
                requestPermissionIfNeeded(Manifest.permission.CAMERA)
                return true
            }
        })
        dialogStack.push(d)
    }

    public fun requestBluetoothPermission() {
        EasyPermissions.requestPermissions(
            this@BaseActivity,
            getString(R.string.permission_title_location_and_device),
            PermissionsUtil.REQUEST_PERMISSION_BLUE,
            *PermissionsUtil.getBlePermissions()
        )
    }

    fun checkBluetoothPermission(): Boolean {
        if (!PermissionsUtil.hasBlePermission(this@BaseActivity)) {
            val d = ModalDialog(this)
            d.zOrder = ZORDER_LOCATION_PERM_DIALOG
            d.setTitle(getString(R.string.search_title_location_and_device))
            d.setMessage(getString(R.string.permission_title_location_and_device))
            d.setNegativeButton(getString(R.string.cancel), object : GuardedOnClickListener() {
                override fun onClicked(): Boolean {
                    return true
                }
            })
            d.setPositiveButton(getString(R.string.search), object : GuardedOnClickListener() {
                override fun onClicked(): Boolean {
                    requestBluetoothPermission()
                    return true
                }
            })
            dialogStack.push(d)
            return false
        }
        return true
    }

    @JvmOverloads
    fun showConfirmDialog(
        title: String?,
        msg: String?,
        positiveBtnText: String? = null,
        negativeBtnText: String? = null,
        positiveCb: GuardedOnClickListener? = null,
        negativeCb: GuardedOnClickListener? = null
    ) {
        val d = ModalDialog(this)
        d.zOrder = ZORDER_GENERAL_CONFIRM_DIALOG
        d.setTitle(title)
        d.setMessage(msg)
        d.setNegativeButton(negativeBtnText ?: getString(R.string.cancel), negativeCb)
        d.setPositiveButton(positiveBtnText ?: getString(R.string.ok), positiveCb)
        dialogStack.push(d)
    }

    fun showHintDialog(msg: String?) {
        showHintDialog("提示", msg)
    }

    @JvmOverloads
    fun showHintDialog(
        title: String?, msg: String?, positiveBtnText: String? = getString(
            R.string.ok
        ), positiveCb: GuardedOnClickListener? = null
    ) {
        val d = ModalDialog(this)
        d.zOrder = ZORDER_GENERAL_HINT_DIALOG
        d.setTitle(title)
        d.hideNegativeButton()
        d.setPositiveButton(positiveBtnText, positiveCb)
        d.setMessage(msg)
        d.setCancelable(true)
        dialogStack.push(d)
    }

    fun showNetworkDisconnectDialog() {
        showNetworkDisconnectDialog(getString(R.string.phone_no_network_title), getString(R.string.phone_no_network_message))
    }

    fun showNetworkDisconnectDialog(title: String?,
                                    msg: String?) {
        if (_disconnectNetworkDialog == null) {
            _disconnectNetworkDialog = ModalDialog(this)
            _disconnectNetworkDialog!!.setTitle(title)
            _disconnectNetworkDialog!!.setMessage(msg)
            _disconnectNetworkDialog!!.setNegativeButton(
                getString(R.string.cancel),
                object : GuardedOnClickListener() {
                    override fun onClicked(): Boolean {
                        _disconnectNetworkDialog!!.dismiss()
                        return true
                    }
                })
            _disconnectNetworkDialog!!.setPositiveButton(
                getString(R.string.setting),
                object : GuardedOnClickListener() {
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
            _disconnectNetworkDialog = ModalDialog(this)
            _disconnectNetworkDialog!!.setTitle(getString(R.string.device_offline_remote))
            _disconnectNetworkDialog!!.setMessage(getString(R.string.remote_model_offline_message))
            _disconnectNetworkDialog!!.setNegativeButton(
                getString(R.string.cancel),
                object : GuardedOnClickListener() {
                    override fun onClicked(): Boolean {
                        _disconnectNetworkDialog!!.dismiss()
                        return true
                    }
                })
            _disconnectNetworkDialog!!.setPositiveButton(
                getString(R.string.setting),
                object : GuardedOnClickListener() {
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

    fun showDisconnectDialog(){
        if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            showBlueToothDisconnectDialog()
        } else {
            if(!NetUtil.isConnectedAvailableNetwork()) {
                showNetworkDisconnectDialog()
            } else {
                showDeviceOfflineDialog();
            }
        }
    }
    fun showDisconnectToast(){
        if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            ToastUtil.s(getString(R.string.info_bluetooth_offline))
        }else{
            ToastUtil.s(getString(R.string.info_network_disconnect))
        }
    }


    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun listenStoragePermissionGranted(fragment: BaseFragment<*>?) {
        _storagePermForwardFragment = WeakReference(fragment)
    }

    fun checkStoragePermission(): Boolean {
        val perm = javaClass.getAnnotation(RequireStoragePermission::class.java)
        return checkStoragePermission(perm != null && perm.mandatory)
    }

    fun checkStoragePermission(mandatory: Boolean): Boolean {
        if (!hasStoragePermission()) {
            val d = ModalDialog(this)
            d.zOrder = ZORDER_STORAGE_PERM_DIALOG
            d.setTitle(getString(R.string.permission_storage_title))
            d.setMessage(getString(R.string.permission_storage_message))
            d.setNegativeButton(getString(R.string.cancel), object : GuardedOnClickListener() {
                override fun onClicked(): Boolean {
                    if (mandatory) {
                        showMandatoryStoragePermissionDialog()
                    }
                    return true
                }
            })
            d.setPositiveButton(getString(R.string.ok), object : GuardedOnClickListener() {
                override fun onClicked(): Boolean {
                    requestStoragePermission()
                    return true
                }
            })
            dialogStack.push(d)
            return false
        }
        return true
    }

    fun checkTakePhotoPermission(mandatory: Boolean): Boolean {
        if (!PermissionsUtil.hasPermission(this@BaseActivity, Manifest.permission.CAMERA)) {
            val d = ModalDialog(this)
            d.zOrder = ZORDER_CAMERA_PERM_DIALOG
            d.setTitle(getString(R.string.permission_camera_title))
            d.setMessage(getString(R.string.permission_camera_message))
            d.setNegativeButton(getString(R.string.cancel), object : GuardedOnClickListener() {
                override fun onClicked(): Boolean {
                    if (mandatory) {
                        showMandatoryCameraPermissionDialog()
                    }
                    return true
                }
            })
            d.setPositiveButton(getString(R.string.ok), object : GuardedOnClickListener() {
                override fun onClicked(): Boolean {
                    requestPermissionIfNeeded(Manifest.permission.CAMERA)
                    return true
                }
            })
            dialogStack.push(d)
            return false
        }
        return true
    }

    protected fun requestPermissionIfNeeded(perm: String): Boolean {
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            _pendingPerm = perm
            _singlePermLauncher!!.launch(perm)
            return false
        }
        return true
    }

    protected open fun onStoragePermissionGranted() {
    }

    protected open fun onOnePermissionGranted(perm: String?) {
    }

    protected open fun onOnePermissionDenied(perm: String?) {
    }

    companion object {
        @JvmField
        protected val TAG: String = BaseActivity::class.java.simpleName

        // for storage permission
        private const val ZORDER_LOADING = Int.MAX_VALUE
        private const val ZORDER_STORAGE_PERM_DIALOG = -1000
        private const val ZORDER_STORAGE_PERM_MANDATORY_DIALOG = -1001
        private const val ZORDER_GENERAL_CONFIRM_DIALOG = -1002
        private const val ZORDER_GENERAL_HINT_DIALOG = -1003
        private const val ZORDER_CAMERA_PERM_DIALOG = -1004
        private const val ZORDER_LOCATION_PERM_DIALOG = -1005
    }
}
