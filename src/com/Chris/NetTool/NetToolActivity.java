package com.Chris.NetTool;

import android.os.Bundle;
import android.os.PowerManager;
import android.os.CountDownTimer;
import android.os.SystemClock;

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

public class NetToolActivity extends FragmentActivity implements SettingsFragment.OnPingListener,
    GraphsFragment.OnWifiInfoListener, DatagramReceiver.DatagramReceiverListener,
    StreamerFragment.StreamerFragmentListener {

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

        Log.d(TAG, "Destroy, finishing=" + isFinishing());
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

        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        final int notificationMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        final int notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        final int beepVolume = (int)(((float)notificationVolume * 100) / notificationMaxVolume);

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

                Toast.makeText(this, "Starting streamer in " + randomDelay + "s", 1).show();

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
            fragmentGraphs.clearPlotChunkDownloadTime();
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

        mDatagramReceiver = null;

        streamerStop();

        Intent intent = getIntent();
        finish();

        // wait until datagram receiver is done
        SystemClock.sleep(10000);

        startActivity(intent);
    }
}

