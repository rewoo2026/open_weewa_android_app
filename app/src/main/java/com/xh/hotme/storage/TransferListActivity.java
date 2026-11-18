package com.xh.hotme.storage;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.flyco.tablayout.listener.CustomTabEntity;
import com.flyco.tablayout.listener.OnTabSelectListener;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.BaseFragment;
import com.xh.hotme.databinding.ActivityTransferListBinding;
import com.xh.hotme.utils.ClickGuard;

import java.util.ArrayList;
import java.util.List;

/**
 * date   : 2023/6/22
 * desc   :传输文件
 */

public class TransferListActivity extends BaseActivity<ActivityTransferListBinding> {
    final List<BaseFragment<?>> mFragmentList = new ArrayList<>();
    final String[] titleName = new String[] { "下载", "上传" };

    final int _activeCatIndex = 0;

    @Override
    protected void initView() {
        viewBinding.title.tvTitle.setText("传输列表");
        viewBinding.title.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        viewBinding.viewPager.setOffscreenPageLimit(3);
        viewBinding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                viewBinding.tabs.setCurrentTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // setup pager
        viewBinding.viewPager.setAdapter(new VideoPagerAdapter(getSupportFragmentManager()));


        viewBinding.tabs.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(int position) {
                viewBinding.viewPager.setCurrentItem(position);
            }

            @Override
            public void onTabReselect(int position) {

            }
        });
    }

    @Override
    protected void initData() {
        mFragmentList.add(new DownloadListFragment());
        mFragmentList.add(new UploadListFragment());

        // add tab
        ArrayList<CustomTabEntity> tabEntities = new ArrayList<>();
        for(String s : titleName) {
            tabEntities.add(new TabEntity(s, 0, 0));
        }
        viewBinding.tabs.setTabData(tabEntities);

        // set to init cat index
        if(viewBinding.viewPager.getCurrentItem() != _activeCatIndex) {
            viewBinding.viewPager.setCurrentItem(_activeCatIndex, false);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
}
