package com.xh.hotme.storage;

import android.graphics.Color;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.base.SwitchType;
import com.xh.hotme.databinding.FragmentGeneralVideoListBinding;
import com.xh.hotme.storage.filetransfer.ITaskDataNotifier;
import com.xh.hotme.storage.filetransfer.TaskItemAdapter;
import com.xh.hotme.storage.filetransfer.TaskManager;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.SwipeHelper;

import java.util.List;

/**
 * @Description:
 * @Author: Maclay
 * @Date: 2023/6/9 14:19
 */
public class DownloadListFragment extends BaseFragment<FragmentGeneralVideoListBinding> implements ITaskDataNotifier {
    private TaskItemAdapter adapter;

    private SwipeHelper _swipeHelper;

    @Override
    protected void initView() {
        _viewBinding.noDataView.setText(R.string.no_transfer_data);
        _viewBinding.rv.setLayoutManager(new LinearLayoutManager(getContext()));
        _viewBinding.rv.setEmptyView(_viewBinding.lltEmpty);

        adapter = new TaskItemAdapter(getActivity(), TaskManager.getImpl().getDownloadViewList());
        adapter.setOnItemClickListener(position -> {
            // 当前如果有一行处于滑动删除按钮可见状态,则点击操作会缩回
            if(_swipeHelper.hasSwiped()) {
                _swipeHelper.recoverSwiped();
            }
        });
        _viewBinding.rv.setAdapter(adapter);

        _swipeHelper = new SwipeHelper(getContext(), _viewBinding.rv, TaskItemAdapter.ITEM_FILE_TRANSFER_TASK) {
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons) {
                underlayButtons.add(new SwipeHelper.UnderlayButton(
                    getString(R.string.delete),
                    0,
                    Color.parseColor("#FF3C30"),
                    pos -> {
                        if(TaskManager.getImpl().deleteDownloadTaskAt(pos)) {
                            // TODO 如果任务是未完成状态,要不要删本地文件呢,而且还有json同步问题
                        }
                        _swipeHelper.clearSwiped();
                    }
                ));
            }
        };

        // register
        TaskManager.getImpl().registerDataNotifier(this);
    }

    @Override
    protected SwitchType getSwitchType() {
        return SwitchType.VIEW_PAGER;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // unregister
        TaskManager.getImpl().unregisterDataNotifier(this);
    }

    @Override
    protected void initData() {
        AppTrace.d("DownloadListFragment", "downloadList：" + GsonUtils.GsonString(TaskManager.getImpl().getDownloadViewList()));
        if(!TaskManager.getImpl().getDownloadList().isEmpty()) {
            _viewBinding.lltEmpty.setVisibility(View.GONE);
        }
        postNotifyDataChanged();
    }

    @Override
    public void postNotifyDataChanged() {
        _viewBinding.rv.post(() -> adapter.notifyDataSetChanged());
    }

    @Override
    public void postNotifyItemChanged(int position) {
        _viewBinding.rv.post(() -> adapter.notifyItemChanged(position));
    }

    @Override
    public boolean watchUpload() {
        return false;
    }
}