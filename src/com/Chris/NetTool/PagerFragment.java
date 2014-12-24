package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Gravity;

import android.content.pm.PackageManager.NameNotFoundException;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.view.ViewPager;

public class PagerFragment extends Fragment {
    private static final String TAG = "PagerFragment";

    static final String tag_fragment_graphs = "android:switcher:" + R.id.view_pager + ":0";
    static final String tag_fragment_streamer = "android:switcher:" + R.id.view_pager + ":1";
    static final String tag_fragment_settings = "fragment_settings";
    static final String tag_fragment_pager = "fragment_pager";

    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        NetToolFragmentPagerAdapter adapter =
            new NetToolFragmentPagerAdapter(((FragmentActivity)mActivity).getSupportFragmentManager());

        LinearLayout layout = new LinearLayout(mActivity);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // view pager

        ViewPager viewPager = new ViewPager(mActivity);

        viewPager.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT, 1));

        layout.addView(viewPager);

        viewPager.setId(R.id.view_pager);
        viewPager.setOffscreenPageLimit(1);

        viewPager.setAdapter(adapter);

        // driver description and version

        RelativeLayout layoutBottom = new RelativeLayout(mActivity);

        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT);

        bottomParams.setMargins(10, 0, 10, 5);

        layoutBottom.setLayoutParams(bottomParams);

        layout.addView(layoutBottom);

        // wlan0's driver description

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT);

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

        TextView textDriverDesc = new TextView(mActivity);

        layoutBottom.addView(textDriverDesc, layoutParams);

        textDriverDesc.setText("wlan0's driver: " + Utils.wlan0DriverDesc());

        // version

        layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT);

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        TextView textVersion = new TextView(mActivity);

        layoutBottom.addView(textVersion, layoutParams);

        String versionString = "UNDEFINED";

        try {
            versionString = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        textVersion.setText("Version: " + versionString);

        // add fragments

        if (savedInstanceState == null) {
            GraphsFragment fragmentGraphs = new GraphsFragment();
            StreamerFragment fragmentStreamer = new StreamerFragment();
            SettingsFragment fragmentSettings = new SettingsFragment();

            ((FragmentActivity)mActivity).getSupportFragmentManager().beginTransaction()
                .add(R.id.view_pager, fragmentGraphs, tag_fragment_graphs)
                .add(R.id.view_pager, fragmentStreamer, tag_fragment_streamer)
                .add(android.R.id.content, fragmentSettings, tag_fragment_settings)
                .hide(fragmentSettings)
                .commit();
        }

        return layout;
    }

    private static class NetToolFragmentPagerAdapter extends FragmentPagerAdapter {
        public NetToolFragmentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            // fragments should be already in fragment manager
            return null;
        }
    }
}
