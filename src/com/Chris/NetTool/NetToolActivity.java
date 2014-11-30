package com.Chris.NetTool;

import android.os.Bundle;
import android.os.PowerManager;

import android.content.Context;

import android.view.Window;
import android.view.WindowManager;
import android.view.KeyEvent;

import android.widget.Toast;

import android.net.wifi.WifiManager;

import android.util.Log;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class NetToolActivity extends FragmentActivity implements SettingsFragment.OnPingListener,
    GraphsFragment.OnWifiInfoListener, DatagramReceiver.DatagramReceiverListener {

    private static final String TAG = "NetToolActivity";

    private PowerManager.WakeLock mWakeLock = null;
    private DatagramReceiver mDatagramReceiver = null;
    private WifiManager.WifiLock mWifiLock = null;
    private WifiManager.MulticastLock mMulticastLock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_DeviceDefault);

        super.onCreate(savedInstanceState);

        Log.d(TAG, "Created");

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (savedInstanceState == null) {
            PagerFragment fragmentPager = new PagerFragment();

            getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragmentPager, PagerFragment.tag_fragment_pager)
                .commit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showSettings();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "Started");

        mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE))
            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

        mWakeLock.acquire();

        // acquire wifi lock
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE))
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG + "_WifiLock");

        mWifiLock.acquire();

        // acquire multicast lock
        mMulticastLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE))
            .createMulticastLock(TAG + "_MulticastLock");

        mMulticastLock.acquire();

        // create listening datagram socket
        if (mDatagramReceiver == null) {
            mDatagramReceiver = new DatagramReceiver(55555);

            mDatagramReceiver.setListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "Pause");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "Resume");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "Stop");

        if (mWakeLock != null) {
            mWakeLock.release();

            mWakeLock = null;
        }

        if (mWifiLock != null) {
            mWifiLock.release();

            mWifiLock = null;
        }

        if (mMulticastLock != null) {
            mMulticastLock.release();

            mMulticastLock = null;
        }

        // stop listening datagram socket
        if (mDatagramReceiver != null) {
            mDatagramReceiver.stop();

            mDatagramReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroy");
    }

    @Override
    public void onPingStart(String address) {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.pingStart(address);
        }
    }

    @Override
    public void onPingStop() {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.pingStop();
        }
    }

    @Override
    public void onPingLog(String line) {
        SettingsFragment fragmentSettings = (SettingsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_settings);

        if (fragmentSettings != null) {
            fragmentSettings.pingLog(line);
        }
    }

    @Override
    public void onServerAddressObtained(int address) {
        SettingsFragment fragmentSettings = (SettingsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_settings);

        if (fragmentSettings != null) {
            fragmentSettings.setPingServerAddress(address);
        }
    }

    @Override
    public void onDatagramReceived(String datagramMessage) {
        Toast.makeText(this, "DATAGRAM:\n" + datagramMessage, 1).show();
    }

    private void showSettings() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.beginTransaction()
            .hide(fragmentManager.findFragmentByTag(PagerFragment.tag_fragment_pager))
            .show(fragmentManager.findFragmentByTag(PagerFragment.tag_fragment_settings))
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(null)
            .commit();
    }
}

