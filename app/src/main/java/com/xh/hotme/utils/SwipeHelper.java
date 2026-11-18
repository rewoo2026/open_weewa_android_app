package com.xh.hotme.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.HotmeApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class SwipeHelper extends ItemTouchHelper.SimpleCallback {
    public static final int BUTTON_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, HotmeApplication.getContext().getResources().getDisplayMetrics());
    private final RecyclerView recyclerView;
    private List<UnderlayButton> buttons;
    private final GestureDetector gestureDetector;
    private int swipedPos = -1;
    private float swipeThreshold = 0.5f;
    private final Map<Integer, List<UnderlayButton>> buttonsBuffer;
    private final Queue<Integer> recoverQueue;
    private final Paint _clearPaint;
    private final int _swipableItemType;
    private ItemTouchHelper _itemTouchHelper;

    private class ItemState {
        public float startItemX = 0;
        public float startDx = 0;
        public boolean recovering = false;
    }

    private final Map<Integer, ItemState> _itemStates;

    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            for(UnderlayButton button : buttons) {
                if(button.onClick(e.getX(), e.getY()))
                    break;
            }

            return true;
        }
    };

    private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent e) {
            if(swipedPos < 0) return false;
            Point point = new Point((int) e.getRawX(), (int) e.getRawY());

            RecyclerView.ViewHolder swipedViewHolder = recyclerView.findViewHolderForAdapterPosition(swipedPos);
            if(swipedViewHolder != null) {
                View swipedItem = swipedViewHolder.itemView;
                Rect rect = new Rect();
                swipedItem.getGlobalVisibleRect(rect);

                if(e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_MOVE) {
                    if(rect.top < point.y && rect.bottom > point.y) {
                        gestureDetector.onTouchEvent(e);
                    } else {
                        recoverQueue.add(swipedPos);
                        swipedPos = -1;
                        recoverSwipedItem();
                    }
                }
            }
            return false;
        }
    };

    public SwipeHelper(Context context, RecyclerView recyclerView, int itemType) {
        super(0, ItemTouchHelper.LEFT);
        this.recyclerView = recyclerView;
        this.buttons = new ArrayList<>();
        _swipableItemType = itemType;
        this.gestureDetector = new GestureDetector(context, gestureListener);
        this.recyclerView.setOnTouchListener(onTouchListener);
        buttonsBuffer = new HashMap<>();
        _itemStates = new HashMap<>();
        _clearPaint = new Paint();
        _clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        recoverQueue = new LinkedList<Integer>() {
            @Override
            public boolean add(Integer o) {
                if(contains(o))
                    return false;
                else
                    return super.add(o);
            }
        };

        attachSwipe();
    }

    public boolean hasSwiped() {
        return swipedPos != -1;
    }

    public void clearSwiped() {
        swipedPos = -1;
        recoverQueue.clear();
    }

    public void recoverSwiped() {
        recoverQueue.add(swipedPos);
        swipedPos = -1;
        recoverSwipedItem();
    }

    private ItemState getItemState(int pos) {
        ItemState st = _itemStates.get(pos);
        if(st == null) {
            st = new ItemState();
            _itemStates.put(pos, st);
        }
        return st;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();

        if(swipedPos != pos)
            recoverQueue.add(swipedPos);

        swipedPos = pos;

        if(buttonsBuffer.containsKey(swipedPos))
            buttons = buttonsBuffer.get(swipedPos);
        else
            buttons.clear();

        buttonsBuffer.clear();
        swipeThreshold = 0.5f * buttons.size() * BUTTON_WIDTH;
        recoverSwipedItem();
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return swipeThreshold;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return 0.1f * defaultValue;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return 5.0f * defaultValue;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        int pos = viewHolder.getBindingAdapterPosition();
        float translationX = dX;
        View itemView = viewHolder.itemView;

        // if not video, don't draw buttons
        if(viewHolder.getItemViewType() != _swipableItemType) {
            super.onChildDraw(c, recyclerView, viewHolder, 0, 0, actionState, isCurrentlyActive);
            return;
        }

        // 他在做recover的时候, dX不会变, 所以需要根据itemview的起始便宜动态调整dX, 不然画出来效果不对
        ItemState st = getItemState(pos);
        if(st.recovering) {
            if(st.startItemX == 0f) {
                st.startItemX = itemView.getTranslationX();
                st.startDx = dX;
            }
            dX = st.startDx * itemView.getTranslationX() / st.startItemX;
            if(-dX <= 1) {
                st.recovering = false;
                st.startItemX = 0f;
                _itemTouchHelper.pendingRemoveAnimation(viewHolder);
            }
        }

        if(pos < 0) {
            swipedPos = pos;
            return;
        }

        if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if(dX < 0) {
                List<UnderlayButton> buffer = new ArrayList<>();
                if(!buttonsBuffer.containsKey(pos)) {
                    instantiateUnderlayButton(viewHolder, buffer);
                    buttonsBuffer.put(pos, buffer);
                } else {
                    buffer = buttonsBuffer.get(pos);
                }

                translationX = dX * buffer.size() * BUTTON_WIDTH / itemView.getWidth();
                drawButtons(c, itemView, buffer, pos, translationX);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive);
    }

    private synchronized void recoverSwipedItem() {
        while(!recoverQueue.isEmpty()) {
            int pos = recoverQueue.poll();
            if(pos > -1) {
                ItemState st = getItemState(pos);
                st.recovering = true;
                recyclerView.getAdapter().notifyItemChanged(pos);
            }
        }
    }

    private void drawButtons(Canvas c, View itemView, List<UnderlayButton> buffer, int pos, float dX) {
        float right = itemView.getRight();
        float dButtonWidth = (-1) * dX / buffer.size();

        for(UnderlayButton button : buffer) {
            float left = right - dButtonWidth;
            button.onDraw(
                c,
                new RectF(
                    left,
                    itemView.getTop(),
                    right,
                    itemView.getBottom()
                ),
                pos
            );

            right = left;
        }
    }

    public void attachSwipe() {
        _itemTouchHelper = new ItemTouchHelper(this);
        _itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public abstract void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons);

    public static class UnderlayButton {
        private final String text;
        private final int imageResId;
        private final int color;
        private int pos;
        private RectF clickRegion;
        private final UnderlayButtonClickListener clickListener;

        public UnderlayButton(String text, int imageResId, int color, UnderlayButtonClickListener clickListener) {
            this.text = text;
            this.imageResId = imageResId;
            this.color = color;
            this.clickListener = clickListener;
        }

        public boolean onClick(float x, float y) {
            if(clickRegion != null && clickRegion.contains(x, y)) {
                clickListener.onClick(pos);
                return true;
            }

            return false;
        }

        public void onDraw(Canvas c, RectF rect, int pos) {
            Paint p = new Paint();

            // Draw background
            p.setColor(color);
            c.drawRect(rect, p);

            // Draw Text
            p.setColor(Color.WHITE);
            p.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, HotmeApplication.getContext().getResources().getDisplayMetrics()));

            Rect r = new Rect();
            float cHeight = rect.height();
            float cWidth = rect.width();
            p.setTextAlign(Paint.Align.LEFT);
            p.getTextBounds(text, 0, text.length(), r);
            float x = cWidth / 2f - r.width() / 2f - r.left;
            float y = cHeight / 2f + r.height() / 2f - r.bottom;
            c.drawText(text, rect.left + x, rect.top + y, p);

            clickRegion = rect;
            this.pos = pos;
        }
    }

    public interface UnderlayButtonClickListener {
        void onClick(int pos);
    }
}