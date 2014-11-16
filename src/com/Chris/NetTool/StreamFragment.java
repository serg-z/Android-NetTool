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

        mVideoAddress.setText("http://");

        layout.addView(mVideoAddress);

        mVideoView = new VideoView(mActivity);

        mVideoView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 300));

        layout.addView(mVideoView);

        mVideoView.setVisibility(View.VISIBLE);

        // control buttons

        LinearLayout layoutH = new LinearLayout(mActivity);

        layoutH.setOrientation(LinearLayout.HORIZONTAL);
        layoutH.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        layout.addView(layoutH);

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

        layout.addView(layoutH);

        mButtonRandomSeek = new Button(mActivity);

        mButtonRandomSeek.setOnClickListener(this);

        layoutH.addView(mButtonRandomSeek);

        mButtonRandomSeek.setText("Random Seek");

        mButtonRandomSeek.setEnabled(false);

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
