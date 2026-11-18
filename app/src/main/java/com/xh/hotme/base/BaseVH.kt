package com.xh.hotme.base

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseVH<T, K : ViewBinding?>(@JvmField val viewBinding: K) : RecyclerView.ViewHolder(
    viewBinding!!.root
) {
    @JvmField
    protected var _model: T? = null

    abstract fun update(model: T, position: Int)

    fun getModel(): T? {
        return _model
    }
}
