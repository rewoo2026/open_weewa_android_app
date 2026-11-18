package com.xh.hotme.storage.filetransfer;

import androidx.annotation.Keep;

@Keep
public class DownloadTaskCompletedEvent {
    public TaskModel model;

    public DownloadTaskCompletedEvent(TaskModel model) {
        this.model = model;
    }

    public TaskModel getModel() {
        return model;
    }
}
