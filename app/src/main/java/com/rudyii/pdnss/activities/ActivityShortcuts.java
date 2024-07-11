package com.rudyii.pdnss.activities;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.common.Constants.PDNSS_GOOGLE;
import static com.rudyii.pdnss.common.Constants.PDNSS_OFF;
import static com.rudyii.pdnss.common.Constants.PDNSS_ON;
import static com.rudyii.pdnss.common.Utils.showWarning;
import static com.rudyii.pdnss.common.Utils.updatePdnsModeSettings;
import static com.rudyii.pdnss.services.QuickTileService.refreshQsTile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.rudyii.pdnss.R;

public class ActivityShortcuts extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setVisible(false);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        setFinishOnTouchOutside(true);

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case PDNSS_GOOGLE:
                    try {
                        updatePdnsModeSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                        refreshQsTile();
                    } catch (Exception e) {
                        showWarning(getString(R.string.txt_missing_permissions_warning));
                    }
                    break;
                case PDNSS_ON:
                    try {
                        updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                        refreshQsTile();
                    } catch (Exception e) {
                        showWarning(getString(R.string.txt_missing_permissions_warning));
                    }
                    break;
                case PDNSS_OFF:
                    try {
                        updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                        refreshQsTile();
                    } catch (Exception e) {
                        showWarning(getString(R.string.txt_missing_permissions_warning));
                    }
                    break;
            }
        }
        this.finish();
    }
}
