package com.rudyii.pdnss;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.TileService;

import com.rudyii.pdnss.services.NetworkMonitor;
import com.rudyii.pdnss.services.QuickTile;

public class PrivateDnsSwitcherApplication extends Application {
    private static PrivateDnsSwitcherApplication instance;

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        registerServices();
    }

    private void registerServices() {
        TileService.requestListeningState(this, new ComponentName(this, QuickTile.class));

        if (NetworkMonitor.isStopped()) {
            Intent service = new Intent(getApplicationContext(), NetworkMonitor.class);
            getApplicationContext().startService(service);
        }
    }
}
