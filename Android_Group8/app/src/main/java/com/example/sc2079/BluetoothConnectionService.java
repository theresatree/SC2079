package com.example.sc2079;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "Debugging Tag";
    private static final String appName = "MDP_Grp_8";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    Intent connectionStatus;
    //
    public static boolean BluetoothConnectionStatus=false;
    private static ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = context;
        startAcceptThread();
    }


    ///////////////////////////////////////////////////////////////////  ACCEPT THREAD    /////////////////////////////////////////////////////////////////
    //This thread will be running while listening for an incoming connection. Behaves like a
    //server-side client. Runs until connection is accepted or cancelled.
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket ServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID);
                Log.d(TAG, "Accept Thread: Setting up Server using: " + MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Accept Thread: IOException: " + e.getMessage());
            }
            ServerSocket = tmp;
        }
        public void run(){
            Log.d(TAG, "run: AcceptThread Running. ");
            BluetoothSocket socket =null;
            try {
                Log.d(TAG, "run: RFCOM server socket start here...");

                socket = ServerSocket.accept();
            }catch (IOException e){
                Log.e(TAG, "run: IOException: " + e.getMessage());
            }
            if(socket!=null){
                connected(socket, socket.getRemoteDevice());
            }
            Log.i(TAG, "END AcceptThread");
        }
        public void cancel(){
            Log.d(TAG, "cancel: Cancelling AcceptThread");
            try{
                ServerSocket.close();
            } catch(IOException e){
                Log.e(TAG, "cancel: Failed to close AcceptThread ServerSocket " + e.getMessage());
            }
        }
    }

    ///////////////////////////////////////////////////////////////////  CONNECT THREAD    /////////////////////////////////////////////////////////////////
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.d(TAG, "RUN: mConnectThread");
            for(ParcelUuid uuid : mmDevice.getUuids())
            {
                deviceUUID = UUID.fromString(uuid.toString());
                try {
                    Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " + MY_UUID);
                    tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
                } catch (IOException e) {
                    Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
                }
                mmSocket = tmp;
                //mBluetoothAdapter.cancelDiscovery();

                try {
                    mmSocket.connect();

                    Log.d(TAG, "RUN: ConnectThread connected.");
                    break;


                } catch (IOException e) {
                    try {
                        mmSocket.close();
                        Log.d(TAG, "RUN: ConnectThread socket closed.");
                    } catch (IOException e1) {
                        Log.e(TAG, "RUN: ConnectThread: Unable to close connection in socket." + e1.getMessage());
                    }
                    Log.d(TAG, "RUN: ConnectThread: could not connect to UUID." + MY_UUID);
                    try {

                    } catch (Exception z) {
                        z.printStackTrace();
                        Log.e(TAG,"error here");
                    }

                }
                try {
                    mProgressDialog.dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            connected(mmSocket, mmDevice);
        }

        public void cancel(){
            Log.d(TAG, "cancel: Closing Client Socket");
            try{
                mmSocket.close();
            } catch(IOException e){
                Log.e(TAG, "cancel: Failed to close ConnectThread mSocket " + e.getMessage());
            }
        }
    }

    ///////////////////////////////////////////////////////////////////  METHOD TO START ACCEPT/CONNECT THREAD ///////////////////////////////////////
    public synchronized void startAcceptThread(){
        Log.d(TAG, "start");

        //Cancel any thread attempting to make a connection
        if(mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread=null;
        }

        //If accept thread is null we want to start a new one
        if(mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    // Start connection
    // AcceptThread starts and listens for a connection
    // Afterwards, ConnectThread starts and attempts to connect to other device's AcceptThread
    public void startClientThread(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started.");
        try {
            // removed the progress dialog because it kept getting stuck on top and not disappearing
            //mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...", true);
        } catch (Exception e) {
            Log.d(TAG, "StartClientThread Dialog show failure");
        }
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mSocket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            this.mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device", mmDevice);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
            BluetoothConnectionStatus = true;

            // Update UI
//            TextView status = Home.getBluetoothStatus();
//            status.setText("Connected");
//            status.setTextColor(Color.GREEN);
//
//            TextView device = Home.getConnectedDevice();
//            device.setText(mmDevice.getName());

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        // From the repo I followed, mentioned that sometimes only 1 char may be sent, have not encountered this yet
        public void run() {
            byte[] buffer = new byte[1024]; // buffer to store info from the stream
            int bytes; // bytes returned from read()

            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes); // convert bytes to string, so we can see the message
                    Log.d(TAG, "InputStream: " + incomingMessage);
                    handleIncomingBTMessage(incomingMessage);

                    // Display the incoming message on the chat by starting incomingMessage intent
                    // SHIFTED INTO handleIncomingBTMessage
                    //Intent incomingMessageIntent = new Intent("incomingMessage");
                    //incomingMessageIntent.putExtra("msg", incomingMessage);

                    //LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading input stream. " + e.getMessage());

                    // Update the UI to show disconnected and update the connection status
                    //TextView status = Home.getBluetoothStatus();
                    //status.setText("Disconnected");
                    //status.setTextColor(Color.RED);

                    // Start intent to update the ConnectionStatus in the event of a disconnect, also triggers reconnection attempt
                    connectionStatus = new Intent("ConnectionStatus");
                    connectionStatus.putExtra("Status", "disconnected");
                    connectionStatus.putExtra("Device", mmDevice);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
                    BluetoothConnectionStatus = false;

                    break;
                }
            }
        }

        // Used to determine what type of message was received from the Rpi
        private void handleIncomingBTMessage(String msg){
            Log.d(TAG, "handleIncomingBTMessage: New Incoming Message: " + msg);
            // try to convert the message into a Json Object
            try{
                JSONObject msgJSON = new JSONObject(msg);
                Log.i(TAG, "Successfully converted into JSON: " + msg);
                String msgType = msgJSON.getString("type");
                switch(msgType.toUpperCase()){
                    case "IMAGE-REC":
                        // create a broadcastreceiver to handle the intent
                        // send an intent using SendIntent to update the obstacles
                        return;
                    case "CHAT":
                        // Can test using {"type":"CHAT","msg":"helloworld"}, in AMDTool
                        String message = msgJSON.getString("msg");
                        sendIntent("incomingMessage", message);
                        Log.d(TAG, "handleIncomingBTMessage: FOUND A CHAT MESSAGE");
                        return;
                    case "OBSTACLE-INCOMING":
                        // Handle OBSTACLE-INCOMING messages
                        int obstacleID = msgJSON.getInt("obstacleID");  // Get obstacle ID (use getInt() for integer values)
                        String obstacleText = msgJSON.getString("obstacleText");  // Get obstacle text

                        // Log the obstacle data
                        Log.d(TAG, "handleIncomingBTMessage: FOUND OBSTACLE-INCOMING - ID: " + obstacleID + ", Text: " + obstacleText);

                        // Send data to the fragment
                        Intent intent = new Intent("obstacleUpdate");

                        // Put the data into the intent
                        intent.putExtra("obstacleID", obstacleID);
                        intent.putExtra("obstacleText", obstacleText);

                        // Send the intent (you can use LocalBroadcastManager or standard broadcast based on your needs)
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                        return;
                }
            } catch(Exception e){
                // Not a JSON obj
                JSONObject msgJSON = new JSONObject();
                //msgJSON.put("msg", msg);
                //sendIntent("incomingMessage", msgJSON.toString());
            }
        }

        // Used to send messages out
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: "+text);
            try {
                outStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream. "+e.getMessage());
            }
        }


        public void cancel(){
            Log.d(TAG, "cancel: Closing Client Socket");
            try{
                mSocket.close();
            } catch(IOException e){
                Log.e(TAG, "cancel: Failed to close ConnectThread mSocket " + e.getMessage());
            }
        }
    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice device) {
        Log.d(TAG, "connected: Starting.");

        // Changed here, everything below is added
        mmDevice =  device;
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    public static void write(byte[] out){
        ConnectedThread tmp;

        Log.d(TAG, "write: Write is called." );
        mConnectedThread.write(out);
    }

    private void sendIntent(String intentAction, String content)
    {
        Intent sendingIntent = new Intent(intentAction);
        sendingIntent.putExtra("msg", content);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(sendingIntent);
    }
}

