package com.xh.hotme.record.score;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xh.hotme.base.BaseQuickAdapter;
import com.xh.hotme.base.BaseViewHolder;
import com.xh.hotme.R;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;

import java.util.List;

/**
 * date   : 2023/6/19
 * desc   :
 */
public class GameTagAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    private final static String TAG = GameTagAdapter.class.getSimpleName();

    final Context _context;

    TagCallback _callBack;


    int _selectedItem = 0;


    public GameTagAdapter(Context context, List data) {
        super(R.layout.score_center_tag_item_tag, data);
        _context = context;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, final String tag) {
        int position = baseViewHolder.getLayoutPosition();
        TextView tvTagName = baseViewHolder.findView(R.id.tvContent);
        tvTagName.setText(tag);

        tvTagName.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                AppTrace.d(TAG, "click guest");
                if (_callBack != null) {
                    _callBack.onClick(tag, position);
                }
                return true;
            }
        });

        if (position == 0) {
            tvTagName.setBackgroundResource(position == _selectedItem ? R.drawable.sc_tag_item_top_bg_selected : R.drawable.sc_tag_item_top_bg);
        } else if (position == getData().size() - 1) {
            tvTagName.setBackgroundResource(position == _selectedItem ? R.drawable.sc_tag_item_bottom_bg_selected : R.drawable.sc_tag_item_bottom_bg);
        } else {
            tvTagName.setBackgroundResource(position == _selectedItem ? R.drawable.sc_tag_item_bg_selected : R.drawable.sc_tag_item_bg);
        }

    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setListener(TagCallback l) {
        _callBack = l;
    }

    public void setSelectedItem(int index) {
        _selectedItem = index;
    }


    public interface TagCallback {
        void onClick(String content, int pos);

    }
}
