package com.xh.hotme.storage.filetransfer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.xh.hotme.base.BaseVH;
import com.xh.hotme.databinding.ListItemFileTransferBinding;
import com.xh.hotme.databinding.ListItemFileTransferGroupBinding;

import java.util.List;

public class TaskItemAdapter extends RecyclerView.Adapter<BaseVH<TaskModel, ? extends ViewBinding>> {
    private final List<TaskModel> _models;

    public static final int ITEM_FILE_TRANSFER_TASK = 0;
    public static final int ITEM_UPLOADING_GROUP = 1;
    public static final int ITEM_UPLOADED_GROUP = 2;
    public static final int ITEM_DOWNLOADING_GROUP = 3;
    public static final int ITEM_DOWNLOADED_GROUP = 4;

    public interface OnItemClick {
        void onItemClick(int position);
    }

    Context _context;

    private OnItemClick _onItemClick;

    public OnItemClick getOnItemClickListener() {
        return _onItemClick;
    }

    public void setOnItemClickListener(OnItemClick onItemClick) {
        _onItemClick = onItemClick;
    }

    public TaskItemAdapter(Context context, List<TaskModel> models) {
        _context = context;
        _models = models;
    }

    @Override
    public int getItemViewType(int position) {
        return _models.get(position).getUiType();
    }

    @NonNull
    @Override
    public BaseVH<TaskModel, ? extends ViewBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == ITEM_FILE_TRANSFER_TASK) {
            return new TaskItemViewHolder(_context, ListItemFileTransferBinding.inflate(
                LayoutInflater.from(_context), parent, false
            ));
        }
        return new TaskGroupViewHolder(ListItemFileTransferGroupBinding.inflate(
            LayoutInflater.from(_context), parent, false
        ));
    }

    @Override
    public void onBindViewHolder(BaseVH<TaskModel, ? extends ViewBinding> holder, int position) {
        TaskModel model = _models.get(position);
        TaskManager.getImpl().updateViewHolder(model.getId(), holder);
        holder.update(model, position);
    }

    @Override
    public int getItemCount() {
        return _models.size();
    }
}