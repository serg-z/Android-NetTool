package com.Chris.NetTool;

import android.widget.SeekBar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Rect;

import android.content.Context;

import android.util.Log;

public class Slider extends SeekBar implements SeekBar.OnSeekBarChangeListener {
    private int mMin = 0;
    private int mStep = 0;
    private String mLabel = "";

    private Paint mTextPaint;
    private Rect mTextBounds;

    public Slider(Context context) {
        super(context);

        setOnSeekBarChangeListener(this);

        setPadding(getThumbOffset(), 25, getThumbOffset(), 25);

        // TODO: fix thumb's position at start
        setThumb(null);

        mTextPaint = new Paint();
        mTextBounds = new Rect();
    }

    public synchronized void setLabel(String label) {
        mLabel = label;
    }

    public synchronized String getLabel() {
        return mLabel;
    }

    public synchronized void setMin(int min) {
        mMin = min;

        int newMax = super.getMax() - mMin;

        if (newMax < 0) {
            newMax = 0;
        }

        super.setMax(newMax);
    }

    public synchronized int getMin() {
        return mMin;
    }

    @Override
    public synchronized void setMax(int max) {
        super.setMax(max - mMin);
    }

    @Override
    public synchronized int getMax() {
        return super.getMax() + mMin;
    }

    public synchronized void setStep(int step) {
        mStep = step;
    }

    public synchronized int getStep() {
        return mStep;
    }

    public synchronized int getAdjustedProgress() {
        return getProgress() + mMin;
    }

    public synchronized void setAdjustedProgress(int progress) {
        setProgress(progress - mMin);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(20);

        // label

        String text = mLabel;

        mTextPaint.getTextBounds(text, 0, text.length(), mTextBounds);

        int x = 0;
        int y = mTextBounds.height();

        canvas.drawText(text, x, y, mTextPaint);

        // value

        text = String.valueOf(getAdjustedProgress());

        mTextPaint.getTextBounds(text, 0, text.length(), mTextBounds);

        x = getWidth() / 2 - mTextBounds.width();
        y = getHeight() / 2 - mTextBounds.centerY();

        canvas.drawText(text, x, y, mTextPaint);

        // step labels

        /*
        if (isStepEffective()) {
            x = getThumbOffset();

            int interval = (getWidth() - getThumbOffset() * 2) / ((getMax() - mMin) / mStep);

            for (int i = mMin; i <= getMax(); i += mStep) {
                text = String.valueOf(i);

                mTextPaint.getTextBounds(text, 0, text.length(), mTextBounds);

                // TODO: after thumb's fixed, return to 40
                y = getHeight() / 2 + 30;

                canvas.drawText(text, x - mTextBounds.centerX(), y, mTextPaint);

                x += interval;
            }
        }
        */
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (isStepEffective()) {
            progress = ((int)Math.round(progress / mStep)) * mStep;

            seekBar.setProgress(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private boolean isStepEffective() {
        return mStep > 0 && getMax() - mMin >= mStep;
    }
}
