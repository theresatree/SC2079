package com.example.sc2079;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothSetUpActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    public static final String TAG = "BluetoothSetUp"; // Helps to filter logs from BluetoothActivity

    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>(); //Hold array of all bluetooth devices discovered
    public ArrayList<BluetoothDevice> mBTPairedDevices = new ArrayList<>(); //Hold array of all bluetooth devices discovered
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;
    ListView lvPairedDevices;

    BluetoothConnectionService mBluetoothConnection;
    Button btnStartConnection;
    Button btnSend;
    EditText etSend;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    BluetoothDevice mBTDevice;

    TextView incomingMessages;
    StringBuilder messages;

    // Stuff that is used for reconnecting
    Handler reconnectionHandler = new Handler();
    boolean retryConnection = false; // this flag tells us whether or not we have tried reconnecting yet

    Runnable reconnectionRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                if(BluetoothConnectionService.BluetoothConnectionStatus == false)
                {
                    Log.d(TAG, "Reconnecting...");
                    startBTConnection(mBTDevice, MY_UUID_INSECURE);
                    //Toast.makeText(this, "Reconnection Successful", Toast.LENGTH_SHORT).show(); // not sure why toast throwing an error here
                    Log.d(TAG, "Reconnection Sucessful");
                }
                reconnectionHandler.removeCallbacks(reconnectionRunnable);
                retryConnection = false;
            } catch (Exception e) {
                Log.d(TAG, "Reconnection Failed");
                e.printStackTrace();
                Log.d(TAG, "Failed to reconnect, retrying in 5s...");
            }
        }
    };

    //BroadcastReceiver for ACTION_STATE_CHANGED (ENABLING OF BLUETOOTH)
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth State: 'ON'");
                        Toast.makeText(context, "Bluetooth is enabled", Toast.LENGTH_LONG).show();
                        checkForPairedDevices(); // Check for paired devices
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "Bluetooth State: 'TURNING ON'");
                        break;
                }
            }
        }
    };

    //BroadcastReceiver for ACTION_FOUND (DISCOVERY)
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "Found Device: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.list_item_bluetooth, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Bluetooth device scanning finished.");
                Toast.makeText(context, "Scan completed", Toast.LENGTH_LONG).show();
                unregisterReceiver(mBroadcastReceiver2);
            }
        }
    };

    //BroadcastReceiver for ACTION_BOND_STATE_CHANGED (PAIRING)
    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //3 case we can check
                //Case 1: Bonded devices:
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver3 State: BOND_BONDED");
                    mBTDevice=mDevice; // Set the current bluetooth device
                    mBTPairedDevices.clear(); // Clear the list of devices
                    checkForPairedDevices(); // Generate a new list of devices
                }
                //Case 2: Creating a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcastReceiver3 State: BOND_BONDING");
                }
                //Case 3: Breaking the bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BroadcastReceiver3 State: BOND_NONE");
                }
            }
        }
    };

    // Receiver to handle events related to connection (connecting, reconnecting, disconnecting)
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");

            if(status.equals("connected")){
                Log.d(TAG, "mBroadcastReceiver4: Device is now connected to " + mDevice.getName());
                Toast.makeText(context, "Device is now connected to " + mDevice.getName(), Toast.LENGTH_SHORT).show();
            }
            else if(status.equals("disconnected") && retryConnection == false){
                Log.d(TAG, "mBroadcastReceiver4: Disconnected from " + mDevice.getName());
                Toast.makeText(context, "Disconnected from " + mDevice.getName(), Toast.LENGTH_SHORT).show();

                // Attempt to reconnect
                mBluetoothConnection = new BluetoothConnectionService(getApplicationContext());

                retryConnection = true; // By setting this to true, we only try to reconnect once
                reconnectionHandler.postDelayed(reconnectionRunnable, 5000); // wait 5s before trying to reconnect
            }

        }
    };

    // This is for getting incoming messages
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("msg");

            // messages is a StringBuilder (think of it as just a normal string)
            // Then whenever we receive a message, we just append to this string
            // After updating the "messages", we will update the UI element "incomingMessages"
            messages.append(text + "\n");
            incomingMessages.setText(messages);

            // Check what is the string we received

        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestory: called.");
        super.onDestroy();

        try {
            unregisterReceiver(mBroadcastReceiver1);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver mBroadcastReceiver1 not registered.");
        }

        try {
            unregisterReceiver(mBroadcastReceiver2);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver mBroadcastReceiver2 not registered.");
        }

        try {
            unregisterReceiver(mBroadcastReceiver3);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver mBroadcastReceiver3 not registered.");
        }

        try {
            unregisterReceiver(mBroadcastReceiver4);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver mBroadcastReceiver4 not registered.");
        }

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver mReceiver not registered.");
        }

        mBluetoothAdapter.cancelDiscovery();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bluetooth);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getBTPermission(); //Call the permissions
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Initialise bluetooth adapter
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices); // Initialise List View
        lvPairedDevices = (ListView) findViewById(R.id.lvPairedDevices);
        mBTDevices = new ArrayList<>(); // Initialise a new list of Devices
        mBTPairedDevices = new ArrayList<>();

        btnStartConnection = (Button) findViewById(R.id.btnStartConnection);
        btnSend = (Button) findViewById(R.id.btnSend);
        etSend = (EditText) findViewById(R.id.etSend);

        incomingMessages = (TextView) findViewById(R.id.incomingMessage);
        messages = new StringBuilder();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        lvNewDevices.setOnItemClickListener(BluetoothSetUpActivity.this);
        lvPairedDevices.setOnItemClickListener(BluetoothSetUpActivity.this);

        checkForPairedDevices();// Check for paired devices
        // Broadcasts when bond state change (E.G PAIRING)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver3, filter);

        // BroadcastReceiver related to Connection Events
        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver4, filter2);

        Button btnBTOn = findViewById(R.id.btnBTOn);
        btnBTOn.setOnClickListener(view -> enableBluetooth());

        Button btnScan = findViewById(R.id.btnScan);
        btnScan.setOnClickListener(view -> discoverBluetooth());

        btnStartConnection.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                startConnection();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);

                etSend.setText("");
            }
        });

    }

    // Create method for starting connection
    // Connection will fail and app will crash if not paired.
    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }

    // Starting the chat service method
    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initialising RFCOMM Bluetooth Connection");

        mBluetoothConnection.startClientThread(device,uuid);
    }

    public void getBTPermission(){
        // For some reason, we need to ask for bluetooth permission during runtime to enable.
        // Therefore, we got to repeat the permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and above, request Bluetooth permissions
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private void enableBluetooth(){
        //  If device does not support bluetooth
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Device does not support Bluetooth");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);

            // The broadcast receiver will catch the state change from the bluetooth when we enable
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if (mBluetoothAdapter.isEnabled()){
            Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_LONG).show();
        }
    }

    private void checkForPairedDevices(){
        if (mBluetoothAdapter!=null && mBluetoothAdapter.isEnabled()){
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    mBTPairedDevices.add(device);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    Log.d(TAG, "Paired Device: " + deviceName + ", " + deviceHardwareAddress);
                    mDeviceListAdapter = new DeviceListAdapter(this, R.layout.list_item_bluetooth, mBTPairedDevices);
                    lvPairedDevices.setAdapter(mDeviceListAdapter);
                }
            }
            else{
                Log.d(TAG, "No Paired Devices");
            }
        }
    }

    public void discoverBluetooth(){
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable bluetooth first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "Cancelling ongoing scan...");
            return;
        }
        Log.d(TAG, "Scanning for bluetooth devices");
        Toast.makeText(this, "Scanning for bluetooth devices", Toast.LENGTH_SHORT).show();
        mBTDevices.clear(); // Reset to 0
        mBluetoothAdapter.startDiscovery();
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        discoverDevicesIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBroadcastReceiver2, discoverDevicesIntent);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView.getId() == R.id.lvNewDevices) {
            // This is the ListView for new devices
            Log.d(TAG, "Clicked on a New Device");
            String deviceName = mBTDevices.get(i).getName();
            String deviceAddress = mBTDevices.get(i).getAddress();
            Log.d(TAG, "New Device: " + deviceName + ": " + deviceAddress);

            // Try to pair the selected device
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            //Start connection service after bonding
            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(this);

        } else if (adapterView.getId() == R.id.lvPairedDevices) {
            // This is the ListView for paired devices found during scanning
            Log.d(TAG, "Clicked on a Paired Device");
            BluetoothDevice device = (BluetoothDevice) adapterView.getItemAtPosition(i);
            String deviceName = mBTPairedDevices.get(i).getName();
            String deviceAddress = mBTPairedDevices.get(i).getAddress();
            Log.d(TAG, "Paired Device: " + deviceName + ": " + deviceAddress);

            // Try to pair with the selected device
            Log.d(TAG, "Trying to pair with " + deviceName);
            Toast.makeText(this, "Trying to pair with " + deviceName, Toast.LENGTH_LONG).show();
            mBTPairedDevices.get(i).createBond();

            //Start connection service after bonding
            mBTDevice = mBTPairedDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(this);
        }
    }
}