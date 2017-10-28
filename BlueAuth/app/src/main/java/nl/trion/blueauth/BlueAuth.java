package nl.trion.blueauth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by irrlicht on 10/26/17.
 */

public class BlueAuth{
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice hk01;
    TextView debugTxt;
    Button authBtn;
    Context context;
    ProtState state;
    private static final int CHALLENGE_SIZE = 128;
    private static final int RESPONSE_SIZE = 128;
    private static final BigInteger SECRET  = new BigInteger("15980825276566239590571158220152381980628708270401139258438696993815358932193248003061016270236075851772094737326384267412827716920179674685527846374113965466104433712614446748114391739196249056212267061311800716727253417012485494558500192103499136306280819443234478481313221062228784985720161016328001681489");
    private static final BigInteger MODULUS = new BigInteger("125069900423969625513167183696456491266355822058428405013171349998050773625405886598592049594275340709050366945325919003276151192237618263657205453525233716561923402213695623567778362333117173702622386572910559830547887242097802023428298910714674944609028413754889990570586265421100578971326792564496946845503");

    private enum ProtState { STATE_OPEN, STATE_CONNECTING, STATE_CONNECTED, STATE_REC_CHALLENGE, STATE_SEN_RESPONSE,
                            STATE_REC_RESULT, STATE_CLOSING, STATE_CLOSED }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(MainActivity.TAG, deviceHardwareAddress);
                if(deviceName != null ) {
                    Log.d(MainActivity.TAG, deviceName);
                    if(deviceName.equals(MainActivity.COMPUTER_NAME)) {
                        debugTxt.setText("Found " + MainActivity.COMPUTER_NAME);
                        hk01 = device;
                        authBtn.setEnabled(true);
                        mBluetoothAdapter.cancelDiscovery();
                        context.unregisterReceiver(this);
                    }
                }
            }
        }
    };

    public BlueAuth(Context context, TextView debugTxt, Button authBtn) {
        this.context = context;
        this.debugTxt = debugTxt;
        this.authBtn = authBtn;
        state = ProtState.STATE_CLOSED;
    }

    private boolean getIfBonded() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for (BluetoothDevice bd : pairedDevices) {
                if (bd.getName().equals(MainActivity.COMPUTER_NAME)) {
                    hk01 = bd;
                    return true;
                }
            }
        }
        return false;
    }

    public void findDevice() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                hk01 = mBluetoothAdapter.getRemoteDevice(MainActivity.COMPUTER_BT_MAC);
                if(hk01 != null && MainActivity.COMPUTER_NAME.equals(hk01.getName())) {
                    return "Device found (MAC found)";
                } else if(getIfBonded()) {
                    return "Device found (already bonded)";
                } else {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    context.registerReceiver(mReceiver, filter);
                    mBluetoothAdapter.startDiscovery();
                    Log.d(MainActivity.TAG, "registered receiver");
                    return "Not found. Searching for "+ MainActivity.COMPUTER_NAME +"...";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                debugTxt.setText(result);
                if(hk01 != null && MainActivity.COMPUTER_NAME.equals(hk01.getName())) {
                    authBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    public void unregister() {
        try{
            context.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // receiver not registered
        }
        mBluetoothAdapter.cancelDiscovery();
    }

    public boolean bluetoothSupported() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            debugTxt.setText("You do not support bluetooth or it is disabled");
            return false;
        }
        debugTxt.setText("You support bluetooth");
        return true;
    }

    public void connect() {
        new AsyncTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                if(hk01 == null) {
                    return MainActivity.COMPUTER_NAME + " not found (yet)...";
                }
                BluetoothSocket socket;
                try {
                    Method m = hk01.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
                    socket = (BluetoothSocket) m.invoke(hk01, 1);
                    publishProgress("Connecting to remote host...");
                    socket.connect();
                    if(socket.isConnected()) {
                        unregister();
                        // Open Streams
                        InputStream is;
                        OutputStream os;
                        try {
                            is = socket.getInputStream();
                            os = socket.getOutputStream();
                        } catch (IOException e) {
                            e.printStackTrace();
                            socket.close();
                            return "IO error";
                        }
                        // Do Protocol
                        // Receive challenge
                        byte[] chal = new byte[CHALLENGE_SIZE];
                        int nr_of_bytes = is.read(chal);
                        byte[] challenge = Arrays.copyOfRange(chal, 0, nr_of_bytes);
                        Log.d(MainActivity.TAG, "Received challenge: " + new BigInteger(new String(challenge, "ascii")).toString());
                        // Send response
                        BigInteger challengeInt = new BigInteger(new String(challenge, "ascii"));
                        BigInteger responseInt = challengeInt.modPow(SECRET, MODULUS);
                        Log.d(MainActivity.TAG, "Response: " + responseInt.toString());
                        os.write(responseInt.toByteArray());

                        // Receive result
                        byte[] result = new byte[1];
                        nr_of_bytes = is.read(result);
                        if(nr_of_bytes != 1) {
                            socket.close();
                            return "Result is not correct: " + new String(result);
                        }

                        // If we got here we can close, because the protocol succeeded!
                        socket.close();
                        return "Protocol succes, with result: " + new String(result);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    return "Connection failed, is the host offline?";
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                debugTxt.setText(s);
                authBtn.setEnabled(true);
            }

            @Override
            protected void onProgressUpdate(String... values) {
                String str = values[0];
                if(values.length > 0) {
                    str = values[values.length - 1];
                }
                Log.d(MainActivity.TAG, "onProgressUpdate: " + str);
                debugTxt.setText(str);
            }
        }.execute();
    }
}
