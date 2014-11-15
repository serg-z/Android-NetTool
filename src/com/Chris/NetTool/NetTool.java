package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.util.Log;

public class NetTool extends Activity {
    private static final String TAG = "NetTool";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Created");
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "Started");
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "Pause");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "Resume");
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
