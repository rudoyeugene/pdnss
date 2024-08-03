package com.rudyii.pdnss.common;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.PrivateDnsSwitcherApplication.getContext;
import static com.rudyii.pdnss.common.Constants.PDNS_STATE_CHANGED;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.common.Constants.STATE_NOTIFICATION_ID;
import static com.rudyii.pdnss.common.Constants.STATE_NOTIFICATION_NAME;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_GOOGLE_STRING;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_OFF_STRING;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_ON_STRING;
import static com.rudyii.pdnss.common.PdnsModeType.OFF;
import static com.rudyii.pdnss.common.PdnsModeType.OFF_WHILE_TRUSTED_WIFI;
import static com.rudyii.pdnss.common.PdnsModeType.OFF_WHILE_VPN;
import static com.rudyii.pdnss.common.PdnsModeType.ON;
import static com.rudyii.pdnss.common.PdnsModeType.ON_WHILE_CELLULAR;
import static com.rudyii.pdnss.services.QuickTile.refreshQsTile;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.rudyii.pdnss.R;
import com.rudyii.pdnss.activities.ActivityMain;

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

    public static void showWarning(String warningMessage) {
        Toast.makeText(getContext(), warningMessage,
                Toast.LENGTH_LONG).show();
    }

    public static void updatePdnsSettingsOnNetworkChange() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

        if (capabilities != null) {
            boolean isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            boolean isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            boolean isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

            if (isVpn) {
                boolean pDnsStateOn = VALUE_PRIVATE_DNS_MODE_ON_STRING.equals(getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                boolean disableWhileVnp = getSharedPrefs().getBoolean(getContext().getString(R.string.settings_name_disable_while_vpn), false);
                if (disableWhileVnp && pDnsStateOn) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                    updateLastPdnsState(OFF_WHILE_VPN);
                    refreshQsTile();
                    showNotification(getContext().getString(R.string.txt_notification_state_title,
                                    getContext().getString(R.string.txt_notification_state_body_disabled)),
                            getContext().getString(
                                    R.string.txt_notification_state_body,
                                    getContext().getString(R.string.txt_notification_state_body_disabled),
                                    getContext().getString(R.string.txt_notification_state_body_on_vpn)));
                }
            } else if (isWiFi && isAllNeededLocationPermissionsGranted()) {
                String apName = getWifiApName();
                if (itTrustedWiFiAp(apName) && trustedWiFiModeOn()) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                    updateLastPdnsState(OFF_WHILE_TRUSTED_WIFI);
                    refreshQsTile();
                    showNotification(getContext().getString(R.string.txt_notification_state_title,
                                    getContext().getString(R.string.txt_notification_state_body_disabled)),
                            getContext().getString(
                                    R.string.txt_notification_state_body,
                                    getContext().getString(R.string.txt_notification_state_body_disabled),
                                    getContext().getString(R.string.txt_notification_state_body_on_trusted_ap)));
                } else if (!itTrustedWiFiAp(apName) && trustedWiFiModeOn()) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                    updateLastPdnsState(ON);
                    refreshQsTile();
                    showNotification(getContext().getString(R.string.txt_notification_state_title,
                                    getContext().getString(R.string.txt_notification_state_body_enabled)),
                            getContext().getString(
                                    R.string.txt_notification_state_body,
                                    getContext().getString(R.string.txt_notification_state_body_enabled),
                                    getContext().getString(R.string.txt_notification_state_body_on_untrusted_ap)));
                }
            } else if (isCellular) {
                boolean pDnsStateOff = VALUE_PRIVATE_DNS_MODE_OFF_STRING.equals(getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                boolean enableWhileCellular = getSharedPrefs().getBoolean(getContext().getString(R.string.settings_name_enable_while_cellular), false);
                if (enableWhileCellular && pDnsStateOff) {
                    updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                    updateLastPdnsState(ON_WHILE_CELLULAR);
                    refreshQsTile();
                    showNotification(getContext().getString(R.string.txt_notification_state_title,
                                    getContext().getString(R.string.txt_notification_state_body_enabled)),
                            getContext().getString(
                                    R.string.txt_notification_state_body,
                                    getContext().getString(R.string.txt_notification_state_body_enabled),
                                    getContext().getString(R.string.txt_notification_state_body_on_cellular)));
                }
            } else {
                switch (getLastKnownState()) {
                    case OFF_WHILE_VPN:
                    case OFF_WHILE_TRUSTED_WIFI:
                        updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                        updateLastPdnsState(ON);
                        refreshQsTile();
                        showNotification(getContext().getString(R.string.txt_notification_state_title,
                                        getContext().getString(R.string.txt_notification_state_body_enabled)),
                                getContext().getString(
                                        R.string.txt_notification_state_body_on_last_state));
                }
            }
        }
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(PDNS_STATE_CHANGED));
    }

    public static void showNotification(String title, String body) {
        Intent activityIntent = new Intent(getContext(), ActivityMain.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, activityIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(getContext(), STATE_NOTIFICATION_NAME)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setOngoing(false)
                .build();

        notificationManager.notify(STATE_NOTIFICATION_ID, notification);
    }

    public static boolean trustedWiFiModeOn() {
        return getSharedPrefs().getBoolean(getContext().getString(R.string.settings_name_trust_wifi), false);
    }

    public static ConnectionType getConnectionType() {
        ConnectionType connectionType = ConnectionType.UNKNOWN;
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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

    public static String getWifiApName() {
        if (ConnectionType.WIFI.equals(getConnectionType())) {
            WifiManager mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
            if (isAllNeededLocationPermissionsGranted() && mWifiManager != null) {
                WifiInfo info = mWifiManager.getConnectionInfo();
                return info.getSSID().replace("\"", "");
            } else {
                return "missing permissions";
            }
        } else {
            return "cellular";
        }
    }

    public static boolean itTrustedWiFiAp(String apName) {
        if (isAllNeededLocationPermissionsGranted()) {
            Set<String> trustedAps = getSharedPrefs().getStringSet(getContext().getString(R.string.settings_name_trust_wifi_ap_set), Collections.emptySet());

            return trustedAps.contains(apName);
        } else {
            return false;
        }
    }

    public static boolean trustUntrustApByName(String apName) {
        Set<String> trustedAps = getSharedPrefs().getStringSet(getContext().getString(R.string.settings_name_trust_wifi_ap_set), Collections.emptySet());
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
        editor.putStringSet(getContext().getString(R.string.settings_name_trust_wifi_ap_set), trustedApsLocalCopy);
        editor.apply();

        return result;
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

    public static boolean isWriteSecureSettingsPermissionGranted() {
        return PackageManager.PERMISSION_GRANTED == getContext().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS);
    }
}
