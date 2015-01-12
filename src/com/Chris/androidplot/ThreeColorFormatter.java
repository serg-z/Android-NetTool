package com.Chris.androidplot;

import com.androidplot.xy.XYPlot;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.ui.SeriesRenderer;

public class ThreeColorFormatter extends LineAndPointFormatter {
    private int mThresholdGreen;
    private int mThresholdYellow;

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
