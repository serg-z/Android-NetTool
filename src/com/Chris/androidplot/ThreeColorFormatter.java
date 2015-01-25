package com.Chris.androidplot;

import com.androidplot.xy.XYPlot;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.ui.SeriesRenderer;

/*
 * Extended from AndroidPlot's LineAndPointFormatter this class
 * provides renderer for drawing three-colored plots and allows
 * to specify thresholds for Y-values where the colors should change.
 *
 * Formatter's instance is a place where can be stored parameters
 * that may be different for each individual series object (color,
 * line width and etc.).
 *
 * Series is an object where values are storred.
 */

public class ThreeColorFormatter extends LineAndPointFormatter {
    private int mThresholdGreen;
    private int mThresholdYellow;

    /*
     * Thresholds are passed to constructor and currently it's the
     * only way to specify them.
     */

    public ThreeColorFormatter(int thresholdGreen, int thresholdYellow) {
        super();

        mThresholdGreen = thresholdGreen;
        mThresholdYellow = thresholdYellow;
    }

    public int getThresholdGreen() {
        return mThresholdGreen;
    }

    public int getThresholdYellow() {
        return mThresholdYellow;
    }

    @Override
    public Class<? extends SeriesRenderer> getRendererClass() {
        return ThreeColorRenderer.class;
    }

    @Override
    public SeriesRenderer getRendererInstance(XYPlot plot) {
        return new ThreeColorRenderer(plot);
    }
}
