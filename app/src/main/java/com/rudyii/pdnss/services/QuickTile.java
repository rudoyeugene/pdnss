package com.rudyii.pdnss.services;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;

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
        if (!getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted()) {
            getAppContext().getNotificationsUtils()
                    .showWarning(getString(R.string.txt_missing_permissions_warning));
            return;
        }

        PdnsModeType currentState = getAppContext().getSettingsUtils().getPDNSState();
        if (currentState == PdnsModeType.ON) {
            getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
            getAppContext().getSettingsUtils().updateLastPdnsState(PdnsModeType.OFF);
            getAppContext().getSecurityScoreUtil().disabled();
        } else {
            getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
            getAppContext().getSettingsUtils().updateLastPdnsState(PdnsModeType.ON);
            getAppContext().getSecurityScoreUtil().enabled();
        }

        PdnsMonitorService.refreshNotification(this);
        Log.i(APP_NAME, "total enabled: " + getAppContext().getSecurityScoreUtil().getTotalEnabledTime());
        Log.i(APP_NAME, "score: " + getAppContext().getSecurityScoreUtil().getSecurityScore());
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        getAppContext().setQuickTile(this);
        if (!getAppContext().isMonitorServiceRunning()) {
            PdnsMonitorService.start(this);
        }
        getAppContext().getSettingsUtils().syncExternalStateChanges();
        updateTile();
    }

    @Override
    public void onStopListening() {
        getAppContext().setQuickTile(null);
        super.onStopListening();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        getAppContext().setQuickTile(this);
        if (!getAppContext().isMonitorServiceRunning()) {
            PdnsMonitorService.start(this);
        }
        updateTile();
    }

    @Override
    public void onTileRemoved() {
        getAppContext().setQuickTile(null);
        super.onTileRemoved();
    }

    @Override
    public void onDestroy() {
        getAppContext().setQuickTile(null);
        super.onDestroy();
    }

    public void updateTile() {
        Tile qsTile = getQsTile();
        if (qsTile == null) {
            return;
        }

        PdnsModeType pdnsState = getAppContext().getSettingsUtils().getPDNSState();
        String specifier = getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER);

        qsTile.setLabel(getString(R.string.txt_dns_state_quicktile));
        qsTile.setSubtitle(specifier);
        qsTile.setState(pdnsState == PdnsModeType.ON ? STATE_ACTIVE : STATE_INACTIVE);
        qsTile.updateTile();

        getAppContext().getAppState().updatePdnsState(SystemClock.elapsedRealtime());
    }

    private PrivateDnsSwitcherApplication getAppContext() {
        return (PrivateDnsSwitcherApplication) getApplicationContext();
    }
}