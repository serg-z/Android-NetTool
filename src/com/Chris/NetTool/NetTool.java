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

import android.text.format.Formatter;

import android.util.Pair;
import android.util.Log;

import android.graphics.Color;

import java.util.ArrayList;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class NetTool extends Activity {
    private static final String TAG = "NetTool";

    Handler mTimerHandler = new Handler();
    Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();

            mTimerHandler.postDelayed(this, 1000);
        }
    };

    WifiManager mWifiManager;

    LineChart mChartRssi, mChartLinkSpeed, mChartRx, mChartTx;
    ArrayList<Integer> mChartDataRssi = new ArrayList<Integer>();

    void updateUI() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();

        int rssi = wifiInfo.getRssi();
        int linkSpeed = wifiInfo.getLinkSpeed();

        ((TextView)findViewById(R.id.text_mac)).setText(wifiInfo.getMacAddress());
        ((TextView)findViewById(R.id.text_local_ip)).setText(Formatter.formatIpAddress(wifiInfo.getIpAddress()));
        ((TextView)findViewById(R.id.text_ssid)).setText(wifiInfo.getSSID());
        ((TextView)findViewById(R.id.text_bssid)).setText(wifiInfo.getBSSID());
        ((TextView)findViewById(R.id.text_server_address)).setText(Formatter.formatIpAddress(dhcpInfo.serverAddress));
        ((TextView)findViewById(R.id.text_rssi)).setText(String.valueOf(rssi) + " dBm");
        ((TextView)findViewById(R.id.text_link_speed)).setText(String.valueOf(linkSpeed) + " " + WifiInfo.LINK_SPEED_UNITS);
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

        // create charts

        LinearLayout chartsLayoutV = new LinearLayout(this);

        layout.addView(chartsLayoutV);

        chartsLayoutV.setOrientation(LinearLayout.VERTICAL);

        // rssi and link speed

        LinearLayout chartsLayoutH = new LinearLayout(this);

        chartsLayoutV.addView(chartsLayoutH);

        chartsLayoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        // rssi

        mChartRssi = new LineChart(this);

        chartsLayoutH.addView(mChartRssi);

        mChartRssi.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        mChartRssi.setUnit(" dBm");

        setupChart(mChartRssi, "RSSI");

        // link speed

        mChartLinkSpeed = new LineChart(this);

        chartsLayoutH.addView(mChartLinkSpeed);

        mChartLinkSpeed.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        mChartLinkSpeed.setUnit(" " + WifiInfo.LINK_SPEED_UNITS);

        setupChart(mChartLinkSpeed, "Link Speed");

        // Rx and Tx

        chartsLayoutH = new LinearLayout(this);

        chartsLayoutV.addView(chartsLayoutH);

        chartsLayoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        // Rx

        mChartRx = new LineChart(this);

        chartsLayoutH.addView(mChartRx);

        mChartRx.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        mChartRx.setUnit("");

        setupChart(mChartRx, "Rx");

        // Tx

        mChartTx = new LineChart(this);

        chartsLayoutH.addView(mChartTx);

        mChartTx.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        mChartTx.setUnit("");

        setupChart(mChartTx, "Tx");
    }

    private void setupChart(LineChart chart, String description) {
        chart.setDescription(description);
        chart.setDrawUnitsInChart(true);
        chart.setStartAtZero(false);
        chart.setDrawGridBackground(true);
        chart.setDrawBorder(true);
        chart.setDrawXLabels(true);
        chart.setDrawYValues(false);
        chart.setHighlightIndicatorEnabled(false);

        addEmptyData(chart, description);

        chart.getLegend().setTextColor(Color.WHITE);
        chart.getXLabels().setTextColor(Color.WHITE);
        chart.getYLabels().setTextColor(Color.WHITE);
    }

    private void addEmptyData(LineChart chart, String name) {
        String[] xVals = new String[10];

        for (int i = 0; i < 10; i++)
            xVals[i] = "" + i;

        LineDataSet set = new LineDataSet(null, name);

        set.setLineWidth(1.0f);
        set.setDrawCircles(false);
        set.setDrawCubic(true);
        set.setCubicIntensity(0.05f);
        set.setColor(Color.RED);

        LineData data = new LineData(xVals);

        data.addDataSet(set);

        chart.setData(data);
        chart.invalidate();
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
