package com.Chris.androidplot;

import com.Chris.util.Util;

import android.graphics.Color;
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
import com.androidplot.util.PixelUtils;

/*
 * ThreeColorRenderer is used to render three-colored plots
 * (currently the colors are set to green, yellow and red - from
 * the top to the bottom).
 *
 * The path is broken into separate paths (one for each color)
 * depending on it's intersection with threshold line. Afterwards
 * each path is rendered with it's own line and fill color.
 */

public class ThreeColorRenderer extends LineAndPointRenderer<ThreeColorFormatter> {
    private Paint mPaintLineGreen;
    private Paint mPaintLineYellow;
    private Paint mPaintLineRed;
    private Paint mPaintFillGreen;
    private Paint mPaintFillYellow;
    private Paint mPaintFillRed;

    public ThreeColorRenderer(XYPlot plot) {
        super(plot);

        // line paint

        mPaintLineGreen = new Paint();
        mPaintLineYellow = new Paint();
        mPaintLineRed = new Paint();

        final float lineStrokeWidthDp = 1.5f;

        mPaintLineGreen.setColor(Color.GREEN);
        mPaintLineGreen.setStrokeWidth(PixelUtils.dpToPix(lineStrokeWidthDp));
        mPaintLineGreen.setStyle(Paint.Style.STROKE);
        mPaintLineGreen.setAntiAlias(true);

        mPaintLineYellow.setColor(Color.YELLOW);
        mPaintLineYellow.setStrokeWidth(PixelUtils.dpToPix(lineStrokeWidthDp));
        mPaintLineYellow.setStyle(Paint.Style.STROKE);
        mPaintLineYellow.setAntiAlias(true);

        mPaintLineRed.setColor(Color.RED);
        mPaintLineRed.setStrokeWidth(PixelUtils.dpToPix(lineStrokeWidthDp));
        mPaintLineRed.setStyle(Paint.Style.STROKE);
        mPaintLineRed.setAntiAlias(true);

        // fill paint

        final int alpha = 200;

        mPaintFillGreen = new Paint();
        mPaintFillYellow = new Paint();
        mPaintFillRed = new Paint();

        mPaintFillGreen.setARGB(alpha, 100, 200, 100);
        mPaintFillGreen.setAntiAlias(true);

        mPaintFillYellow.setARGB(alpha, 200, 200, 100);
        mPaintFillYellow.setAntiAlias(true);

        mPaintFillRed.setARGB(alpha, 200, 100, 100);
        mPaintFillRed.setAntiAlias(true);
    }

    @Override
    protected void drawSeries(Canvas canvas, RectF plotArea, XYSeries series, LineAndPointFormatter formatter) {
        PointF thisPoint;
        PointF lastPoint = null;
        PointF firstPoint = null;

        Path pathGreen = null, pathYellow = null, pathRed = null;
        Path pathFillGreen = null, pathFillYellow = null, pathFillRed = null;

        Path path = null, pathFill = null;

        Path prevPath = null, prevPathFill = null;

        final Number minX = getPlot().getCalculatedMinX();
        final Number maxX = getPlot().getCalculatedMaxX();
        final Number minY = getPlot().getCalculatedMinY();
        final Number maxY = getPlot().getCalculatedMaxY();

        // convert thresholds retrieved from formatter to pixel units

        final float thresholdGreenPix = ValPixConverter.valToPix(
            0.0f,
            ((ThreeColorFormatter)formatter).getThresholdGreen(),
            plotArea,
            minX,
            maxX,
            minY,
            maxY).y;

        final float thresholdYellowPix = ValPixConverter.valToPix(
            0.0f,
            ((ThreeColorFormatter)formatter).getThresholdYellow(),
            plotArea,
            minX,
            maxX,
            minY,
            maxY).y;

        // loop through values of series
        for (int i = 0; i < series.size(); ++i) {
            Number x = series.getX(i);
            Number y = series.getY(i);

            // convert coordinates of point to pixel units
            if (y != null && x != null) {
                thisPoint = ValPixConverter.valToPix(
                    x,
                    y,
                    plotArea,
                    minX,
                    maxX,
                    minY,
                    maxY);
            } else {
                thisPoint = null;
            }

            // we can draw this point
            if (thisPoint != null) {
                // the path wasn't broken and we have second point for line
                if (lastPoint != null) {
                    // loop through possible variations of intersection:
                    // 0 - line from lastPoint to thisPoint with threshold green
                    // 1 - line from lastPoint to thisPoint with threshold yellow
                    // 2 - line from thisPoint to lastPoint with threshold yellow
                    // 3 - line from thisPoint to lastPoint with threshold green
                    for (int j = 0; j < 4; ++j) {
                        final float threshold;

                        // which threshold should we use for intersection check
                        switch (j) {
                            case 0:
                            case 3:
                                threshold = thresholdGreenPix;

                                break;

                            case 1:
                            case 2:
                                threshold = thresholdYellowPix;

                                break;

                            default:
                                throw new UnsupportedOperationException("Not implemented");
                        }

                        final PointF a, b;

                        // which way the line goes
                        switch (j) {
                            case 0:
                            case 1:
                                a = lastPoint;
                                b = thisPoint;

                                break;

                            case 2:
                            case 3:
                                a = thisPoint;
                                b = lastPoint;

                                break;

                            default:
                                throw new UnsupportedOperationException("Not implemented");
                        }


                        // detect intersection and cut the path into two pieces - one segment with already defined
                        // color and another for further intersection check
                        if (a.y < threshold && b.y >= threshold) {
                            // get intersection point where the segment should split into two
                            PointF p = Util.intersection(
                                new PointF(lastPoint.x, threshold), new PointF(thisPoint.x, threshold),
                                lastPoint, thisPoint);

                            if (p != null) {
                                // select suitable path where new segment will be added
                                switch (j) {
                                    case 0:
                                        {
                                            if (pathGreen == null) {
                                                pathGreen = new Path();
                                                pathFillGreen = new Path();
                                            }

                                            path = pathGreen;
                                            pathFill = pathFillGreen;
                                        }

                                        break;

                                    case 1:
                                    case 3:
                                        {
                                            if (pathYellow == null) {
                                                pathYellow = new Path();
                                                pathFillYellow = new Path();
                                            }

                                            path = pathYellow;
                                            pathFill = pathFillYellow;
                                        }

                                        break;

                                    case 2:
                                        {
                                            if (pathRed == null) {
                                                pathRed = new Path();
                                                pathFillRed = new Path();
                                            }

                                            path = pathRed;
                                            pathFill = pathFillRed;
                                        }

                                        break;

                                    default:
                                        throw new UnsupportedOperationException("Not implemented");
                                }

                                // add vertical line from bottom to current position if the path wasn't used before
                                if (prevPath != path) {
                                    path.moveTo(lastPoint.x, lastPoint.y);

                                    pathFill.moveTo(lastPoint.x, plotArea.bottom);
                                    pathFill.lineTo(lastPoint.x, lastPoint.y);
                                }

                                // add new segment to path and close fill path with vertical line going from
                                // segment's end to the bottom
                                appendToPath(path, p, lastPoint);
                                appendToPath(pathFill, p, lastPoint);

                                pathFill.lineTo(p.x, plotArea.bottom);

                                lastPoint = p;

                                prevPath = path;
                            }
                        }
                    }
                }

                if (thisPoint.y < thresholdGreenPix) {
                    if (pathGreen == null) {
                        pathGreen = new Path();
                        pathFillGreen = new Path();
                    }

                    path = pathGreen;
                    pathFill = pathFillGreen;
                } else if (thisPoint.y < thresholdYellowPix) {
                    if (pathYellow == null) {
                        pathYellow = new Path();
                        pathFillYellow = new Path();
                    }

                    path = pathYellow;
                    pathFill = pathFillYellow;
                } else {
                    if (pathRed == null) {
                        pathRed = new Path();
                        pathFillRed = new Path();
                    }

                    path = pathRed;
                    pathFill = pathFillRed;
                }

                if (firstPoint == null) {
                    firstPoint = thisPoint;

                    path.moveTo(firstPoint.x, firstPoint.y);

                    pathFill.moveTo(firstPoint.x, plotArea.bottom);
                    pathFill.lineTo(firstPoint.x, firstPoint.y);
                }

                if (lastPoint != null) {
                    if (prevPath != null && prevPath != path) {
                        path.moveTo(lastPoint.x, lastPoint.y);

                        prevPathFill.lineTo(lastPoint.x, plotArea.bottom);

                        pathFill.moveTo(lastPoint.x, plotArea.bottom);
                        pathFill.lineTo(lastPoint.x, lastPoint.y);
                    }

                    appendToPath(path, thisPoint, lastPoint);
                    appendToPath(pathFill, thisPoint, lastPoint);
                }

                lastPoint = thisPoint;
            } else {
                if (lastPoint != null) {
                    if (pathGreen != null) {
                        drawPaths(canvas, plotArea, firstPoint, lastPoint, pathGreen, pathFillGreen, mPaintLineGreen,
                            mPaintFillGreen);
                    }

                    if (pathYellow != null) {
                        drawPaths(canvas, plotArea, firstPoint, lastPoint, pathYellow, pathFillYellow, mPaintLineYellow,
                            mPaintFillYellow);
                    }

                    if (pathRed != null) {
                        drawPaths(canvas, plotArea, firstPoint, lastPoint, pathRed, pathFillRed, mPaintLineRed,
                            mPaintFillRed);
                    }
                }

                firstPoint = null;
                lastPoint = null;
            }

            prevPath = path;
            prevPathFill = pathFill;
        }

        if (firstPoint != null) {
            if (pathGreen != null) {
                drawPaths(canvas, plotArea, firstPoint, lastPoint, pathGreen, pathFillGreen, mPaintLineGreen,
                    mPaintFillGreen);
            }

            if (pathYellow != null) {
                drawPaths(canvas, plotArea, firstPoint, lastPoint, pathYellow, pathFillYellow, mPaintLineYellow,
                    mPaintFillYellow);
            }

            if (pathRed != null) {
                drawPaths(canvas, plotArea, firstPoint, lastPoint, pathRed, pathFillRed, mPaintLineRed,
                    mPaintFillRed);
            }
        }
    }

    private void drawPaths(Canvas canvas, RectF plotArea, PointF firstPoint, PointF lastPoint, Path pathLine,
            Path pathFill, Paint paintLine, Paint paintFill) {
        pathFill.lineTo(lastPoint.x, plotArea.bottom);
        pathFill.lineTo(firstPoint.x, plotArea.bottom);
        pathFill.close();

        canvas.drawPath(pathFill, paintFill);
        canvas.drawPath(pathLine, paintLine);
    }
}
