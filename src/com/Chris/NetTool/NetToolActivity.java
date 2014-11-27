package com.Chris.NetTool;

import android.os.Bundle;
import android.os.PowerManager;

import android.widget.LinearLayout;

import android.content.Context;

import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;

import android.util.Log;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class NetToolActivity extends FragmentActivity implements SettingsFragment.OnPingListener,
    GraphsFragment.OnWifiInfoListener {

    private static final String TAG = "NetToolActivity";

    private static final String tag_fragment_graphs = "android:switcher:" + R.id.view_pager + ":0";
    private static final String tag_fragment_stream = "android:switcher:" + R.id.view_pager + ":1";
    private static final String tag_fragment_settings = "android:switcher:" + R.id.view_pager + ":2";

    private NetToolFragmentPagerAdapter mAdapter;
    private ViewPager mViewPager;
    private PowerManager.WakeLock mWakeLock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Created");

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mAdapter = new NetToolFragmentPagerAdapter(getSupportFragmentManager());

        LinearLayout layout = new LinearLayout(this);

        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setContentView(layout);

        mViewPager = new ViewPager(this);

        mViewPager.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layout.addView(mViewPager);

        mViewPager.setId(R.id.view_pager);
        mViewPager.setOffscreenPageLimit(2);

        mViewPager.setAdapter(mAdapter);

        if (savedInstanceState == null) {
            GraphsFragment fragmentGraphs = new GraphsFragment();
            StreamFragment fragmentStream = new StreamFragment();
            SettingsFragment fragmentSettings = new SettingsFragment();

            getSupportFragmentManager().beginTransaction()
                .add(R.id.view_pager, fragmentGraphs, tag_fragment_graphs)
                .add(R.id.view_pager, fragmentStream, tag_fragment_stream)
                .add(R.id.view_pager, fragmentSettings, tag_fragment_settings)
                .commit();
        }
    }

    private static class NetToolFragmentPagerAdapter extends FragmentPagerAdapter {
        public NetToolFragmentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            // fragments should be already in fragment manager
            return null;
        }
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
            .findFragmentByTag(tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.pingStart(address);
        }
    }

    @Override
    public void onPingStop() {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.pingStop();
        }
    }

    @Override
    public void onServerAddressObtained(int address) {
        SettingsFragment fragmentSettings = (SettingsFragment)getSupportFragmentManager()
            .findFragmentByTag(tag_fragment_settings);

        if (fragmentSettings != null) {
            fragmentSettings.setPingServerAddress(address);
        }
    }
}

