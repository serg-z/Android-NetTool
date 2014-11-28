package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.widget.LinearLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class PagerFragment extends Fragment {
    private static final String TAG = "PagerFragment";

    static final String tag_fragment_graphs = "android:switcher:" + R.id.view_pager + ":0";
    static final String tag_fragment_stream = "android:switcher:" + R.id.view_pager + ":1";
    static final String tag_fragment_settings = "android:switcher:" + R.id.view_pager + ":2";

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

        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        layout.setFilterTouchesWhenObscured(true);

        ViewPager viewPager = new ViewPager(mActivity);

        viewPager.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layout.addView(viewPager);

        viewPager.setId(R.id.view_pager);
        viewPager.setOffscreenPageLimit(2);

        viewPager.setAdapter(adapter);

        if (savedInstanceState == null) {
            GraphsFragment fragmentGraphs = new GraphsFragment();
            StreamFragment fragmentStream = new StreamFragment();
            SettingsFragment fragmentSettings = new SettingsFragment();

            ((FragmentActivity)mActivity).getSupportFragmentManager().beginTransaction()
                .add(R.id.view_pager, fragmentGraphs, tag_fragment_graphs)
                .add(R.id.view_pager, fragmentStream, tag_fragment_stream)
                .add(R.id.view_pager, fragmentSettings, tag_fragment_settings)
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
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            // fragments should be already in fragment manager
            return null;
        }
    }
}
