package com.xh.hotme.base

/**
 * zorder大的显示在上面, 并且是一个个显示, 不是一起都显示, 这就需要在前一个消失的时候通知stack,
 * 然后stack显示下一个. 另外, 如果两个dialog zorder相同, 后者会顶掉前者
 */
class DialogStack {
    private val _dialogs: MutableList<StackableDialog> = ArrayList()

    @Synchronized
    fun push(d: StackableDialog) {
        // search stack
        val size = _dialogs.size
        var i = size - 1
        while (i >= 0) {
            val cur = _dialogs[i]
            if (d.zOrder == cur.zOrder) {
                // zorder一样的就顶掉, 先接触其和stack的关联, 这样他不会调用pop
                cur.assignStack(null)
                if (cur.isShowing) {
                    cur.dismiss()
                }
                _dialogs.removeAt(i)
            } else if (d.zOrder > cur.zOrder) {
                // 如果大于当前zorder, 则插入到这个位置, 如果当前的对话框正在显示, 则要隐藏(不是dismiss)
                _dialogs.add(d)
                if (cur.isShowing) {
                    cur.hide()
                }
                break
            }
            i--
        }

        // 如果堆栈为空, 或者zorder最小, 则i会为-1, 那么插入到最底层
        if (i == -1) {
            _dialogs.add(0, d)
        }

        // bind it with stack
        d.assignStack(this)

        // 如果其位于堆栈顶, 则显示这个对话框
        if (d === _dialogs[_dialogs.size - 1]) {
            d.show()
        }
    }

    @Synchronized
    fun clear() {
        _dialogs.forEach {
            it.assignStack(null)
            if (it.isShowing) {
                it.dismiss()
            }
        }
        _dialogs.clear()
    }

    @Synchronized
    fun remove(d: StackableDialog) {
        // if empty, return
        if (_dialogs.isEmpty()) return

        // if not top dialog, just remove
        // if top dialog, remove it and show new top dialog
        var top = _dialogs[_dialogs.size - 1]
        val onTop = top === d
        if (onTop) {
            // remove top
            _dialogs.removeAt(_dialogs.size - 1)

            // show new top
            if (!_dialogs.isEmpty()) {
                top = _dialogs[_dialogs.size - 1]
                top.show()
            }
        } else {
            _dialogs.remove(d)
        }
    }

    @Synchronized
    fun removeByZOrder(zOrder: Int) {
        // if empty, return
        if (_dialogs.isEmpty()) return

        // find by zorder and remove
        val size = _dialogs.size
        var i = size - 1
        while (i >= 0) {
            val d = _dialogs[i]
            if (d != null && zOrder == d.zOrder) {
                d.assignStack(null)
                if (d.isShowing) {
                    d.dismiss()
                }
                _dialogs.removeAt(i)
                break
            }
        }
    }
}
