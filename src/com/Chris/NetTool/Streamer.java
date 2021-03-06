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
        public void onStreamerDownloadingFailed(String message);
        public void onStreamerFinished(boolean stoppedByUser);
        public void onStreamerChunkDownloadTime(long time);
        public void onStreamerRandomSeekCompleted();
    }

    public class InvalidContentSizeException extends Exception {
        public InvalidContentSizeException(String message) {
            super(message);
        }
    }

    private static final class MessageId {
        private MessageId() {}

        public static final int STREAM_DOWNLOADING_STARTED    = 1;
        public static final int STREAM_DOWNLOADING_FINISHED   = 2;
        public static final int STREAM_DOWNLOADING_PROGRESS   = 3;
        public static final int DEPTH_BUFFER_SIZE_CHANGED     = 4;
        public static final int STREAM_DOWNLOADING_FAILED     = 5;
        public static final int STREAM_CHUNK_TIME_OF_ARRIVAL  = 6;
        public static final int STREAM_RANDOM_SEEK_COMPLETED  = 7;
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
    private final long mBufferCapacity;

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
    // TODO: encapsulate it into StreamerBuffer
    private int mBufferDepth = 0;

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
            final int messageId = inputMessage.what;

            switch (messageId) {
                case MessageId.STREAM_DOWNLOADING_STARTED:
                    if (mStreamerListener != null) {
                        mStreamerListener.onStreamerDownloadingStarted();
                    }

                    break;

                case MessageId.STREAM_DOWNLOADING_FINISHED:
                    if (mStreamerListener != null) {
                        if (mStoppedByUser) {
                            mStreamerBuffer.clear(true);

                            mBufferDepth = 0;
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

                case MessageId.STREAM_DOWNLOADING_PROGRESS:
                    if (mStreamerListener != null) {
                        if (!mStoppedByUser)
                        {
                            int progress = (Integer)inputMessage.obj;

                            mStreamerListener.onStreamerDownloadingProgressChanged(progress);
                        }
                    }

                    break;

                case MessageId.DEPTH_BUFFER_SIZE_CHANGED:
                    if (mStreamerListener != null) {
                        long bufferSize = (Long)inputMessage.obj;
                        int load = (int)((bufferSize * 100.0f) / mBufferCapacity);

                        mBufferDepth = load;

                        Log.d(TAG, String.format("Buffer load: %d%%", mBufferDepth));

                        mStreamerListener.onStreamerBufferDepthChanged(mBufferDepth);

                        // stop streamer and notify if
                        // buffer is empty and downloading is finished
                        if (bufferSize == 0 && mConnectionThread == null) {
                            stopStreamer();

                            mStreamerListener.onStreamerFinished(mStoppedByUser);
                        }
                    }

                    break;

                case MessageId.STREAM_DOWNLOADING_FAILED:
                    mStreamerBuffer.clear(true);

                    mBufferDepth = 0;

                    if (mStreamerListener != null) {
                        mStreamerListener.onStreamerDownloadingFailed((String)inputMessage.obj);
                    }

                    break;

                case MessageId.STREAM_CHUNK_TIME_OF_ARRIVAL:
                    if (mStreamerListener != null) {
                        long time = (Long)inputMessage.obj;

                        mStreamerListener.onStreamerChunkDownloadTime(time);
                    }

                    break;

                case MessageId.STREAM_RANDOM_SEEK_COMPLETED:
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

        // if bitrate equals to 0 then we're downloading whole range of data and there's no need in chunk and buffer
        if (mBitrate == 0) {
            mChunkSize = 0;
            mBufferSize = 0;
        } else {
            mChunkSize = chunkSize;
            mBufferSize = bufferSize;
        }

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

            mBufferDepth = 0;
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

            mBufferDepth = 0;
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

    public long getBufferCapacity() {
        return mBufferCapacity;
    }

    public int getBufferDepth() {
        return mBufferDepth;
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
        private long mSize = 0L;

        public synchronized void put(long size) {
            mSize += size;

            Log.d(TAG, String.format("StreamBuffer: Put %d (%d)", size, mSize));

            mHandler.obtainMessage(MessageId.DEPTH_BUFFER_SIZE_CHANGED, mSize)
                .sendToTarget();
        }

        public synchronized void take(long size) {
            take(size, true);
        }

        private synchronized void take(long size, boolean sendMessage) {
            if (mSize == 0L || size <= 0L) {
                return;
            }

            mSize -= Math.min(mSize, size);

            Log.d(TAG, String.format("StreamerBuffer: Take %d (%d)", size, mSize));

            if (sendMessage) {
                mHandler.obtainMessage(MessageId.DEPTH_BUFFER_SIZE_CHANGED, mSize)
                    .sendToTarget();
            }

            setConnectionThreadPaused(false);
        }

        public synchronized long getSize() {
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
                "ConnectionThread: Started [bitrate=%d, buffer=%d, chunk=%d, connect_TO=%d, read_TO=%d]",
                mBitrate, mBufferSize, mChunkSize, mConnectTimeout, mReadTimeout));

            try {
                long contentSize = -1L;
                long totalReceivedSize = 0L;

                boolean stop = false;

                byte[] byteData = new byte[1024];

                while (totalReceivedSize != contentSize && !stop) {
                    // thread stopping logic
                    synchronized (mConnectionThreadStopLock) {
                        if (mConnectionThreadStopped) {
                            Log.d(TAG, "ConnectionThread: Stopping");

                            stop = true;

                            continue;
                        }
                    }

                    // thread pausing logic
                    synchronized (mConnectionThreadPauseLock) {
                        if (mConnectionThreadPaused) {
                            while (mConnectionThreadPaused) {
                                try {
                                    Log.d(TAG, "ConnectionThread: Pause");
                                    mConnectionThreadPauseLock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            Log.d(TAG, "ConnectionThread: Continue");
                        }
                    }

                    // TODO: move into separate function
                    // thread stopping logic
                    synchronized (mConnectionThreadStopLock) {
                        if (mConnectionThreadStopped) {
                            Log.d(TAG, "ConnectionThread: Stopping");

                            stop = true;

                            continue;
                        }
                    }

                    if (mBufferSize > 0) {
                        long bufferCurrentSize = mStreamerBuffer.getSize();

                        if (bufferCurrentSize + mChunkDataSize > mBufferCapacity) {
                            Log.d(TAG, String.format("ConnectionThread: Waiting buffer %d + %d (= %d) >= %d",
                                bufferCurrentSize, mChunkDataSize, bufferCurrentSize + mChunkDataSize,
                                mBufferCapacity));

                            setConnectionThreadPaused(true);

                            continue;
                        }

                        Log.d(TAG, String.format("ConnectionThread: Buffer available (empty: %d)",
                            mBufferCapacity - mStreamerBuffer.getSize()));
                    }

                    // random seek logic
                    if (getConnectionThreadRandomSeek() && contentSize != -1L) {
                        totalReceivedSize = (long)((new Random()).nextInt(100) * 0.01f * contentSize);

                        Log.d(TAG, String.format("ConnectionThread: Random seek to %d (%d%%)",
                            totalReceivedSize, (int)((totalReceivedSize * 100.0f) / contentSize)));

                        setConnectionThreadRandomSeek(false);

                        // send "random seek completed" message to Stream instance

                        mHandler.obtainMessage(MessageId.STREAM_RANDOM_SEEK_COMPLETED, null)
                            .sendToTarget();
                    }

                    long time;

                    HttpURLConnection connection = (HttpURLConnection)mUrl.openConnection();

                    connection.setConnectTimeout(mConnectTimeout);
                    connection.setReadTimeout(mReadTimeout);

                    try {
                        long rangeFrom;
                        long rangeTo;

                        if (contentSize == -1L) {
                            rangeFrom = 0L;
                            rangeTo = 0L;
                        } else {
                            rangeFrom = totalReceivedSize;
                            rangeTo = Math.min(contentSize - 1L, rangeFrom + mChunkDataSize - 1L);
                        }

                        // request whole range of data if the bitrate equals to 0
                        boolean wholeRange = mBitrate == 0;

                        if (wholeRange) {
                            Log.d(TAG, "ConnectionThread: Requesting whole range");
                        } else {
                            Log.d(TAG, String.format("ConnectionThread: Requesting %d-%d (bytes: %d)",
                                rangeFrom, rangeTo, rangeTo - rangeFrom + 1));
                        }

                        connection.setChunkedStreamingMode(0);

                        connection.setRequestProperty("Accept-Encoding", "identity");
                        connection.setRequestProperty("Cache-Control", "max-age=0");

                        if (wholeRange) {
                            connection.setRequestProperty("Range", "bytes=0-");
                        } else {
                            connection.setRequestProperty("Range", "bytes=" + rangeFrom + "-" + rangeTo);
                        }

                        if (wholeRange) {
                            // send "stream started" message to Stream instance

                            mHandler.obtainMessage(MessageId.STREAM_DOWNLOADING_STARTED, null)
                                .sendToTarget();
                        } else if (contentSize == -1L) {
                            time = SystemClock.elapsedRealtime();

                            String[] sp = connection.getHeaderField("Content-Range").split("/");

                            if (sp.length == 2) {
                                contentSize = Long.valueOf(sp[1].trim());

                                Log.d(TAG, "ConnectionThread: Content size: " + contentSize);

                                // send "stream started" message to Stream instance

                                mHandler.obtainMessage(MessageId.STREAM_DOWNLOADING_STARTED, null)
                                    .sendToTarget();

                                // send "chunk time of arrival" message

                                time = SystemClock.elapsedRealtime() - time;

                                mHandler.obtainMessage(MessageId.STREAM_CHUNK_TIME_OF_ARRIVAL, time)
                                    .sendToTarget();

                                continue;
                            }

                            if (contentSize == -1L) {
                                throw new InvalidContentSizeException("Can't obtain content size");
                            }
                        }

                        time = SystemClock.elapsedRealtime();

                        long receivedSize;

                        if (wholeRange) {
                            receivedSize = 0L;
                        } else {
                            Matcher m = Pattern.compile("bytes\\s(\\d+)-(\\d+).*")
                                .matcher(connection.getHeaderField("Content-Range"));

                            if (m.matches()) {
                                long receivedFrom = Long.valueOf(m.group(1));
                                long receivedTo = Long.valueOf(m.group(2));

                                receivedSize = receivedTo - receivedFrom + 1L;

                                Log.d(TAG, String.format("ConnectionThread: Received: %d-%d (bytes: %d)",
                                    receivedFrom, receivedTo, receivedSize));
                            } else {
                                throw new InvalidContentSizeException("Can't obtain content size");
                            }
                        }

                        Log.d(TAG, "ConnectionThread: Reading data");

                        InputStream inputStream = connection.getInputStream();

                        while (inputStream.read(byteData) != -1) {
                            // thread stopping logic
                            synchronized (mConnectionThreadStopLock) {
                                if (mConnectionThreadStopped) {
                                    Log.d(TAG, "ConnectionThread: Stopping");

                                    stop = true;

                                    break;
                                }
                            }

                            // thread pausing logic
                            synchronized (mConnectionThreadPauseLock) {
                                if (mConnectionThreadPaused) {
                                    while (mConnectionThreadPaused) {
                                        try {
                                            Log.d(TAG, "ConnectionThread: Pause");
                                            mConnectionThreadPauseLock.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    Log.d(TAG, "ConnectionThread: Continue");
                                }
                            }

                            // TODO: move into separate function
                            // thread stopping logic
                            synchronized (mConnectionThreadStopLock) {
                                if (mConnectionThreadStopped) {
                                    Log.d(TAG, "ConnectionThread: Stopping");

                                    stop = true;

                                    break;
                                }
                            }

                            // random seek logic
                            if (getConnectionThreadRandomSeek()) {
                                if (contentSize != -1L) {
                                    totalReceivedSize = (long)((new Random()).nextInt(100) * 0.01f * contentSize);

                                    Log.d(TAG, String.format("ConnectionThread: Random seek to %d (%d%%)",
                                        totalReceivedSize, (int)((totalReceivedSize * 100.0f) / contentSize)));
                                } else {
                                    Log.d(TAG, "ConnectionThread: Reset random seek flag");
                                }

                                setConnectionThreadRandomSeek(false);

                                // send "random seek completed" message to Stream instance

                                mHandler.obtainMessage(MessageId.STREAM_RANDOM_SEEK_COMPLETED, null)
                                    .sendToTarget();
                            }
                        }

                        // buffer size will be 0 if the Streamer set to request whole range
                        if (mBufferSize > 0) {
                            mStreamerBuffer.put(receivedSize);
                        }

                        Log.d(TAG, "ConnectionThread: Finished reading data");

                        int streamingProgress;

                        if (wholeRange) {
                            streamingProgress = 100;
                        } else {
                            totalReceivedSize += receivedSize;

                            streamingProgress = (int)((totalReceivedSize * 100.0f) / contentSize);
                        }

                        // send "streaming downloading progress" message to Stream instance

                        mHandler.obtainMessage(MessageId.STREAM_DOWNLOADING_PROGRESS, streamingProgress)
                            .sendToTarget();

                        // send "chunk time of arrival" message

                        time = SystemClock.elapsedRealtime() - time;

                        mHandler.obtainMessage(MessageId.STREAM_CHUNK_TIME_OF_ARRIVAL, time)
                            .sendToTarget();

                        // whole range is received - stopping

                        if (wholeRange) {
                            stop = true;
                        }
                    } finally {
                        connection.disconnect();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();

                Log.d(TAG, "ConnectionThread: Failed");

                mHandler.obtainMessage(MessageId.STREAM_DOWNLOADING_FAILED, e.toString())
                    .sendToTarget();

                return;
            }

            Log.d(TAG, "ConnectionThread: Finished");

            mHandler.obtainMessage(MessageId.STREAM_DOWNLOADING_FINISHED, null)
                .sendToTarget();
        }
    }
}
