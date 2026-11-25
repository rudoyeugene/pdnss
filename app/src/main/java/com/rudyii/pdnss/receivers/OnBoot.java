package com.rudyii.pdnss.receivers;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.BOOT_COMPLETED;
import static com.rudyii.pdnss.types.Constants.QUICKBOOT_POWERON;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.utils.PdnsPolicyScheduler;

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
            Log.i(APP_NAME, "Boot completed (" + action + "), scheduling Private DNS evaluation");
            Context appContext = context.getApplicationContext();
            if (appContext instanceof PrivateDnsSwitcherApplication app) {
                app.registerNetworkCallback();
                PdnsPolicyScheduler.scheduleEvaluation(app, "receiver:" + action, null, null);
            }
        }
    }
}