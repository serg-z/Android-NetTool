package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.text.format.Formatter;

import android.support.v4.app.Fragment;

public class SettingsFragment extends Fragment implements View.OnClickListener {
    private Activity mActivity;
    private OnPingListener mPingCallback;

    private Button mButtonPingStart, mButtonPingStop;
    private EditText mEditPingAddress;

    public interface OnPingListener {
        public void onPingStart(String address);
        public void onPingStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        LinearLayout layout = new LinearLayout(mActivity);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // ping buttons

        mEditPingAddress = new EditText(mActivity);

        mEditPingAddress.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT));

        layout.addView(mEditPingAddress);

        mEditPingAddress.setSingleLine(true);
        mEditPingAddress.setText("localhost");

        LinearLayout layoutH = new LinearLayout(mActivity);

        layoutH.setOrientation(LinearLayout.HORIZONTAL);
        layoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        layout.addView(layoutH);

        mButtonPingStart = new Button(mActivity);

        layoutH.addView(mButtonPingStart);

        mButtonPingStart.setText("Start");
        mButtonPingStart.setOnClickListener(this);

        mButtonPingStop = new Button(mActivity);

        layoutH.addView(mButtonPingStop);

        mButtonPingStop.setText("Stop");
        mButtonPingStop.setOnClickListener(this);

        return layout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mPingCallback = (OnPingListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPingListener");
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mButtonPingStart) {
            mPingCallback.onPingStart(mEditPingAddress.getText().toString());
        } else if (view == mButtonPingStop) {
            mPingCallback.onPingStop();
        }
    }

    public void setPingServerAddress(int address) {
        mEditPingAddress.setText(Formatter.formatIpAddress(address));
    }
}
