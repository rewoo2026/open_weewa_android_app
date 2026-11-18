package com.xh.hotme.base

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import com.xh.hotme.HotmeApplication
import com.xh.hotme.utils.AppTrace
import java.lang.ref.WeakReference
import java.util.Stack

enum class ActivityStackManager {
    impl;

    private val _activityStack = Stack<WeakReference<Activity>>()

    private var _topActivity: WeakReference<Activity>? = null

    var currentActivity: Activity?
        get() {
            if (_topActivity != null) {
                return _topActivity!!.get()
            }
            return null
        }
        set(activity) {
            _topActivity = WeakReference(activity)
        }

    fun isActivityVisible(klass: Class<out Activity?>): Boolean {
        val act = currentActivity
        return act != null && act.javaClass == klass
    }

    fun popSingleActivity(targetPopActivity: Activity?) {
        if (targetPopActivity != null) {
            AppTrace.d("ActivityStackManager", "popActivity: " + targetPopActivity.localClassName)
            popSingleActivity(targetPopActivity.javaClass)
        }
    }

    fun popSingleActivity(popActivity: Class<out Activity?>?) {
        if (null == popActivity) {
            AppTrace.d("ActivityStackManager", "cls is null")
        } else {
            val var2: Iterator<WeakReference<Activity>> =
                _activityStack.iterator()

            while (var2.hasNext()) {
                val activity = var2.next().get()
                if (null != activity && activity.javaClass == popActivity) {
                    activity.finish()
                }
            }
        }
    }

    fun hasActivity(activity: Activity?): Boolean {
        if (activity == null) {
            return false
        } else {
            val var2: Iterator<WeakReference<Activity>> =
                _activityStack.iterator()

            var act: Activity?
            do {
                if (!var2.hasNext()) {
                    return false
                }

                act = var2.next().get()
            } while (act == null || act.javaClass != activity.javaClass)

            return true
        }
    }

    fun hasActivity(activity: Class<out Activity?>?): Boolean {
        if (activity == null) {
            return false
        } else {
            val var2: Iterator<WeakReference<Activity>> =
                _activityStack.iterator()

            var act: Activity?
            do {
                if (!var2.hasNext()) {
                    return false
                }

                act = var2.next().get()
            } while (act == null || act.javaClass != activity)

            return true
        }
    }

    fun pushActivity(targetPushActivity: Activity) {
        AppTrace.d("ActivityStackManager", "pushActivity: " + targetPushActivity.localClassName)
        _activityStack.add(WeakReference(targetPushActivity))
    }

    fun removeActivity(destroyedActivity: Activity) {
        // Lifecycle cleanup must remove the exact instance. Calling finish()
        // here can close the replacement Activity during configuration changes.
        _activityStack.removeAll { it.get() == null || it.get() === destroyedActivity }
        if (_topActivity?.get() === destroyedActivity) {
            _topActivity = null
        }
    }

    fun popOtherActivity(remainActivity: Class<out Activity?>?) {
        if (null == remainActivity) {
            AppTrace.d("ActivityStackManager", "cls is null")
        } else {
            val var2: Iterator<WeakReference<Activity>> =
                _activityStack.iterator()

            while (var2.hasNext()) {
                val activity = var2.next().get()
                if (null != activity && activity.javaClass != remainActivity) {
                    activity.finish()
                }
            }

            AppTrace.d("ActivityStackManager", "activity num is : " + _activityStack.size)
        }
    }

    fun popAllActivity() {
        while (true) {
            val activity = this.currentActivity
            if (activity == null) {
                AppTrace.d("ActivityStackManager", "activity num is : " + _activityStack.size)
                return
            }

            activity.finish()
            this.popSingleActivity(activity)
        }
    }

    fun getTopActivityName(): String? {
        var topActivityClassName: String? = null
        val activityManager =
            HotmeApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTaskInfos = activityManager.getRunningTasks(1)
        if (runningTaskInfos != null) {
            val f = runningTaskInfos[0].topActivity
            topActivityClassName = f!!.className
        }

        return topActivityClassName
    }
}
