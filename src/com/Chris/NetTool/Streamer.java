package com.Chris.NetTool;

import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.SystemClock;

import android.util.Log;

import java.net.URL;
import java.net.HttpURLConnection;

import java.lang.Thread;
import java.lang.Runnable;
import java.lang.UnsupportedOperationException;

import java.util.Random;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.InputStream;

public class Streamer {
    private static final String TAG = "Streamer";

    public interface StreamerListener {
        public void onStreamerDownloadingStarted();
        public void onStreamerDownloadingFinished();
        public void onStreamerDownloadingProgressChanged(int downloadingProgress);
        public void onStreamerBufferDepthChanged(int bufferDepth);
        public void onStreamerDownloadingFailed();
        public void onStreamerFinished(boolean stoppedByUser);
        public void onStreamerChunkDownloadTime(long time);
        public void onStreamerRandomSeekCompleted();
    }

    public class InvalidContentSizeException extends Exception {
        public InvalidContentSizeException(String message) {
            super(message);
        }
    }

    private enum MessageId {
        STREAM_DOWNLOADING_STARTED,
        STREAM_DOWNLOADING_FINISHED,
        STREAM_DOWNLOADING_PROGRESS,
        DEPTH_BUFFER_SIZE_CHANGED,
        STREAM_DOWNLOADING_FAILED,
        STREAM_CHUNK_TIME_OF_ARRIVAL,
        STREAM_RANDOM_SEEK_COMPLETED
    }

    private final URL mUrl;
    // in Kbps
    private final int mBitrate;
    // in seconds
    private final int mChunkSize;
    // in seconds
    private final int mBufferSize;

    // sizes in bytes
    private final int mDataSizePerSecond;
    private final int mChunkDataSize;
    private final int mBufferCapacity;

    // in milliseconds
    private final int mConnectTimeout;
    private final int mReadTimeout;

    private StreamerListener mStreamerListener = null;

    private Thread mConnectionThread;

    private StreamerBuffer mStreamerBuffer = new StreamerBuffer();
    private Object mConnectionThreadPauseLock = new Object();
    private Object mConnectionThreadStopLock = new Object();
    private boolean mConnectionThreadPaused = false;
    private boolean mConnectionThreadStopped = false;
    private boolean mStoppedByUser = false;
    private boolean mConnectionThreadRandomSeek = false;
    private boolean mPaused = false;

    private Handler mTimerHandler = new Handler();

    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            mStreamerBuffer.take(mDataSizePerSecond);

            mTimerHandler.postDelayed(this, 1000);
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            final MessageId messageId = MessageId.values()[inputMessage.what];

            switch (messageId) {
                case STREAM_DOWNLOADING_STARTED:
                    if (mStreamerListener != null) {
                        mStreamerListener.onStreamerDownloadingStarted();
                    }

                    break;

                case STREAM_DOWNLOADING_FINISHED:
                    if (mStreamerListener != null) {
                        if (mStoppedByUser) {
                            mStreamerBuffer.clear(true);
                        } else {
                            mStreamerListener.onStreamerDownloadingFinished();
                        }

                        // notify that streamer is done if it's running in "no buffering" mode
                        if (mBufferSize == 0 || mStoppedByUser) {
                            mStreamerListener.onStreamerFinished(mStoppedByUser);
                        }
                    }

                    mConnectionThread = null;

                    break;

                case STREAM_DOWNLOADING_PROGRESS:
                    if (mStreamerListener != null) {
                        if (!mStoppedByUser)
                        {
                            int progress = (Integer)inputMessage.obj;

                            mStreamerListener.onStreamerDownloadingProgressChanged(progress);
                        }
                    }

                    break;

                case DEPTH_BUFFER_SIZE_CHANGED:
                    if (mStreamerListener != null) {
                        int bufferSize = (Integer)inputMessage.obj;
                        int load = (int)((float)(bufferSize * 100) / mBufferCapacity);

                        Log.d(TAG, String.format("Buffer load: %d%%", load));

                        mStreamerListener.onStreamerBufferDepthChanged(load);

                        // stop streamer and notify if
                        // buffer is empty and downloading is finished
                        if (bufferSize == 0 && mConnectionThread == null) {
                            stopStreamer();

                            mStreamerListener.onStreamerFinished(mStoppedByUser);
                        }
                    }

                    break;

                case STREAM_DOWNLOADING_FAILED:
                    mStreamerBuffer.clear(true);

                    if (mStreamerListener != null) {
                        mStreamerListener.onStreamerDownloadingFailed();
                    }

                    break;

                case STREAM_CHUNK_TIME_OF_ARRIVAL:
                    if (mStreamerListener != null) {
                        long time = (Long)inputMessage.obj;

                        mStreamerListener.onStreamerChunkDownloadTime(time);
                    }

                    break;

                case STREAM_RANDOM_SEEK_COMPLETED:
                    if (mStreamerListener != null) {
                        mStreamerListener.onStreamerRandomSeekCompleted();
                    }

                    break;

                default:
                    throw new UnsupportedOperationException("MessageId " + messageId + " is not implemented");
            }
        }
    };

    public Streamer(URL url, int bitrate, int chunkSize, int bufferSize, int connectTimeout, int readTimeout) {
        mUrl = url;
        mBitrate = bitrate;
        mChunkSize = chunkSize;
        mBufferSize = bufferSize;

        mDataSizePerSecond = mBitrate * (1000 / 8);
        mChunkDataSize = mDataSizePerSecond * mChunkSize;
        mBufferCapacity = mDataSizePerSecond * mBufferSize;

        mConnectTimeout = connectTimeout;
        mReadTimeout = readTimeout;

        createAndStartConnectionThread();

        if (mBufferSize > 0) {
            mTimerHandler.postDelayed(mTimerRunnable, 1000);
        }
    }

    private void createAndStartConnectionThread() {
        if (mConnectionThread != null) {
            throw new IllegalStateException("Connection thread is already created");
        }

        mConnectionThread = new Thread(new ConnectionThread(mUrl, mBitrate, mChunkSize, mBufferSize));

        mConnectionThread.start();
    }

    public void stop() {
        mStoppedByUser = true;

        stopStreamer();
    }

    public void randomSeek() {
        if (mBufferSize > 0) {
            mStreamerBuffer.clear(false);
        }

        if (mConnectionThread == null) {
            createAndStartConnectionThread();
        }

        setConnectionThreadRandomSeek(true);

        setConnectionThreadPaused(false);

        // restart emptying buffer
        mTimerHandler.removeCallbacks(mTimerRunnable);
        mTimerHandler.postDelayed(mTimerRunnable, 1000);
    }

    public void setPaused(boolean paused) {
        if (mPaused == paused) {
            return;
        }

        if (paused) {
            setConnectionThreadPaused(true);

            if (mBufferSize > 0) {
                mTimerHandler.removeCallbacks(mTimerRunnable);
            }
        } else {
            if (mBufferSize > 0) {
                mTimerHandler.postDelayed(mTimerRunnable, 1000);
            }

            setConnectionThreadPaused(false);
        }

        mPaused = paused;
    }

    private void stopStreamer() {
        if (mConnectionThread != null && mConnectionThread.isAlive()) {
            stopConnectionThread();
        } else if (mBufferSize > 0) {
            mTimerHandler.removeCallbacks(mTimerRunnable);

            mStreamerBuffer.clear(true);
        }
    }

    public void setStreamerListener(StreamerListener listener) {
        mStreamerListener = listener;
    }

    public int getBitrate() {
        return mBitrate;
    }

    public int getChunkSize() {
        return mChunkSize;
    }

    public int getBufferSize() {
        return mBufferSize;
    }

    public int getBufferCapacity() {
        return mBufferCapacity;
    }

    private void setConnectionThreadPaused(boolean paused) {
        if (mConnectionThread != null) {
            if (paused) {
                synchronized (mConnectionThreadPauseLock) {
                    mConnectionThreadPaused = true;
                }
            } else {
                synchronized (mConnectionThreadPauseLock) {
                    mConnectionThreadPaused = false;

                    mConnectionThreadPauseLock.notifyAll();
                }
            }
        }
    }

    private void stopConnectionThread() {
        if (mConnectionThread != null) {
            synchronized (mConnectionThreadStopLock) {
                mConnectionThreadStopped = true;
            }
        }

        setConnectionThreadPaused(false);
    }

    private synchronized void setConnectionThreadRandomSeek(boolean value) {
        mConnectionThreadRandomSeek = value;
    }

    private synchronized boolean getConnectionThreadRandomSeek() {
        return mConnectionThreadRandomSeek;
    }

    private class StreamerBuffer {
        private int mSize = 0;

        public synchronized void put(int size) {
            mSize += size;

            Log.d(TAG, String.format("PUT %d (%d)", size, mSize));

            mHandler.obtainMessage(MessageId.DEPTH_BUFFER_SIZE_CHANGED.ordinal(), mSize)
                .sendToTarget();
        }

        public synchronized void take(int size) {
            take(size, true);
        }

        private synchronized void take(int size, boolean sendMessage) {
            if (mSize == 0 || size <= 0) {
                return;
            }

            mSize -= Math.min(mSize, size);

            Log.d(TAG, String.format("TAKE %d (%d)", size, mSize));

            if (sendMessage) {
                mHandler.obtainMessage(MessageId.DEPTH_BUFFER_SIZE_CHANGED.ordinal(), mSize)
                    .sendToTarget();
            }

            setConnectionThreadPaused(false);
        }

        public synchronized int getSize() {
            return mSize;
        }

        public synchronized void clear(boolean sendMessage) {
            take(mSize, sendMessage);
        }
    }

    private class ConnectionThread implements Runnable {
        private URL mUrl;

        public ConnectionThread(URL url, int bitrate, int chunkSize, int bufferSize) {
            super();

            mUrl = url;
        }

        @Override
        public void run() {
            Log.d(TAG, String.format(
                "Connection thread started [bitrate=%d, buffer=%d, chunk=%d, connect_TO=%d, read_TO=%d]",
                mBitrate, mBufferSize, mChunkSize, mConnectTimeout, mReadTimeout));

            try {
                int contentSize = -1;
                int totalReceivedSize = 0;

                boolean stop = false;

                byte[] byteData = new byte[1024];

                while (totalReceivedSize != contentSize && !stop) {
                    // thread stopping logic
                    synchronized (mConnectionThreadStopLock) {
                        if (mConnectionThreadStopped) {
                            Log.d(TAG, "Stopping connection thread");

                            stop = true;

                            continue;
                        }
                    }

                    // thread pausing logic
                    synchronized (mConnectionThreadPauseLock) {
                        // TODO: remove logging
                        if (mConnectionThreadPaused) {
                            while (mConnectionThreadPaused) {
                                try {
                                    Log.d(TAG, "*** CT: PAUSE");
                                    mConnectionThreadPauseLock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            Log.d(TAG, "*** CT: CONTINUE");
                        }
                    }

                    // TODO: move into separate function
                    // thread stopping logic
                    synchronized (mConnectionThreadStopLock) {
                        if (mConnectionThreadStopped) {
                            Log.d(TAG, "Stopping connection thread");

                            stop = true;

                            continue;
                        }
                    }

                    if (mBufferSize > 0) {
                        int bufferCurrentSize = mStreamerBuffer.getSize();

                        if (bufferCurrentSize + mChunkDataSize > mBufferCapacity) {
                            Log.d(TAG, String.format("Waiting buffer %d + %d (= %d) >= %d",
                                bufferCurrentSize, mChunkDataSize, bufferCurrentSize + mChunkDataSize,
                                mBufferCapacity));

                            setConnectionThreadPaused(true);

                            continue;
                        }

                        Log.d(TAG, String.format("Buffer available (empty: %d)",
                            mBufferCapacity - mStreamerBuffer.getSize()));
                    }

                    // random seek logic
                    // TODO: remove logging
                    if (getConnectionThreadRandomSeek() && contentSize != -1) {
                        totalReceivedSize = (new Random()).nextInt(contentSize);

                        Log.d(TAG, "*** CT: RANDOM SEEK TO " + totalReceivedSize);

                        setConnectionThreadRandomSeek(false);

                        // send "random seek completed" message to Stream instance

                        mHandler.obtainMessage(MessageId.STREAM_RANDOM_SEEK_COMPLETED.ordinal(), null)
                            .sendToTarget();
                    }

                    long time;

                    HttpURLConnection connection = (HttpURLConnection)mUrl.openConnection();

                    connection.setConnectTimeout(mConnectTimeout);
                    connection.setReadTimeout(mReadTimeout);

                    try {
                        int rangeFrom;
                        int rangeTo;

                        if (contentSize == -1) {
                            rangeFrom = 0;
                            rangeTo = 0;
                        } else {
                            rangeFrom = totalReceivedSize;
                            rangeTo = Math.min(contentSize - 1, rangeFrom + mChunkDataSize - 1);
                        }

                        Log.d(TAG, String.format("Requesting %d-%d (bytes: %d)", rangeFrom, rangeTo,
                            rangeTo - rangeFrom + 1));

                        connection.setChunkedStreamingMode(0);

                        connection.setRequestProperty("Accept-Encoding", "identity");
                        connection.setRequestProperty("Cache-Control", "max-age=0");
                        connection.setRequestProperty("Range", "bytes=" + rangeFrom + "-" + rangeTo);

                        if (contentSize == -1) {
                            time = SystemClock.elapsedRealtime();

                            String[] sp = connection.getHeaderField("Content-Range").split("/");

                            if (sp.length == 2) {
                                contentSize = Integer.valueOf(sp[1].trim());

                                Log.d(TAG, "Content size: " + contentSize);

                                // send "stream started" message to Stream instance

                                mHandler.obtainMessage(MessageId.STREAM_DOWNLOADING_STARTED.ordinal(), null)
                                    .sendToTarget();

                                // send "chunk time of arrival" message

                                time = SystemClock.elapsedRealtime() - time;

                                mHandler.obtainMessage(MessageId.STREAM_CHUNK_TIME_OF_ARRIVAL.ordinal(), time)
                                    .sendToTarget();

                                continue;
                            }

                            if (contentSize == -1) {
                                throw new InvalidContentSizeException("Can't obtain content size");
                            }
                        }

                        time = SystemClock.elapsedRealtime();

                        Matcher m = Pattern.compile("bytes\\s(\\d+)-(\\d+).*")
                            .matcher(connection.getHeaderField("Content-Range"));

                        int receivedSize = 0;

                        if (m.matches()) {
                            int receivedFrom = Integer.valueOf(m.group(1));
                            int receivedTo = Integer.valueOf(m.group(2));

                            receivedSize = receivedTo - receivedFrom + 1;

                            Log.d(TAG, String.format("Received: %d-%d (bytes: %d)", receivedFrom, receivedTo,
                                receivedSize));
                        } else {
                            throw new InvalidContentSizeException("Can't obtain content size");
                        }

                        Log.d(TAG, "** STREAMER: READING DATA");

                        InputStream inputStream = connection.getInputStream();

                        while (inputStream.read(byteData) != -1) {
                        }

                        if (mBufferSize > 0) {
                            mStreamerBuffer.put(receivedSize);
                        }

                        Log.d(TAG, "** STREAMER: FINISHED READING DATA");

                        totalReceivedSize += receivedSize;

                        int streamingProgress = (int)((float)(totalReceivedSize * 100) / contentSize);

                        // send "streaming downloading progress" message to Stream instance

                        mHandler.obtainMessage(MessageId.STREAM_DOWNLOADING_PROGRESS.ordinal(), streamingProgress)
                            .sendToTarget();

                        // send "chunk time of arrival" message

                        time = SystemClock.elapsedRealtime() - time;

                        mHandler.obtainMessage(MessageId.STREAM_CHUNK_TIME_OF_ARRIVAL.ordinal(), time)
                            .sendToTarget();

                        Log.d(TAG, "=====");
                    } finally {
                        connection.disconnect();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();

                Log.d(TAG, ">> Connection thread failed");

                mHandler.obtainMessage(MessageId.STREAM_DOWNLOADING_FAILED.ordinal(), null)
                    .sendToTarget();

                return;
            }

            Log.d(TAG, ">> Connection thread finished");

            mHandler.obtainMessage(MessageId.STREAM_DOWNLOADING_FINISHED.ordinal(), null)
                .sendToTarget();
        }
    }
}
