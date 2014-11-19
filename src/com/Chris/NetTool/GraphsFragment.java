package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;

import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Context;

import android.net.DhcpInfo;
import android.net.TrafficStats;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.ScanResult;

import android.text.format.Formatter;

import android.util.Pair;
import android.util.Log;

import android.graphics.Color;

import com.androidplot.Plot;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.BoundaryMode;

import android.support.v4.app.Fragment;

import java.text.DecimalFormat;

public class GraphsFragment extends Fragment {
    private static final String TAG = "GraphsFragment";
    private static final int HISTORY_SIZE = 120;

    Handler mTimerHandler = new Handler();

    Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();

            mTimerHandler.postDelayed(this, 1000);
        }
    };

    WifiManager mWifiManager;

    XYPlot mPlotRssi, mPlotLinkSpeed, mPlotRxTx;
    SimpleXYSeries mSeriesRx, mSeriesTx;

    Activity mActivity;

    long mLastRx = -1, mLastTx = -1;

    public void pause() {
        mTimerHandler.removeCallbacks(mTimerRunnable);

        mLastRx = -1;
        mLastTx = -1;
    }

    public void resume() {
        mTimerHandler.postDelayed(mTimerRunnable, 0);
    }

    void updateUI() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();

        String bssid = wifiInfo.getBSSID();

        for (ScanResult r : mWifiManager.getScanResults()) {
            if (r.BSSID.equals(bssid)) {
                String text = r.frequency + " MHz";

                Integer channel = Frequencies.sChannels.get(r.frequency);

                if (channel != null) {
                    text += " (ch " + channel + ")";
                }

                ((TextView)mActivity.findViewById(R.id.text_frequency)).setText(text);
            }
        }

        int rssi = wifiInfo.getRssi();
        int linkSpeed = wifiInfo.getLinkSpeed();
        int serverAddress = dhcpInfo.serverAddress;

        ((TextView)mActivity.findViewById(R.id.text_mac)).setText(wifiInfo.getMacAddress());
        ((TextView)mActivity.findViewById(R.id.text_local_ip)).setText(Formatter.formatIpAddress(wifiInfo.getIpAddress()));
        ((TextView)mActivity.findViewById(R.id.text_ssid)).setText(wifiInfo.getSSID());
        ((TextView)mActivity.findViewById(R.id.text_bssid)).setText(bssid);
        ((TextView)mActivity.findViewById(R.id.text_server_address)).setText(Formatter.formatIpAddress(serverAddress));
        ((TextView)mActivity.findViewById(R.id.text_rssi)).setText(String.valueOf(rssi) + " dBm");
        ((TextView)mActivity.findViewById(R.id.text_link_speed)).setText(String.valueOf(linkSpeed) + " " + WifiInfo.LINK_SPEED_UNITS);

        ((NetToolActivity)mActivity).setServerAddress(serverAddress);

        // get rx & tx, exclude mobile data

        long rx = TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes();
        long tx = TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes();

        long rxPerSec = 0, txPerSec = 0;

        if (mLastRx != -1) {
            rxPerSec = rx - mLastRx;
        }

        if (mLastTx != -1) {
            txPerSec = tx - mLastTx;
        }

        mLastRx = rx;
        mLastTx = tx;

        rxPerSec *= 8e-6;
        txPerSec *= 8e-6;

        addValueToPlotSeries(mPlotRssi, rssi);
        addValueToPlotSeries(mPlotLinkSpeed, linkSpeed);

        if (mPlotRxTx != null) {
            addValueToSeries(mSeriesRx, rxPerSec);
            addValueToSeries(mSeriesTx, txPerSec);

            mPlotRxTx.redraw();
        }
    }

    void addValueToPlotSeries(XYPlot plot, float value) {
        if (plot != null) {
            SimpleXYSeries series = (SimpleXYSeries)plot.getSeriesSet().iterator().next();

            addValueToSeries(series, value);

            plot.redraw();
        }
    }

    void addValueToSeries(SimpleXYSeries series, float value) {
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

    private SimpleXYSeries addSeries(XYPlot plot, String label, int lineColor, int fillColor) {
        SimpleXYSeries series = new SimpleXYSeries(label);

        series.useImplicitXVals();

        plot.addSeries(series, new LineAndPointFormatter(lineColor, null, fillColor, null));

        return series;
    }

    void addRow(TableLayout table, String label, int id) {
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

        mActivity = getActivity();

        mWifiManager = (WifiManager)mActivity.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroy");

        pause();

        mWifiManager = null;

        mPlotRssi = null;
        mPlotLinkSpeed = null;
        mPlotRxTx = null;

        mActivity = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "View Created");

        LinearLayout layout = new LinearLayout(mActivity);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout lh = new LinearLayout(mActivity);

        lh.setOrientation(LinearLayout.HORIZONTAL);
        lh.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        layout.addView(lh);

        TableLayout tableLayout0 = new TableLayout(mActivity);
        TableLayout tableLayout1 = new TableLayout(mActivity);

        tableLayout0.setColumnStretchable(1, true);
        tableLayout1.setColumnStretchable(1, true);

        tableLayout0.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));
        tableLayout1.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        lh.addView(tableLayout0);
        lh.addView(tableLayout1);

        addRow(tableLayout0, "Local IP", R.id.text_local_ip);
        addRow(tableLayout0, "SSID", R.id.text_ssid);
        addRow(tableLayout0, "Server IP", R.id.text_server_address);
        addRow(tableLayout0, "Link Speed", R.id.text_link_speed);

        addRow(tableLayout1, "MAC", R.id.text_mac);
        addRow(tableLayout1, "BSSID", R.id.text_bssid);
        addRow(tableLayout1, "RSSI", R.id.text_rssi);
        addRow(tableLayout1, "Frequency", R.id.text_frequency);

        // create plots

        LinearLayout plotsLayoutV = new LinearLayout(mActivity);

        plotsLayoutV.setOrientation(LinearLayout.VERTICAL);

        layout.addView(plotsLayoutV);

        // rssi and link speed

        LinearLayout plotsLayoutH = new LinearLayout(mActivity);

        plotsLayoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        plotsLayoutV.addView(plotsLayoutH);

        // rssi

        mPlotRssi = new XYPlot(mActivity, "RSSI");

        mPlotRssi.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        plotsLayoutH.addView(mPlotRssi);

        setupPlot(mPlotRssi);
        addSeries(mPlotRssi, "", Color.BLUE, Color.rgb(100, 100, 200));

        mPlotRssi.setRangeLabel("dBm");
        mPlotRssi.setRangeBoundaries(-100, -40, BoundaryMode.FIXED);

        // link speed

        mPlotLinkSpeed = new XYPlot(mActivity, "Link Speed");

        mPlotLinkSpeed.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        plotsLayoutH.addView(mPlotLinkSpeed);

        setupPlot(mPlotLinkSpeed);
        addSeries(mPlotLinkSpeed, "", Color.BLUE, Color.rgb(100, 100, 200));

        mPlotLinkSpeed.setRangeLabel(WifiInfo.LINK_SPEED_UNITS);
        mPlotLinkSpeed.setRangeBoundaries(0, 135, BoundaryMode.FIXED);

        // Rx and Tx

        plotsLayoutH = new LinearLayout(mActivity);

        plotsLayoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        plotsLayoutV.addView(plotsLayoutH);

        // Rx / Tx

        mPlotRxTx = new XYPlot(mActivity, "Rx / Tx");

        mPlotRxTx.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        plotsLayoutH.addView(mPlotRxTx);

        setupPlot(mPlotRxTx, false);

        mSeriesRx = addSeries(mPlotRxTx, "Rx", Color.BLUE, Color.rgb(100, 100, 200));
        mSeriesTx = addSeries(mPlotRxTx, "Tx", Color.RED, Color.rgb(200, 100, 100));

        mPlotRxTx.setRangeLabel("Mbps");
        // TODO: change to 0-40 on logarithmic scale
        mPlotRxTx.setRangeBoundaries(0, 24, BoundaryMode.FIXED);

        return layout;
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "Pause");

        pause();
    }

    @Override
    public void onResume () {
        super.onResume();

        Log.d(TAG, "Resume");

        resume();
    }
}
