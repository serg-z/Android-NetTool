package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.ImageButton;
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

public class PagerFragment extends Fragment implements ImageButton.OnClickListener {
    private static final String TAG = "PagerFragment";

    static final String tag_fragment_graphs = "android:switcher:" + R.id.view_pager + ":0";
    static final String tag_fragment_stream = "android:switcher:" + R.id.view_pager + ":1";
    static final String tag_fragment_settings = "android:switcher:" + R.id.view_pager + ":2";
    static final String tag_fragment_pager = "fragment_pager";

    private Activity mActivity;

    private ImageButton mButtonSettings;

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

        // version

        LinearLayout layoutBottom = new LinearLayout(mActivity);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT);

        layoutParams.gravity = Gravity.RIGHT;

        layoutBottom.setLayoutParams(layoutParams);

        layout.addView(layoutBottom);

        layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT);

        layoutParams.gravity = Gravity.CENTER_VERTICAL;

        TextView textVersion = new TextView(mActivity);

        layoutBottom.addView(textVersion, layoutParams);

        String versionString = "UNDEFINED";

        try {
            versionString = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        textVersion.setText("Version: " + versionString);

        // settings button

        mButtonSettings = new ImageButton(mActivity);

        mButtonSettings.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT));

        layoutBottom.addView(mButtonSettings);

        mButtonSettings.setImageResource(android.R.drawable.ic_menu_manage);
        mButtonSettings.setOnClickListener(this);

        // add fragments

        if (savedInstanceState == null) {
            GraphsFragment fragmentGraphs = new GraphsFragment();
            StreamFragment fragmentStream = new StreamFragment();
            SettingsFragment fragmentSettings = new SettingsFragment();

            ((FragmentActivity)mActivity).getSupportFragmentManager().beginTransaction()
                .add(R.id.view_pager, fragmentGraphs, tag_fragment_graphs)
                .add(R.id.view_pager, fragmentStream, tag_fragment_stream)
                .commit();
        }

        return layout;
    }

    @Override
    public void onClick(View view) {
        if (view == mButtonSettings) {
            FragmentManager fragmentManager = ((FragmentActivity)mActivity).getSupportFragmentManager();

            fragmentManager.beginTransaction()
                .hide(fragmentManager.findFragmentByTag(tag_fragment_pager))
                .add(android.R.id.content, new SettingsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
        }
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
