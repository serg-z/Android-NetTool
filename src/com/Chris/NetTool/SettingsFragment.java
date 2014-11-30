package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.content.Intent;

import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.text.format.Formatter;

import android.support.v4.app.Fragment;

public class SettingsFragment extends Fragment implements View.OnClickListener {
    private Activity mActivity;
    private OnPingListener mPingCallback;

    private Button mButtonPingStart, mButtonPingStop, mButtonPingLogShare;
    private EditText mEditPingAddress;
    private TextView mPingLog;

    public interface OnPingListener {
        public void onPingStart(String address);
        public void onPingStop();
        public void onPingLog(String line);
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

        // ping address

        mEditPingAddress = new EditText(mActivity);

        mEditPingAddress.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT));

        layout.addView(mEditPingAddress);

        mEditPingAddress.setSingleLine(true);
        mEditPingAddress.setText("localhost");

        // ping buttons

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

        // ping log

        mPingLog = new TextView(mActivity);

        mPingLog.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT));

        layout.addView(mPingLog);

        mPingLog.setLines(20);

        // ping share

        mButtonPingLogShare = new Button(mActivity);

        mButtonPingLogShare.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT));

        layout.addView(mButtonPingLogShare);

        mButtonPingLogShare.setText("Share ping log");
        mButtonPingLogShare.setOnClickListener(this);

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
            pingStart();
        } else if (view == mButtonPingStop) {
            pingStop();
        } else if (view == mButtonPingLogShare) {
            Intent intent = new Intent(Intent.ACTION_SEND);

            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Ping Log");
            intent.putExtra(Intent.EXTRA_TEXT, mPingLog.getText());

            mActivity.startActivity(Intent.createChooser(intent, "Share ping log"));
        }
    }

    public void setPingServerAddress(int address) {
        mEditPingAddress.setText(Formatter.formatIpAddress(address));
    }

    public void setPingServerAddress(String address) {
        mEditPingAddress.setText(address);
    }

    public void pingLog(String line) {
        mPingLog.setText(line + "\n" + mPingLog.getText());
    }

    public void pingStart() {
        mPingCallback.onPingStart(mEditPingAddress.getText().toString());
    }

    public void pingStop() {
        mPingCallback.onPingStop();
    }
}
