package com.Chris.NetTool;

import android.app.Activity;
import android.app.AlertDialog;

import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.VideoView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

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
    VideoView mVideoView = null;
    EditText mVideoAddress;
    Button mButtonPlay, mButtonPause, mButtonStop, mButtonRandomSeek;
    Slider mSliderBitrate, mSliderBufferSize, mSliderChunkSize;
    VerticalProgressBar mProgressBarBufferDepth;
    boolean mVideoIsPaused = false;
    CheckBox mCheckBoxRepeat, mCheckBoxUseVideoView;
    TextView mTextStatus;
    LinearLayout mLayoutLeft;

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

        mLayoutLeft = new LinearLayout(mActivity);

        mLayoutLeft.setOrientation(LinearLayout.VERTICAL);
        mLayoutLeft.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        layoutHorizontal.addView(mLayoutLeft);

        // control buttons

        LinearLayout layoutH = new LinearLayout(mActivity);

        layoutH.setOrientation(LinearLayout.HORIZONTAL);
        layoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mLayoutLeft.addView(layoutH);

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

        //

        layoutH = new LinearLayout(mActivity);

        layoutH.setOrientation(LinearLayout.HORIZONTAL);
        layoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mLayoutLeft.addView(layoutH);

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

        mLayoutLeft.addView(mSliderBitrate);

        //

        mSliderBufferSize = new Slider(mActivity);

        mSliderBufferSize.setLabel("Buffer size (seconds)");
        mSliderBufferSize.setMin(0);
        mSliderBufferSize.setMax(360);

        mSliderBufferSize.setAdjustedProgress(240);

        mLayoutLeft.addView(mSliderBufferSize);

        //

        mSliderChunkSize = new Slider(mActivity);

        mSliderChunkSize.setLabel("Chunk size (seconds)");
        mSliderChunkSize.setMin(1);
        mSliderChunkSize.setMax(20);

        mSliderChunkSize.setAdjustedProgress(2);

        mLayoutLeft.addView(mSliderChunkSize);

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

        mTextStatus = new TextView(mActivity);

        mLayoutLeft.addView(mTextStatus);

        mTextStatus.setText("...");

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

                mTextStatus.setText("Preparing...");

                break;

            case READY_TO_PLAY:
                mButtonPlay.setEnabled(true);
                mButtonPause.setEnabled(false);
                mButtonStop.setEnabled(false);
                //mButtonRandomSeek.setEnabled(false);

                mCheckBoxUseVideoView.setEnabled(true);
                mCheckBoxRepeat.setEnabled(true);

                mSliderBitrate.setEnabled(true);
                mSliderBufferSize.setEnabled(true);
                mSliderChunkSize.setEnabled(true);

                mTextStatus.setText("Ready to play");

                break;

            case PLAYING:
                mButtonPlay.setEnabled(false);
                mButtonPause.setEnabled(true);
                mButtonStop.setEnabled(true);
                //mButtonRandomSeek.setEnabled(true);

                mCheckBoxUseVideoView.setEnabled(false);
                mCheckBoxRepeat.setEnabled(true);

                mSliderBitrate.setEnabled(false);
                mSliderBufferSize.setEnabled(false);
                mSliderChunkSize.setEnabled(false);

                if (mCheckBoxUseVideoView.isChecked()) {
                    mTextStatus.setText("Playing...");
                } else {
                    mTextStatus.setText(String.format(
                        "Streaming: bitrate=%d, buffer=%d, chunk=%d",
                        mStreamer.getBitrate(),
                        mStreamer.getBufferSize(),
                        mStreamer.getChunkSize()));
                }

                break;

            case PAUSED:
                mButtonPlay.setEnabled(true);
                mButtonPause.setEnabled(false);
                mButtonStop.setEnabled(false);
                //mButtonRandomSeek.setEnabled(false);

                mCheckBoxUseVideoView.setEnabled(false);
                mCheckBoxRepeat.setEnabled(true);

                mSliderBitrate.setEnabled(false);
                mSliderBufferSize.setEnabled(false);
                mSliderChunkSize.setEnabled(false);

                mTextStatus.setText("Paused");

                break;

            default:
                throw new UnsupportedOperationException("UIState " + state + " is not implemented");
        }
    }

    public void onClick(View view) {
        if (view == mButtonPlay) {
            streamStart();
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

                removeVideoView();

                setUIState(UIState.READY_TO_PLAY);
            } else {
                if (mStreamer != null) {
                    mStreamer.stop();
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

    private void streamStart() {
        URL url = null;

        try {
            url = new URL(mVideoAddress.getText().toString().trim());
        } catch (MalformedURLException e) {
            e.printStackTrace();

            mVideoAddress.setError("URL is malformed");

            return;
        }

        if (mCheckBoxUseVideoView.isChecked()) {
            if (!mVideoIsPaused) {
                removeVideoView();

                mVideoView = new VideoView(mActivity);

                mVideoView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 300));

                mLayoutLeft.addView(mVideoView);

                mVideoView.setOnPreparedListener(this);
                mVideoView.setOnCompletionListener(this);
                mVideoView.setOnErrorListener(this);

                mVideoView.setVideoURI(Uri.parse(url.toString()));
            }

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
                if (mStreamer != null) {
                    mStreamer.stop();

                    mStreamer = null;
                }

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
    }

    private void removeVideoView() {
        if (mVideoView != null) {
            mLayoutLeft.removeView(mVideoView);

            mVideoView = null;
        }
    }

    private void showStreamDownloadingProgress(int progress) {
        mProgressBarBufferDepth.setSecondaryProgress(progress);
        mProgressBarBufferDepth.invalidate();
    }

    @Override
    public void onStreamDownloadingStarted() {
        Toast.makeText(mActivity, R.string.stream_downloading_started, 1).show();

        setUIState(UIState.PLAYING);
    }

    @Override
    public void onStreamDownloadingFinished() {
        Toast.makeText(mActivity, R.string.stream_downloading_finished, 1).show();
    }

    @Override
    public void onStreamDownloadingProgress(int progress) {
        showStreamDownloadingProgress(progress);
    }

    @Override
    public void onStreamDepthBufferLoadChanged(int value) {
        mProgressBarBufferDepth.setProgress(value);
        mProgressBarBufferDepth.invalidate();
    }

    @Override
    public void onStreamDownloadingFailed() {
        Toast.makeText(mActivity, R.string.stream_downloading_failed, 1).show();

        setUIState(UIState.READY_TO_PLAY);
    }

    @Override
    public void onStreamerFinished(boolean stoppedByUser) {
        Toast.makeText(mActivity, R.string.streamer_finished, 1).show();

        if (!stoppedByUser && mCheckBoxRepeat.isChecked()) {
            streamStart();

            return;
        }

        showStreamDownloadingProgress(0);

        setUIState(UIState.READY_TO_PLAY);
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
            if (mCheckBoxRepeat.isChecked()) {
                streamStart();

                return;
            }

            removeVideoView();

            setUIState(UIState.READY_TO_PLAY);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mCheckBoxUseVideoView.isChecked()) {
            removeVideoView();

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);

            alertDialogBuilder
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton("OK", null)
                .setMessage("Can't play this video")
                .setTitle("Video view");

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

            setUIState(UIState.READY_TO_PLAY);

            return true;
        } else {
            // will cause onCompletion to be called
            return false;
        }
    }
}
