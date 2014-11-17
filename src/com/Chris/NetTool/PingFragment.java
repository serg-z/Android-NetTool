package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;
import android.os.AsyncTask;

import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.View;

import android.text.format.Formatter;

import android.util.Log;

import android.support.v4.app.Fragment;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class PingFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "PingFragment";

    Activity mActivity;
    Button mButtonStart, mButtonStop;
    EditText mAddress;
    int mServerAddress = 0;
    PingTask mPingTask;
    TextView mPingLog;

    public void setServerAddress(int address) {
        if (mServerAddress != address) {
            mServerAddress = address;

            mAddress.setText(Formatter.formatIpAddress(mServerAddress));
        }
    }

    public void appendPingLog(String line) {
        if (mPingLog != null) {
            mPingLog.setText(line + "\n" + mPingLog.getText());
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
        mButtonStart.setOnClickListener(this);

        mButtonStop = new Button(mActivity);

        layoutH.addView(mButtonStop);

        mButtonStop.setText("Stop");
        mButtonStop.setOnClickListener(this);

        mPingLog = new TextView(mActivity);

        layout.addView(mPingLog);

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "Resume");
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "Pause");
    }

    protected void startPing() {
        if (mPingTask != null && mPingTask.getStatus() == AsyncTask.Status.FINISHED) {
            mPingTask.stop();

            mPingTask = null;
        }

        if (mPingTask == null) {
            mPingTask = new PingTask();

            mPingTask.mPingFragment = this;
        }

        if (mPingTask.getStatus() != AsyncTask.Status.RUNNING) {
            mPingTask.execute(mAddress.getText().toString());
        }
    }

    protected void stopPing() {
        if (mPingTask != null && mPingTask.getStatus() == AsyncTask.Status.RUNNING) {
            mPingTask.stop();

            mPingTask = null;
        }
    }

    public void onClick(View view) {
        if (view == mButtonStart) {
            startPing();
        } else if (view == mButtonStop) {
            stopPing();
        }
    }

    class PingTask extends AsyncTask<String, Void, Void> {
        PipedOutputStream mPipedOut;
        PipedInputStream mPipedIn;
        LineNumberReader mReader;
        Process mProcess;
        PingFragment mPingFragment;

        @Override
        protected void onPreExecute() {
            mPipedOut = new PipedOutputStream();

            try {
                mPipedIn = new PipedInputStream(mPipedOut);
                mReader = new LineNumberReader(new InputStreamReader(mPipedIn));
            } catch (IOException e) {
                cancel(true);
            }
        }

        public void stop() {
            Process p = mProcess;

            if (p != null) {
                p.destroy();
            }

            cancel(true);
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                mProcess = new ProcessBuilder()
                    .command("/system/bin/ping", params[0])
                    .redirectErrorStream(true)
                    .start();

                try {
                    InputStream in = mProcess.getInputStream();
                    OutputStream out = mProcess.getOutputStream();

                    byte[] buffer = new byte[1024];
                    int count;

                    while ((count = in.read(buffer)) != -1) {
                        mPipedOut.write(buffer, 0, count);
                        publishProgress();

                        if (isCancelled()) {
                            Log.d(TAG, "PingTask cancelled");

                            break;
                        }
                    }

                    out.close();
                    in.close();

                    mPipedOut.close();
                    mPipedIn.close();
                } finally {
                    mProcess.destroy();
                    mProcess = null;
                }
            } catch (IOException e) {
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            try {
                while (mReader.ready()) {
                    String line = mReader.readLine();

                    if (mPingFragment != null) {
                        mPingFragment.appendPingLog(line);
                    }

                    Log.d(TAG, "OUTPUT: " + line);
                }
            } catch (IOException t) {
            }
        }
    }
}
