package com.Chris.androidplot;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Canvas;

import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;

import com.androidplot.util.ValPixConverter;

/*
 * LogarithmRenderer class is used for rendering plots in logarithmic
 * scale.
 */

public class LogarithmRenderer extends LineAndPointRenderer<LogarithmFormatter> {
    private Path mPath = new Path();

    public LogarithmRenderer(XYPlot plot) {
        super(plot);
    }

    @Override
    protected void drawSeries(Canvas canvas, RectF plotArea, XYSeries series, LineAndPointFormatter formatter) {
        PointF thisPoint, firstPoint = null, lastPoint = null;

        Paint linePaint = formatter.getLinePaint();

        for (int i = 0; i < series.size(); ++i) {
            Number x = series.getX(i);
            Number y = series.getY(i);

            double yVal = y.doubleValue();

            if (yVal != 0.0) {
                // convert the value to base 10 logarithm
                y = Math.log10(yVal);
            }

            // convert coordinates of point to pixel units
            if (y != null && x != null) {
                thisPoint = ValPixConverter.valToPix(
                    x,
                    y,
                    plotArea,
                    getPlot().getCalculatedMinX(),
                    getPlot().getCalculatedMaxX(),
                    getPlot().getCalculatedMinY(),
                    getPlot().getCalculatedMaxY());
            } else {
                thisPoint = null;
            }

            // if we have paint and point - we can draw this point
            if (linePaint != null && thisPoint != null) {
                // this is the first point
                if (firstPoint == null) {
                    firstPoint = thisPoint;

                    // set up path's starting point
                    mPath.moveTo(firstPoint.x, firstPoint.y);
                }

                // the path wasn't broken - append new point to it
                if (lastPoint != null) {
                    appendToPath(mPath, thisPoint, lastPoint);
                }

                lastPoint = thisPoint;
            } else {
                // if we've added any points - render the path
                if (lastPoint != null) {
                    renderPath(canvas, plotArea, mPath, firstPoint, lastPoint, formatter);
                }

                firstPoint = null;
                // indicate that the path was broken
                lastPoint = null;
            }
        }

        // if we had any points - render the path
        if (firstPoint != null) {
            // mPath will be rewinded at the end of "renderPath"
            renderPath(canvas, plotArea, mPath, firstPoint, lastPoint, formatter);
        }
    }
}
