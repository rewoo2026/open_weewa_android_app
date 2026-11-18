package com.xh.hotme.base

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RequireStoragePermission(val mandatory: Boolean)