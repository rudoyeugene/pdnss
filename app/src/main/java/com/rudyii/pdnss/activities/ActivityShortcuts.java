package com.rudyii.pdnss.activities;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.types.Constants.PDNSS_GOOGLE;
import static com.rudyii.pdnss.types.Constants.PDNSS_OFF;
import static com.rudyii.pdnss.types.Constants.PDNSS_ON;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;

public class ActivityShortcuts extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(true);
        Intent intent = getIntent();

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case PDNSS_GOOGLE:
                    try {
                        getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                        getAppContext().refreshQuickTile();
                        getAppContext().getSecurityScoreUtil().disabled();
                    } catch (Exception e) {
                        getAppContext().getNotificationsUtils().showWarning(getString(R.string.txt_missing_permissions_warning));
                    }
                    break;
                case PDNSS_ON:
                    try {
                        getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                        getAppContext().refreshQuickTile();
                        getAppContext().getSecurityScoreUtil().enabled();
                    } catch (Exception e) {
                        getAppContext().getNotificationsUtils().showWarning(getString(R.string.txt_missing_permissions_warning));
                    }
                    break;
                case PDNSS_OFF:
                    try {
                        getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                        getAppContext().refreshQuickTile();
                        getAppContext().getSecurityScoreUtil().disabled();
                    } catch (Exception e) {
                        getAppContext().getNotificationsUtils().showWarning(getString(R.string.txt_missing_permissions_warning));
                    }
                    break;
            }
        }
        finish();
    }

    private PrivateDnsSwitcherApplication getAppContext() {
        return (PrivateDnsSwitcherApplication) getApplicationContext();
    }
}
