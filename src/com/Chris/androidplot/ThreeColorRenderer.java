package com.Chris.androidplot;

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

public class ThreeColorRenderer extends LineAndPointRenderer<ThreeColorFormatter> {
    // cross product
    private static float cross(PointF a, PointF b) {
        return a.x * b.y - a.y * b.x;
    }

    // intersection point of two segments (or null if segments are collinear)
    private static PointF intersection(PointF a0, PointF a1, PointF b0, PointF b1) {
        PointF v0 = PixelUtils.sub(a1, a0);
        PointF v1 = PixelUtils.sub(b1, b0);

        float n = cross(PixelUtils.sub(a0, b0), v1);
        float m = cross(v1, v0);

        if (Math.abs(m) < 1e-8) {
            return null;
        }

        float u = n / m;

        if (Math.abs(u) < 1e-8) {
            return null;
        }

        v1.x *= u;
        v1.y *= u;

        return PixelUtils.add(b0, v1);
    }

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

        for (int i = 0; i < series.size(); ++i) {
            Number x = series.getX(i);
            Number y = series.getY(i);

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

            if (thisPoint != null) {
                if (lastPoint != null) {
                    for (int j = 0; j < 4; ++j) {
                        final float threshold;

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


                        if (a.y < threshold && b.y >= threshold) {
                            PointF p = intersection(
                                new PointF(lastPoint.x, threshold), new PointF(thisPoint.x, threshold),
                                lastPoint, thisPoint);

                            if (p != null) {
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

                                if (prevPath != path) {
                                    path.moveTo(lastPoint.x, lastPoint.y);

                                    pathFill.moveTo(lastPoint.x, plotArea.bottom);
                                    pathFill.lineTo(lastPoint.x, lastPoint.y);
                                }

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
