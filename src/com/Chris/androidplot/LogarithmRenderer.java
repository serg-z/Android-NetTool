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

public class LogarithmRenderer extends LineAndPointRenderer<LogarithmFormatter> {
    public LogarithmRenderer(XYPlot plot) {
        super(plot);
    }

    @Override
    protected void drawSeries(Canvas canvas, RectF plotArea, XYSeries series, LineAndPointFormatter formatter) {
        Path path = null;

        PointF thisPoint, firstPoint = null, lastPoint = null;

        Paint linePaint = formatter.getLinePaint();

        for (int i = 0; i < series.size(); ++i) {
            Number x = series.getX(i);
            Number y = series.getY(i);

            double yVal = y.doubleValue();

            if (yVal != 0.0) {
                y = Math.log10(yVal);
            }

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

            if (linePaint != null && thisPoint != null) {
                if (firstPoint == null) {
                    firstPoint = thisPoint;

                    path = new Path();

                    path.moveTo(firstPoint.x, firstPoint.y);
                }

                if (lastPoint != null) {
                    appendToPath(path, thisPoint, lastPoint);
                }

                lastPoint = thisPoint;
            } else {
                if (lastPoint != null) {
                    renderPath(canvas, plotArea, path, firstPoint, lastPoint, formatter);
                }

                firstPoint = null;
                lastPoint = null;
            }
        }

        if (firstPoint != null) {
            renderPath(canvas, plotArea, path, firstPoint, lastPoint, formatter);
        }
    }
}
