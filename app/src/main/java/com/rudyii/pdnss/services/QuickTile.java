package com.rudyii.pdnss.services;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static com.rudyii.pdnss.types.Constants.PDNS_STATE_CHANGED;
import static com.rudyii.pdnss.types.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.types.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.types.Constants.VALUE_PRIVATE_DNS_MODE_ON_STRING;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.types.PdnsModeType;

public class QuickTile extends TileService {
    private static Tile tile;

    @Override
    public void onCreate() {
        super.onCreate();
        getAppContext().setQuickTile(this);
    }

    @Override
    public void onClick() {
        super.onClick();
        String pDNSState = getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);

        if (VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(pDNSState)) {
            getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
            getAppContext().getSettingsUtils().updateLastPdnsState(PdnsModeType.OFF);
        } else {
            getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
            getAppContext().getSettingsUtils().updateLastPdnsState(PdnsModeType.ON);
        }
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        updateTile();
        sendBroadcast(new Intent(PDNS_STATE_CHANGED));
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        getAppContext().setQuickTile(null);
        tile = null;
    }

    public void updateTile() {
        String pDnsState = getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
        tile = getQsTile();

        tile.setLabel(getString(R.string.txt_dns_state_quicktile));
        tile.setSubtitle(getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));

        if (VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(pDnsState)) {
            tile.setState(STATE_ACTIVE);
        } else {
            tile.setState(STATE_INACTIVE);
        }
        tile.updateTile();
    }

    private PrivateDnsSwitcherApplication getAppContext() {
        return (PrivateDnsSwitcherApplication) getApplicationContext();
    }
}
