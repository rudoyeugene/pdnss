package com.rudyii.pdnss.utils;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.types.Constants.APP_NAME;
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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.services.PdnsMonitorService;
import com.rudyii.pdnss.types.ConnectionType;
import com.rudyii.pdnss.types.NetworkSnapshot;
import com.rudyii.pdnss.types.PdnsModeType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SettingsUtils {
    private final PrivateDnsSwitcherApplication context;
    private volatile String cachedWifiSsid;

    public SettingsUtils(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    public String getCachedWifiSsid() {
        return cachedWifiSsid;
    }

    public void setCachedWifiSsid(@Nullable String ssid) {
        if (ssid != null && !ssid.isEmpty() && !"<unknown ssid>".equals(ssid)) {
            cachedWifiSsid = ssid.replace("\"", "");
        }
    }

    public void clearCachedWifiInfo() {
        cachedWifiSsid = null;
    }

    public void updatePdnsSettingsOnNetworkChange(@Nullable NetworkSnapshot snapshot) {
        ConnectivityManager connectivityManager = context.getConnectivityManager();
        Network targetNetwork = snapshot != null ? snapshot.network() : connectivityManager.getActiveNetwork();
        NetworkCapabilities liveCaps = targetNetwork != null
                ? connectivityManager.getNetworkCapabilities(targetNetwork)
                : null;

        if (liveCaps == null && snapshot == null) {
            Log.i(APP_NAME, "No active network capabilities yet; skipping policy evaluation");
            return;
        }

        boolean isVpn = liveCaps != null
                ? liveCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                : snapshot.isVpn();
        boolean isWiFi = liveCaps != null
                ? liveCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                : snapshot.isWiFi();
        boolean isCellular = liveCaps != null
                ? liveCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                : snapshot.isCellular();

        boolean stateChanged = false;
        String currentMode = getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
        PdnsModeType lastKnownState = getLastKnownState();
        boolean hasLocationPerm = context.getPermissionsUtils().isAllNeededLocationPermissionsGranted();
        boolean disableWhileVpn = getSharedPrefs().getBoolean(
                context.getString(R.string.settings_name_disable_while_vpn), false);
        boolean enableWhileCellular = getSharedPrefs().getBoolean(
                context.getString(R.string.settings_name_enable_while_cellular), false);

        Log.i(APP_NAME, "Evaluating policy: vpn=" + isVpn
                + ", wifi=" + isWiFi
                + ", cell=" + isCellular
                + ", hasLocationPerm=" + hasLocationPerm
                + ", currentMode=" + currentMode
                + ", lastKnownState=" + lastKnownState);

        if (isVpn && disableWhileVpn) {
            boolean pDnsStateOff = VALUE_PRIVATE_DNS_MODE_OFF_STRING.equals(currentMode);
            if (!pDnsStateOff || OFF_WHILE_VPN != lastKnownState) {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                updateLastPdnsState(OFF_WHILE_VPN);
                context.getSecurityScoreUtil().disabled();
                PdnsMonitorService.refreshNotification(
                        context,
                        context.getString(R.string.txt_notification_state_title,
                                context.getString(R.string.txt_notification_state_body_disabled)),
                        context.getString(
                                R.string.txt_notification_state_body,
                                context.getString(R.string.txt_notification_state_body_disabled),
                                context.getString(R.string.txt_notification_state_body_on_vpn)),
                        false);
                stateChanged = true;
            }
        } else if (isWiFi && trustedWiFiModeOn() && hasLocationPerm) {
            String apName = (snapshot != null && snapshot.wifiSsid() != null)
                    ? snapshot.wifiSsid()
                    : getWifiApName();
            if (apName == null || apName.isEmpty() || context.getString(R.string.txt_none).equals(apName)) {
                Log.i(APP_NAME, "Wi-Fi connected but SSID not resolved yet; waiting for next capability update");
                return;
            }
            if (itTrustedWiFiAp(apName)) {
                boolean pDnsStateOff = VALUE_PRIVATE_DNS_MODE_OFF_STRING.equals(currentMode);
                if (!pDnsStateOff || OFF_WHILE_TRUSTED_WIFI != lastKnownState) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                    updateLastPdnsState(OFF_WHILE_TRUSTED_WIFI);
                    context.getSecurityScoreUtil().disabled();
                    PdnsMonitorService.refreshNotification(
                            context,
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
                boolean pDnsStateOn = VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(currentMode);
                if (!pDnsStateOn || ON != lastKnownState) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                    updateLastPdnsState(ON);
                    context.getSecurityScoreUtil().enabled();
                    PdnsMonitorService.refreshNotification(
                            context,
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
        } else if (isCellular && enableWhileCellular) {
            boolean pDnsStateOn = VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(currentMode);
            if (!pDnsStateOn || ON_WHILE_CELLULAR != lastKnownState) {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                updateLastPdnsState(ON_WHILE_CELLULAR);
                context.getSecurityScoreUtil().enabled();
                PdnsMonitorService.refreshNotification(
                        context,
                        context.getString(R.string.txt_notification_state_title,
                                context.getString(R.string.txt_notification_state_body_enabled)),
                        context.getString(
                                R.string.txt_notification_state_body,
                                context.getString(R.string.txt_notification_state_body_enabled),
                                context.getString(R.string.txt_notification_state_body_on_cellular)),
                        true);
                stateChanged = true;
            }
        } else {
            stateChanged = restoreDefaultStateIfAutoSwitched(currentMode, lastKnownState);
        }

        if (stateChanged) {
            context.refreshQuickTile();
            context.getAppState().updatePdnsState(SystemClock.elapsedRealtime());
        }
    }

    private boolean restoreDefaultStateIfAutoSwitched(String currentMode, PdnsModeType lastState) {
        if (lastState == OFF_WHILE_VPN || lastState == OFF_WHILE_TRUSTED_WIFI) {
            updateLastPdnsState(ON);
            if (!VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(currentMode)) {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                context.getSecurityScoreUtil().enabled();
                PdnsMonitorService.refreshNotification(
                        context,
                        context.getString(R.string.txt_notification_state_title,
                                context.getString(R.string.txt_notification_state_body_enabled)),
                        context.getString(R.string.txt_notification_state_body_on_last_state),
                        true);
                return true;
            }
        } else if (lastState == ON_WHILE_CELLULAR) {
            updateLastPdnsState(OFF);
            if (!VALUE_PRIVATE_DNS_MODE_OFF_STRING.equals(currentMode)) {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                context.getSecurityScoreUtil().disabled();
                PdnsMonitorService.refreshNotification(
                        context,
                        context.getString(R.string.txt_notification_state_title,
                                context.getString(R.string.txt_notification_state_body_disabled)),
                        context.getString(R.string.txt_notification_state_body_on_last_state),
                        false);
                return true;
            }
        }
        return false;
    }

    public void syncExternalStateChanges() {
        PdnsModeType actual = getPDNSState();
        PdnsModeType recorded = getLastKnownState();
        boolean externalChangeDetected = false;

        if (actual == ON) {
            context.getSecurityScoreUtil().enabled();
            if (recorded == OFF || recorded == GOOGLE || recorded == UNKNOWN) {
                updateLastPdnsState(ON);
                externalChangeDetected = true;
            }
        } else if (actual == OFF || actual == GOOGLE) {
            context.getSecurityScoreUtil().disabled();
            if (recorded == ON || recorded == ON_WHILE_CELLULAR || recorded == UNKNOWN) {
                updateLastPdnsState(actual);
                externalChangeDetected = true;
            }
        }
        if (externalChangeDetected || !PdnsMonitorService.hasCachedNotification()) {
            PdnsMonitorService.refreshNotification(context);
        }
        if (externalChangeDetected) {
            context.refreshQuickTile();
        }
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
            PdnsMonitorService.refreshNotification(context);
            context.refreshQuickTile();
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
            if (!context.getPermissionsUtils().isAllNeededLocationPermissionsGranted()) {
                return "missing permissions";
            }

            ConnectivityManager cm = context.getConnectivityManager();
            Network activeNetwork = cm != null ? cm.getActiveNetwork() : null;
            NetworkCapabilities caps = activeNetwork != null ? cm.getNetworkCapabilities(activeNetwork) : null;
            if (caps != null && caps.getTransportInfo() instanceof WifiInfo capWifiInfo) {
                String capSsid = capWifiInfo.getSSID();
                if (capSsid != null && !"<unknown ssid>".equals(capSsid)) {
                    cachedWifiSsid = capSsid.replace("\"", "");
                    return cachedWifiSsid;
                }
            }

            if (cachedWifiSsid != null && !cachedWifiSsid.isEmpty()) {
                return cachedWifiSsid;
            }

            WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (mWifiManager != null) {
                @SuppressWarnings("deprecation")
                WifiInfo info = mWifiManager.getConnectionInfo();
                String ssid = info != null ? info.getSSID() : null;
                if (ssid != null && !"<unknown ssid>".equals(ssid)) {
                    cachedWifiSsid = ssid.replace("\"", "");
                    return cachedWifiSsid;
                }
            }
            return context.getString(R.string.txt_none);
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
}