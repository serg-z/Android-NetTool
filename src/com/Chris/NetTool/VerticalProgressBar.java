package com.Chris.NetTool;

import android.widget.ProgressBar;

import android.content.Context;

import android.util.AttributeSet;

import android.graphics.Canvas;

public class VerticalProgressBar extends ProgressBar {
    private int x, y, z, w;

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
    }

    public VerticalProgressBar(Context context) {
        super(context);
    }

    public VerticalProgressBar(Context context, AttributeSet attributes, int defStyle) {
        super(context, attributes, defStyle);
    }

    public VerticalProgressBar(Context context, AttributeSet attributes) {
        super(context, attributes);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);

        this.x = w;
        this.y = h;
        this.z = oldw;
        this.w = oldh;
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);

        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas canvas) {
        canvas.rotate(-90);
        canvas.translate(-getHeight(), 0);

        super.onDraw(canvas);
    }

    @Override
    public synchronized void setProgress(int progress) {
        if (progress >= 0) {
            super.setProgress(progress);
        } else {
            super.setProgress(0);
        }

        onSizeChanged(x, y, z, w);
    }
}
