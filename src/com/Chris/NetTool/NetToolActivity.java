package com.Chris.NetTool;

import android.os.Bundle;
import android.os.PowerManager;

import android.content.Context;

import android.view.Window;
import android.view.WindowManager;
import android.view.KeyEvent;

import android.util.Log;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class NetToolActivity extends FragmentActivity implements SettingsFragment.OnPingListener,
    GraphsFragment.OnWifiInfoListener {

    private static final String TAG = "NetToolActivity";

    private PowerManager.WakeLock mWakeLock = null;

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

