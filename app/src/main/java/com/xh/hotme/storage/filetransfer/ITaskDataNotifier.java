package com.xh.hotme.storage.filetransfer;

public interface ITaskDataNotifier {
    void postNotifyDataChanged();

    void postNotifyItemChanged(int position);

    boolean watchUpload();
}
