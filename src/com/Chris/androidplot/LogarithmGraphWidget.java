package com.Chris.androidplot;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;

import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;

import com.androidplot.util.PixelUtils;

/*
 * Extension of AndroidPlot's XYGraphWidget.
 *
 * Reimplements way of drawing ticks on Y-axis (range) and converts
 * labels on those ticks from logarithmic value to it's power of 10,
 * i.e. to pow(10, x).
 */

public class LogarithmGraphWidget extends XYGraphWidget {
    protected XYPlot mPlot;

    /* Constructor's implementation copied from super class. */

    public LogarithmGraphWidget(XYPlot plot) {
        super(
            plot.getLayoutManager(),
            plot,
            new SizeMetrics(
                PixelUtils.dpToPix(18), // DEFAULT_GRAPH_WIDGET_H_DP
                SizeLayoutType.FILL,
                PixelUtils.dpToPix(10), // DEFAULT_GRAPH_WIDGET_W_DP
                SizeLayoutType.FILL));

        mPlot = plot;

        Paint backgroundPaint = new Paint();

        backgroundPaint.setColor(Color.DKGRAY);
        backgroundPaint.setStyle(Paint.Style.FILL);

        setBackgroundPaint(backgroundPaint);

        position(
            PixelUtils.dpToPix(0), // DEFAULT_GRAPH_WIDGET_X_OFFSET_DP
            XLayoutStyle.ABSOLUTE_FROM_RIGHT,
            PixelUtils.dpToPix(0), // DEFAULT_GRAPH_WIDGET_Y_OFFSET_DP
            YLayoutStyle.ABSOLUTE_FROM_CENTER,
            AnchorPosition.RIGHT_MIDDLE);

        final int margin = 3;

        setMarginTop(PixelUtils.dpToPix(margin)); // DEFAULT_GRAPH_WIDGET_TOP_MARGIN_DP
        setMarginBottom(PixelUtils.dpToPix(margin)); // DEFAULT_GRAPH_WIDGET_BOTTOM_MARGIN_DP
        setMarginLeft(PixelUtils.dpToPix(margin)); // DEFAULT_GRAPH_WIDGET_LEFT_MARGIN_DP
        setMarginRight(PixelUtils.dpToPix(margin)); // DEFAULT_GRAPH_WIDGET_RIGHT_MARGIN_DP
    }

     /*
      * This function converts number on tick of Y axis from logarithm
      * to power of 10 and calls parent's draw function.
      */

    public void drawRangeTick(Canvas canvas, float yPix, Number yVal, Paint labelPaint, Paint linePaint,
            boolean drawLineOnly) {
        double yValD = yVal.doubleValue();

        if (yValD > 0.0) {
            yVal = Math.pow(10, yValD);
        }

        super.drawRangeTick(canvas, yPix, yVal, labelPaint, linePaint, drawLineOnly);
    }
}
