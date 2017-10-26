package nl.trion.blueauth;

import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.widget.TextView;

import java.util.Enumeration;

/**
 * Created by irrlicht on 10/26/17.
 */

public class BlueAuth extends AsyncTask<>{

    private enum state { STATE_OPEN, STATE_CONNECTING, STATE_CONNECTED, STATE_REC_CHALLENGE, STATE_SEN_RESPONSE,
                            STATE_REC_RESULT, STATE_CLOSING, STATE_CLOSED }

    public BlueAuth() {
        
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        return null;
    }
}
