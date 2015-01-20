package com.Chris.NetTool;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;

import android.content.Context;
import android.content.Intent;

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
import java.util.List;
import java.util.ArrayList;

public class NetToolActivity extends FragmentActivity implements SettingsFragment.OnPingListener,
    GraphsFragment.OnWifiInfoListener, DatagramReceiver.DatagramReceiverListener,
    StreamerFragment.StreamerFragmentListener {

    private static final String TAG = "NetToolActivity";
    // delay before screen will go to lowered brightness (5 minutes in milliseconds)
    private static final int sDimDelay = 5 * 60 * 1000;

    private static boolean sBeepEnabled = false;

    private DatagramReceiver mDatagramReceiver = null;
    private WifiManager.WifiLock mWifiLock = null;
    private WifiManager.MulticastLock mMulticastLock = null;
    private CountDownTimer mCountDownTimerStreamerStart = null;
    private boolean mBrightnessIsDefault = true;
    private List<Integer> mDatagramIds = new ArrayList<Integer>();

    private Handler mTimerHandler = new Handler();

    private Runnable mTimerRunnableDim = new Runnable() {
        @Override
        public void run() {
            WindowManager.LayoutParams attr = getWindow().getAttributes();

            // set brightness to minimum value (20%)
            attr.screenBrightness = 0.2f;

            getWindow().setAttributes(attr);

            mBrightnessIsDefault = false;

            // queue this runnable to be run again
            mTimerHandler.postDelayed(this, sDimDelay);
        }
    };

    static public boolean getBeepEnabled() {
        return sBeepEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_DeviceDefault);

        super.onCreate(savedInstanceState);

        Log.d(TAG, "Created");

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        getWindow().addFlags(
              WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

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

        // dump wlan0's stats (if any)

        String stats = "wlan0's stats:\n" + Utils.dumpStats() + "\n=====\n";

        // to log
        Log.d(TAG, stats);

        // show toast
        Toast.makeText(this, stats, Toast.LENGTH_SHORT).show();

        // add to log in settings
        SettingsFragment fragmentSettings = (SettingsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_settings);

        if (fragmentSettings != null) {
            fragmentSettings.pingLog(stats);
        }

        // start dim runnable which will dim the display if there is no user interaction for defined period of time

        mTimerHandler.postDelayed(mTimerRunnableDim, sDimDelay);
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

            mDatagramReceiver.setListener(null);

            mDatagramReceiver = null;
        }

        // don't run dim runnable while the app is in background

        mTimerHandler.removeCallbacks(mTimerRunnableDim);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroy, finishing=" + isFinishing());
    }

    @Override
    public void onUserInteraction() {
        // restart dim timer

        mTimerHandler.removeCallbacks(mTimerRunnableDim);
        mTimerHandler.postDelayed(mTimerRunnableDim, sDimDelay);

        // put the brightness back to default value

        if (!mBrightnessIsDefault) {
            WindowManager.LayoutParams attr = getWindow().getAttributes();

            // using value less than 0 will put brightness into using preferred value mode
            attr.screenBrightness = -1.0f;

            getWindow().setAttributes(attr);

            mBrightnessIsDefault = true;
        }
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

            // start pinging
            fragmentSettings.pingStart();
        }
    }

    @Override
    public void onDatagramReceived(String datagramMessage) {
        // search for message id
        for (String line : datagramMessage.split("\n")) {
            final String[] lineArray = line.trim().split("=");

            if (lineArray.length != 2) {
                Log.d(TAG, "Erroneous line: " + line);

                continue;
            }

            final String name = lineArray[0];
            final String value = lineArray[1];

            if (name.equals("message_id")) {
                final int message_id = Integer.valueOf(value);

                // ignore "-1" ID
                if (message_id == -1) {
                    break;
                }

                // reject the message if it's already added to the list
                if (mDatagramIds.contains(message_id)) {
                    Log.d(TAG, "Datagram ID is already recorded: " + message_id);

                    return;
                }

                // add the message to the list and proceed with it
                mDatagramIds.add(message_id);

                break;
            }
        }

        Toast.makeText(this, "DATAGRAM:\n" + datagramMessage, Toast.LENGTH_SHORT).show();

        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        final int notificationMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        final int notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        final int beepVolume = (int)((notificationVolume * 100.0f) / notificationMaxVolume);

        new ToneGenerator(AudioManager.STREAM_ALARM, beepVolume)
            .startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);

        boolean countdownBeep = false;
        boolean startStream = false;
        boolean stopStream = false;
        int startStreamDelayMax = 0;
        int pingCommand = -1;

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
                startStream = true;
                startStreamDelayMax = Integer.valueOf(value);
            } else if (name.equals("streamer_stop") && value.equals("1")) {
                stopStream = true;
            } else if (name.equals("bitrate")) {
                StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_streamer);

                if (fragmentStreamer != null) {
                    fragmentStreamer.setBitrate(Integer.valueOf(value));
                }
            } else if (name.equals("buffer_size")) {
                StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_streamer);

                if (fragmentStreamer != null) {
                    fragmentStreamer.setBufferSize(Integer.valueOf(value));
                }
            } else if (name.equals("chunk_size")) {
                StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_streamer);

                if (fragmentStreamer != null) {
                    fragmentStreamer.setChunkSize(Integer.valueOf(value));
                }
            } else if (name.equals("ping_ip")) {
                SettingsFragment fragmentSettings = (SettingsFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_settings);

                if (fragmentSettings != null) {
                    fragmentSettings.setPingServerAddress(value);
                }
            } else if (name.equals("ping")) {
                pingCommand = value.equals("on") ? 1 : 0;
            } else if (name.equals("repeat")) {
                StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_streamer);

                if (fragmentStreamer != null) {
                    fragmentStreamer.setRepeat(value.equals("true"));
                }
            } else if (name.equals("use_video_view")) {
                StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_streamer);

                if (fragmentStreamer != null) {
                    fragmentStreamer.setUseVideoView(value.equals("true"));
                }
            } else if (name.equals("restart") && value.equals("1")) {
                restart();
            } else if (name.equals("streamer_connect_timeout")) {
                StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_streamer);

                if (fragmentStreamer != null) {
                    fragmentStreamer.setStreamerConnectTimeout(Integer.valueOf(value));
                }
            } else if (name.equals("streamer_read_timeout")) {
                StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_streamer);

                if (fragmentStreamer != null) {
                    fragmentStreamer.setStreamerReadTimeout(Integer.valueOf(value));
                }
            } else if (name.equals("ping_packet_size")) {
                GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
                    .findFragmentByTag(PagerFragment.tag_fragment_graphs);

                if (fragmentGraphs != null) {
                    fragmentGraphs.setPingPacketSize(Integer.valueOf(value));
                }
            } else if (name.equals("beeping_enabled")) {
                sBeepEnabled = value.equals("true");
            }
        }

        // start stream
        if (startStream) {
            if (startStreamDelayMax == 0) {
                streamerStart();
            } else {
                if (mCountDownTimerStreamerStart != null) {
                    mCountDownTimerStreamerStart.cancel();
                }

                // delay in range [1, value]
                final int randomDelay = (new Random()).nextInt(startStreamDelayMax) + 1;

                Toast.makeText(this, "Starting streamer in " + randomDelay + "s", Toast.LENGTH_SHORT).show();

                final ToneGenerator toneGenerator;

                if (countdownBeep) {
                    toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, beepVolume);
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

        // stop stream
        if (stopStream) {
            streamerStop();
        }

        // start/stop ping
        if (pingCommand > -1) {
            SettingsFragment fragmentSettings = (SettingsFragment)getSupportFragmentManager()
                .findFragmentByTag(PagerFragment.tag_fragment_settings);

            if (fragmentSettings != null) {
                if (pingCommand == 1) {
                    fragmentSettings.pingStart();
                } else {
                    fragmentSettings.pingStop();
                }
            }
        }
    }

    @Override
    public void onStreamerFragmentDownloadingStarted() {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.setPlotChunkAddZeroes(false);
        }
    }

    @Override
    public void onStreamerFragmentDownloadingProgressChanged(int downloadingProgress) {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.setStreamerDownloadingProgress(downloadingProgress);
        }
    }

    @Override
    public void onStreamerFragmentBufferDepthChanged(int bufferDepth) {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.setStreamerBufferDepth(bufferDepth);
        }
    }

    @Override
    public void onStreamerFragmentChunkDownloadTime(long time) {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.addChunkDownloadTime(time);
        }
    }

    @Override
    public void onStreamerFragmentFailed() {
        onStreamerFragmentFinished();
    }

    @Override
    public void onStreamerFragmentFinished() {
        GraphsFragment fragmentGraphs = (GraphsFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_graphs);

        if (fragmentGraphs != null) {
            fragmentGraphs.setPlotChunkAddZeroes(true);
        }
    }

    private void showSettings() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        SettingsFragment fragmentSettings = (SettingsFragment)fragmentManager
            .findFragmentByTag(PagerFragment.tag_fragment_settings);

        if (fragmentSettings.isVisible()) {
            return;
        }

        fragmentManager.beginTransaction()
            .hide(fragmentManager.findFragmentByTag(PagerFragment.tag_fragment_pager))
            .show(fragmentSettings)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(null)
            .commit();
    }

    private void streamerStart() {
        StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_streamer);

        if (fragmentStreamer != null) {
            fragmentStreamer.streamStop();

            fragmentStreamer.streamStart();
        }
    }

    private void streamerStop() {
        StreamerFragment fragmentStreamer = (StreamerFragment)getSupportFragmentManager()
            .findFragmentByTag(PagerFragment.tag_fragment_streamer);

        if (fragmentStreamer != null) {
            fragmentStreamer.streamStop();
        }
    }

    private void restart() {
        mDatagramReceiver.stop();

        mDatagramReceiver.setListener(null);

        mDatagramReceiver = null;

        streamerStop();

        Intent intent = getIntent();
        finish();

        startActivity(intent);
    }
}

