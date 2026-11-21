package com.rudyii.pdnss.utils;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
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
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.provider.Settings;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.types.ConnectionType;
import com.rudyii.pdnss.types.PdnsModeType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SettingsUtils {
    private final PrivateDnsSwitcherApplication context;

    public SettingsUtils(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    public void updatePdnsSettingsOnNetworkChange(Network network) {
        ConnectivityManager connectivityManager = context.getConnectivityManager();
        if (network == null) {
            network = connectivityManager.getActiveNetwork();
        }
        NetworkCapabilities capabilities = network != null
                ? connectivityManager.getNetworkCapabilities(network)
                : null;

        boolean stateChanged = false;

        if (capabilities != null) {
            boolean isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            boolean isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            boolean isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

            if (isVpn) {
                boolean pDnsStateOn = VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                boolean disableWhileVpn = getSharedPrefs().getBoolean(context.getString(R.string.settings_name_disable_while_vpn), false);
                if (disableWhileVpn && pDnsStateOn && OFF_WHILE_VPN != getLastKnownState()) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                    updateLastPdnsState(OFF_WHILE_VPN);
                    context.getSecurityScoreUtil().disabled();
                    context.getNotificationsUtils().showNotification(
                            context.getString(R.string.txt_notification_state_title,
                                    context.getString(R.string.txt_notification_state_body_disabled)),
                            context.getString(
                                    R.string.txt_notification_state_body,
                                    context.getString(R.string.txt_notification_state_body_disabled),
                                    context.getString(R.string.txt_notification_state_body_on_vpn)),
                            false);
                    stateChanged = true;
                }
            } else if (isWiFi && context.getPermissionsUtils().isAllNeededLocationPermissionsGranted()) {
                String apName = getWifiApName();
                if (trustedWiFiModeOn()) {
                    if (itTrustedWiFiAp(apName)) {
                        boolean pDnsStateOff = VALUE_PRIVATE_DNS_MODE_OFF_STRING.equals(getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                        if (!pDnsStateOff || OFF_WHILE_TRUSTED_WIFI != getLastKnownState()) {
                            updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                            updateLastPdnsState(OFF_WHILE_TRUSTED_WIFI);
                            context.getSecurityScoreUtil().disabled();
                            context.getNotificationsUtils().showNotification(
                                    context.getString(R.string.txt_notification_state_title,
                                            context.getString(R.string.txt_notification_state_body_disabled)),
                                    context.getString(
                                            R.string.txt_notification_state_body_with_ap_name,
                                            context.getString(R.string.txt_notification_state_body_disabled),
                                            context.getString(R.string.txt_notification_state_body_on_trusted_ap),
                                            apName),
                                    false);
                            stateChanged = true;
                        }
                    } else {
                        boolean pDnsStateOn = VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                        if (!pDnsStateOn || ON != getLastKnownState()) {
                            updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                            updateLastPdnsState(ON);
                            context.getSecurityScoreUtil().enabled();
                            context.getNotificationsUtils().showNotification(
                                    context.getString(R.string.txt_notification_state_title,
                                            context.getString(R.string.txt_notification_state_body_enabled)),
                                    context.getString(
                                            R.string.txt_notification_state_body_with_ap_name,
                                            context.getString(R.string.txt_notification_state_body_enabled),
                                            context.getString(R.string.txt_notification_state_body_on_untrusted_ap),
                                            apName),
                                    true);
                            stateChanged = true;
                        }
                    }
                } else {
                    stateChanged = restoreIfNeededAfterAutoOff();
                }
            } else if (isCellular) {
                boolean pDnsStateOff = VALUE_PRIVATE_DNS_MODE_OFF_STRING.equals(getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                boolean enableWhileCellular = getSharedPrefs().getBoolean(
                        context.getString(R.string.settings_name_enable_while_cellular), false);
                if (enableWhileCellular && pDnsStateOff) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                    updateLastPdnsState(ON_WHILE_CELLULAR);
                    context.getSecurityScoreUtil().enabled();
                    context.getNotificationsUtils().showNotification(
                            context.getString(R.string.txt_notification_state_title,
                                    context.getString(R.string.txt_notification_state_body_enabled)),
                            context.getString(
                                    R.string.txt_notification_state_body,
                                    context.getString(R.string.txt_notification_state_body_enabled),
                                    context.getString(R.string.txt_notification_state_body_on_cellular)),
                            true);
                    stateChanged = true;
                } else {
                    stateChanged = restoreIfNeededAfterAutoOff();
                }
            } else {
                stateChanged = restoreIfNeededAfterAutoOff();
            }
        } else {
            stateChanged = restoreIfNeededAfterAutoOff();
        }

        if (stateChanged) {
            context.refreshQuickTile();
            context.getAppState().updatePdnsState(SystemClock.elapsedRealtime());
        }
    }

    private boolean restoreIfNeededAfterAutoOff() {
        PdnsModeType lastState = getLastKnownState();
        if (lastState == OFF_WHILE_VPN || lastState == OFF_WHILE_TRUSTED_WIFI) {
            updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
            updateLastPdnsState(ON);
            context.getSecurityScoreUtil().enabled();
            context.getNotificationsUtils().showNotification(
                    context.getString(R.string.txt_notification_state_title,
                            context.getString(R.string.txt_notification_state_body_enabled)),
                    context.getString(R.string.txt_notification_state_body_on_last_state),
                    true);
            return true;
        }
        return false;
    }

    public PdnsModeType getLastKnownState() {
        String saved = getSharedPrefs().getString(context.getString(R.string.settings_name_last_pdns_state), OFF.name());
        try {
            return PdnsModeType.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return OFF;
        }
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
        try {
            Settings.Global.putString(context.getContentResolver(), SETTINGS_PRIVATE_DNS_SPECIFIER,
                    pDnsUrl);
        } catch (SecurityException e) {
            context.getNotificationsUtils().showWarning(context.getString(R.string.txt_missing_permissions_warning));
        }
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
        String pDNSState = getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
        if (pDNSState == null) {
            return UNKNOWN;
        }
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

    public String getSettingsValue(String name) {
        String result = Settings.Global.getString(context.getContentResolver(), name);
        return result == null ? context.getString(R.string.txt_none) : result;
    }

    public SharedPreferences getSharedPrefs() {
        return context.getSharedPreferences(
                context.getString(R.string.settings_name), Context.MODE_PRIVATE);
    }

    public SharedPreferences.Editor getSharedPrefsEditor() {
        return getSharedPrefs().edit();
    }

    public boolean trustedWiFiModeOn() {
        return getSharedPrefs().getBoolean(context.getString(R.string.settings_name_trust_wifi), false);
    }

    public ConnectionType getConnectionType() {
        ConnectionType connectionType = ConnectionType.UNKNOWN;
        ConnectivityManager connectivityManager = context.getConnectivityManager();
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = network != null
                ? connectivityManager.getNetworkCapabilities(network)
                : null;

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
            if (context.getPermissionsUtils().isAllNeededLocationPermissionsGranted() && mWifiManager != null) {
                @SuppressWarnings("deprecation")
                WifiInfo info = mWifiManager.getConnectionInfo();
                String ssid = info != null ? info.getSSID() : null;
                if (ssid != null && !"<unknown ssid>".equals(ssid)) {
                    return ssid.replace("\"", "");
                }
                return context.getString(R.string.txt_none);
            } else {
                return "missing permissions";
            }
        } else {
            return "cellular";
        }
    }

    public boolean itTrustedWiFiAp(String apName) {
        if (apName != null && context.getPermissionsUtils().isAllNeededLocationPermissionsGranted()) {
            Set<String> trustedAps = getSharedPrefs().getStringSet(context.getString(R.string.settings_name_trust_wifi_ap_set), Collections.emptySet());
            return trustedAps.contains(apName);
        } else {
            return false;
        }
    }

    public boolean trustUntrustApByName(String apName) {
        if (apName == null) {
            return false;
        }
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

    public boolean isLightTheme() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_NO;
    }
}