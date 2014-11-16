package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.TextView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.support.v4.app.Fragment;

public class StreamFragment extends Fragment {
    Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(mActivity);

        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        TextView tv = new TextView(mActivity);

        layout.addView(tv);

        tv.setText("Stream");

        return layout;
    }
}
