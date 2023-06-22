package com.rudyii.pdnss.common;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.Application.getContext;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_OFF_STRING;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;

import android.provider.Settings;
import android.widget.Toast;

import com.rudyii.pdnss.R;

public class Utils {
    public static void updatePdnsModeSettings(int mode) {
        try {
            Settings.Global.putString(getContext().getContentResolver(), SETTINGS_PRIVATE_DNS_MODE,
                    getPrivateDnsModeAsString(mode));
        } catch (SecurityException e) {
            showWarning(getContext().getString(R.string.missing_permissions_warning));
        }
    }

    public static void updatePdnsUrl(String pDnsUrl) {
        Settings.Global.putString(getContext().getContentResolver(), SETTINGS_PRIVATE_DNS_SPECIFIER,
                pDnsUrl);
    }

    public static String getPrivateDnsModeAsString(int mode) {
        switch (mode) {
            case PRIVATE_DNS_MODE_OFF:
                return VALUE_PRIVATE_DNS_MODE_OFF_STRING;
            case PRIVATE_DNS_MODE_OPPORTUNISTIC:
                return VALUE_PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING;
            case PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                return VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;
            default:
                throw new IllegalArgumentException(getContext().getString(R.string.error_unknown_pdns_mode, mode));
        }
    }

    public static String getSettingsValue(String name) {
        String result = Settings.Global.getString(getContext().getContentResolver(), name);
        return result == null ? getContext().getString(R.string.none) : result;
    }

    public static String getPDNSState() {
        String pDNSState = getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
        if (pDNSState == null) {
            return getContext().getString(R.string.dns_state_unknown);
        } else {
            switch (pDNSState) {
                case VALUE_PRIVATE_DNS_MODE_OFF_STRING:
                    return getContext().getString(R.string.dns_state_off);
                case VALUE_PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING:
                    return getContext().getString(R.string.dns_state_auto);
                case VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING:
                    return getContext().getString(R.string.dns_state_on);
                default:
                    return getContext().getString(R.string.dns_state_unknown);
            }
        }
    }

    public static void showWarning(String warningMessage) {
        Toast.makeText(getContext(), warningMessage,
                Toast.LENGTH_LONG).show();
    }

}
