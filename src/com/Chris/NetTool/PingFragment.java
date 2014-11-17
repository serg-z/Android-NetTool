package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.View;

import android.text.format.Formatter;

import android.support.v4.app.Fragment;

public class PingFragment extends Fragment {
    Activity mActivity;
    Button mButtonStart, mButtonStop;
    EditText mAddress;
    int mServerAddress = 0;

    public void setServerAddress(int address) {
        if (mServerAddress != address) {
            mServerAddress = address;

            mAddress.setText(Formatter.formatIpAddress(mServerAddress));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(mActivity);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mAddress = new EditText(mActivity);

        mAddress.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        layout.addView(mAddress);

        LinearLayout layoutH = new LinearLayout(mActivity);

        layoutH.setOrientation(LinearLayout.HORIZONTAL);
        layoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        layout.addView(layoutH);

        mButtonStart = new Button(mActivity);

        layoutH.addView(mButtonStart);

        mButtonStart.setText("Start");

        mButtonStop = new Button(mActivity);

        layoutH.addView(mButtonStop);

        mButtonStop.setText("Stop");

        return layout;
    }
}
