package nl.trion.blueauth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice machineBTDevice;
    private TextView debugTxt;
    private ProgressBar progressBar;
    private ImageView resultImageView;
    private ImageView statusImageView;
    private Context context;
    private static final int CHALLENGE_SIZE = 1024;
    private static final int RESPONSE_SIZE = 1024;
    private BlueAuthDevice bad;
    private SharedPreferences sp;

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
                    if(deviceName.equals(bad.hostName)) {
                        debugTxt.setText("Found " + bad.hostName);
                        machineBTDevice = device;
                        mBluetoothAdapter.cancelDiscovery();
                        context.unregisterReceiver(this);
                    }
                }
            }
        }
    };

    public BlueAuth(Activity activity, BlueAuthDevice bad) {
        this.context = activity;
        this.debugTxt = (TextView) ((MainActivity) activity).findViewById(R.id.debugTxt);
        this.progressBar = (ProgressBar) ((MainActivity) activity).findViewById(R.id.progressBar);
        this.resultImageView = (ImageView) ((MainActivity) activity).findViewById(R.id.resultImageView);
        this.statusImageView = (ImageView) ((MainActivity) activity).findViewById(R.id.statusImageView);

        Log.d(MainActivity.TAG, bad.toString());

        this.bad = bad;
        this.sp = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private boolean getIfBonded() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for (BluetoothDevice bd : pairedDevices) {
                if (bd.getName().equals(bad.hostMac) &&
                        bd.getAddress().equals(bad.hostMac)) {
                    machineBTDevice = bd;
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
                machineBTDevice = mBluetoothAdapter.getRemoteDevice(bad.hostMac);
                if(machineBTDevice != null && bad.hostName.equals(machineBTDevice.getName())) {
                    return "Device found (MAC found)";
                } else if(getIfBonded()) {
                    return "Device found (already bonded)";
                } else {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    context.registerReceiver(mReceiver, filter);
                    mBluetoothAdapter.startDiscovery();
                    Log.d(MainActivity.TAG, "registered receiver");
                    return "Not found. Searching for "+ bad.hostName +"...";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                debugTxt.setText(result);
                if(machineBTDevice != null && bad.hostName.equals(machineBTDevice.getName())) {
                    connect();
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

    private void toggle(boolean done, int color) {
        statusImageView.setBackgroundColor(color);
        if(done) {
            progressBar.setVisibility(View.GONE);
            statusImageView.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            statusImageView.setVisibility(View.GONE);
        }
    }

    private void toggle(boolean done) {
        toggle(done, Color.RED);
    }

    public boolean bluetoothSupported() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            debugTxt.setText("You do not support bluetooth or it is disabled");
            toggle(true);
            return false;
        }
        debugTxt.setText("You support bluetooth");
        return true;
    }

    public void connect() {
        new AsyncTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                if(machineBTDevice == null) {
                    return bad.hostName + " not found error...";
                }
                BluetoothSocket socket;
                try {
                    Method m = machineBTDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
                    socket = (BluetoothSocket) m.invoke(machineBTDevice, 1);
                    publishProgress("Connecting to host...");
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
                        Log.d(MainActivity.TAG, "Received challenge: (" + String.valueOf(nr_of_bytes) + ")" + new BigInteger(1, challenge).toString());
                        // Send response
                        BigInteger resul = sign(new BigInteger(1, challenge));
                        Log.d(MainActivity.TAG, "Sending response: (" + String.valueOf(resul.toByteArray().length) + ")" + resul.toString());
                        os.write(resul.toByteArray());

                        // Receive result for last UI update
                        byte[] result = new byte[1];
                        nr_of_bytes = is.read(result);
                        if(nr_of_bytes != 1) {
                            socket.close();
                            return "Result error, not correct: " + new String(result);
                        }

                        // If we got here we can close, because the protocol succeeded!
                        socket.close();
                        if(new String(result).equals("y")) {
                            return "Protocol succes!";
                        } else {
                            return "Protocol error";
                        }

                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    return "Connection error, is the host offline?";
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                debugTxt.setText(s);
                if(s.contains("error")) {
                    toggle(true);
                } else {
                    toggle(true, Color.GREEN);
                }
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

    private BigInteger getPrivateExponent() {
        return new BigInteger(sp.getString(bad.toString() + "PRIV", "0"));
    }

    private BigInteger getPublicExponent() {
        return new BigInteger(sp.getString(bad.toString() + "PUBL", "0"));
    }

    private BigInteger getModulus() {
        return new BigInteger(sp.getString(bad.toString() + "MODU", "0"));
    }

    public boolean verify(BigInteger response, BigInteger challenge) throws Exception{
        return challenge.equals(response.modPow(getPublicExponent(), getModulus()));
    }

    public BigInteger sign(BigInteger challenge) throws Exception {
        return challenge.modPow(getPrivateExponent(), getModulus());
    }
}
