package com.rudyii.pdnss;

import static com.rudyii.pdnss.common.Utils.updatePdnsSettingsOnNetworkChange;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.service.quicksettings.TileService;

import androidx.annotation.NonNull;

import com.rudyii.pdnss.services.QuickTileService;

public class PrivateDnsSwitcherApplication extends Application {
    private static PrivateDnsSwitcherApplication instance;

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        registerReceivers();
        registerQuickTile();
    }

    private void registerQuickTile() {
        TileService.requestListeningState(this, new ComponentName(this, QuickTileService.class));
    }

    private void registerReceivers() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                updatePdnsSettingsOnNetworkChange(network);
            }
        });
    }
}
