package com.Chris.NetTool;

import android.app.Activity;

import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.VideoView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.net.Uri;

import android.support.v4.app.Fragment;

import android.util.Log;

public class StreamFragment extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "StreamFragment";

    Activity mActivity;
    VideoView mVideoView;
    EditText mVideoAddress;
    Button mButtonPlay, mButtonPause, mButtonStop, mButtonRandomSeek;
    SeekBar mSeekBarBitrate, mSeekBarBufferSize, mSeekBarChunkSize;
    TextView mTextParams;
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

        mCheckBoxUseVideoView = new CheckBox(mActivity);

        layoutH.addView(mCheckBoxUseVideoView);

        mCheckBoxUseVideoView.setText("[use video view]");

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

        TextView textView = new TextView(mActivity);

        layoutLeft.addView(textView);

        textView.setText("Bitrate");

        mSeekBarBitrate = new SeekBar(mActivity);

        layoutLeft.addView(mSeekBarBitrate);

        mSeekBarBitrate.setMax(1500);
        mSeekBarBitrate.setProgress(1000);

        mSeekBarBitrate.setOnSeekBarChangeListener(this);

        //

        textView = new TextView(mActivity);

        layoutLeft.addView(textView);

        textView.setText("Buffer size");

        mSeekBarBufferSize = new SeekBar(mActivity);

        layoutLeft.addView(mSeekBarBufferSize);

        mSeekBarBufferSize.setMax(240);
        mSeekBarBufferSize.setProgress(16);

        mSeekBarBufferSize.setOnSeekBarChangeListener(this);

        mSeekBarBufferSize.setEnabled(false);

        //

        textView = new TextView(mActivity);

        layoutLeft.addView(textView);

        textView.setText("Chunk size");

        mSeekBarChunkSize = new SeekBar(mActivity);

        layoutLeft.addView(mSeekBarChunkSize);

        mSeekBarChunkSize.setMax(20);
        mSeekBarChunkSize.setProgress(2);

        mSeekBarChunkSize.setOnSeekBarChangeListener(this);

        //

        mTextParams = new TextView(mActivity);

        layoutLeft.addView(mTextParams);

        // right

/*
        LinearLayout layoutRight = new LinearLayout(mActivity);

        layoutRight.setOrientation(LinearLayout.VERTICAL);
        layoutRight.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 0.5f));

        layoutHorizontal.addView(layoutRight);

        VerticalProgressBar progressBarBufferDepth = new VerticalProgressBar(mActivity, null, android.R.attr.progressBarStyleHorizontal);

        progressBarBufferDepth.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        layoutRight.addView(progressBarBufferDepth);

        progressBarBufferDepth.setMax(100);
        progressBarBufferDepth.setProgress(30);
*/

        mVideoView = new VideoView(mActivity);

        mVideoView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 300));

        layoutLeft.addView(mVideoView);

        mVideoView.setVisibility(View.VISIBLE);

        onProgressChanged(null, 0, false);

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
                int bitrate = mSeekBarBitrate.getProgress();
                int bufferSize = mSeekBarBufferSize.getProgress();
                int chunkSize = mSeekBarChunkSize.getProgress();

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

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mTextParams.setText(String.format(
            "Bitrate: %d, Buffer size: %d, Chunk size: %d",
            mSeekBarBitrate.getProgress(),
            mSeekBarBufferSize.getProgress(),
            mSeekBarChunkSize.getProgress()));
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
