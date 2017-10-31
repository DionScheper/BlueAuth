package nl.trion.blueauth;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by irrlicht on 10/29/17.
 */

public class BlueAuthDevice {
    String username;
    String hostName;
    String hostMac;

    public BlueAuthDevice(String username, String hostName, String hostMac) {
        this.username = username;
        this.hostName = hostName;
        this.hostMac  = hostMac ;
    }

    @Override
    public String toString() {
        return this.username + ";" + this.hostName + ";" + this.hostMac;
    }

    public static BlueAuthDevice fromString(String s) {
        String[] list = s.split(";");
        if(list.length != 3) {
            throw new UnsupportedOperationException("This is not a BlueAuthDevice");
        }
        return new BlueAuthDevice(list[0], list[1], list[2]);
    }

    @Override
    public boolean equals(Object obj) {
        if(!obj.getClass().equals(this.getClass()))
            return false;
        BlueAuthDevice bad = (BlueAuthDevice) obj;
        return this.hostMac.equals(bad.hostMac) && this.username.equals(bad.username) && this.hostName.equals(bad.hostName);
    }

    public static boolean saveDevices(List<BlueAuthDevice> devices, Context context) {
        File devicesFile = new File(context.getFilesDir(), "devices");
        try {
            if(!devicesFile.exists()) {
                devicesFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(devicesFile);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (BlueAuthDevice bad : devices) {
                bw.write(bad.toString());
                bw.newLine();
            }
            bw.close();
            fos.close();
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static List<BlueAuthDevice> getDevices(Context context) {
        File devicesFile = new File(context.getFilesDir(), "devices");
        List<BlueAuthDevice> devicesList = new ArrayList<>();
        try {
            if(!devicesFile.exists()) {
                devicesFile.createNewFile();
            }
            FileInputStream fis = new FileInputStream(devicesFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            String line = reader.readLine();
            while (line != null) {
                try {
                    devicesList.add(BlueAuthDevice.fromString(line));
                } catch(UnsupportedOperationException e) {

                }
                line = reader.readLine();
            }

            reader.close();
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return new ArrayList<>();
        }
        return devicesList;
    }

    public List<BlueAuthDevice> getFromList(List<BlueAuthDevice> devices) {
        List<BlueAuthDevice> list = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).hostName.equals(this.hostName) && devices.get(i).hostMac.equals(this.hostMac)) {
                list.add(devices.get(i));
            }
        }
        return list;
    }
}
