package com.Chris.NetTool;

import android.app.Activity;

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

import android.text.InputType;

import android.net.Uri;

import android.support.v4.app.Fragment;

import android.util.Log;

public class StreamFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "StreamFragment";

    Activity mActivity;
    VideoView mVideoView;
    EditText mVideoAddress;
    Button mButtonPlay, mButtonPause, mButtonStop, mButtonRandomSeek;
    Slider mSliderBitrate, mSliderBufferSize, mSliderChunkSize;
    boolean mVideoIsPaused = false;
    CheckBox mCheckBoxUseVideoView;

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

        CheckBox checkBoxRepeat = new CheckBox(mActivity);

        layoutH.addView(checkBoxRepeat);

        checkBoxRepeat.setText("Repeat");

        checkBoxRepeat.setEnabled(false);

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

        VerticalProgressBar progressBarBufferDepth = new VerticalProgressBar(mActivity, null, android.R.attr.progressBarStyleHorizontal);

        progressBarBufferDepth.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layoutRight.addView(progressBarBufferDepth);

        progressBarBufferDepth.setMax(100);
        progressBarBufferDepth.setProgress(30);

        LinearLayout layoutVideo = new LinearLayout(mActivity);

        layoutVideo.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layoutLeft.addView(layoutVideo);

        mVideoView = new VideoView(mActivity);

        mVideoView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 300));

        layoutVideo.addView(mVideoView);

        mVideoView.setVisibility(View.VISIBLE);

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
        }
    }

    public void onClick(View view) {
        if (view == mButtonPlay) {
            if (!mVideoIsPaused) {
                mVideoView.setVideoURI(Uri.parse(mVideoAddress.getText().toString()));
            }

            mVideoIsPaused = false;

            if (mCheckBoxUseVideoView.isChecked()) {
                mVideoView.start();
            } else {
                int bitrate = mSliderBitrate.getAdjustedProgress();
                int bufferSize = mSliderBufferSize.getAdjustedProgress();
                int chunkSize = mSliderChunkSize.getAdjustedProgress();

                if (bitrate > 0 && bufferSize > 0 && chunkSize > 0) {
                    mStreamer = new Streamer(mVideoAddress.getText().toString(), bitrate, chunkSize, bufferSize);
                }
            }
        } else if (view == mButtonPause) {
            if (mCheckBoxUseVideoView.isChecked()) {
                mVideoView.pause();
            } else {
            }

            mVideoIsPaused = true;
        } else if (view == mButtonStop) {
            if (mCheckBoxUseVideoView.isChecked()) {
                mVideoView.stopPlayback();
            } else {
                if (mStreamer != null) {
                    mStreamer.stop();

                    mStreamer = null;
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
}
