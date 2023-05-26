package com.rudyii.pdnss;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
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


    public static String getPrivateDnsModeAsString(int mode) {
        switch (mode) {
            case PRIVATE_DNS_MODE_OFF:
                return VALUE_PRIVATE_DNS_MODE_OFF_STRING;
            case PRIVATE_DNS_MODE_OPPORTUNISTIC:
                return VALUE_PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING;
            case PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                return VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;
            default:
                throw new IllegalArgumentException("Invalid private dns mode: " + mode);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "com.rudyii.pdnss.AUTO":
                    try {
                        updateSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                    } catch (Exception e) {
                        showWarning();
                    }
                    break;
                case "com.rudyii.pdnss.ENABLE":
                    try {
                        updateSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                    } catch (Exception e) {
                        showWarning();
                    }
                    break;
                case "com.rudyii.pdnss.DISABLE":
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
        Toast.makeText(getApplicationContext(), "Missing permissions, please use ADB to grant permissions",
                Toast.LENGTH_LONG).show();
    }
}
