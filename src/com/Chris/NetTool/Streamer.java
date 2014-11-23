package com.Chris.NetTool;

import android.os.Handler;
import android.os.Message;
import android.os.Looper;

import android.util.Log;

import java.net.URL;
import java.net.HttpURLConnection;

import java.lang.Thread;
import java.lang.Runnable;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Streamer {
    private static final String TAG = "Streamer";

    public interface OnDepthBufferLoadChangedListener {
        public void onDepthBufferLoadChanged(int value);
    }

    public class InvalidContentSizeException extends Exception {
        public InvalidContentSizeException(String message) {
            super(message);
        }
    }

    // in Kbps
    final int mBitrate;
    // in seconds
    final int mChunkSize;
    // in seconds
    final int mBufferSize;

    // sizes in bytes
    final int mDataSizePerSecond;
    final int mChunkDataSize;
    final int mBufferCapacity;

    OnDepthBufferLoadChangedListener mOnDepthBufferLoadChangedListener = null;

    Thread mConnectionThread;

    DepthBuffer mDepthBuffer = new DepthBuffer();

    Handler mTimerHandler = new Handler();

    Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            mDepthBuffer.take(mDataSizePerSecond);

            mTimerHandler.postDelayed(this, 1000);
        }
    };

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            if (mOnDepthBufferLoadChangedListener != null) {
                int load = (int)((float)((Integer)inputMessage.obj * 100) / mBufferCapacity);

                Log.d(TAG, String.format("Depth buffer load: %d%%", load));

                mOnDepthBufferLoadChangedListener.onDepthBufferLoadChanged(load);
            }
        }
    };

    Streamer(URL url, int bitrate, int chunkSize, int bufferSize) {
        mBitrate = bitrate;
        mChunkSize = chunkSize;
        mBufferSize = bufferSize;

        mDataSizePerSecond = mBitrate * (1000 / 8);
        mChunkDataSize = mDataSizePerSecond * mChunkSize;
        mBufferCapacity = mDataSizePerSecond * mBufferSize;

        mConnectionThread = new Thread(new ConnectionThread(url, mBitrate, mChunkSize, mBufferSize));

        mConnectionThread.start();

        mTimerHandler.postDelayed(mTimerRunnable, 1000);
    }

    public void stop() {
        mConnectionThread.interrupt();

        mTimerHandler.removeCallbacks(mTimerRunnable);
    }

    public void setOnDepthBufferLoadChangedListener(OnDepthBufferLoadChangedListener listener) {
        mOnDepthBufferLoadChangedListener = listener;
    }

    class DepthBuffer {
        int mSize = 0;

        public synchronized void put(int size) {
            mSize += size;

            Log.d(TAG, String.format("PUT %d (%d)", size, mSize));

            Message message = mHandler.obtainMessage(0, new Integer(mSize));

            message.sendToTarget();
        }

        public synchronized void take(int size) {
            if (mSize == 0) {
                return;
            }

            mSize -= Math.min(mSize, size);

            Log.d(TAG, String.format("TAKE %d (%d)", size, mSize));

            Message message = mHandler.obtainMessage(0, new Integer(mSize));

            message.sendToTarget();
        }

        public synchronized int getSize() {
            return mSize;
        }
    }

    class ConnectionThread implements Runnable {
        URL mUrl;

        ConnectionThread(URL url, int bitrate, int chunkSize, int bufferSize) {
            super();

            mUrl = url;
        }

        @Override
        public void run() {
            Log.d(TAG, String.format("Connection thread started [bitrate=%d, buffer=%d, chunk=%d]", mBitrate, mBufferSize, mChunkSize));

            try {
                int contentSize = -1;
                int receivedContentSize = 0;

                int rangeFrom = 0;
                int rangeTo = 0;

                while (receivedContentSize != contentSize) {
                    if (Thread.interrupted()) {
                        Log.d(TAG, "Interrupting connection thread");

                        return;
                    }

                    int bufferCurrentSize = mDepthBuffer.getSize();

                    if (bufferCurrentSize + mChunkDataSize > mBufferCapacity) {
                        Log.d(TAG, String.format("Waiting buffer %d + %d (= %d) >= %d",
                            bufferCurrentSize, mChunkDataSize, bufferCurrentSize + mChunkDataSize, mBufferCapacity));
                    }

                    while (mDepthBuffer.getSize() + mChunkDataSize > mBufferCapacity) {
                    }

                    Log.d(TAG, String.format("Buffer available (empty: %d)", mBufferCapacity - mDepthBuffer.getSize()));

                    HttpURLConnection connection = (HttpURLConnection)mUrl.openConnection();

                    Log.d(TAG, String.format("Requesting %d-%d", rangeFrom, rangeTo));

                    try {
                        connection.setChunkedStreamingMode(0);

                        connection.setRequestProperty("Accept-Encoding", "identity");
                        connection.setRequestProperty("Cache-Control", "max-age=0");
                        connection.setRequestProperty("Range", "bytes=" + rangeFrom + "-" + rangeTo);

                        if (contentSize == -1) {
                            String[] sp = connection.getHeaderField("Content-Range").split("/");

                            if (sp.length == 2) {
                                contentSize = Integer.valueOf(sp[1].trim());

                                Log.d(TAG, "Content size: " + contentSize);

                                rangeTo = Math.min(contentSize - 1, mChunkDataSize - 1);

                                continue;
                            }

                            if (contentSize == -1) {
                                throw new InvalidContentSizeException("Can't obtain content size");
                            }
                        }

                        Pattern p = Pattern.compile("bytes\\s(\\d+)-(\\d+).*");
                        Matcher m = p.matcher(connection.getHeaderField("Content-Range"));

                        int receivedSize = 0;

                        if (m.matches()) {
                            int receivedFrom = Integer.valueOf(m.group(1));
                            int receivedTo = Integer.valueOf(m.group(2));

                            receivedSize = receivedTo - receivedFrom + 1;

                            Log.d(TAG, String.format("Received: %d-%d (bytes: %d)", receivedFrom, receivedTo, receivedSize));
                        } else {
                            throw new InvalidContentSizeException("Can't obtain content size");
                        }

                        mDepthBuffer.put(receivedSize);

                        Log.d(TAG, "=====");

                        rangeFrom = rangeTo + 1;
                        rangeTo = Math.min(contentSize - 1, rangeFrom + mChunkDataSize - 1);

                        receivedContentSize += receivedSize;
                    } finally {
                        connection.disconnect();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d(TAG, ">> Connection thread finished");
        }
    }
}
