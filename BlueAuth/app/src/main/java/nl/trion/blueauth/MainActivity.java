package nl.trion.blueauth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView debugTxt;
    Button authBtn;
    String TAG = "BlueAuth";
    int REQUEST_COARSE_LOCATION = 3;

    String COMPUTER_NAME = "HK01";
    String COMPUTER_BT_MAC = "5C:51:4F:58:CB:D5";

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice hk01;

    private boolean getIfBonded() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for (BluetoothDevice bd : pairedDevices) {
                if (bd.getName().equals(COMPUTER_NAME)) {
                    hk01 = bd;
                    return true;
                }
            }
        }
        return false;
    }
    private void connect() {
        if(hk01 == null) {
            debugTxt.setText(COMPUTER_NAME + " not found yet...");
            return;
        }
        debugTxt.setText(COMPUTER_NAME + ", now connecting...");
        BluetoothSocket socket;
        try {
            Method m = hk01.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
            socket = (BluetoothSocket) m.invoke(hk01, 1);
            socket.connect();
            if(socket.isConnected()) {
                debugTxt.setText("I think we are connected");
                try {
                    unregisterReceiver(mReceiver);
                } catch(IllegalArgumentException e) {
                    // Receiver not registered
                }
                // Open Streams
                InputStream is;
                OutputStream os;
                try {
                    is = socket.getInputStream();
                    os = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                doProtocol();
                socket.close();
                return;
            }
        } catch(Exception e) {
            e.printStackTrace();
            debugTxt.setText("Connection failed, is the host down?");
            Log.d(TAG, "Exception");
        }
    }

    private void doProtocol() {
        // Read challenge
        // Send response
        // Read status
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, deviceHardwareAddress);
                if(deviceName != null ) {
                    Log.d(TAG, deviceName);
                    if(deviceName.equals(COMPUTER_NAME)) {
                        debugTxt.setText("Found " + COMPUTER_NAME);
                        hk01 = device;
                        mBluetoothAdapter.cancelDiscovery();
                    }
                }
            }
        }
    };

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        }
    }

    private boolean bluetoothSupported() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            debugTxt.setText("You do not support bluetooth or it is disabled");
            return false;
        }
        debugTxt.setText("You support bluetooth");
        return true;
    }

    private void unregisterMyReceiver() {
        try{
            unregisterReceiver(mReceiver);
            mBluetoothAdapter.cancelDiscovery();
        } catch (Exception e) {
            // receiver not registered
        }
    }

    private void findDevice() {
        hk01 = mBluetoothAdapter.getRemoteDevice(COMPUTER_BT_MAC);
        if(hk01 != null && hk01.getName().equals(COMPUTER_NAME)) {
            debugTxt.setText("Device found (MAC found)");
        } else if(getIfBonded()) {
            debugTxt.setText("Device found (already bonded)");
        } else {
            debugTxt.setText("Not found. Searching for "+ COMPUTER_NAME +"...");
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
            mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "registered receiver");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        debugTxt = (TextView) findViewById(R.id.debugTxt);
        authBtn = (Button) findViewById(R.id.authBtn);
        authBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });

        // Request Permissions
        requestPermission();
        // Test Bluetooth support
        if(!bluetoothSupported()) return;
        // Unregister receiver
        unregisterMyReceiver();
        // Find the Device
        findDevice();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mReceiver);
        } catch(IllegalArgumentException e) {
            // Receiver was not registered
        }
    }
}
