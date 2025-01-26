package com.example.sc2079;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionService";
    private static final String appName = "SC2079";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

/////////////////////////////////////////////////////////////////  ACCEPT THREAD    /////////////////////////////////////////////////////////////////

    // Thread that constantly runs that is always listening for connection.
    // It runs until a connection is accepted/cancelled
    private class AcceptThread extends Thread {
        // Local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            //Create a new listening servet socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread: Setting up server using: " + MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }
            mmServerSocket = tmp;
        }

        // This method automatically runs in a thread.
        // No need to call it
        public void run() {
            Log.d(TAG, "AcceptThread: AcceptThread Running");

            BluetoothSocket socket = null;

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "AcceptThread: Server Socket start");
                socket = mmServerSocket.accept(); // will only go past once its successful
                Log.d(TAG, "AcceptThread: Server Socket accepted connection.");
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }


            if (socket != null) {
                connected(socket, mmDevice);
            }

            Log.d(TAG, "AcceptThread: End mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "AcceptThread: Cancelling AcceptThread");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: Cancelling of ServerSocket failed. IOException: " + e.getMessage());
            }
        }
    }

/////////////////////////////////////////////////////////////////  CONNECT THREAD    /////////////////////////////////////////////////////////////////
    // initiates bluetooth connection after accepting

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: Started");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.d(TAG, "ConnectThread: Running");

            // Get a BluetoothSocket for a connection with the given bluetooth device
            // We then create a RFCOMM socket using the UUID for bluetooth communication
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRFCommSocket using UUID" + MY_UUID_INSECURE);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRFCommSocket. IOException: " + e.getMessage());
            }
            mmSocket = tmp;

            // Cancel discovery once connection is made because it will slow down connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                //Make a connection to BluetoothSocket
                //Blocking call, will only return on success or exception
                mmSocket.connect();
                Log.d(TAG, "ConnectThread: connectThread connected");

            } catch (IOException e) {
                try {
                    mmSocket.close();
                    Log.d(TAG, "ConnectThread: Closed Socket");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: Unable to close connection in socket. IOException: " + e1.getMessage());
                }
                Log.d(TAG, "ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE);
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            Log.d(TAG, "ConnectThread: Cancelling ConnectThread");
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Cancelling of mmSocket failed. IOException: " + e.getMessage());
            }
        }
    }

/////////////////////////////////////////////////////////////////  METHOD TO START ACCEPT/CONNECT THREAD /////////////////////////////////////////////////////////////////

    // Called by Activity onResume()
    // Specifically to start AcceptThread to begin session in listening mode
    public synchronized void start() {
        Log.d(TAG, "Start");

        //Case 1: Cancel any thread attempting to start a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        //Case 2: Start a new AcceptThread if it is null
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    // AcceptThread starts and sit waiting for a connection
    // Then ConnectThread starts and attempts to make a connection with other devices' AcceptThread
    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started");

        //initprogress dialog
        try{
            mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please wait...", true);
        } catch (NullPointerException e){

        }

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG, "ConnectedThreat: Starting");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Dimiss the progressDialog when connection is established
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            try {
                tmpOut = mmSocket.getOutputStream();
                tmpIn = mmSocket.getInputStream();

            } catch (IOException e) {
                Log.e(TAG, "ConnectedThreat: Couldn't get input or output stream");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes; // bytes returned from read();

            // Keep listening to the InputStream until an exception occurs
            while(true){
                // Read from the inputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes); // Convert to string
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);

                } catch (IOException e) {
                    Log.e(TAG, "InputStream: Error reading from InputStream. " +  e.getMessage());
                    break; // Break loop if exception occurs
                }

            }
        }

        // Call this method from the MainActivity to send data to remote device
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "Writing to OutputStream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to OutputStream. " + e.getMessage());
            }
        }


        public void cancel() {
            try{
                mmSocket.close();
            } catch(IOException e){
                Log.e(TAG, "ConnectedThread: Error in cancelling. IOException: " + e.getMessage());
            }

        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice){
        Log.d(TAG, "connected: Starting");

        // Start the thread to manage connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    // Method to write to the ConnectedThread in an asynchronous manner
    public void write (byte[] out){
//        // Create temporary object
//        ConnectedThread r;

        Log.d(TAG, "write: Write Called");
        mConnectedThread.write(out);
    }
}



