package com.xh.hotme.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.xh.hotme.R;
import com.xh.hotme.utils.DensityUtil;


public class OvalImageView extends androidx.appcompat.widget.AppCompatImageView {
    public static final int TYPE_ALL = 0;
    public static final int TYPE_ONLY_TOP_LEFT = 1;
    public static final int TYPE_ONLY_BOTTOM = 2;
    public static final int TYPE_ONLY_TOP = 3;
    private float[] rids;
    private Paint m_paint;
    private int ridType = TYPE_ALL;
    private float radius;
    private int boderColor = 0xffe6e6e6;
    private float edgeWidth = 2;
    private boolean hasBoder = true;
    private boolean isCircle;
    private int wRatio = -1;
    private int hRatio = -1;

    public OvalImageView(Context context) {
        super(context);
        radius = DensityUtil.dip2px(10);
        initView();
    }

    public OvalImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OvalImageView);
        if(typedArray != null) {
            ridType = typedArray.getInt(R.styleable.OvalImageView_rid_type, TYPE_ALL);
            isCircle = typedArray.getBoolean(R.styleable.OvalImageView_circle, false);
            hasBoder = typedArray.getBoolean(R.styleable.OvalImageView_has_boder, false);
            radius = typedArray.getDimension(R.styleable.OvalImageView_oval_radius, DensityUtil.dip2px(10));
            boderColor = typedArray.getColor(R.styleable.OvalImageView_color_edge, 0xffe6e6e6);
            edgeWidth = typedArray.getDimension(R.styleable.OvalImageView_width_edge, 2);
            wRatio = typedArray.getColor(R.styleable.OvalImageView_wRatio, -1);
            hRatio = typedArray.getColor(R.styleable.OvalImageView_hRatio, -1);
            typedArray.recycle();
        }
        initView();
    }

    public void setIsCircle(boolean circle) {
        isCircle = circle;
        postInvalidate();
    }

    public void setBoderColor(int color) {
        boderColor = color;
        m_paint.setColor(boderColor);
        postInvalidate();
    }

    private void initView() {
        setScaleType(ScaleType.CENTER_CROP);
        m_paint = new Paint();
        m_paint.setAntiAlias(true);
        m_paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(edgeWidth);
        m_paint.setColor(boderColor);
//        m_paint.setColor(0xffff0000);

        setRadius(radius);
    }

    public void setRadius(float radius) {
        this.radius = radius;
        float rid = radius;
        switch(ridType) {
            case TYPE_ALL:
                rids = new float[] { rid, rid, rid, rid, rid, rid, rid, rid };
                break;
            case TYPE_ONLY_TOP_LEFT:
                rids = new float[] { rid, rid, 0, 0, 0, 0, 0, 0 };
                break;
            case TYPE_ONLY_TOP:
                rids = new float[] { rid, rid, rid, rid, 0, 0, 0, 0 };
                break;
            case TYPE_ONLY_BOTTOM:
                rids = new float[] { 0, 0, 0, 0, rid, rid, rid, rid };
                break;
        }
    }

    //    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        setMeasuredDimension(width, height);
//    }

    /**
     * 画图
     *
     * @param canvas
     */
    protected void onDraw(Canvas canvas) {
        //禁用硬件加速
//        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        Path path = new Path();
        int w = this.getWidth();
        int h = this.getHeight();
        if(isCircle) {
            path.addCircle(w / 2, h / 2, w / 2, Path.Direction.CW);
        } else {
            path.addRoundRect(new RectF(0, 0, w, h), rids, Path.Direction.CW);
        }
        canvas.clipPath(path);
        super.onDraw(canvas);

        if(hasBoder) {
            if(TYPE_ALL == ridType || TYPE_ONLY_TOP == ridType) {
//            canvas.drawPath(path, m_paint);//小米 绘制gif有白边
                if(isCircle) {
                    canvas.drawCircle(w / 2, h / 2, w / 2, m_paint);
                } else {
                    Path path1 = new Path();
                    path1.arcTo(new RectF(0, 0, 2 * radius, 2 * radius), 180, 90);
                    path1.arcTo(new RectF(w - 2 * radius, 0, w, 2 * radius), 270, 90);
                    path1.arcTo(new RectF(w - 2 * radius, h - 2 * radius, w, h), 0, 90);
                    path1.arcTo(new RectF(0, h - 2 * radius, 2 * radius, h), 90, 90);
                    path1.close();
                    canvas.drawPath(path1, m_paint);
                }
            }
        }
    }

    public void setwhRatio(int wRatio, int hRatio) {
        this.wRatio = wRatio;
        this.hRatio = hRatio;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(wRatio != -1 && hRatio != -1) {
            final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
//            final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(widthSize, Math.round(widthSize * hRatio / wRatio));
        }
    }
}