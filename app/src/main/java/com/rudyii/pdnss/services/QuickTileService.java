package com.rudyii.pdnss.services;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static com.rudyii.pdnss.common.Constants.PDNS_STATE_CHANGED;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;
import static com.rudyii.pdnss.common.Utils.getSettingsValue;
import static com.rudyii.pdnss.common.Utils.updateLastPdnsState;
import static com.rudyii.pdnss.common.Utils.updatePdnsModeSettings;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.rudyii.pdnss.R;

public class QuickTileService extends TileService {
    private static Tile tile;

    public static void refreshQsTile() {
        if (tile != null) {
            String PDNSState = getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);

            if (PDNSState.equals(VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING)) {
                tile.setState(STATE_ACTIVE);
            } else {
                tile.setState(STATE_INACTIVE);
            }
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        String pDNSState = getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
        Tile tile = getQsTile();

        if (VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING.equals(pDNSState)) {
            updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
            updateLastPdnsState(false);
            tile.setState(STATE_INACTIVE);
        } else {
            updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
            updateLastPdnsState(true);
            tile.setState(STATE_ACTIVE);
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
        tile = null;
    }

    private void updateTile() {
        String pDnsState = getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
        tile = getQsTile();
        tile.setSubtitle(getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));

        if (VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING.equals(pDnsState)) {
            tile.setLabel(getString(R.string.dns_state_quicktile, getString(R.string.dns_state_on)));
            tile.setState(STATE_ACTIVE);
        } else {
            tile.setLabel(getString(R.string.dns_state_quicktile, getString(R.string.dns_state_off)));
            tile.setState(STATE_INACTIVE);
        }
        tile.updateTile();
    }
}
