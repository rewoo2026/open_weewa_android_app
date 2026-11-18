package com.xh.hotme.base

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import java.lang.ref.WeakReference

open class StackableDialog : Dialog, DialogInterface.OnDismissListener {
    // 越大越在上面
    var zOrder = 0
    private var _ownerStack: WeakReference<DialogStack?>? = null

    init {
        setOnDismissListener(this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    protected constructor(
        context: Context,
        cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener)

    protected val isInStack: Boolean
        get() = _ownerStack != null && _ownerStack!!.get() != null

    fun assignStack(stack: DialogStack?) {
        _ownerStack = if (stack == null) {
            null
        } else {
            WeakReference(stack)
        }
    }

    final override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        var l = listener
        if (listener !== this) {
            l = DialogInterface.OnDismissListener { dialog: DialogInterface ->
                listener?.onDismiss(dialog)
                onDismiss(dialog)
            }
        }
        super.setOnDismissListener(l)
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (isInStack) {
            val stack = _ownerStack!!.get()
            stack!!.remove(this)
        }
    }
}
