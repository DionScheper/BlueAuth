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
import android.os.Handler;
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
import java.lang.reflect.Method;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    TextView debugTxt;
    Button authBtn;
    int REQUEST_COARSE_LOCATION = 3;

    public static final String TAG = "BlueAuth";
    public static final String COMPUTER_NAME = "HK01";
    public static final String COMPUTER_BT_MAC = "5C:51:4F:58:CB:D5";

    BlueAuth ba;

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        debugTxt = (TextView) findViewById(R.id.debugTxt);
        authBtn = (Button) findViewById(R.id.authBtn);
        ba = new BlueAuth(this, debugTxt, authBtn);

        authBtn.setEnabled(false);
        // Request Permissions
        requestPermission();
        // Check bluetooth support
        if(!ba.bluetoothSupported()) return;
        // Set authenticate button
        authBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                ba.connect();
            }
        });
        // Find the device (Async)
        ba.findDevice();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ba.unregister();
    }
}
