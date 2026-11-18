package com.xh.hotme.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.flyco.tablayout.listener.CustomTabEntity;
import com.flyco.tablayout.listener.OnTabSelectListener;
import com.xh.hotme.R;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.databinding.HomeFragmentIcloudBinding;
import com.xh.hotme.storage.CameraVideoFragment;
import com.xh.hotme.storage.CloudVideoFragment;
import com.xh.hotme.storage.PhoneVideoFragment;
import com.xh.hotme.storage.TransferListActivity;
import com.xh.hotme.storage.filetransfer.TaskManager;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.widget.ModalDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

@IEventBus
public class CloudFragment extends BaseFragment<HomeFragmentIcloudBinding> implements View.OnClickListener, OnTabSelectListener, ViewPager.OnPageChangeListener {
    private static final String TAG = CloudFragment.class.getSimpleName();

    @Keep
    public static class RequestChangeTabEvent {
        public final int tab;

        public RequestChangeTabEvent(int tab) {
            this.tab = tab;
        }
    }

    final List<Fragment> mFragmentList = new ArrayList<>();

    int _activeCatIndex = 0;

    String[] titleName = new String[3];


    @Keep
    public static CloudFragment newInstance() {
        return new CloudFragment();
    }

    @Override
    protected void initView() {
        titleName = getResources().getStringArray(R.array.storage_type);

        _viewBinding.ivTransmission.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                Intent intent = new Intent(getActivity(), TransferListActivity.class);
                startActivity(intent);
                return true;
            }
        });

        mFragmentList.add(new PhoneVideoFragment());
        mFragmentList.add(new CloudVideoFragment());
        mFragmentList.add(CameraVideoFragment.newInstance());

        _viewBinding.viewPager.setOffscreenPageLimit(3);
        _viewBinding.viewPager.addOnPageChangeListener(this);

        // setup pager
        _viewBinding.viewPager.setAdapter(new VideoPagerAdapter(getChildFragmentManager()));

        // set to init cat index
        if(_viewBinding.viewPager.getCurrentItem() != _activeCatIndex) {
            _viewBinding.viewPager.setCurrentItem(_activeCatIndex, false);
        }

        // setup tabs
        _viewBinding.tabs.setOnTabSelectListener(this);

        // add tab
        ArrayList<CustomTabEntity> tabEntities = new ArrayList<>();
        for(String s : titleName) {
            tabEntities.add(new TabEntity(s, 0, 0));
        }

        // set tabs
        _viewBinding.tabs.setViewPager(_viewBinding.viewPager);
//        viewBinding.tabs.setTabData(tabEntities);
        _viewBinding.tabs.setCurrentTab(0);
    }

    @Override
    protected void initData() {
//        checkPermission();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRequestChangeTabEvent(RequestChangeTabEvent event) {
        _viewBinding.tabs.setCurrentTab(event.tab);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        _viewBinding.tabs.setCurrentTab(position);
        _activeCatIndex = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onTabSelect(int position) {
        _activeCatIndex = position;
        _viewBinding.viewPager.setCurrentItem(position);
    }

    @Override
    public void onTabReselect(int position) {

    }

    public class TabEntity implements CustomTabEntity {
        public final String title;
        public final int selectedIcon;
        public final int unSelectedIcon;

        public TabEntity(String title, int selectedIcon, int unSelectedIcon) {
            this.title = title;
            this.selectedIcon = selectedIcon;
            this.unSelectedIcon = unSelectedIcon;
        }

        @Override
        public String getTabTitle() {
            return title;
        }

        @Override
        public int getTabSelectedIcon() {
            return selectedIcon;
        }

        @Override
        public int getTabUnselectedIcon() {
            return unSelectedIcon;
        }
    }


    private class VideoPagerAdapter extends FragmentPagerAdapter {
        public VideoPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return titleName.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titleName[position];
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

    }

    public boolean isCameraFragment() {
        return _activeCatIndex == 2;
    }
}
