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
    private GraphsFragment.OnPingListener mPingCallback;

    private Button mButtonPingStart, mButtonPingStop, mButtonShareLog;
    private EditText mEditPingAddress;
    private TextView mTextViewLog;

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

        // log text view

        mTextViewLog = new TextView(mActivity);

        mTextViewLog.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT));

        layout.addView(mTextViewLog);

        mTextViewLog.setLines(20);

        // log share button

        mButtonShareLog = new Button(mActivity);

        mButtonShareLog.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT));

        layout.addView(mButtonShareLog);

        mButtonShareLog.setText("Share NetTool log");
        mButtonShareLog.setOnClickListener(this);

        return layout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mPingCallback = (GraphsFragment.OnPingListener)activity;
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
        } else if (view == mButtonShareLog) {
            Intent intent = new Intent(Intent.ACTION_SEND);

            intent.setType("text/plain");

            intent.putExtra(Intent.EXTRA_SUBJECT, "NetTool Log");
            intent.putExtra(Intent.EXTRA_TEXT, mTextViewLog.getText());

            mActivity.startActivity(Intent.createChooser(intent, "Share NetTool log"));
        }
    }

    public void setPingServerAddress(int address) {
        mEditPingAddress.setText(Formatter.formatIpAddress(address));
    }

    public void setPingServerAddress(String address) {
        mEditPingAddress.setText(address);
    }

    public void prependLog(String line) {
        mTextViewLog.setText(line + "\n" + mTextViewLog.getText());
    }

    public void pingStart() {
        mPingCallback.onPingStart(mEditPingAddress.getText().toString());
    }

    public void pingStop() {
        mPingCallback.onPingStop();
    }
}
