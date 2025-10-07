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
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;

public class ActivityShortcuts extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(true);

        Intent intent = getIntent();
        try {
            if (intent != null && intent.getAction() != null) {
                switch (intent.getAction()) {
                    case PDNSS_GOOGLE:
                        getAppContext().getSettingsUtils()
                                .updatePdnsModeSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                        getAppContext().refreshQuickTile();
                        getAppContext().getSecurityScoreUtil().disabled();
                        break;
                    case PDNSS_ON:
                        getAppContext().getSettingsUtils()
                                .updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                        getAppContext().refreshQuickTile();
                        getAppContext().getSecurityScoreUtil().enabled();
                        break;
                    case PDNSS_OFF:
                        getAppContext().getSettingsUtils()
                                .updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                        getAppContext().refreshQuickTile();
                        getAppContext().getSecurityScoreUtil().disabled();
                        break;
                }
                getAppContext().getAppState().updatePdnsState(SystemClock.currentThreadTimeMillis());
                getAppContext().getNotificationsUtils().showNotification(getAppContext().getString(R.string.txt_notification_state_title,
                                resolveTitle(intent.getAction())),
                        getAppContext().getString(
                                R.string.txt_notification_state_body_manual,
                                resolveState(intent.getAction())),
                        resolveStateBool(intent.getAction()));
            }
        } catch (Exception e) {
            getAppContext().getNotificationsUtils()
                    .showWarning(getString(R.string.txt_missing_permissions_warning));
        } finally {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);

            finish();
        }
    }

    @NonNull
    private String resolveTitle(String action) {
        return PDNSS_OFF.equals(action) ? getAppContext().getString(R.string.txt_notification_state_body_disabled) : getAppContext().getString(R.string.txt_notification_state_body_enabled);
    }

    private boolean resolveStateBool(String action) {
        return !PDNSS_OFF.equals(action);
    }

    private String resolveState(String action) {
        return switch (action) {
            case PDNSS_OFF -> getAppContext().getString(R.string.txt_dns_state_off);
            case PDNSS_GOOGLE -> getAppContext().getString(R.string.txt_dns_state_google);
            case PDNSS_ON -> getAppContext().getString(R.string.txt_dns_state_on);
            default -> "UNKNOWN";
        };
    }

    private PrivateDnsSwitcherApplication getAppContext() {
        return (PrivateDnsSwitcherApplication) getApplicationContext();
    }
}
