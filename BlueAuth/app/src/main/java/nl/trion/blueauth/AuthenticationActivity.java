package nl.trion.blueauth;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AuthenticationActivity extends AppCompatActivity implements BlueAuthCallback{
    public static final String TAG = "BlueAuth";
    private TextView debugTxt;
    private ProgressBar progressBar;
    private ImageView statusImageView;

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
        // Finish the activity if the App has been left
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.debugTxt = (TextView) findViewById(R.id.debugTxt);
        this.progressBar = (ProgressBar) findViewById(R.id.progressBar);
        this.statusImageView = (ImageView) findViewById(R.id.statusImageView);

        BlueAuth ba = new BlueAuth(this, BlueAuthDevice.fromString(getIntent().getStringExtra("BlueAuthDevice")));

        // Check bluetooth support
        if(ba.bluetoothSupported()) {
            ba.connect();
        }
    }

    private void toggle(boolean done, int color, int drawable) {
        statusImageView.setBackgroundColor(color);
        if(done) {
            progressBar.setVisibility(View.GONE);
            statusImageView.setVisibility(View.VISIBLE);
            statusImageView.setImageDrawable(getDrawable(drawable));
        } else {
            progressBar.setVisibility(View.VISIBLE);
            statusImageView.setVisibility(View.GONE);
        }
    }

    @Override
    public void blueAuthError(String text) {
        toggle(true, Color.RED, android.R.drawable.stat_sys_warning);
        debugTxt.setText(text);
    }

    @Override
    public void blueAuthProgress(String progress) {
        toggle(false, Color.LTGRAY, android.R.drawable.stat_sys_warning);
        debugTxt.setText(progress);
    }

    @Override
    public void blueAuthSucces(String text) {
        toggle(true, Color.GREEN, android.R.drawable.stat_sys_data_bluetooth);
        debugTxt.setText(text);
    }

    @Override
    public Context getContext() {
        return getBaseContext();
    }

    @Override
    public void setRetryOnClick(View.OnClickListener listener) {
        statusImageView.setOnClickListener(listener);
    }
}
