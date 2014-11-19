package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.VideoView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.net.Uri;

import android.support.v4.app.Fragment;

import android.util.Log;

public class StreamFragment extends Fragment implements View.OnClickListener {
    Activity mActivity;
    VideoView mVideoView;
    EditText mVideoAddress;
    Button mButtonPlay, mButtonPause, mButtonStop, mButtonRandomSeek;
    boolean mVideoIsPaused = false;

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

        mVideoAddress.setText("http://ravewireless.com/content/StandardCycleContent/NewFamily/LATAM_ContentFull/1/2001436/divx_LATAM-MadMenChristmas_eng_video_level4_vbv9600_1200kbps.mkv");

        layout.addView(mVideoAddress);

        LinearLayout layoutHorizontal = new LinearLayout(mActivity);

        layoutHorizontal.setOrientation(LinearLayout.HORIZONTAL);
        layoutHorizontal.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        layout.addView(layoutHorizontal);

        // left

        LinearLayout layoutLeft = new LinearLayout(mActivity);

        layoutLeft.setOrientation(LinearLayout.VERTICAL);
        layoutLeft.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.5f));

        layoutHorizontal.addView(layoutLeft);

        mVideoView = new VideoView(mActivity);

        mVideoView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 300));

        layoutLeft.addView(mVideoView);

        mVideoView.setVisibility(View.VISIBLE);

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

        SeekBar seekBarStreamRate = new SeekBar(mActivity);

        layoutLeft.addView(seekBarStreamRate);

        SeekBar seekBarTargetBufferDepth = new SeekBar(mActivity);

        layoutLeft.addView(seekBarTargetBufferDepth);

        SeekBar seekBarBlockSize = new SeekBar(mActivity);

        layoutLeft.addView(seekBarBlockSize);

        // right

        LinearLayout layoutRight = new LinearLayout(mActivity);

        layoutRight.setOrientation(LinearLayout.VERTICAL);
        layoutRight.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 0.5f));

        layoutHorizontal.addView(layoutRight);

        VerticalProgressBar progressBarBufferDepth = new VerticalProgressBar(mActivity, null, android.R.attr.progressBarStyleHorizontal);

        progressBarBufferDepth.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layoutRight.addView(progressBarBufferDepth);

        progressBarBufferDepth.setMax(100);
        progressBarBufferDepth.setProgress(30);

        return layout;
    }

    public void onClick(View view) {
        if (view == mButtonPlay) {
            if (!mVideoIsPaused) {
                mVideoView.setVideoURI(Uri.parse(mVideoAddress.getText().toString()));
            }

            mVideoView.start();

            mVideoIsPaused = false;
        } else if (view == mButtonPause) {
            mVideoView.pause();

            mVideoIsPaused = true;
        } else if (view == mButtonStop) {
            mVideoView.stopPlayback();

            mVideoIsPaused = false;
        } else if (view == mButtonRandomSeek) {
            mVideoView.seekTo(0);
        }
    }
}
