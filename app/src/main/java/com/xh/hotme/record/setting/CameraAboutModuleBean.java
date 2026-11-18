package com.xh.hotme.record.setting;

import android.view.View;

public class CameraAboutModuleBean {
    public final String title;

    public final String content;

    public final View.OnClickListener listener;

    public CameraAboutModuleBean(String title, String content) {
        this.title = title;
        this.content = content;
        this.listener = null;
    }

    public CameraAboutModuleBean(String title, String content, View.OnClickListener l) {
        this.title = title;
        this.content = content;
        this.listener = l;
    }
}
