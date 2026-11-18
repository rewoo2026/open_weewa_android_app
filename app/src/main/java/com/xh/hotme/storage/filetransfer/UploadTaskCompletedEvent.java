package com.xh.hotme.storage.filetransfer;

import androidx.annotation.Keep;

@Keep
public class UploadTaskCompletedEvent {
    public TaskModel model;

    public UploadTaskCompletedEvent(TaskModel model) {
        this.model = model;
    }

    public TaskModel getModel() {
        return model;
    }
}
