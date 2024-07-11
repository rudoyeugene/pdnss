package com.rudyii.pdnss.common;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.PrivateDnsSwitcherApplication.getContext;
import static com.rudyii.pdnss.common.Constants.PDNS_STATE_CHANGED;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_GOOGLE_STRING;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_OFF_STRING;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_ON_STRING;
import static com.rudyii.pdnss.common.PdnsModeType.OFF;
import static com.rudyii.pdnss.common.PdnsModeType.OFF_WHILE_TRUSTED_WIFI;
import static com.rudyii.pdnss.common.PdnsModeType.OFF_WHILE_VPN;
import static com.rudyii.pdnss.common.PdnsModeType.ON;
import static com.rudyii.pdnss.services.QuickTileService.refreshQsTile;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.rudyii.pdnss.R;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Utils {

    public static PdnsModeType getLastKnownState() {
        return PdnsModeType.valueOf(getSharedPrefs().getString(getContext().getString(R.string.settings_name_last_pdns_state), OFF.name()));
    }

    public static void updatePdnsModeSettings(int mode) {
        try {
            Settings.Global.putString(getContext().getContentResolver(), SETTINGS_PRIVATE_DNS_MODE,
                    getPrivateDnsModeAsString(mode));
        } catch (SecurityException e) {
            showWarning(getContext().getString(R.string.txt_missing_permissions_warning));
        }
    }

    public static void updateLastPdnsState(PdnsModeType lastState) {
        SharedPreferences.Editor editor = getSharedPrefsEditor();
        editor.putString(getContext().getString(R.string.settings_name_last_pdns_state), lastState.name());
        editor.apply();
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
                return VALUE_PRIVATE_DNS_MODE_GOOGLE_STRING;
            case PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                return VALUE_PRIVATE_DNS_MODE_ON_STRING;
            default:
                throw new IllegalArgumentException(getContext().getString(R.string.txt_error_unknown_pdns_mode, mode));
        }
    }

    public static String getSettingsValue(String name) {
        String result = Settings.Global.getString(getContext().getContentResolver(), name);
        return result == null ? getContext().getString(R.string.txt_none) : result;
    }

    public static String getPDNSState() {
        String pDNSState = getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
        if (pDNSState == null) {
            return getContext().getString(R.string.txt_dns_state_unknown);
        } else {
            switch (pDNSState) {
                case VALUE_PRIVATE_DNS_MODE_OFF_STRING:
                    return getContext().getString(R.string.txt_dns_state_off);
                case VALUE_PRIVATE_DNS_MODE_GOOGLE_STRING:
                    return getContext().getString(R.string.txt_dns_state_google);
                case VALUE_PRIVATE_DNS_MODE_ON_STRING:
                    return getContext().getString(R.string.txt_dns_state_on);
                default:
                    return getContext().getString(R.string.txt_dns_state_unknown);
            }
        }
    }

    public static void showWarning(String warningMessage) {
        Toast.makeText(getContext(), warningMessage,
                Toast.LENGTH_LONG).show();
    }

    public static void updatePdnsSettingsOnNetworkChange(Network network) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities != null) {
            boolean isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            boolean isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);

            if (isVpn) {
                boolean pDnsStateOn = VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                boolean disableWhileVnp = getSharedPrefs().getBoolean(getContext().getString(R.string.settings_name_disable_while_vpn), false);
                if (disableWhileVnp && pDnsStateOn) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                    updateLastPdnsState(OFF_WHILE_VPN);
                    refreshQsTile();
                }
            } else if (isWiFi && isAllNeededLocationPermissionsGranted()) {
                String ssidName = getWifiSsidName();
                if (getWifiTrusted(ssidName) && getSharedPrefs().getBoolean(getContext().getString(R.string.settings_name_trust_wifi), false)) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                    updateLastPdnsState(OFF_WHILE_TRUSTED_WIFI);
                    refreshQsTile();
                }
            } else {
                switch (getLastKnownState()) {
                    case OFF_WHILE_VPN:
                    case OFF_WHILE_TRUSTED_WIFI:
                        updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                        updateLastPdnsState(ON);
                        refreshQsTile();

                }
            }
        }
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(PDNS_STATE_CHANGED));
    }

    public static String getWifiSsidName() {
        WifiManager mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        if (isAllNeededLocationPermissionsGranted() && mWifiManager != null) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            return info.getSSID();
        } else {
            return "unsupported";
        }
    }

    public static int getWifiSsidColorCode(String ssidName) {
        Set<String> trustedSsids = getSharedPrefs().getStringSet(getContext().getString(R.string.settings_name_trust_wifi_ssid_set), Collections.emptySet());

        return trustedSsids.contains(ssidName) ? Color.GREEN : Color.RED;
    }

    public static boolean getWifiTrusted(String ssidName) {
        if (isAllNeededLocationPermissionsGranted()) {
            Set<String> trustedSsids = getSharedPrefs().getStringSet(getContext().getString(R.string.settings_name_trust_wifi_ssid_set), Collections.emptySet());

            return trustedSsids.contains(ssidName);
        } else {
            return false;
        }
    }

    public static int trustUntrustSsidName(String ssidName) {
        Set<String> trustedSsids = getSharedPrefs().getStringSet(getContext().getString(R.string.settings_name_trust_wifi_ssid_set), Collections.emptySet());
        Set<String> trustedSsidsLocalCopy = new HashSet<>(trustedSsids);

        int code;
        if (trustedSsids.contains(ssidName)) {
            trustedSsidsLocalCopy.remove(ssidName);
            code = Color.RED;
        } else {
            trustedSsidsLocalCopy.add(ssidName);
            code = Color.GREEN;
        }

        SharedPreferences.Editor editor = getSharedPrefsEditor();
        editor.putStringSet(getContext().getString(R.string.settings_name_trust_wifi_ssid_set), trustedSsidsLocalCopy);
        editor.apply();

        return code;
    }

    public static SharedPreferences getSharedPrefs() {
        return getContext().getSharedPreferences(
                getContext().getString(R.string.settings_name), Context.MODE_PRIVATE);
    }

    public static SharedPreferences.Editor getSharedPrefsEditor() {
        return getContext().getSharedPreferences(
                getContext().getString(R.string.settings_name), Context.MODE_PRIVATE).edit();
    }

    public static boolean isLocationPermissionsGranted() {
        return getSharedPrefs().getBoolean(getContext().getString(R.string.settings_location_permissions_granted), false);
    }

    public static boolean isAllNeededLocationPermissionsGranted() {
        return isLocationPermissionsGranted()
                && PackageManager.PERMISSION_GRANTED == getContext().checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                && PackageManager.PERMISSION_GRANTED == getContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                && PackageManager.PERMISSION_GRANTED == getContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }
}
