package com.xh.hotme.base

/**
 * 使用ActionSheet的样式显示对话框, 即从底部升起的样式
 * -1表示使用对话框自身高度, >0表示用屏幕高度减去heightGap得到对话框高度
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class SheetStyle(val heightGap: Int = -1)