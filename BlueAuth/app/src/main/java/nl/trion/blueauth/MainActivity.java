package nl.trion.blueauth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "BlueAuth";
    private BlueAuth ba;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView resultImageView = (ImageView) findViewById(R.id.resultImageView);
        resultImageView.setVisibility(View.GONE);
        ba = new BlueAuth(this, BlueAuthDevice.fromString(getIntent().getStringExtra("BlueAuthDevice")));

        // Check bluetooth support
        if(!ba.bluetoothSupported()) return;
        // Find the device (Async)
        ba.findDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ba.unregister();
    }
}
