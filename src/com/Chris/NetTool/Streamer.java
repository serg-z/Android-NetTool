package com.Chris.NetTool;

import android.util.Log;

import java.net.URL;
import java.net.HttpURLConnection;

import java.lang.Thread;
import java.lang.Runnable;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Streamer {
    private static final String TAG = "Streamer";

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

    Thread mConnectionThread;

    Streamer(String urlString, int bitrate, int chunkSize, int bufferSize) {
        mBitrate = bitrate;
        mChunkSize = chunkSize;
        mBufferSize = bufferSize;

        mConnectionThread = new Thread(new ConnectionThread(urlString, mBitrate, mChunkSize, mBufferSize));

        mConnectionThread.start();
    }

    public void stop() {
        mConnectionThread.interrupt();
    }

    class ConnectionThread implements Runnable {
        String mUrlString;

        ConnectionThread(String urlString, int bitrate, int chunkSize, int bufferSize) {
            super();

            mUrlString = urlString;
        }

        @Override
        public void run() {
            Log.d(TAG, String.format("Connection thread started [bitrate=%d, buffer=%d, chunk=%d]", mBitrate, mBufferSize, mChunkSize));

            // sizes in bytes
            final int oneSecondDataSize = mBitrate * (1000 / 8);
            final int chunkDataSize = oneSecondDataSize * mChunkSize;
            final int bufferDataSize = oneSecondDataSize * mBufferSize;

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

                    URL url = new URL(mUrlString);

                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();

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

                                rangeTo = Math.min(contentSize - 1, chunkDataSize - 1);

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

                        Log.d(TAG, "=====");

                        rangeFrom = rangeTo + 1;
                        rangeTo = Math.min(contentSize - 1, rangeFrom + chunkDataSize - 1);

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
