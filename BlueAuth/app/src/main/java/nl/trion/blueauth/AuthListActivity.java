package nl.trion.blueauth;

import android.Manifest;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

public class AuthListActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<BlueAuthDevice> devicesAdapter;
    private ListView devicesListView;
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;
    private static final int REQUEST_COARSE_LOCATION = 3;
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    private static final int AUTHENTICATION_DURATION_SECONDS = 30;
    private KeyguardManager mKeyguardManager;
    private long LAST_TIME_LOGIN = 0;

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_COARSE_LOCATION);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.auth_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.navigation_add_device:
                addDeviceDialog();
                return true;
            case R.id.navigation_publish_keys:
                savePublicKeys(BlueAuthDevice.getDevices(this));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_list);
        devicesListView = (ListView) findViewById(R.id.devicesListView);
        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, BlueAuthDevice.getDevices(this));
        devicesListView.setAdapter(devicesAdapter);
        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent connectIntent = new Intent(view.getContext(), AuthenticationActivity.class);
                connectIntent.putExtra("BlueAuthDevice", adapterView.getItemAtPosition(i).toString());
                startActivity(connectIntent);
            }
        });

        devicesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                editDeviceDialog((BlueAuthDevice) adapterView.getItemAtPosition(i));
                return true;
            }
        });

        bluetoothEnabled();
        requestPermission();
    }

    @Override
    protected void onResume() {
        if((new Date()).getTime() - LAST_TIME_LOGIN > AUTHENTICATION_DURATION_SECONDS * 1000) {
            showAuthenticationScreen();
        }
        super.onResume();
    }

    private void bluetoothEnabled() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }
    }

    public void addDeviceDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_device_dialog);
        Button addBtn = dialog.findViewById(R.id.addBtn);
        final EditText username = dialog.findViewById(R.id.username);
        final EditText hostmac = dialog.findViewById(R.id.hostmac);

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String unString = username.getText().toString();
                String hmString = hostmac.getText().toString();
                if(validInput(hmString, unString)) {
                    BlueAuthDevice bad = new BlueAuthDevice(hmString, unString);
                    List<BlueAuthDevice> devices = BlueAuthDevice.getDevices(view.getContext());
                    devices.add(bad);
                    BlueAuthDevice.saveDevices(devices, view.getContext());
                    devicesAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_list_item_1, BlueAuthDevice.getDevices(view.getContext()));
                    devicesListView.setAdapter(devicesAdapter);
                    generateKeyPair(bad);
                    Toast.makeText(view.getContext(), "Added device", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    public void editDeviceDialog(final BlueAuthDevice bad) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.edit_device_dialog);
        Button genKeys = dialog.findViewById(R.id.genKeyBtn);
        Button submitBtn = dialog.findViewById(R.id.submitBtn);
        Button deleteBtn = dialog.findViewById(R.id.deleteBtn);
        final EditText username = dialog.findViewById(R.id.username);
        final EditText hostmac = dialog.findViewById(R.id.hostmac);
        username.setText(bad.username);
        hostmac.setText(bad.hostMac);

        genKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String unString = username.getText().toString();
                final String hmString = hostmac.getText().toString();
                if(validInput(hmString, unString)) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
                    alertDialogBuilder.setTitle("Create RSA key pair")
                            .setMessage("Are you sure you want to create new keys? You have to add the public exponents to the end point.")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    generateKeyPair(new BlueAuthDevice(hmString, unString));
                                    Toast.makeText(getBaseContext(), "Generated Keys", Toast.LENGTH_SHORT).show();
                                }
                            }).show();
                }
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
                alertDialogBuilder.setTitle("Delete device")
                        .setMessage("Are you sure you want to delete this device? The private key will be lost.")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(getBaseContext(), "Deleting device", Toast.LENGTH_SHORT).show();
                                List<BlueAuthDevice> devices = BlueAuthDevice.getDevices(getBaseContext());
                                devices.remove(bad);
                                BlueAuthDevice.saveDevices(devices, getBaseContext());
                                devicesAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1, BlueAuthDevice.getDevices(getBaseContext()));
                                devicesListView.setAdapter(devicesAdapter);
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), "Saved device", Toast.LENGTH_SHORT).show();
                String unString = username.getText().toString();
                String hmString = hostmac.getText().toString();
                if(validInput(hmString, unString)) {
                    BlueAuthDevice newbad = new BlueAuthDevice(hmString, unString);
                    List<BlueAuthDevice> devices = BlueAuthDevice.getDevices(view.getContext());
                    devices.remove(bad);
                    devices.add(newbad);
                    BlueAuthDevice.saveDevices(devices, view.getContext());

                    devicesAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_list_item_1, BlueAuthDevice.getDevices(view.getContext()));
                    devicesListView.setAdapter(devicesAdapter);
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    private boolean validInput(String username, String hostmac) {
        return username.indexOf(';') == -1 && hostmac.indexOf(';') == -1;
    }

    private boolean savePublicKeys(List<BlueAuthDevice> devices) {
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File publicKeysFile = new File(documents, "devices.txt");
            try {
                if(!documents.exists()) {
                    documents.mkdirs();
                }
                if(!publicKeysFile.exists()) {
                    publicKeysFile.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(publicKeysFile);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                for (BlueAuthDevice bad : devices) {
                    bw.write(bad.toString() + ": " + sp.getString(bad.toString() + "PUBL", ""));
                    bw.newLine();
                    bw.write(bad.toString() + ": " + sp.getString(bad.toString() + "MODU", ""));
                    bw.newLine();
                }
                bw.close();
                fos.close();
                Toast.makeText(getBaseContext(), "Public keys published", Toast.LENGTH_SHORT).show();
                MediaScannerConnection.scanFile(this,
                        new String[] { publicKeysFile.toString() }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {}
                        });
            } catch(IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        for (BlueAuthDevice bad : devices) {
            Log.d("KEY", bad.toString() + ";;;" + sp.getString(bad.toString() + "PUBL", ""));
            Log.d("KEY", bad.toString() + ";;;" + sp.getString(bad.toString() + "MODU", ""));
        }
        return true;
    }

    private void generateKeyPair(BlueAuthDevice bad) {
        final BlueAuthDevice thrBad = bad;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA);
                    kpg.initialize(2048);
                    KeyPair kp = kpg.generateKeyPair();
                    RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
                    RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
                    spe = sp.edit();
                    spe.putString(thrBad.toString() + "PRIV", privateKey.getPrivateExponent().toString());
                    spe.putString(thrBad.toString() + "PUBL", publicKey.getPublicExponent().toString());
                    spe.putString(thrBad.toString() + "MODU", publicKey.getModulus().toString());
                    spe.commit();
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(getBaseContext(), "Keypair generated", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                // Challenge completed, do not ask for the next 30 seconds
                LAST_TIME_LOGIN = (new Date()).getTime();
                Log.d(AuthenticationActivity.TAG, "User Authenticated");
            } else {
                // The user canceled or didn’t complete the lock screen operation. Go to error/cancellation flow.
                finish();
            }
        }
    }

    private void showAuthenticationScreen() {
/*        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent("BlueAuth Confirm Identity", "BlueAuth protects your identity and keyring. Your key ring will only be unlocked if you have acces to a method to unlock the phone");
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        }*/
    }
}
