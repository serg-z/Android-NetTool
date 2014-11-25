package com.Chris.NetTool;

import android.app.Activity;
import android.app.AlertDialog;

import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.VideoView;
import android.widget.Button;
import android.widget.CheckBox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.media.MediaPlayer;

import android.text.InputType;

import android.net.Uri;

import android.support.v4.app.Fragment;

import android.util.Log;

import java.net.URL;
import java.net.MalformedURLException;

import java.lang.UnsupportedOperationException;

public class StreamFragment extends Fragment implements View.OnClickListener, Streamer.StreamerListener,
    MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "StreamFragment";

    private enum UIState {
        EVERYTHING_DISABLED,
        READY_TO_PLAY,
        PLAYING,
        PAUSED
    }

    Activity mActivity;
    VideoView mVideoView;
    EditText mVideoAddress;
    Button mButtonPlay, mButtonPause, mButtonStop, mButtonRandomSeek;
    Slider mSliderBitrate, mSliderBufferSize, mSliderChunkSize;
    VerticalProgressBar mProgressBarBufferDepth;
    boolean mVideoIsPaused = false;
    CheckBox mCheckBoxRepeat, mCheckBoxUseVideoView;

    Streamer mStreamer;

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

        mVideoAddress = new EditText(mActivity);

        mVideoAddress.setSingleLine(true);
        mVideoAddress.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        mVideoAddress.setText("http://ravewireless.com/content/StandardCycleContent/NewFamily/LATAM_ContentFull/1/2001436/divx_LATAM-MadMenChristmas_eng_video_level4_vbv9600_1200kbps.mkv");

        layout.addView(mVideoAddress);

        LinearLayout layoutHorizontal = new LinearLayout(mActivity);

        layoutHorizontal.setOrientation(LinearLayout.HORIZONTAL);
        layoutHorizontal.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layout.addView(layoutHorizontal);

        // left

        LinearLayout layoutLeft = new LinearLayout(mActivity);

        layoutLeft.setOrientation(LinearLayout.VERTICAL);
        layoutLeft.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        layoutHorizontal.addView(layoutLeft);

        // control buttons

        LinearLayout layoutH = new LinearLayout(mActivity);

        layoutH.setOrientation(LinearLayout.HORIZONTAL);
        layoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        layoutLeft.addView(layoutH);

        mButtonPlay = new Button(mActivity);

        layoutH.addView(mButtonPlay);

        mButtonPlay.setText("Play");

        mButtonPlay.setOnClickListener(this);

        mButtonPause = new Button(mActivity);

        layoutH.addView(mButtonPause);

        mButtonPause.setText("Pause");

        mButtonPause.setOnClickListener(this);

        mButtonStop = new Button(mActivity);

        layoutH.addView(mButtonStop);

        mButtonStop.setText("Stop");

        mButtonStop.setOnClickListener(this);

        mCheckBoxRepeat = new CheckBox(mActivity);

        layoutH.addView(mCheckBoxRepeat);

        mCheckBoxRepeat.setText("Repeat");

        mCheckBoxRepeat.setEnabled(false);

        //

        layoutH = new LinearLayout(mActivity);

        layoutH.setOrientation(LinearLayout.HORIZONTAL);
        layoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        layoutLeft.addView(layoutH);

        mButtonRandomSeek = new Button(mActivity);

        mButtonRandomSeek.setOnClickListener(this);

        layoutH.addView(mButtonRandomSeek);

        mButtonRandomSeek.setText("Random Seek");

        mButtonRandomSeek.setEnabled(false);

        //

        mCheckBoxUseVideoView = new CheckBox(mActivity);

        layoutH.addView(mCheckBoxUseVideoView);

        mCheckBoxUseVideoView.setText("[use video view]");

        //

        mSliderBitrate = new Slider(mActivity);

        mSliderBitrate.setLabel("Bitrate (Kbps)");
        mSliderBitrate.setMin(400);
        mSliderBitrate.setMax(1600);
        mSliderBitrate.setStep(200);

        mSliderBitrate.setAdjustedProgress(1400);

        layoutLeft.addView(mSliderBitrate);

        //

        mSliderBufferSize = new Slider(mActivity);

        mSliderBufferSize.setLabel("Buffer size (seconds)");
        mSliderBufferSize.setMin(0);
        mSliderBufferSize.setMax(360);

        mSliderBufferSize.setAdjustedProgress(240);

        layoutLeft.addView(mSliderBufferSize);

        //

        mSliderChunkSize = new Slider(mActivity);

        mSliderChunkSize.setLabel("Chunk size (seconds)");
        mSliderChunkSize.setMin(1);
        mSliderChunkSize.setMax(20);

        mSliderChunkSize.setAdjustedProgress(2);

        layoutLeft.addView(mSliderChunkSize);

        // right

        LinearLayout layoutRight = new LinearLayout(mActivity);

        layoutRight.setOrientation(LinearLayout.VERTICAL);
        layoutRight.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        layoutHorizontal.addView(layoutRight);

        mProgressBarBufferDepth = new VerticalProgressBar(mActivity, null, android.R.attr.progressBarStyleHorizontal);

        mProgressBarBufferDepth.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layoutRight.addView(mProgressBarBufferDepth);

        mProgressBarBufferDepth.setMax(100);
        mProgressBarBufferDepth.setProgress(0);

        LinearLayout layoutVideo = new LinearLayout(mActivity);

        layoutVideo.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layoutLeft.addView(layoutVideo);

        mVideoView = new VideoView(mActivity);

        mVideoView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 300));

        layoutVideo.addView(mVideoView);

        mVideoView.setVisibility(View.VISIBLE);

        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnErrorListener(this);

        setUIState(UIState.READY_TO_PLAY);

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mStreamer != null) {
            mStreamer.stop();

            mProgressBarBufferDepth.setProgress(0);
        }
    }

    private void setUIState(UIState state) {
        switch (state) {
            case EVERYTHING_DISABLED:
                mButtonPlay.setEnabled(false);
                mButtonPause.setEnabled(false);
                mButtonStop.setEnabled(false);
                //mButtonRandomSeek.setEnabled(false);

                mCheckBoxUseVideoView.setEnabled(false);
                mCheckBoxRepeat.setEnabled(false);

                mSliderBitrate.setEnabled(false);
                mSliderBufferSize.setEnabled(false);
                mSliderChunkSize.setEnabled(false);

                break;

            case READY_TO_PLAY:
                mButtonPlay.setEnabled(true);
                mButtonPause.setEnabled(false);
                mButtonStop.setEnabled(false);
                //mButtonRandomSeek.setEnabled(false);

                mCheckBoxUseVideoView.setEnabled(true);
                //mCheckBoxRepeat.setEnabled(true);

                mSliderBitrate.setEnabled(true);
                mSliderBufferSize.setEnabled(true);
                mSliderChunkSize.setEnabled(true);

                break;

            case PLAYING:
                mButtonPlay.setEnabled(false);
                mButtonPause.setEnabled(true);
                mButtonStop.setEnabled(true);
                //mButtonRandomSeek.setEnabled(true);

                mCheckBoxUseVideoView.setEnabled(false);
                //mCheckBoxRepeat.setEnabled(true);

                mSliderBitrate.setEnabled(false);
                mSliderBufferSize.setEnabled(false);
                mSliderChunkSize.setEnabled(false);

                break;

            case PAUSED:
                mButtonPlay.setEnabled(true);
                mButtonPause.setEnabled(false);
                mButtonStop.setEnabled(false);
                //mButtonRandomSeek.setEnabled(false);

                mCheckBoxUseVideoView.setEnabled(false);
                //mCheckBoxRepeat.setEnabled(true);

                mSliderBitrate.setEnabled(false);
                mSliderBufferSize.setEnabled(false);
                mSliderChunkSize.setEnabled(false);

                break;

            default:
                throw new UnsupportedOperationException("UIState " + state + " is not implemented");
        }
    }

    public void onClick(View view) {
        if (view == mButtonPlay) {
            URL url = null;

            try {
                url = new URL(mVideoAddress.getText().toString().trim());
            } catch (MalformedURLException e) {
                e.printStackTrace();

                mVideoAddress.setError("URL is malformed");

                return;
            }

            if (!mVideoIsPaused) {
                mVideoView.setVideoURI(Uri.parse(url.toString()));
            }

            if (mCheckBoxUseVideoView.isChecked()) {
                mVideoView.start();
            } else {
                int bitrate = mSliderBitrate.getAdjustedProgress();
                int bufferSize = mSliderBufferSize.getAdjustedProgress();
                int chunkSize = mSliderChunkSize.getAdjustedProgress();

                if (bufferSize != 0 && bufferSize < chunkSize) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);

                    alertDialogBuilder
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton("OK", null)
                        .setMessage("Buffer size should be greater or equal to chunk size (or zero, if disabled)")
                        .setTitle("Invalid buffer size");


                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();

                    return;
                }

                if (bitrate > 0 && bufferSize >= 0 && chunkSize > 0) {
                    mStreamer = new Streamer(url, bitrate, chunkSize, bufferSize);

                    mStreamer.setStreamerListener(this);
                }
            }

            if (mCheckBoxUseVideoView.isChecked() && mVideoIsPaused) {
                setUIState(UIState.PLAYING);
            } else {
                setUIState(UIState.EVERYTHING_DISABLED);
            }

            mVideoIsPaused = false;
        } else if (view == mButtonPause) {
            if (mCheckBoxUseVideoView.isChecked()) {
                mVideoView.pause();

                setUIState(UIState.PAUSED);
            } else {
            }

            mVideoIsPaused = true;
        } else if (view == mButtonStop) {
            if (mCheckBoxUseVideoView.isChecked()) {
                mVideoView.stopPlayback();

                setUIState(UIState.READY_TO_PLAY);
            } else {
                if (mStreamer != null) {
                    mStreamer.stop();

                    mStreamer = null;

                    mProgressBarBufferDepth.setProgress(0);
                }
            }

            mVideoIsPaused = false;
        } else if (view == mButtonRandomSeek) {
            if (mCheckBoxUseVideoView.isChecked()) {
                mVideoView.seekTo(0);
            } else {
            }
        }
    }

    @Override
    public void onStreamStarted() {
        setUIState(UIState.PLAYING);
    }

    @Override
    public void onStreamStopped() {
        setUIState(UIState.READY_TO_PLAY);
    }

    @Override
    public void onStreamDepthBufferLoadChanged(int value) {
        mProgressBarBufferDepth.setProgress(value);
        mProgressBarBufferDepth.invalidate();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mCheckBoxUseVideoView.isChecked()) {
            setUIState(UIState.PLAYING);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mCheckBoxUseVideoView.isChecked()) {
            setUIState(UIState.READY_TO_PLAY);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO: show error message
        // will cause onCompletion to be called
        return false;
    }
}
