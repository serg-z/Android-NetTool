package com.Chris.NetTool;

import android.widget.SeekBar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Rect;

import android.content.Context;

import android.util.Log;

public class Slider extends SeekBar implements SeekBar.OnSeekBarChangeListener {
    int mMin = 0;
    int mStep = 0;
    String mLabel = "";

    public Slider(Context context) {
        super(context);

        setOnSeekBarChangeListener(this);

        setPadding(getThumbOffset(), 25, getThumbOffset(), 25);

        // TODO: fix thumb's position at start
        setThumb(null);
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

        Paint textPaint = new Paint();

        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20);

        Rect bounds = new Rect();

        // label

        String text = mLabel;

        textPaint.getTextBounds(text, 0, text.length(), bounds);

        int x = 0;
        int y = bounds.height();

        canvas.drawText(text, x, y, textPaint);

        // value

        text = String.valueOf(getAdjustedProgress());

        textPaint.getTextBounds(text, 0, text.length(), bounds);

        x = getWidth() / 2 - bounds.width();
        y = getHeight() / 2 - bounds.centerY();

        canvas.drawText(text, x, y, textPaint);

        // step labels

        if (isStepEffective()) {
            x = getThumbOffset();

            int interval = (getWidth() - getThumbOffset() * 2) / ((getMax() - mMin) / mStep);

            for (int i = mMin; i <= getMax(); i += mStep) {
                text = String.valueOf(i);

                textPaint.getTextBounds(text, 0, text.length(), bounds);

                // TODO: after thumb's fixed, return to 40
                y = getHeight() / 2 + 30;

                canvas.drawText(text, x - bounds.centerX(), y, textPaint);

                x += interval;
            }
        }
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
