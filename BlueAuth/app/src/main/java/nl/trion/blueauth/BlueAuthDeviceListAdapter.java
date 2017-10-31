package nl.trion.blueauth;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * Created by irrlicht on 10/30/17.
 */

public class BlueAuthDeviceListAdapter extends BaseAdapter {
    private List<BlueAuthDevice> data;

    public BlueAuthDeviceListAdapter(List<BlueAuthDevice> data) {
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return null;
    }
}
