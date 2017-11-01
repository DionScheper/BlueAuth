package nl.trion.blueauth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;

/**
 * Created by irrlicht on 10/26/17.
 */

public class BlueAuth{
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice machineBTDevice;
    private BlueAuthCallback callback;
    private static final int CHALLENGE_SIZE = 128;
    private BlueAuthDevice bad;
    private SharedPreferences sp;

    public BlueAuth(BlueAuthCallback callback, BlueAuthDevice bad) {
        this.bad = bad;
        this.sp = PreferenceManager.getDefaultSharedPreferences(callback.getContext());
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.callback = callback;
    }

    public boolean bluetoothSupported() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            callback.blueAuthError("You do not support bluetooth or it is disabled");
            return false;
        }
        callback.blueAuthProgress("You support bluetooth");
        return true;
    }

    public void connect() {
        new AsyncTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    machineBTDevice = mBluetoothAdapter.getRemoteDevice(bad.hostMac);
                    Method m = machineBTDevice.getClass().getMethod("createInsecureRfcommSocket", int.class);
                    BluetoothSocket socket = (BluetoothSocket) m.invoke(machineBTDevice, 1);
                    publishProgress("Connecting to host...");
                    socket.connect();
                    if(socket.isConnected()) {
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
                        byte[] challenge = new byte[CHALLENGE_SIZE];
                        int nr_of_bytes = is.read(challenge);
                        BigInteger resul = sign(new BigInteger(1, challenge));
                        Log.d(AuthenticationActivity.TAG, "Received challenge: (" + String.valueOf(nr_of_bytes) + ")" + new BigInteger(1, challenge).toString());
                        // Send response
                        Log.d(AuthenticationActivity.TAG, "Sending response: (" + String.valueOf(resul.toByteArray().length) + ")" + resul.toString());
                        os.write(resul.toByteArray());

                        // Receive result for last UI update
                        byte[] result = new byte[1];
                        nr_of_bytes = is.read(result);
                        if(nr_of_bytes != 1) {
                            socket.close();
                            return "Result error, protocol is not followed.";
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
                if(s.contains("error")) {
                    callback.blueAuthError(s);
                } else {
                    callback.blueAuthSucces(s);
                }
                callback.setRetryOnClick(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.blueAuthProgress("Retry");
                        connect();
                    }
                });
            }

            @Override
            protected void onProgressUpdate(String... values) {
                String str = values[0];
                if(values.length > 0) {
                    str = values[values.length - 1];
                }
                Log.d(AuthenticationActivity.TAG, "onProgressUpdate: " + str);
                callback.blueAuthProgress(str);
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
