package com.rudyii.pdnss;

import static com.rudyii.pdnss.common.Utils.updatePdnsSettingsOnNetworkChange;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.TileService;

import com.rudyii.pdnss.services.NetworkMonitor;
import com.rudyii.pdnss.services.QuickTile;

public class PrivateDnsSwitcherApplication extends Application {
    private static PrivateDnsSwitcherApplication instance;
    private static BroadcastReceiver broadcastReceiver;

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    public static BroadcastReceiver getNetworkChangeBroadcastReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updatePdnsSettingsOnNetworkChange();
                }
            };
        }

        return broadcastReceiver;
    }
    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        registerServices();
    }

    private void registerServices() {
        TileService.requestListeningState(this, new ComponentName(this, QuickTile.class));

        Intent service = new Intent(this, NetworkMonitor.class);
        if (NetworkMonitor.isStopped()) {
            startForegroundService(service);
        }
    }
}
