package nl.trion.blueauth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "BlueAuth";
    private BlueAuth ba;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
