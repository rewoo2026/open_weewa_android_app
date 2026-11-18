package com.xh.hotme.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Created by changjinfu on 2016/9/5.
 */
public class MyRecyclerView extends RecyclerView {
    private LoadMoreInterface loadMoreInterface;
    /**
     * 当数据为空时展示的View
     */
    private View mEmptyView;

    public MyRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        addOnScrollListener(mOnScrollListener);
        ((SimpleItemAnimator) getItemAnimator()).setSupportsChangeAnimations(false);
    }

    public void setLoadMoreInterface(LoadMoreInterface loadMoreInterface) {
        this.loadMoreInterface = loadMoreInterface;
    }


    private final AdapterDataObserver emptyObserver = new AdapterDataObserver() {
        @SuppressLint("LongLogTag")
        @Override
        public void onChanged() {
            Adapter<?> adapter = getAdapter(); //这种写发跟之前我们之前看到的ListView的是一样的，判断数据为空否，再进行显示或者隐藏
            if(adapter != null && mEmptyView != null) {
                if(adapter.getItemCount() == 0) {
                    mEmptyView.setVisibility(View.VISIBLE);
                    MyRecyclerView.this.setVisibility(View.GONE);
                } else {
                    mEmptyView.setVisibility(View.GONE);
                    MyRecyclerView.this.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    /**
     * * @param emptyView 展示的空view
     */
    public void setEmptyView(View emptyView) {
        mEmptyView = emptyView;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        if(adapter != null) {
            //这里用了观察者模式，同时把这个观察者添加进去，
            // 至于这个模式怎么用，谷歌一下，不多讲了，因为这个涉及到了Adapter的一些原理，感兴趣可以点进去看看源码，还是受益匪浅的
            adapter.registerAdapterDataObserver(emptyObserver);
        }
        //当setAdapter的时候也调一次（实际上，经我粗略验证，不添加貌似也可以。不行就给添上呗，多大事嘛）
        emptyObserver.onChanged();
    }

    private final OnScrollListener mOnScrollListener = new OnScrollListener() {

        private int lastVisibleItem;
        private boolean refreshTag = true;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            lastVisibleItem = getLastVisiblePosition();
            refreshTag = dy <= 0;
        }

        /**
         * 获取最后一条展示的位置
         */
        private int getLastVisiblePosition() {
            int position;
            if(getLayoutManager() instanceof LinearLayoutManager) {
                position = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
            } else if(getLayoutManager() instanceof GridLayoutManager) {
                position = ((GridLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
            } else if(getLayoutManager() instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) getLayoutManager();
                int[] lastPositions =
                    layoutManager.findLastVisibleItemPositions(new int[layoutManager.getSpanCount()]);
                position = getMaxPosition(lastPositions);
            } else {
                position = getLayoutManager().getItemCount() - 1;
            }
            return position;
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if(!refreshTag && newState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItem + 1 == getAdapter().getItemCount()) {
                if(loadMoreInterface != null) {
                    loadMoreInterface.loadMore();
                }
            }
        }
    };

    /**
     * 获得最大的位置
     */
    private int getMaxPosition(int[] positions) {
        int size = positions.length;
        int maxPosition = Integer.MIN_VALUE;
        for(int position : positions) {
            maxPosition = Math.max(maxPosition, position);
        }
        return maxPosition;
    }

    public interface LoadMoreInterface {
        void loadMore();
    }
}
