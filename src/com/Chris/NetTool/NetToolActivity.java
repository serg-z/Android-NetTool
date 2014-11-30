package com.Chris.NetTool;

import android.os.Bundle;
import android.os.PowerManager;
import android.os.CountDownTimer;

import android.content.Context;

import android.view.Window;
import android.view.WindowManager;
import android.view.KeyEvent;

import android.widget.Toast;

import android.media.ToneGenerator;
import android.media.AudioManager;

import android.net.wifi.WifiManager;

import android.util.Log;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.util.Random;

public class NetToolActivity extends FragmentActivity implements SettingsFragment.OnPingListener,
    GraphsFragment.OnWifiInfoListener, DatagramReceiver.DatagramReceiverListener {

    private static final String TAG = "NetToolActivity";

    private PowerManager.WakeLock mWakeLock = null;
    private DatagramReceiver mDatagramReceiver = null;
    private WifiManager.WifiLock mWifiLock = null;
    private WifiManager.MulticastLock mMulticastLock = null;
    private CountDownTimer mCountDownTimerStreamerStart = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_DeviceDefault);

        super.onCreate(savedInstanceState);

        Log.d(TAG, "Created");

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (savedInstanceState == null) {
            PagerFragment fragmentPager = new PagerFragment();

            getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragmentPager, PagerFragment.tag_fragment_pager)
                .commit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showSettings();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "Started");

        mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE))
            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

        mWakeLock.acquire();

        // acquire wifi lock
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE))
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG + "_WifiLock");

        mWifiLock.acquire();

        // acquire multicast lock
        mMulticastLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE))
            .createMulticastLock(TAG + "_MulticastLock");

        mMulticastLock.acquire();

        // create listening datagram socket
        if (mDatagramReceiver == null) {
            mDatagramReceiver = new DatagramReceiver(55555);

            mDatagramReceiver.setListener(this);
        }
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

        if (mWakeLock != null) {
            mWakeLock.release();

            mWakeLock = null;
        }

        if (mWifiLock != null) {
            mWifiLock.release();

            mWifiLock = null;
        }

        if (mMulticastLock != null) {
            mMulticastLock.release();

            mMulticastLock = null;
        }

        // stop listening datagram socket
        if (mDatagramReceiver != null) {
            mDatagramReceiver.stop();

            mDatagramReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroy");
    }

    @Override
    public void onPingStart(String address) {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.pingStart(address);
        }
    }

    @Override
    public void onPingStop() {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.pingStop();
        }
    }

    @Override
    public void onPingLog(String line) {
        SettingsFragment fragmentSettings = (SettingsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_settings);

        if (fragmentSettings != null) {
            fragmentSettings.pingLog(line);
        }
    }

    @Override
    public void onServerAddressObtained(int address) {
        SettingsFragment fragmentSettings = (SettingsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_settings);

        if (fragmentSettings != null) {
            fragmentSettings.setPingServerAddress(address);
        }
    }

    @Override
    public void onDatagramReceived(String datagramMessage) {
        Toast.makeText(this, "DATAGRAM:\n" + datagramMessage, 1).show();

        boolean countdownBeep = false;

        for (String line : datagramMessage.split("\n")) {
            final String[] lineArray = line.trim().split("=");

            if (lineArray.length != 2) {
                Log.d(TAG, "Erroneous line: " + line);

                continue;
            }

            final String name = lineArray[0];
            final String value = lineArray[1];

            if (name.equals("address")) {
                StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_streamer);

                if (fragmentStreamer != null) {
                    fragmentStreamer.setVideoAddress(value);
                }
            } else if (name.equals("countdown_beep")) {
                countdownBeep = value.equals("on");
            } else if (name.equals("random_start_delay")) {
                final int delay = Integer.valueOf(value);

                if (delay == 0) {
                    streamerStart();

                    continue;
                }

                if (mCountDownTimerStreamerStart != null) {
                    mCountDownTimerStreamerStart.cancel();
                }

                // delay in range [1, value]
                final int randomDelay = (new Random()).nextInt(delay) + 1;

                Toast.makeText(this, "Starting streamer in " + randomDelay + "s", 1).show();

                final ToneGenerator toneGenerator;

                if (countdownBeep) {
                    toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                } else {
                    toneGenerator = null;
                }

                mCountDownTimerStreamerStart =
                    new CountDownTimer(randomDelay * 1000 + 100, 900) {
                        public void onTick(long millisUntilFinished) {
                            if (toneGenerator != null) {
                                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                            }
                        }

                        public void onFinish() {
                            streamerStart();
                        }
                    }.start();
            }
        }
    }

    private void showSettings() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.beginTransaction()
            .hide(fragmentManager.findFragmentByTag(PagerFragment.tag_fragment_pager))
            .show(fragmentManager.findFragmentByTag(PagerFragment.tag_fragment_settings))
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(null)
            .commit();
    }

    private void streamerStart() {
        StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_streamer);

        if (fragmentStreamer != null) {
            fragmentStreamer.streamStart();
        }
    }
}

