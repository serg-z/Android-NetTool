package com.Chris.NetTool;

import android.os.Handler;
import android.os.Message;
import android.os.Looper;

import android.util.Log;

import java.net.URL;
import java.net.HttpURLConnection;

import java.lang.Thread;
import java.lang.Runnable;
import java.lang.UnsupportedOperationException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Streamer {
    private static final String TAG = "Streamer";

    public interface StreamerListener {
        public void onStreamStarted();
        public void onStreamStopped();
        public void onStreamDepthBufferLoadChanged(int value);
        public void onStreamDepthBufferIsEmpty();
        public void onStreamPlaybackFailed();
    }

    public class InvalidContentSizeException extends Exception {
        public InvalidContentSizeException(String message) {
            super(message);
        }
    }

    private enum MessageId {
        STREAM_STARTED,
        STREAM_STOPPED,
        DEPTH_BUFFER_SIZE_CHANGED,
        STREAM_PLAYBACK_FAILED
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

    StreamerListener mStreamerListener = null;

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
            final MessageId messageId = MessageId.values()[inputMessage.what];

            switch (messageId) {
                case STREAM_STARTED:
                    if (mStreamerListener != null) {
                        mStreamerListener.onStreamStarted();
                    }

                    break;

                case STREAM_STOPPED:
                    if (mStreamerListener != null) {
                        mStreamerListener.onStreamStopped();
                    }

                    break;

                case DEPTH_BUFFER_SIZE_CHANGED:
                    if (mStreamerListener != null) {
                        int bufferSize = (Integer)inputMessage.obj;
                        int load = (int)((float)(bufferSize * 100) / mBufferCapacity);

                        Log.d(TAG, String.format("Depth buffer load: %d%%", load));

                        mStreamerListener.onStreamDepthBufferLoadChanged(load);

                        if (bufferSize == 0) {
                            mStreamerListener.onStreamDepthBufferIsEmpty();

                            stop();
                        }
                    }

                    break;

                case STREAM_PLAYBACK_FAILED:
                    if (mStreamerListener != null) {
                        mStreamerListener.onStreamPlaybackFailed();
                    }

                    break;

                default:
                    throw new UnsupportedOperationException("MessageId " + messageId + " is not implemented");
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

        if (mBufferSize > 0) {
            mTimerHandler.postDelayed(mTimerRunnable, 1000);
        }
    }

    public void stop() {
        if (mConnectionThread.isAlive()) {
            mConnectionThread.interrupt();
        }

        if (mBufferSize > 0) {
            mTimerHandler.removeCallbacks(mTimerRunnable);

            mDepthBuffer.clear();
        }
    }

    public void setStreamerListener(StreamerListener listener) {
        mStreamerListener = listener;
    }

    public int getBufferCapacity() {
        return mBufferCapacity;
    }

    class DepthBuffer {
        int mSize = 0;

        public synchronized void put(int size) {
            mSize += size;

            Log.d(TAG, String.format("PUT %d (%d)", size, mSize));

            mHandler.obtainMessage(MessageId.DEPTH_BUFFER_SIZE_CHANGED.ordinal(), Integer.valueOf(mSize))
                .sendToTarget();
        }

        public synchronized void take(int size) {
            if (mSize == 0 || size <= 0) {
                return;
            }

            mSize -= Math.min(mSize, size);

            Log.d(TAG, String.format("TAKE %d (%d)", size, mSize));

            mHandler.obtainMessage(MessageId.DEPTH_BUFFER_SIZE_CHANGED.ordinal(), Integer.valueOf(mSize))
                .sendToTarget();
        }

        public synchronized int getSize() {
            return mSize;
        }

        public synchronized void clear() {
            take(mSize);
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

                boolean stop = false;

                while (receivedContentSize != contentSize && !stop) {
                    if (Thread.interrupted()) {
                        Log.d(TAG, "Interrupting connection thread");

                        stop = true;

                        continue;
                    }

                    if (mBufferSize > 0) {
                        int bufferCurrentSize = mDepthBuffer.getSize();

                        if (bufferCurrentSize + mChunkDataSize > mBufferCapacity) {
                            Log.d(TAG, String.format("Waiting buffer %d + %d (= %d) >= %d",
                                bufferCurrentSize, mChunkDataSize, bufferCurrentSize + mChunkDataSize, mBufferCapacity));
                        }

                        while (mDepthBuffer.getSize() + mChunkDataSize > mBufferCapacity) {
                        }

                        Log.d(TAG, String.format("Buffer available (empty: %d)", mBufferCapacity - mDepthBuffer.getSize()));
                    }

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

                                // send "stream started" message to Stream instance

                                mHandler.obtainMessage(MessageId.STREAM_STARTED.ordinal(), null)
                                    .sendToTarget();

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

                        if (mBufferSize > 0) {
                            mDepthBuffer.put(receivedSize);
                        }

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

                mHandler.obtainMessage(MessageId.STREAM_PLAYBACK_FAILED.ordinal(), null)
                    .sendToTarget();
            }

            Log.d(TAG, ">> Connection thread finished");

            mHandler.obtainMessage(MessageId.STREAM_STOPPED.ordinal(), null)
                .sendToTarget();
        }
    }
}
