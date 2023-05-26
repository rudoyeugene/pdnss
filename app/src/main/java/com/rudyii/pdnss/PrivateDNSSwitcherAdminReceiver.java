package com.rudyii.pdnss;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class PrivateDNSSwitcherAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    private static final String TAG = "PrivateDNSSwitcher";

    /**
     * Called when this application is approved to be a device administrator.
     */
    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        Toast.makeText(context, "Device admin is enabled",
                Toast.LENGTH_LONG).show();
        Log.d(TAG, "onEnabled");
    }

    /**
     * Called when this application is no longer the device administrator.
     */
    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        super.onDisabled(context, intent);
        Toast.makeText(context, "Device admin is disabled",
                Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDisabled");
    }
}
