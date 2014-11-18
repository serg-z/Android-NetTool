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

    private static final String tag_fragment_graphs = "android:switcher:" + R.id.view_pager + ":0";
    private static final String tag_fragment_stream = "android:switcher:" + R.id.view_pager + ":1";
    private static final String tag_fragment_ping   = "android:switcher:" + R.id.view_pager + ":2";

    NetToolFragmentPagerAdapter mAdapter;
    ViewPager mViewPager;
    PowerManager.WakeLock mWakeLock = null;

    public void setServerAddress(int address) {
        PingFragment fragment = (PingFragment)getSupportFragmentManager().findFragmentByTag(tag_fragment_ping);

        if (address != 0 && fragment != null) {
            fragment.setServerAddress(address);
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

        if (savedInstanceState == null) {
            GraphsFragment fragmentGraphs = new GraphsFragment();
            StreamFragment fragmentStream = new StreamFragment();
            PingFragment fragmentPing = new PingFragment();

            getSupportFragmentManager().beginTransaction()
                .add(R.id.view_pager, fragmentGraphs, tag_fragment_graphs)
                .add(R.id.view_pager, fragmentStream, tag_fragment_stream)
                .add(R.id.view_pager, fragmentPing, tag_fragment_ping)
                .commit();
        }
    }

    public static class NetToolFragmentPagerAdapter extends FragmentPagerAdapter {
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

