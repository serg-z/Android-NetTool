package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;

import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;

import android.content.Context;

import android.net.DhcpInfo;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

import android.text.format.Formatter;

import android.util.Log;

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

    void addRow(TableLayout tl, String label, int id) {
        TableRow tr = new TableRow(this);

        tl.addView(tr);

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

        mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        // create ui

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        setContentView(layout);

        TableLayout tableLayout = new TableLayout(this);

        layout.addView(tableLayout);

        addRow(tableLayout, "MAC", R.id.text_mac);
        addRow(tableLayout, "Local IP", R.id.text_local_ip);
        addRow(tableLayout, "SSID", R.id.text_ssid);
        addRow(tableLayout, "BSSID", R.id.text_bssid);
        addRow(tableLayout, "DNS 1", R.id.text_dns1);
        addRow(tableLayout, "DNS 2", R.id.text_dns2);
        addRow(tableLayout, "Gateway", R.id.text_gateway);
        addRow(tableLayout, "Netmask", R.id.text_netmask);
        addRow(tableLayout, "Server Address", R.id.text_server_address);
        addRow(tableLayout, "RSSI", R.id.text_rssi);
        addRow(tableLayout, "Link Speed", R.id.text_link_speed);
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
