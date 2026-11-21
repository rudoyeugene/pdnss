package com.rudyii.pdnss.services;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.types.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.types.Constants.VALUE_PRIVATE_DNS_MODE_ON_STRING;

import android.os.SystemClock;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.types.PdnsModeType;

public class QuickTile extends TileService {

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
            getAppContext().getSecurityScoreUtil().disabled();
        } else {
            getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
            getAppContext().getSettingsUtils().updateLastPdnsState(PdnsModeType.ON);
            getAppContext().getSecurityScoreUtil().enabled();
        }
        Log.i(APP_NAME, "total enabled: " + getAppContext().getSecurityScoreUtil().getTotalEnabledTime());
        Log.i(APP_NAME, "score: " + getAppContext().getSecurityScoreUtil().getSecurityScore());
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        getAppContext().setQuickTile(this);
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        updateTile();
        getAppContext().getAppState().updatePdnsState(SystemClock.elapsedRealtime());
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        getAppContext().setQuickTile(this);
        updateTile();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        getAppContext().setQuickTile(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getAppContext().setQuickTile(null);
    }

    public void updateTile() {
        Tile qsTile = getQsTile();
        if (qsTile == null) {
            return;
        }

        String pDnsState = getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);

        qsTile.setLabel(getString(R.string.txt_dns_state_quicktile));
        qsTile.setSubtitle(getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));

        if (VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(pDnsState)) {
            qsTile.setState(STATE_ACTIVE);
        } else {
            qsTile.setState(STATE_INACTIVE);
        }
        qsTile.updateTile();
        getAppContext().getAppState().updatePdnsState(SystemClock.elapsedRealtime());
    }

    private PrivateDnsSwitcherApplication getAppContext() {
        return (PrivateDnsSwitcherApplication) getApplicationContext();
    }
}