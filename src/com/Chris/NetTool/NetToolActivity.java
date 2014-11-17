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

public class NetToolActivity extends FragmentActivity {
    private static final String TAG = "NetToolActivity";

    NetToolFragmentPagerAdapter mAdapter;
    ViewPager mViewPager;
    PowerManager.WakeLock mWakeLock = null;

    public void setServerAddress(int address) {
        if (address != 0 && mAdapter != null && mAdapter.mFragmentPing != null) {
            mAdapter.mFragmentPing.setServerAddress(address);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Created");

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE))
            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

        mWakeLock.acquire();

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
    }

    public static class NetToolFragmentPagerAdapter extends FragmentPagerAdapter {
        GraphsFragment mFragmentGraphs = null;
        StreamFragment mFragmentStream = null;
        PingFragment mFragmentPing = null;

        public NetToolFragmentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                if (mFragmentGraphs == null) {
                    mFragmentGraphs = new GraphsFragment();
                }

                return mFragmentGraphs;
            } else if (position == 1) {
                if (mFragmentStream == null) {
                    mFragmentStream = new StreamFragment();
                }

                return mFragmentStream;
            } else  {
                if (mFragmentPing == null) {
                    mFragmentPing = new PingFragment();
                }

                return mFragmentPing;
            }
        }
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroy");
    }
}

