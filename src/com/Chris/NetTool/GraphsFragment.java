package com.Chris.NetTool;

import com.Chris.util.Util;

import com.Chris.androidplot.ThreeColorFormatter;
import com.Chris.androidplot.LogarithmFormatter;
import com.Chris.androidplot.LogarithmGraphWidget;

import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.AsyncTask;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;

import android.view.ViewGroup.LayoutParams;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Context;

import android.content.res.Configuration;

import android.media.ToneGenerator;

import android.net.DhcpInfo;
import android.net.TrafficStats;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.ScanResult;

import android.text.format.Formatter;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.DashPathEffect;

import android.util.Log;
import android.util.SparseIntArray;

import android.support.v4.app.Fragment;

import java.text.DecimalFormat;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import com.androidplot.Plot;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.StepFormatter;
import com.androidplot.xy.XYStepMode;
import com.androidplot.xy.XYLegendWidget;
import com.androidplot.xy.YValueMarker;

import com.androidplot.ui.XPositionMetric;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.SizeMetric;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.TableOrder;

import com.androidplot.util.PixelUtils;

/*
 * A Fragment represents a behavior or a portion of user interface in an Activity.
 *
 * GraphsFragment is the fragment which contains all the plots and Wi-Fi related information.
 */

public class GraphsFragment extends Fragment {
    private static final String TAG = "GraphsFragment";
    // maximum count of X values on the plots
    private static final int HISTORY_SIZE = 120;

    /* Interface used to notify others about obtained Wi-Fi information */
    public interface OnWifiInfoListener {
        public void onServerAddressObtained(int address);
    }

    /* Interface used to controll ping */
    public interface OnPingListener {
        public void onPingStart(String address);
        public void onPingStop();
    }

    private OnWifiInfoListener mWifiInfoCallback;
    private OnPingListener mPingCallback;
    private SettingsFragment.OnLogListener mLogCallback;

    private Handler mTimerHandler = new Handler();

    /* This runnable runs every second and updates the UI (Wi-Fi info and plots) */
    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();

            // post this runnable to be run again
            mTimerHandler.postDelayed(this, 1000);
        }
    };

    private WifiManager mWifiManager;

    private XYPlot mPlotRssi, mPlotLinkSpeed, mPlotRxTx;
    private SimpleXYSeries mSeriesRx, mSeriesTx;

    private int mPingAddress = 0;
    private PingTask mPingTask;
    private XYPlot mPlotPing;
    private SimpleXYSeries mSeriesPingSuccess, mSeriesPingFail;

    private XYPlot mPlotStreamer;
    private SimpleXYSeries mSeriesStreamerDownloadingProgress, mSeriesStreamerBufferDepth;
    private int mStreamerDownloadingProgress = 0, mStreamerBufferDepth = 0;

    private XYPlot mPlotChunkDownloadTime;
    private boolean mPlotChunkAddZeroes = true;

    private Activity mActivity;

    private long mLastRx = -1, mLastTx = -1;
    private String mPingResumeAddress;
    private boolean mPingShouldResume = false;
    private SparseIntArray mPingNoAnswer = new SparseIntArray();
    // ping will add 8 bytes on top of that value
    private int mPingPacketSize = 64 - 8;

    /* Prevents runnable, which updates the UI, from running */

    private void pauseTimer() {
        // pause runnable
        mTimerHandler.removeCallbacks(mTimerRunnable);

        // reset previous rx/tx results
        mLastRx = -1;
        mLastTx = -1;
    }

    private void resumeTimer() {
        mTimerHandler.postDelayed(mTimerRunnable, 0);
    }

    private void updateUI() {
        // get Wi-Fi connection and DHCP info
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();

        String bssid = wifiInfo.getBSSID();

        boolean freqNotFound = true;

        // loop through Wi-Fi scan results...
        for (ScanResult r : mWifiManager.getScanResults()) {
            // ...and search for current connection's BSSID
            if (r.BSSID.equals(bssid)) {
                // get from it frequency
                String text = r.frequency + " MHz";

                // convert it to corresponding channel number
                Integer channel = Frequencies.sChannels.get(r.frequency);

                // if it's defined - append it to the text after frequency
                if (channel != null) {
                    text += " (ch " + channel + ")";
                }

                ((TextView)mActivity.findViewById(R.id.text_frequency)).setText(text);

                freqNotFound = false;

                // TODO: break from loop
            }
        }

        if (freqNotFound) {
            ((TextView)mActivity.findViewById(R.id.text_frequency)).setText("not found");
        }

        // read Wi-Fi connection information and put it into text views

        int rssi = wifiInfo.getRssi();
        int linkSpeed = wifiInfo.getLinkSpeed();
        int serverAddress = dhcpInfo.serverAddress;

        ((TextView)mActivity.findViewById(R.id.text_mac)).setText(wifiInfo.getMacAddress());
        ((TextView)mActivity.findViewById(R.id.text_local_ip)).setText(
            Formatter.formatIpAddress(wifiInfo.getIpAddress()));
        ((TextView)mActivity.findViewById(R.id.text_ssid)).setText(wifiInfo.getSSID());
        ((TextView)mActivity.findViewById(R.id.text_bssid)).setText(bssid);
        ((TextView)mActivity.findViewById(R.id.text_server_address)).setText(Formatter.formatIpAddress(serverAddress));
        ((TextView)mActivity.findViewById(R.id.text_rssi)).setText(rssi + " dBm");
        ((TextView)mActivity.findViewById(R.id.text_link_speed)).setText(linkSpeed + " " + WifiInfo.LINK_SPEED_UNITS);

        // set internally used ping server address variable to server address (what will cause notification of
        // mWifiInfoCallback)

        setPingServerAddress(serverAddress);

        // get rx & tx, exclude mobile data

        long rx = TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes();
        long tx = TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes();

        // calculate rx/tx per second using previous results

        long rxPerSec = 0, txPerSec = 0;

        if (mLastRx != -1) {
            rxPerSec = rx - mLastRx;
        }

        if (mLastTx != -1) {
            txPerSec = tx - mLastTx;
        }

        mLastRx = rx;
        mLastTx = tx;

        // convert values from bytes per second to Mbps

        rxPerSec *= 8e-6;
        txPerSec *= 8e-6;

        // update plots

        addValueToPlotSeries(mPlotRssi, rssi);
        addValueToPlotSeries(mPlotLinkSpeed, linkSpeed);

        // make a noise if link speed is below defined threshold
        if (linkSpeed < 10) {
            if (NetToolActivity.getBeepEnabled()) {
                Util.playTone(mActivity, ToneGenerator.TONE_CDMA_ANSWER, 200);
            }
        }

        // update rx/tx plot
        if (mPlotRxTx != null) {
            addValueToSeries(mSeriesRx, rxPerSec);
            addValueToSeries(mSeriesTx, txPerSec);

            mPlotRxTx.redraw();
        }

        // udpate streamer plot
        if (mPlotStreamer != null) {
            addValueToSeries(mSeriesStreamerDownloadingProgress, mStreamerDownloadingProgress);
            addValueToSeries(mSeriesStreamerBufferDepth, mStreamerBufferDepth);

            mPlotStreamer.redraw();
        }

        // if streamer is not running then we should add zeroes to "chunk download time" plot every second
        if (mPlotChunkAddZeroes) {
            addChunkDownloadTime(0);
        }
    }

    public void addChunkDownloadTime(long time) {
        addValueToPlotSeries(mPlotChunkDownloadTime, time);
    }

    public void setPlotChunkAddZeroes(boolean enabled) {
        mPlotChunkAddZeroes = enabled;
    }

    /* Gets first series object from the plot, adds a value to it and redraws the plot */
    private void addValueToPlotSeries(XYPlot plot, float value) {
        if (plot != null) {
            SimpleXYSeries series = (SimpleXYSeries)plot.getSeriesSet().iterator().next();

            addValueToSeries(series, value);

            plot.redraw();
        }
    }

    /* Adds value to series keeping it's size within defined limit */
    private void addValueToSeries(SimpleXYSeries series, float value) {
        if (series != null) {
            if (series.size() > HISTORY_SIZE) {
                series.removeFirst();
            }

            series.addLast(null, value);
        }
    }

    private void setupPlot(XYPlot plot) {
        setupPlot(plot, true);
    }

    /*
     * Sets up plot with predefined paddings, margins, removes the legend if required, removes domain label (label
     * on X axis, removes the border, removes decimal places on axis ticks' labels and sets plot size to predefined
     * value.
     */

    private void setupPlot(XYPlot plot, boolean removeLegend) {
        plot.setGridPadding(0.0f, 10.0f, 5.0f, 0.0f);
        plot.setPlotPadding(0.0f, 0.0f, 0.0f, 0.0f);
        plot.setPlotMargins(0.0f, 0.0f, 3.0f, 0.0f);

        if (removeLegend) {
            plot.getLayoutManager().remove(plot.getLegendWidget());
        }

        plot.getLayoutManager().remove(plot.getDomainLabelWidget());

        plot.setBorderStyle(Plot.BorderStyle.NONE, 0.0f, 0.0f);

        plot.setDomainValueFormat(new DecimalFormat("#"));
        plot.setRangeValueFormat(new DecimalFormat("#"));

        plot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
    }

    /*
     * Special version of "setupPlot" for ping plot.
     * This one, in addition, sets up plot into step mode.
     */

    private void setupPlotPing(XYPlot plot) {
        plot.setGridPadding(0.0f, 10.0f, 5.0f, 0.0f);
        plot.setPlotPadding(0.0f, 0.0f, 0.0f, 0.0f);
        plot.setPlotMargins(0.0f, 0.0f, 3.0f, 0.0f);

        plot.getLayoutManager().remove(plot.getLegendWidget());
        plot.getLayoutManager().remove(plot.getDomainLabelWidget());
        plot.getLayoutManager().remove(plot.getRangeLabelWidget());

        plot.setBorderStyle(Plot.BorderStyle.NONE, 0.0f, 0.0f);

        plot.setDomainValueFormat(new DecimalFormat("#"));
        plot.setRangeValueFormat(new DecimalFormat("#"));

        plot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        plot.setRangeBoundaries(0, 1, BoundaryMode.FIXED);

        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 1);
        plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1);

        plot.setTicksPerRangeLabel(1);
        plot.setTicksPerDomainLabel(30);
    }

    /* Creates and adds series to plot and sets label and formatter on it */

    private SimpleXYSeries addSeries(XYPlot plot, String label, LineAndPointFormatter formatter) {
        SimpleXYSeries series = new SimpleXYSeries(label);

        series.useImplicitXVals();

        plot.addSeries(series, formatter);

        return series;
    }

    /*
     * Creates and adds series to plot and sets label and LineAndPointFormatter with specified line and fill colors 
     * on it
     */

    private SimpleXYSeries addSeries(XYPlot plot, String label, int lineColor, int fillColor) {
        return addSeries(plot, label, new LineAndPointFormatter(lineColor, null, fillColor, null));
    }

    /*
     * Adds row of two TextViews (one for label and another for value) to table layout and sets an ID on "value"
     * text view
     */

    private void addRow(TableLayout table, String label, int id) {
        TableRow tr = new TableRow(mActivity);

        table.addView(tr);

        TextView tv0 = new TextView(mActivity);
        TextView tv1 = new TextView(mActivity);

        tv0.setText(label);
        tv1.setId(id);

        final int p = 5;

        tv0.setPadding(p, 0, p, 0);
        tv1.setPadding(p, 0, p, 0);

        tr.addView(tv0);
        tr.addView(tv1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Created");

        // get parent activity
        mActivity = getActivity();

        // get Wi-Fi manager used for retrieving Wi-Fi connection and scans information
        mWifiManager = (WifiManager)mActivity.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroy");

        // pause UI updating
        pauseTimer();

        mWifiManager = null;

        mPlotRssi = null;
        mPlotLinkSpeed = null;
        mPlotRxTx = null;

        mActivity = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "View Created");

        // create main layout

        LinearLayout layout = new LinearLayout(mActivity);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // wi-fi info text layout

        LinearLayout lh = new LinearLayout(mActivity);

        lh.setOrientation(LinearLayout.HORIZONTAL);
        lh.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        layout.addView(lh);

        TableLayout tableLayout0 = new TableLayout(mActivity);
        TableLayout tableLayout1 = new TableLayout(mActivity);

        tableLayout0.setColumnStretchable(1, true);
        tableLayout1.setColumnStretchable(1, true);

        tableLayout0.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.5f));
        tableLayout1.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.5f));

        lh.addView(tableLayout0);
        lh.addView(tableLayout1);

        // add label/value rows

        addRow(tableLayout0, "Local IP", R.id.text_local_ip);
        addRow(tableLayout0, "SSID", R.id.text_ssid);
        addRow(tableLayout0, "Server IP", R.id.text_server_address);
        addRow(tableLayout0, "Link Speed", R.id.text_link_speed);

        addRow(tableLayout1, "MAC", R.id.text_mac);
        addRow(tableLayout1, "BSSID", R.id.text_bssid);
        addRow(tableLayout1, "RSSI", R.id.text_rssi);
        addRow(tableLayout1, "Freq.", R.id.text_frequency);

        // create plots and add them to layout

        LinearLayout plotsLayoutV = new LinearLayout(mActivity);

        plotsLayoutV.setOrientation(LinearLayout.VERTICAL);

        layout.addView(plotsLayoutV);

        // rssi, link speed, ping

        LinearLayout plotsLayoutH = new LinearLayout(mActivity);

        plotsLayoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.5f));

        plotsLayoutV.addView(plotsLayoutH);

        // rssi

        mPlotRssi = new XYPlot(mActivity, "RSSI", Plot.RenderMode.USE_BACKGROUND_THREAD);

        mPlotRssi.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.3f));

        plotsLayoutH.addView(mPlotRssi);

        setupPlot(mPlotRssi);
        // draw in three colors
        addSeries(mPlotRssi, "", new ThreeColorFormatter(-65, -80));

        mPlotRssi.setRangeLabel("dBm");
        mPlotRssi.setRangeBoundaries(-100, -40, BoundaryMode.FIXED);

        // add dashed line markers to show thresholds

        DashPathEffect dpe = new DashPathEffect(new float[]{ PixelUtils.dpToPix(2), PixelUtils.dpToPix(2) }, 0);

        YValueMarker marker = new YValueMarker(-65, "-65",
            new XPositionMetric(PixelUtils.dpToPix(5), XLayoutStyle.ABSOLUTE_FROM_RIGHT), Color.BLACK, Color.BLACK);

        marker.getTextPaint().setTextSize(PixelUtils.dpToPix(10));
        marker.getLinePaint().setPathEffect(dpe);

        mPlotRssi.addMarker(marker);

        marker = new YValueMarker(-80, "-80",
            new XPositionMetric(PixelUtils.dpToPix(5), XLayoutStyle.ABSOLUTE_FROM_RIGHT), Color.BLACK, Color.BLACK);

        marker.getTextPaint().setTextSize(PixelUtils.dpToPix(10));
        marker.getLinePaint().setPathEffect(dpe);

        mPlotRssi.addMarker(marker);

        // link speed

        mPlotLinkSpeed = new XYPlot(mActivity, "Link Speed", Plot.RenderMode.USE_BACKGROUND_THREAD);

        mPlotLinkSpeed.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.3f));

        plotsLayoutH.addView(mPlotLinkSpeed);

        setupPlot(mPlotLinkSpeed);
        // draw in three colors
        addSeries(mPlotLinkSpeed, "", new ThreeColorFormatter(53, 20));

        mPlotLinkSpeed.setRangeLabel(WifiInfo.LINK_SPEED_UNITS);
        mPlotLinkSpeed.setRangeBoundaries(0, 135, BoundaryMode.FIXED);

        // add dashed line markers to show thresholds

        marker = new YValueMarker(53, "53",
            new XPositionMetric(PixelUtils.dpToPix(5), XLayoutStyle.ABSOLUTE_FROM_RIGHT), Color.BLACK, Color.BLACK);

        marker.getTextPaint().setTextSize(PixelUtils.dpToPix(10));
        marker.getLinePaint().setPathEffect(dpe);

        mPlotLinkSpeed.addMarker(marker);

        marker = new YValueMarker(20, "20",
            new XPositionMetric(PixelUtils.dpToPix(5), XLayoutStyle.ABSOLUTE_FROM_RIGHT), Color.BLACK, Color.BLACK);

        marker.getTextPaint().setTextSize(PixelUtils.dpToPix(10));
        marker.getLinePaint().setPathEffect(dpe);

        mPlotLinkSpeed.addMarker(marker);

        // ping

        mPlotPing = new XYPlot(mActivity, "Ping", Plot.RenderMode.USE_BACKGROUND_THREAD);

        mPlotPing.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.3f));

        plotsLayoutH.addView(mPlotPing);

        setupPlotPing(mPlotPing);

        // ping plot will have two series - green for "success" values and black for "fail"

        mSeriesPingSuccess = new SimpleXYSeries("Success");

        mSeriesPingSuccess.useImplicitXVals();

        StepFormatter stepFormatter = new StepFormatter(Color.GREEN, Color.GREEN);

        stepFormatter.getLinePaint().setStrokeWidth(0);
        stepFormatter.getLinePaint().setAntiAlias(false);
        stepFormatter.setVertexPaint(null);

        mPlotPing.addSeries(mSeriesPingSuccess, stepFormatter);

        mSeriesPingFail = new SimpleXYSeries("Fail");

        mSeriesPingFail.useImplicitXVals();

        stepFormatter = new StepFormatter(Color.BLACK, Color.BLACK);

        stepFormatter.getLinePaint().setStrokeWidth(0);
        stepFormatter.getLinePaint().setAntiAlias(false);
        stepFormatter.setVertexPaint(null);

        mPlotPing.addSeries(mSeriesPingFail, stepFormatter);

        // Rx / Tx, streamer

        plotsLayoutH = new LinearLayout(mActivity);

        plotsLayoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.5f));

        plotsLayoutV.addView(plotsLayoutH);

        // Rx / Tx

        mPlotRxTx = new XYPlot(mActivity, "Rx / Tx", Plot.RenderMode.USE_BACKGROUND_THREAD);

        mPlotRxTx.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.3f));

        plotsLayoutH.addView(mPlotRxTx);

        mPlotRxTx.getLayoutManager().remove(mPlotRxTx.getGraphWidget());

        // draw the plot in logarithm scale
        mPlotRxTx.setGraphWidget(new LogarithmGraphWidget(mPlotRxTx));

        setupPlot(mPlotRxTx, false);

        mSeriesRx = addSeries(mPlotRxTx, "Rx", new LogarithmFormatter(Color.BLUE, Color.argb(128, 100, 100, 200)));
        mSeriesTx = addSeries(mPlotRxTx, "Tx", new LogarithmFormatter(Color.RED, Color.argb(128, 200, 100, 100)));

        mPlotRxTx.setRangeLabel("Mbps");

        mPlotRxTx.setRangeBoundaries(0, Math.log10(80), BoundaryMode.FIXED);

        // streamer plot

        mPlotStreamer = new XYPlot(mActivity, "Buffer Depth", Plot.RenderMode.USE_BACKGROUND_THREAD);

        mPlotStreamer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.3f));

        plotsLayoutH.addView(mPlotStreamer);

        setupPlot(mPlotStreamer, false);

        mSeriesStreamerDownloadingProgress = addSeries(mPlotStreamer, getString(R.string.label_downloading_progress),
            Color.BLUE, Color.argb(128, 100, 100, 200));
        mSeriesStreamerBufferDepth = addSeries(mPlotStreamer, getString(R.string.label_buffer_depth),
            Color.RED, Color.argb(128, 200, 100, 100));

        mPlotStreamer.setRangeLabel("%");
        mPlotStreamer.setRangeBoundaries(0, 100, BoundaryMode.FIXED);

        // modify streamer plot's legend to make it more compact using for this purpose specially written
        // "StreamerTableModel" class

        XYLegendWidget streamerLegendWidget = mPlotStreamer.getLegendWidget();

        Paint textPaint = streamerLegendWidget.getTextPaint();
        float textSize = textPaint.getTextSize();

        streamerLegendWidget.setIconSizeMetrics(
            new SizeMetrics(textSize, SizeLayoutType.ABSOLUTE, textSize, SizeLayoutType.ABSOLUTE));

        SizeMetrics iconSizeMetrics = streamerLegendWidget.getIconSizeMetrics();
        SizeMetric iconWidthMetric = iconSizeMetrics.getWidthMetric();
        SizeMetric iconHeightMetric = iconSizeMetrics.getHeightMetric();

        float iconWidth = iconWidthMetric.getPixelValue(iconWidthMetric.getValue());
        float iconHeight = iconHeightMetric.getPixelValue(iconHeightMetric.getValue());

        // measure widths of labels

        float textWidthDownloadingProgress = textPaint.measureText(
            getText(R.string.label_downloading_progress).toString());
        float textWidthBufferDepth = textPaint.measureText(getText(R.string.label_buffer_depth).toString());

        // calculate suitable cell width
        float cellWidth =
            Math.max(textWidthDownloadingProgress, textWidthBufferDepth) + iconWidth + PixelUtils.dpToPix(5);

        // create and set new model
        mPlotStreamer.getLegendWidget().setTableModel(new StreamerTableModel(cellWidth));

        // resize the legend widget to minimal possible size
        mPlotStreamer.getLegendWidget().setSize(
            new SizeMetrics(
                Math.max(textSize, iconHeight) + PixelUtils.dpToPix(3), SizeLayoutType.ABSOLUTE,
                cellWidth * 2, SizeLayoutType.ABSOLUTE));

        // center-align legend widget
        mPlotStreamer.getLegendWidget().position(0, XLayoutStyle.RELATIVE_TO_CENTER,
           0, YLayoutStyle.ABSOLUTE_FROM_BOTTOM, AnchorPosition.BOTTOM_MIDDLE);

        // chunks plot

        mPlotChunkDownloadTime = new XYPlot(mActivity, getString(R.string.label_plot_chunk_download_time),
            Plot.RenderMode.USE_BACKGROUND_THREAD);

        mPlotChunkDownloadTime.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT, 0.3f));

        plotsLayoutH.addView(mPlotChunkDownloadTime);

        // remove previous graph widget to use our own with reimplemented Y-axis ticks rendering
        mPlotChunkDownloadTime.getLayoutManager().remove(mPlotChunkDownloadTime.getGraphWidget());

        // draw the plot in logarithm scale
        mPlotChunkDownloadTime.setGraphWidget(new LogarithmGraphWidget(mPlotChunkDownloadTime));

        setupPlot(mPlotChunkDownloadTime);

        addSeries(mPlotChunkDownloadTime, "", new LogarithmFormatter(Color.BLUE, Color.argb(128, 100, 100, 200)));

        mPlotChunkDownloadTime.setRangeLabel("ms");
        // set range boundaries as base 10 logarithms
        mPlotChunkDownloadTime.setRangeBoundaries(0, Math.log10(4000), BoundaryMode.FIXED);

        return layout;
    }

    /*
     * This will be called on "configChanges" events (see AndroidManifest.xml"), e.g. on orientation change
     */

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "Configuration changed");

        // redraw all plots after small delay
        mTimerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPlotRssi.redraw();
                    mPlotLinkSpeed.redraw();
                    mPlotRxTx.redraw();
                    mPlotStreamer.redraw();
                    mPlotPing.redraw();
                    mPlotChunkDownloadTime.redraw();
                }
            },
            100);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // check activity for casting into listener types and set callback variables to it

        try {
            mWifiInfoCallback = (OnWifiInfoListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWifiInfoListener");
        }

        try {
            mPingCallback = (OnPingListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPingListener");
        }

        try {
            mLogCallback = (SettingsFragment.OnLogListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnLogListener");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "Pause");

        // pause UI updates
        pauseTimer();
    }

    @Override
    public void onResume () {
        super.onResume();

        Log.d(TAG, "Resume");

        // resume UI updates
        resumeTimer();

        // restart ping if it was running before app pausing
        if (mPingShouldResume) {
            pingStart(mPingResumeAddress);

            mPingShouldResume = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // if ping is running - set the flag to resume it later
        if (mPingTask != null && mPingTask.getStatus() == AsyncTask.Status.RUNNING) {
            mPingShouldResume = true;
        }

        pingStop();
    }

    public void setStreamerDownloadingProgress(int downloadingProgress) {
        // this value will be added to corresponding plot every second
        mStreamerDownloadingProgress = downloadingProgress;
    }

    public void setStreamerBufferDepth(int bufferDepth) {
        // this value will be added to corresponding plot every second
        mStreamerBufferDepth = bufferDepth;
    }

    protected void setPingServerAddress(int address) {
        if (mPingAddress != address) {
            mPingAddress = address;

            // notify listener
            mWifiInfoCallback.onServerAddressObtained(mPingAddress);
        }
    }

    public void setPingPacketSize(int pingPacketSize) {
        if (pingPacketSize < 0) {
            throw new IllegalArgumentException("Ping packet size can't be negative");
        }

        mPingPacketSize = pingPacketSize;
    }

    public void pingStart(String address) {
        mPingResumeAddress = address;

        // stop ping if it's already running
        if (mPingTask != null && mPingTask.getStatus() == AsyncTask.Status.FINISHED) {
            mPingTask.stop();

            mPingTask = null;
        }

        // create ping task and set listener (to fragment)
        if (mPingTask == null) {
            mPingTask = new PingTask();

            mPingTask.mFragment = this;
        }

        // run the ping task
        if (mPingTask.getStatus() != AsyncTask.Status.RUNNING) {
            mPingNoAnswer.clear();

            mPingTask.execute(address, String.valueOf(mPingPacketSize));
        }
    }

    public void pingStop() {
        if (mPingTask != null && mPingTask.getStatus() == AsyncTask.Status.RUNNING) {
            mPingTask.stop();

            mPingTask = null;
        }
    }

    /* Takes ping's log line and converts it to values for plotting */

    private void parsePingLog(String line) {
        // add line to ping log text in settings
        mLogCallback.onPrependLog(line);

        // ignore "PING ... bytes of data" line
        if (Pattern.compile("^PING.*bytes of data\\.").matcher(line).matches()) {
            return;
        }

        // parse the line

        int pingResult = -1;
        float pingTime = 0.0f;

        // successful ping log pattern
        Matcher m = Pattern.compile("^\\d+\\sbytes\\sfrom\\s.*icmp_seq=(\\d+).*\\stime=([\\d\\.]+).*").matcher(line);

        if (m.matches()) {
            pingResult = 1;

            int icmpSeq = Integer.valueOf(m.group(1));
            // convert to seconds
            pingTime = Float.parseFloat(m.group(2)) / 1000.0f;

            // update tracked "no answer yet" bar, if there any with such icmp_seq
            for (int i = 0; i < mPingNoAnswer.size(); ++i) {
                int key = mPingNoAnswer.keyAt(i);
                int value = mPingNoAnswer.valueAt(i);

                if (key == icmpSeq) {
                    mSeriesPingFail.setY(-1.0f, value);
                    mSeriesPingSuccess.setY(pingTime, value);

                    mPingNoAnswer.removeAt(i);

                    mPlotPing.redraw();

                    return;
                }
            }
        } else {
            // "no answer yet for" pattern
            m = Pattern.compile("^no\\sanswer\\syet\\sfor\\sicmp_seq=(\\d+).*").matcher(line);

            if (m.matches()) {
                int icmpSeq = Integer.valueOf(m.group(1));

                pingResult = 0;

                // add for future tracking
                mPingNoAnswer.append(icmpSeq, mSeriesPingFail.size());
            } else {
                // "Network is unreachable" and "Destination Host Unreachable" patterns
                final boolean match =
                    Pattern.compile("^.*Network\\sis\\sunreachable$").matcher(line).matches()
                    || Pattern.compile("^.*Destination\\sHost\\sUnreachable$").matcher(line).matches();

                if (match) {
                    pingResult = 0;
                }
            }
        }

        // update plots with new values

        if (pingResult == 1) {
            addValueToSeries(mSeriesPingSuccess, pingTime);
            addValueToSeries(mSeriesPingFail, -1.0f);
        } else if (pingResult == 0) {
            addValueToSeries(mSeriesPingSuccess, -1.0f);
            addValueToSeries(mSeriesPingFail, 1.0f);
        } else {
            addValueToSeries(mSeriesPingSuccess, -1.0f);
            addValueToSeries(mSeriesPingFail, 0.5f);
        }

        // remove old values and shift ids
        if (mSeriesPingFail.size() > HISTORY_SIZE) {
            for (int i = mPingNoAnswer.size() - 1; i >= 0; --i) {
                int key = mPingNoAnswer.keyAt(i);
                int value = mPingNoAnswer.valueAt(i);

                if (value == 0) {
                    mPingNoAnswer.removeAt(i);
                }
                else {
                    mPingNoAnswer.put(key, value - 1);
                }
            }
        }

        mPlotPing.redraw();
    }

    /*
     * PingTask allows to run ping command and get results from it asynchronously.
     */

    private class PingTask extends AsyncTask<String, Void, Void> {
        private PipedOutputStream mPipedOut;
        private PipedInputStream mPipedIn;
        private LineNumberReader mReader;
        private Process mProcess;
        private GraphsFragment mFragment;

        /* Prepare for command execution */

        @Override
        protected void onPreExecute() {
            mPipedOut = new PipedOutputStream();

            try {
                mPipedIn = new PipedInputStream(mPipedOut);
                mReader = new LineNumberReader(new InputStreamReader(mPipedIn));
            } catch (IOException e) {
                cancel(true);
            }
        }

        public void stop() {
            Process p = mProcess;

            if (p != null) {
                p.destroy();
            }

            cancel(true);
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                // build and start the process

                // params: 0 - address, 1 - packet size
                mProcess = new ProcessBuilder()
                    .command("/system/bin/ping", "-i 1", "-s", params[1], "-O", params[0])
                    .redirectErrorStream(true)
                    .start();

                try {
                    InputStream in = mProcess.getInputStream();
                    OutputStream out = mProcess.getOutputStream();

                    byte[] buffer = new byte[1024];
                    int count;

                    // read output of process
                    while ((count = in.read(buffer)) != -1) {
                        // redirect output and notify about progress UI thread
                        mPipedOut.write(buffer, 0, count);
                        publishProgress();

                        if (isCancelled()) {
                            Log.d(TAG, "PingTask cancelled");

                            break;
                        }
                    }

                    out.close();
                    in.close();

                    mPipedOut.close();
                    mPipedIn.close();
                } finally {
                    mProcess.destroy();
                    mProcess = null;
                }
            } catch (IOException e) {
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            try {
                // read and process the output of ping
                while (mReader.ready()) {
                    String line = mReader.readLine();

                    if (mFragment != null) {
                        mFragment.parsePingLog(line);
                    }

                    Log.d(TAG, "OUTPUT: " + line);
                }
            } catch (IOException t) {
            }
        }
    }

    // streamer plot's table model for legend

    public class StreamerTableModel extends DynamicTableModel {
        final private float mCellWidth;

        public StreamerTableModel(float cellWidth) {
            // 2 columns and 1 row
            super(2, 1, TableOrder.ROW_MAJOR);

            mCellWidth = cellWidth;
        }

        public RectF getCellRect(RectF tableRect, int numElements) {
            RectF cellRect = new RectF(tableRect);

            cellRect.left = tableRect.left;
            cellRect.top = tableRect.top;
            cellRect.bottom = tableRect.bottom;
            cellRect.right = cellRect.left + mCellWidth;

            return cellRect;
        }
    }
}
