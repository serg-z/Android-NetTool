package com.Chris.NetTool;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import java.nio.channels.DatagramChannel;

import android.util.Log;

public class DatagramReceiver {
    private static final String TAG = "DatagramReceiver";

    public interface DatagramReceiverListener {
        public void onDatagramReceived(String datagramMessage);
    }

    private static final class MessageId {
        public static final int RECEIVED = 1;
    }

    private Thread mDatagramThread = null;
    private DatagramReceiverListener mListener = null;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            final int messageId = inputMessage.what;

            switch (messageId) {
                case MessageId.RECEIVED:
                    String datagramMessage = (String)inputMessage.obj;

                    Log.d(TAG, "Datagram received: " + datagramMessage);

                    if (mListener != null) {
                        mListener.onDatagramReceived(datagramMessage);
                    }

                    break;
            }
        }
    };

    public DatagramReceiver(int port) {
        mDatagramThread = new Thread(new DatagramThread(port));

        mDatagramThread.start();
    }

    public void setListener(DatagramReceiverListener listener) {
        mListener = listener;
    }

    public void stop() {
        if (mDatagramThread != null && mDatagramThread.isAlive()) {
            mDatagramThread.interrupt();
        }
    }

    private class DatagramThread implements Runnable {
        private int mPort;

        public DatagramThread(int port) {
            super();

            mPort = port;
        }

        @Override
        public void run() {
            Log.d(TAG, "("+ this + ") Datagram thread started");

            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

            DatagramSocket socket = null;

            try {
                DatagramChannel channel = DatagramChannel.open();

                socket = channel.socket();

                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(mPort));

                socket.setSoTimeout(10000);

                boolean stop = false;

                while (!stop) {
                    if (Thread.interrupted()) {
                        Log.d(TAG, "Interrupting datagram thread");

                        stop = true;

                        continue;
                    }

                    byte[] data = new byte[1024];

                    DatagramPacket packet = new DatagramPacket(data, data.length);

                    Log.d(TAG, "** DATAGRAM WAITING");

                    try {
                        socket.receive(packet);
                    } catch (SocketTimeoutException e) {
                        Log.d(TAG, "Datagram socket timeout");

                        continue;
                    }

                    String dataString = new String(packet.getData(), "UTF-8").trim();

                    mHandler.obtainMessage(MessageId.RECEIVED, dataString)
                        .sendToTarget();

                    Log.d(TAG, "** DATAGRAM END OF LOOP");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }

            Log.d(TAG, "(" + this + ") Datagram thread finished");
        }
    }
}
