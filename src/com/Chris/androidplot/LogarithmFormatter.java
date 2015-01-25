package com.Chris.androidplot;

import com.androidplot.xy.XYPlot;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.ui.SeriesRenderer;

/*
 * Class extended from AndroidPlot's LineAndPointFormatter.
 *
 * Formatter's instance is a place where can be stored parameters
 * that may be different for each individual series object (color,
 * line width and etc.).
 *
 * Series is an object where values are storred.
 *
 * This class allows to provide LogarithmRenderer to series of plot,
 * where it's used.
 */

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
