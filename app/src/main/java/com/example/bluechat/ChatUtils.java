package com.example.bluechat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ChatUtils {
    private Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private AcceptThread acceptThread;
    private static final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final String APP_NAME = "BlueChat";
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;


    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    public synchronized int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    private synchronized void start() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_LISTEN);
    }

    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            if(connectThread != null){
                connectThread.cancel();
                connectThread = null;
            }
        }
        connectThread = new ConnectThread(device);
        connectThread.start();

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTING);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) { // Use a temporary object that is later assigned to Socket because Socket is final.
            this.device = device;
            BluetoothSocket tmp = null;
            try { //Get a BluetoothSocket to connect with the given BluetoothDevice. APP_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }
            socket = tmp;
        }

        public void run() {
            setName("Connect Thread");
            // Cancel discovery because it otherwise slows down the connection.
//            bluetoothAdapter.cancelDiscovery();
            try {  //Connect to the remote device through the socket. This call blocks until it succeeds or throws an exception.
                socket.connect();
            } catch (IOException e) {   // Unable to connect; close the socket and return.
                Log.e("Connect->Run", e.toString());
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("Connect->CloseSocket", e.toString());
                }
                connectionFailed();
                return;
            }
            synchronized (ChatUtils.this) {
                connectThread = null;
            }
            connected(socket,device);
        }

        public void cancel() {   // Closes the client socket and causes the thread to finish.
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if(acceptThread != null){
            acceptThread.cancel();
            acceptThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("Connected->Stream",e.toString());
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;   // bytes returned from read()// Keep listening to the InputStream until an exception occurs.
                while (true){
                    try {
                        bytes = inputStream.read(buffer);    // Read from the InputStream.
                        handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();  // Send the obtained bytes to the UI activity.
                    } catch (IOException e) {
                        connectionLost();
                    }
                }
        }

        public void write(byte[] bytes) {   // Call this from the main activity to send data to the remote device.
            try {
                outputStream.write(bytes);
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, bytes).sendToTarget(); // Share the sent message with the UI activity.
            } catch (IOException e) {
                Toast.makeText(context,"message is not writing.",Toast.LENGTH_SHORT).show();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connected->Cancel", e.toString());
            }
        }
    }

    private void connectionLost() {
        setState(STATE_LISTEN);
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    public void writemsg(byte[] buffer) {
        ConnectedThread connThread;
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                return;
            }
            connThread = connectedThread;
        }
        connThread.write(buffer);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e("Accept->Constructor", e.toString());
            }
            serverSocket = tmp;
        }

        public void run() {
            setName("Accept Thread");
            BluetoothSocket socket = null;
            while (state != STATE_CONNECTED){
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e("Accept->Run", e.toString());
                    try {
                        serverSocket.close();
                    } catch (IOException e1) {
                        Log.e("Accept->Close", e.toString());
                    }
                }
                if (socket != null) {
//                A connection was accepted. Perform work associated with the connection in a separate thread.
                    switch (state) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            connected(socket,socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e("Accept->CloseSocket", e.toString());
                            }
                            break;
                    }
                }
            }

        }

        public void cancel() {   // Closes the connect socket and causes the thread to finish.
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("Accept->CloseServer", e.toString());
            }
        }
    }

    private synchronized void connectionFailed() {
        setState(STATE_LISTEN);
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Can't connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);
        ChatUtils.this.start();
    }

}
