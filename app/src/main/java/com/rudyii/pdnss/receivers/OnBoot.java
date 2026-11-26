package com.rudyii.pdnss.receivers;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.BOOT_COMPLETED;
import static com.rudyii.pdnss.types.Constants.QUICKBOOT_POWERON;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.services.PdnsMonitorService;

public class OnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        String action = intent.getAction();
        if (BOOT_COMPLETED.equals(action)
                || QUICKBOOT_POWERON.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.i(APP_NAME, "System event (" + action + "), starting monitor service and evaluating Private DNS");
            Context appContext = context.getApplicationContext();
            if (appContext instanceof PrivateDnsSwitcherApplication app) {
                PdnsMonitorService.start(app);
                app.registerNetworkCallback();
                app.getSettingsUtils().updatePdnsSettingsOnNetworkChange(null);
            }
        }
    }
}