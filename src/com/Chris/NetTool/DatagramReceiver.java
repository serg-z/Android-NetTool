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

/*
 * DatagramReceiver is used for receiving broadcasted on network messages and notifying the listener about it.
 *
 * The connection runs in separate thread.
 */

public class DatagramReceiver {
    private static final String TAG = "DatagramReceiver";

    /* Interface for listener */

    public interface DatagramReceiverListener {
        public void onDatagramReceived(String datagramMessage);
    }

    private static final class MessageId {
        public static final int RECEIVED = 1;
    }

    private Thread mDatagramThread = null;
    private DatagramReceiverListener mListener = null;

    /* Run new message handler on main looper to make possible inter-thread communication through messages */

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            final int messageId = inputMessage.what;

            switch (messageId) {
                // new message received
                case MessageId.RECEIVED:
                    // get the text from message
                    String datagramMessage = (String)inputMessage.obj;

                    Log.d(TAG, "Datagram received: " + datagramMessage);

                    if (mListener != null) {
                        // send it to the listener
                        mListener.onDatagramReceived(datagramMessage);
                    }

                    break;
            }
        }
    };

    public DatagramReceiver(int port) {
        // start datagram thread
        mDatagramThread = new Thread(new DatagramThread(port));

        mDatagramThread.start();
    }

    public void setListener(DatagramReceiverListener listener) {
        mListener = listener;
    }

    public void stop() {
        if (mDatagramThread != null && mDatagramThread.isAlive()) {
            // TODO: don't use interrupt for stopping
            mDatagramThread.interrupt();
        }
    }

    /* DatagramThread is used to hold datagram connection and read it's messages asynchronously from main thread */

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
                // open connection on channel to make possible port reusing
                DatagramChannel channel = DatagramChannel.open();

                socket = channel.socket();

                // reuse address to make possible two DatagramThreads running concurrently
                // (before one of them finishes)
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(mPort));

                // timeout of reading operations' blocks
                socket.setSoTimeout(10000);

                boolean stop = false;

                // loop continuously, while not stopped by thread's interruption
                while (!stop) {
                    if (Thread.interrupted()) {
                        Log.d(TAG, "Interrupting datagram thread");

                        stop = true;

                        continue;
                    }

                    // data is fixed sized byte array
                    byte[] data = new byte[1024];

                    DatagramPacket packet = new DatagramPacket(data, data.length);

                    Log.d(TAG, "** DATAGRAM WAITING");

                    try {
                        // read datagram packet from socket
                        socket.receive(packet);
                    } catch (SocketTimeoutException e) {
                        Log.d(TAG, "Datagram socket timeout");

                        continue;
                    }

                    // convert the data to utf-8 string and cut the whitespace
                    String dataString = new String(packet.getData(), "UTF-8").trim();

                    // send the message to handler which will pass it to listener
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
