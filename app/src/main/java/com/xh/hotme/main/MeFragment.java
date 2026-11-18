package com.xh.hotme.main;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.databinding.TabFragmentMeBinding;
import com.xh.hotme.me.MeHomeAdapter;
import com.xh.hotme.me.MeModuleBean;
import com.xh.hotme.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class MeFragment extends BaseFragment<TabFragmentMeBinding> {
    MeHomeAdapter _meHomeAdapter;
    Context _context;

    final List<MeModuleBean> _moduleBeanList = new ArrayList<>();

    @Keep
    public static MeFragment newInstance() {
        return new MeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _context = getContext();
    }

    @Override
    protected void initView() {
        _viewBinding.refreshLayout.setOnRefreshListener(() -> {
            if(_meHomeAdapter != null) {
                // reload
                _meHomeAdapter.notifyDataSetChanged();
            }

            new Handler(Looper.getMainLooper()).post(() -> _viewBinding.refreshLayout.setRefreshing(false));

        });

        _meHomeAdapter = new MeHomeAdapter(getActivity());

        initModules();

        _viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        _viewBinding.recyclerView.setAdapter(_meHomeAdapter);
    }

    @Override
    protected void initData() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for(MeModuleBean moduleBean : _moduleBeanList) {
            moduleBean.onDestroy();
        }
    }

    @Override
    protected void onVisibleChanged(boolean isVisible) {
        if(isVisible) {
            _meHomeAdapter.notifyDataSetChanged();
        }
    }

    private void initModules() {
        _moduleBeanList.add(new MeModuleBean(Constants.ME_MODULE_PROFILE));
        _moduleBeanList.add(new MeModuleBean(Constants.ME_MODULE_SETTING));
        _moduleBeanList.add(new MeModuleBean(Constants.ME_MODULE_OUT));
        _meHomeAdapter.setModels(_moduleBeanList);
    }

    public void notifyAdapter() {
        if(_meHomeAdapter != null) {
            _meHomeAdapter.notifyDataSetChanged();
        }
    }
}
