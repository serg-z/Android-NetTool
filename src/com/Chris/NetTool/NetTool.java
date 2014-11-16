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

import android.content.Context;

import android.net.DhcpInfo;

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

public class NetTool extends Activity {
    private static final String TAG = "NetTool";
    private static final int HISTORY_SIZE = 10;

    Handler mTimerHandler = new Handler();
    Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();

            mTimerHandler.postDelayed(this, 1000);
        }
    };

    WifiManager mWifiManager;

    XYPlot mPlotRssi, mPlotLinkSpeed, mPlotRx, mPlotTx;

    void updateUI() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();

        String bssid = wifiInfo.getBSSID();

        for (ScanResult r : mWifiManager.getScanResults()) {
            if (r.BSSID.equals(bssid)) {
                ((TextView)findViewById(R.id.text_frequency)).setText(r.frequency + " MHz");
            }
        }

        int rssi = wifiInfo.getRssi();
        int linkSpeed = wifiInfo.getLinkSpeed();

        ((TextView)findViewById(R.id.text_mac)).setText(wifiInfo.getMacAddress());
        ((TextView)findViewById(R.id.text_local_ip)).setText(Formatter.formatIpAddress(wifiInfo.getIpAddress()));
        ((TextView)findViewById(R.id.text_ssid)).setText(wifiInfo.getSSID());
        ((TextView)findViewById(R.id.text_bssid)).setText(bssid);
        ((TextView)findViewById(R.id.text_server_address)).setText(Formatter.formatIpAddress(dhcpInfo.serverAddress));
        ((TextView)findViewById(R.id.text_rssi)).setText(String.valueOf(rssi) + " dBm");
        ((TextView)findViewById(R.id.text_link_speed)).setText(String.valueOf(linkSpeed) + " " + WifiInfo.LINK_SPEED_UNITS);

        addValueToSeries(mPlotRssi, rssi);
        addValueToSeries(mPlotLinkSpeed, linkSpeed);
    }

    void addValueToSeries(XYPlot plot, float value) {
        if (plot != null) {
            SimpleXYSeries series = (SimpleXYSeries)plot.getSeriesSet().iterator().next();

            if (series.size() > HISTORY_SIZE) {
                series.removeFirst();
            }

            series.addLast(null, value);

            plot.redraw();
        }
    }

    void addRow(TableLayout table, String label, int id) {
        TableRow tr = new TableRow(this);

        table.addView(tr);

        TextView tv0 = new TextView(this);
        TextView tv1 = new TextView(this);

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

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        // create ui

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setContentView(layout);

        LinearLayout lh = new LinearLayout(this);

        lh.setOrientation(LinearLayout.HORIZONTAL);
        lh.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        layout.addView(lh);

        TableLayout tableLayout0 = new TableLayout(this);
        TableLayout tableLayout1 = new TableLayout(this);

        lh.addView(tableLayout0);
        lh.addView(tableLayout1);

        tableLayout0.setColumnStretchable(1, true);
        tableLayout1.setColumnStretchable(1, true);

        tableLayout0.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));
        tableLayout1.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        addRow(tableLayout0, "Local IP", R.id.text_local_ip);
        addRow(tableLayout0, "SSID", R.id.text_ssid);
        addRow(tableLayout0, "Server IP", R.id.text_server_address);
        addRow(tableLayout0, "Link Speed", R.id.text_link_speed);

        addRow(tableLayout1, "MAC", R.id.text_mac);
        addRow(tableLayout1, "BSSID", R.id.text_bssid);
        addRow(tableLayout1, "RSSI", R.id.text_rssi);
        addRow(tableLayout1, "Frequency", R.id.text_frequency);

        // create plots

        LinearLayout plotsLayoutV = new LinearLayout(this);

        layout.addView(plotsLayoutV);

        plotsLayoutV.setOrientation(LinearLayout.VERTICAL);

        // rssi and link speed

        LinearLayout plotsLayoutH = new LinearLayout(this);

        plotsLayoutV.addView(plotsLayoutH);

        plotsLayoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        // rssi

        mPlotRssi = new XYPlot(this, "RSSI");

        plotsLayoutH.addView(mPlotRssi);

        mPlotRssi.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        setupPlot(mPlotRssi);

        mPlotRssi.setRangeLabel("dBm");
        mPlotRssi.setRangeBoundaries(-100, -40, BoundaryMode.FIXED);

        // link speed

        mPlotLinkSpeed = new XYPlot(this, "Link Speed");

        plotsLayoutH.addView(mPlotLinkSpeed);

        mPlotLinkSpeed.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        setupPlot(mPlotLinkSpeed);

        mPlotLinkSpeed.setRangeLabel(WifiInfo.LINK_SPEED_UNITS);
        mPlotLinkSpeed.setRangeBoundaries(0, 135, BoundaryMode.FIXED);

        // Rx and Tx

        plotsLayoutH = new LinearLayout(this);

        plotsLayoutV.addView(plotsLayoutH);

        plotsLayoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        // Rx

        mPlotRx = new XYPlot(this, "Rx");

        plotsLayoutH.addView(mPlotRx);

        mPlotRx.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        setupPlot(mPlotRx);

        mPlotRx.setRangeBoundaries(0, 3000, BoundaryMode.FIXED);

        // Tx

        mPlotTx = new XYPlot(this, "Tx");

        plotsLayoutH.addView(mPlotTx);

        mPlotTx.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        setupPlot(mPlotTx);

        mPlotTx.setRangeBoundaries(0, 3000, BoundaryMode.FIXED);
    }

    private void setupPlot(XYPlot plot) {
        plot.setGridPadding(0.0f, 10.0f, 5.0f, 0.0f);
        plot.setPlotPadding(0.0f, 0.0f, 0.0f, 0.0f);
        plot.setPlotMargins(0.0f, 0.0f, 3.0f, 0.0f);

        plot.getLayoutManager().remove(plot.getLegendWidget());
        plot.getLayoutManager().remove(plot.getDomainLabelWidget());

        plot.setBorderStyle(Plot.BorderStyle.NONE, 0.0f, 0.0f);

        // add series

        SimpleXYSeries series = new SimpleXYSeries("");

        series.useImplicitXVals();

        plot.addSeries(series, new LineAndPointFormatter(Color.rgb(100, 100, 200), null, null, null));
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "Started");
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "Pause");

        mTimerHandler.removeCallbacks(mTimerRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "Resume");

        mTimerHandler.postDelayed(mTimerRunnable, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "Stop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroy");
    }
}
