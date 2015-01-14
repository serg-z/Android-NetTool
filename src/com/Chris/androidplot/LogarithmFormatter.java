package com.Chris.androidplot;

import com.androidplot.xy.XYPlot;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.ui.SeriesRenderer;

public class LogarithmFormatter extends LineAndPointFormatter {
    public LogarithmFormatter(Integer lineColor, Integer fillColor) {
        super(lineColor, null, fillColor, null);
    }

    @Override
    public Class<? extends SeriesRenderer> getRendererClass() {
        return LogarithmRenderer.class;
    }

    @Override
    public SeriesRenderer getRendererInstance(XYPlot plot) {
        return new LogarithmRenderer(plot);
    }
}
