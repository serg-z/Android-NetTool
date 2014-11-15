package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;

import android.view.ViewGroup.LayoutParams;

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
        ((TextView)findViewById(R.id.text_dns1)).setText(Formatter.formatIpAddress(dhcpInfo.dns1));
        ((TextView)findViewById(R.id.text_dns2)).setText(Formatter.formatIpAddress(dhcpInfo.dns2));
        ((TextView)findViewById(R.id.text_gateway)).setText(Formatter.formatIpAddress(dhcpInfo.gateway));
        ((TextView)findViewById(R.id.text_netmask)).setText(Formatter.formatIpAddress(dhcpInfo.netmask));
        ((TextView)findViewById(R.id.text_server_address)).setText(Formatter.formatIpAddress(dhcpInfo.serverAddress));
        ((TextView)findViewById(R.id.text_rssi)).setText(String.valueOf(rssi) + " dBm");
        ((TextView)findViewById(R.id.text_link_speed)).setText(String.valueOf(linkSpeed) + " " + WifiInfo.LINK_SPEED_UNITS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Created");

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

        TableLayout tableLayout = new TableLayout(this);

        lh.addView(tableLayout);

        ArrayList<Pair<String, Integer>> fields = new ArrayList<Pair<String, Integer>>();

        fields.add(Pair.create("MAC", R.id.text_mac));
        fields.add(Pair.create("Local IP", R.id.text_local_ip));
        fields.add(Pair.create("SSID", R.id.text_ssid));
        fields.add(Pair.create("BSSID", R.id.text_bssid));
        fields.add(Pair.create("DNS 1", R.id.text_dns1));
        fields.add(Pair.create("DNS 2", R.id.text_dns2));
        fields.add(Pair.create("Gateway", R.id.text_gateway));
        fields.add(Pair.create("Netmask", R.id.text_netmask));
        fields.add(Pair.create("Server Address", R.id.text_server_address));
        fields.add(Pair.create("RSSI", R.id.text_rssi));
        fields.add(Pair.create("Link Speed", R.id.text_link_speed));

        int j = 0;

        TableRow tr = null;

        final int pairColumns = 2;

        for (Pair<String, Integer> f : fields) {
            if (j % pairColumns == 0) {
                tr = new TableRow(this);

                tableLayout.addView(tr);
            }

            TextView tv0 = new TextView(this);
            TextView tv1 = new TextView(this);

            tv0.setText(f.first);
            tv1.setId(f.second);

            final int p0 = 5;
            final int p1 = 30;

            tv0.setPadding(p1, 0, p0, 0);
            tv1.setPadding(p0, 0, p1, 0);

            tr.addView(tv0);
            tr.addView(tv1);

            ++j;
        }

        for (int i = 1; i < pairColumns * 2; i += 2) {
            tableLayout.setColumnStretchable(i, true);
        }

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
