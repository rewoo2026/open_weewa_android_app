package com.xh.hotme.me.holder;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.me.MeModuleBean;
import com.xh.hotme.me.bean.SettingBean;

import java.util.ArrayList;
import java.util.List;


public class MeSettingHolder extends CommonViewHolder<MeModuleBean> {
    final View _splitSpace;
    final RecyclerView _recyclerView;
    final SettingAdapter _taskAdapter;

    final Activity _context;
    List<SettingBean> _taskList = new ArrayList<>();


    public static MeSettingHolder create(Activity ctx, ViewGroup parent) {
        // load game row, and leave a gap so that next column can be seen
        View view = LayoutInflater.from(ctx)
            .inflate(R.layout.me_layout_setting, parent, false);
        return new MeSettingHolder(ctx, view);
    }

    public MeSettingHolder(Activity context, View view) {
        super(view);

        _context = context;
        this._recyclerView = view.findViewById(R.id.recyclerView);
        this._splitSpace = view.findViewById(R.id.split_space);

        _taskAdapter = new SettingAdapter(_context, _taskList);

        // setup views
        _recyclerView.setLayoutManager(new LinearLayoutManager(context));
        _recyclerView.setAdapter(_taskAdapter);

        _recyclerView.setNestedScrollingEnabled(false);

    }

    @Override
    public void onBind(final MeModuleBean signin, final int position) {
        // name & desc
        final Context ctx = itemView.getContext();

        _splitSpace.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

        initData();
        _taskAdapter.notifyDataSetChanged();
    }


    private void initData() {
        if(_taskList == null) {
            _taskList = new ArrayList<>();
        } else {
            _taskList.clear();
        }

        _taskList.add(new SettingBean(SettingAdapter.SETTING_TYPE_KUAICHUAN, R.mipmap.me_kuaichuan, _context.getString(R.string.ly_me_kuaichuan)));
        _taskList.add(new SettingBean(SettingAdapter.SETTING_TYPE_CAMERA_SETTING, R.mipmap.me_photo, _context.getString(R.string.ly_me_photo_set)));
        _taskList.add(new SettingBean(SettingAdapter.SETTING_TYPE_MANUAL_DOTTING_CLIP, R.mipmap.me_clip_video, _context.getString(R.string.ly_me_manual_dotting_clip)));
        _taskList.add(new SettingBean(SettingAdapter.SETTING_TYPE_AI_CLIP , R.mipmap.me_clip_ai, _context.getString(R.string.ly_me_ai_video_clip)));
        _taskList.add(new SettingBean(SettingAdapter.SETTING_TYPE_ACCOUNT, R.mipmap.me_set, _context.getString(R.string.ly_me_setting)));

    }

}