package com.rudyii.pdnss;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.Constants.PDNSS_AUTO;
import static com.rudyii.pdnss.Constants.PDNSS_OFF;
import static com.rudyii.pdnss.Constants.PDNSS_ON;
import static com.rudyii.pdnss.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.Constants.VALUE_PRIVATE_DNS_MODE_OFF_STRING;
import static com.rudyii.pdnss.Constants.VALUE_PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING;
import static com.rudyii.pdnss.Constants.VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PrivateDNSSwitcherAction extends AppCompatActivity {


    public String getPrivateDnsModeAsString(int mode) {
        switch (mode) {
            case PRIVATE_DNS_MODE_OFF:
                return VALUE_PRIVATE_DNS_MODE_OFF_STRING;
            case PRIVATE_DNS_MODE_OPPORTUNISTIC:
                return VALUE_PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING;
            case PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                return VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;
            default:
                throw new IllegalArgumentException(getString(R.string.error_unknown_pdns_mode, mode));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case PDNSS_AUTO:
                    try {
                        updateSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                    } catch (Exception e) {
                        showWarning();
                    }
                    break;
                case PDNSS_ON:
                    try {
                        updateSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                    } catch (Exception e) {
                        showWarning();
                    }
                    break;
                case PDNSS_OFF:
                    try {
                        updateSettings(PRIVATE_DNS_MODE_OFF);
                    } catch (Exception e) {
                        showWarning();
                    }
                    break;
                default:
                    this.finish();
            }
        }
        this.finish();
    }

    private void updateSettings(int privateDnsModeOpportunistic) {
        Settings.Global.putString(getApplicationContext().getContentResolver(), SETTINGS_PRIVATE_DNS_MODE,
                getPrivateDnsModeAsString(privateDnsModeOpportunistic));
    }

    private void showWarning() {
        Toast.makeText(getApplicationContext(), getString(R.string.missing_permissions_warning),
                Toast.LENGTH_LONG).show();
    }
}
