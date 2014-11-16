package com.Chris.NetTool;

import android.os.Bundle;

import android.widget.LinearLayout;

import android.view.ViewGroup.LayoutParams;
import android.view.Window;

import android.util.Log;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class NetToolActivity extends FragmentActivity {
    public static final String TAG = "NetTool";

    NetToolFragmentPagerAdapter mAdapter;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mAdapter = new NetToolFragmentPagerAdapter(getSupportFragmentManager());

        LinearLayout layout = new LinearLayout(this);

        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setContentView(layout);

        mViewPager = new ViewPager(this);

        mViewPager.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layout.addView(mViewPager);

        mViewPager.setId(R.id.view_pager);

        mViewPager.setAdapter(mAdapter);

        mAdapter.mFragmentGraphs = new GraphsFragment();
        mAdapter.mFragmentStream = new StreamFragment();
    }

    public static class NetToolFragmentPagerAdapter extends FragmentPagerAdapter {
        GraphsFragment mFragmentGraphs;
        StreamFragment mFragmentStream;

        public NetToolFragmentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return mFragmentGraphs;
            } else {
                return mFragmentStream;
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

        mAdapter.mFragmentGraphs.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAdapter.mFragmentGraphs.resume();
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

