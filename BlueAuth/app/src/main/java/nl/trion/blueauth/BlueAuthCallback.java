package nl.trion.blueauth;

import android.content.Context;
import android.view.View;

/**
 * Created by irrlicht on 11/1/17.
 */

public interface BlueAuthCallback {
    void blueAuthError(String text);
    void blueAuthProgress(String progress);
    void blueAuthSucces(String text);
    Context getContext();
    void setRetryOnClick(View.OnClickListener listener);
}
