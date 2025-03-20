package com.rudyii.pdnss.common;


import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.types.Constants.PDNS_STATE_CHANGED;
import static com.rudyii.pdnss.types.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.types.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.types.Constants.VALUE_PRIVATE_DNS_MODE_GOOGLE_STRING;
import static com.rudyii.pdnss.types.Constants.VALUE_PRIVATE_DNS_MODE_OFF_STRING;
import static com.rudyii.pdnss.types.Constants.VALUE_PRIVATE_DNS_MODE_ON_STRING;
import static com.rudyii.pdnss.types.PdnsModeType.GOOGLE;
import static com.rudyii.pdnss.types.PdnsModeType.OFF;
import static com.rudyii.pdnss.types.PdnsModeType.OFF_WHILE_TRUSTED_WIFI;
import static com.rudyii.pdnss.types.PdnsModeType.OFF_WHILE_VPN;
import static com.rudyii.pdnss.types.PdnsModeType.ON;
import static com.rudyii.pdnss.types.PdnsModeType.ON_WHILE_CELLULAR;
import static com.rudyii.pdnss.types.PdnsModeType.UNKNOWN;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.types.ConnectionType;
import com.rudyii.pdnss.types.PdnsModeType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SettingsUtil {
    private final PrivateDnsSwitcherApplication context;

    public SettingsUtil(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    public void updatePdnsSettingsOnNetworkChange(Network network) {
        ConnectivityManager connectivityManager = context.getConnectivityManager();
        if (network == null) {
            network = connectivityManager.getActiveNetwork();
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

        if (capabilities != null) {
            boolean isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            boolean isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            boolean isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

            if (isVpn) {
                boolean pDnsStateOn = VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(context.getSettingsUtil().getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                boolean disableWhileVnp = context.getSettingsUtil().getSharedPrefs().getBoolean(context.getString(R.string.settings_name_disable_while_vpn), false);
                if (disableWhileVnp && pDnsStateOn) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                    updateLastPdnsState(OFF_WHILE_VPN);
                    context.refreshQuickTile();
                    context.getNotificationsUtils().showNotification(context.getString(R.string.txt_notification_state_title,
                                    context.getString(R.string.txt_notification_state_body_disabled)),
                            context.getString(
                                    R.string.txt_notification_state_body,
                                    context.getString(R.string.txt_notification_state_body_disabled),
                                    context.getString(R.string.txt_notification_state_body_on_vpn)),
                            false);
                }
            } else if (isWiFi && context.getPermissionsUtil().isAllNeededLocationPermissionsGranted()) {
                String apName = getWifiApName();
                if (context.getSettingsUtil().itTrustedWiFiAp(apName) && trustedWiFiModeOn()) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                    updateLastPdnsState(OFF_WHILE_TRUSTED_WIFI);
                    context.refreshQuickTile();
                    context.getNotificationsUtils().showNotification(context.getString(R.string.txt_notification_state_title,
                                    context.getString(R.string.txt_notification_state_body_disabled)),
                            context.getString(
                                    R.string.txt_notification_state_body_with_ap_name,
                                    context.getString(R.string.txt_notification_state_body_disabled),
                                    context.getString(R.string.txt_notification_state_body_on_trusted_ap),
                                    apName),
                            false);
                } else if (!itTrustedWiFiAp(apName) && trustedWiFiModeOn()) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                    updateLastPdnsState(ON);
                    context.refreshQuickTile();
                    context.getNotificationsUtils().showNotification(context.getString(R.string.txt_notification_state_title,
                                    context.getString(R.string.txt_notification_state_body_enabled)),
                            context.getString(
                                    R.string.txt_notification_state_body_with_ap_name,
                                    context.getString(R.string.txt_notification_state_body_enabled),
                                    context.getString(R.string.txt_notification_state_body_on_untrusted_ap),
                                    apName),
                            true);
                }
            } else if (isCellular) {
                boolean pDnsStateOff = VALUE_PRIVATE_DNS_MODE_OFF_STRING.equals(getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                boolean enableWhileCellular = getSharedPrefs().getBoolean(context.getString(R.string.settings_name_enable_while_cellular),
                        false);
                if (enableWhileCellular && pDnsStateOff) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                    updateLastPdnsState(ON_WHILE_CELLULAR);
                    context.refreshQuickTile();
                    context.getNotificationsUtils().showNotification(context.getString(R.string.txt_notification_state_title,
                                    context.getString(R.string.txt_notification_state_body_enabled)),
                            context.getString(
                                    R.string.txt_notification_state_body,
                                    context.getString(R.string.txt_notification_state_body_enabled),
                                    context.getString(R.string.txt_notification_state_body_on_cellular)),
                            true);
                }
            } else {
                switch (getLastKnownState()) {
                    case OFF_WHILE_VPN:
                    case OFF_WHILE_TRUSTED_WIFI:
                        updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                        updateLastPdnsState(ON);
                        context.refreshQuickTile();
                        context.getNotificationsUtils().showNotification(context.getString(R.string.txt_notification_state_title,
                                        context.getString(R.string.txt_notification_state_body_enabled)),
                                context.getString(
                                        R.string.txt_notification_state_body_on_last_state),
                                true);
                }
            }
        } else {
            context.getNotificationsUtils().showWarning(context.getString(R.string.txt_missing_permissions_warning));
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(PDNS_STATE_CHANGED));
    }

    public PdnsModeType getLastKnownState() {
        return PdnsModeType.valueOf(getSharedPrefs().getString(context.getString(R.string.settings_name_last_pdns_state), OFF.name()));
    }

    public void updatePdnsModeSettings(int mode) {
        try {
            Settings.Global.putString(context.getContentResolver(), SETTINGS_PRIVATE_DNS_MODE,
                    getPrivateDnsModeAsString(mode));
        } catch (SecurityException e) {
            context.getNotificationsUtils().showWarning(context.getString(R.string.txt_missing_permissions_warning));
        }
    }

    public void updateLastPdnsState(PdnsModeType lastState) {
        SharedPreferences.Editor editor = getSharedPrefsEditor();
        editor.putString(context.getString(R.string.settings_name_last_pdns_state), lastState.name());
        editor.apply();
    }

    public void updatePdnsUrl(String pDnsUrl) {
        Settings.Global.putString(context.getContentResolver(), SETTINGS_PRIVATE_DNS_SPECIFIER,
                pDnsUrl);
    }

    public String getPrivateDnsModeAsString(int mode) {
        switch (mode) {
            case PRIVATE_DNS_MODE_OFF:
                return VALUE_PRIVATE_DNS_MODE_OFF_STRING;
            case PRIVATE_DNS_MODE_OPPORTUNISTIC:
                return VALUE_PRIVATE_DNS_MODE_GOOGLE_STRING;
            case PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                return VALUE_PRIVATE_DNS_MODE_ON_STRING;
            default:
                throw new IllegalArgumentException(context.getString(R.string.txt_error_unknown_pdns_mode, mode));
        }
    }


    public PdnsModeType getPDNSState() {
        String pDNSState = context.getSettingsUtil().getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
        switch (pDNSState) {
            case VALUE_PRIVATE_DNS_MODE_OFF_STRING:
                return OFF;
            case VALUE_PRIVATE_DNS_MODE_GOOGLE_STRING:
                return GOOGLE;
            case VALUE_PRIVATE_DNS_MODE_ON_STRING:
                return ON;
            default:
                return UNKNOWN;
        }
    }

    public float getPDNSStateInFloat() {
        String pDNSState = getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
        switch (pDNSState) {
            case VALUE_PRIVATE_DNS_MODE_GOOGLE_STRING:
                return 2.0f;
            case VALUE_PRIVATE_DNS_MODE_ON_STRING:
                return 3.0f;
            default:
                return 1.0f;
        }
    }

    public String getSettingsValue(String name) {
        String result = Settings.Global.getString(context.getContentResolver(), name);
        return result == null ? context.getString(R.string.txt_none) : result;
    }

    public SharedPreferences getSharedPrefs() {
        return context.getSharedPreferences(
                context.getString(R.string.settings_name), Context.MODE_PRIVATE);
    }

    public SharedPreferences.Editor getSharedPrefsEditor() {
        return context.getSharedPreferences(
                context.getString(R.string.settings_name), Context.MODE_PRIVATE).edit();
    }

    public boolean trustedWiFiModeOn() {
        return getSharedPrefs().getBoolean(context.getString(R.string.settings_name_trust_wifi), false);
    }

    public ConnectionType getConnectionType() {
        ConnectionType connectionType = ConnectionType.UNKNOWN;
        ConnectivityManager connectivityManager = context.getConnectivityManager();
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return ConnectionType.VPN;
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return ConnectionType.WIFI;
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return ConnectionType.CELLULAR;
            }
        }

        return connectionType;
    }

    public String getWifiApName() {
        if (ConnectionType.WIFI.equals(getConnectionType())) {
            WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (context.getPermissionsUtil().isAllNeededLocationPermissionsGranted() && mWifiManager != null) {
                WifiInfo info = mWifiManager.getConnectionInfo();
                return info.getSSID().replace("\"", "");
            } else {
                return "missing permissions";
            }
        } else {
            return "cellular";
        }
    }

    public boolean itTrustedWiFiAp(String apName) {
        if (context.getPermissionsUtil().isAllNeededLocationPermissionsGranted()) {
            Set<String> trustedAps = getSharedPrefs().getStringSet(context.getString(R.string.settings_name_trust_wifi_ap_set), Collections.emptySet());

            return trustedAps.contains(apName);
        } else {
            return false;
        }
    }

    public boolean trustUntrustApByName(String apName) {
        Set<String> trustedAps = getSharedPrefs().getStringSet(context.getString(R.string.settings_name_trust_wifi_ap_set), Collections.emptySet());
        Set<String> trustedApsLocalCopy = new HashSet<>(trustedAps);

        boolean result;
        if (trustedAps.contains(apName)) {
            trustedApsLocalCopy.remove(apName);
            result = false;
        } else {
            trustedApsLocalCopy.add(apName);
            result = true;
        }

        SharedPreferences.Editor editor = getSharedPrefsEditor();
        editor.putStringSet(context.getString(R.string.settings_name_trust_wifi_ap_set), trustedApsLocalCopy);
        editor.apply();

        return result;
    }

}
