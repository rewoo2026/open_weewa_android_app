package com.xh.hotme.base

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.Nullable
import androidx.viewbinding.ViewBinding
import com.xh.hotme.R
import com.xh.hotme.utils.BaseAppUtil
import com.xh.hotme.utils.Constants
import com.xh.hotme.utils.DensityUtil
import com.xh.hotme.utils.DeviceUtil
import com.xh.hotme.utils.ToastUtil
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

abstract class BaseDialog<T : ViewBinding?> @JvmOverloads constructor(
    context: Context,
    themeResId: Int = R.style.hotme_custom_dialog
) : StackableDialog(context, themeResId) {
    @JvmField
    protected var viewBinding: T? = null
    protected var ownerCtx: WeakReference<Context>? = WeakReference(context)

    init {
        initView()
    }

    protected val ownerContext: Context?
        get() = if (ownerCtx != null) ownerCtx!!.get() else null

    private fun initView() {
        setCanceledOnTouchOutside(true)
        initViewBinding()
        if (viewBinding != null) {
            setContentView(viewBinding!!.root)
            onInit()

            // handle sheet style
            if (javaClass.isAnnotationPresent(SheetStyle::class.java)) {
                // set window params
                val sheet = javaClass.getAnnotation(SheetStyle::class.java)
                val window = window!!
                assert(sheet != null)
                val params = window.attributes
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                if (sheet!!.heightGap > 0) {
                    params.height = DeviceUtil.getHeight() - DensityUtil.dip2px(
                        sheet.heightGap.toFloat()
                    )
                }
                params.gravity = Gravity.BOTTOM
                window.attributes = params

                // 让dialog底部充满导航条, 看起来更美观
                var flags = window.decorView.systemUiVisibility
                flags = flags and (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY).inv()
                flags = flags or (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
                window.decorView.systemUiVisibility = flags
                window.navigationBarColor = -0x1
            }
        } else {
            onViewInitErr()
        }
    }

    private fun onViewInitErr() {
        setContentView(R.layout.activity_inititial_err)
        findViewById<View>(R.id.button).setOnClickListener { v: View? -> cancel() }
    }

    protected abstract fun onInit()

    private fun initViewBinding() {
        val type = (javaClass.genericSuperclass as ParameterizedType)
        val cls = type.actualTypeArguments[0] as Class<*>
        try {
            val inflate = cls.getDeclaredMethod("inflate", LayoutInflater::class.java)
            viewBinding = inflate.invoke(null, layoutInflater) as T
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }

    fun showOnCenter(marginDp: Int) {
        val params = window!!.attributes
        params.width = BaseAppUtil.getDeviceWidth() - (2 * DensityUtil.dip2px(marginDp.toFloat()))
        params.windowAnimations = R.style._bottomDialogAnimation
        params.gravity = Gravity.CENTER
        window!!.attributes = params
        show()
    }

    fun showDisconnectToast(){
        if (Constants.ACCESS_MODEL == Constants.ACCESS_MODEL_LOCAL) {
            ToastUtil.s(context.getString(R.string.info_bluetooth_offline))
        }else{
            ToastUtil.s(context.getString(R.string.info_network_disconnect))
        }
    }
}