package com.rudyii.pdnss.common;


import android.Manifest;
import android.content.pm.PackageManager;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;

public class PermissionsUtil {
    private final PrivateDnsSwitcherApplication context;

    public PermissionsUtil(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    public boolean isLocationPermissionsGranted() {
        return context.getSettingsUtil().getSharedPrefs().getBoolean(context.getString(R.string.settings_location_permissions_granted), false);
    }

    public boolean isAllNeededLocationPermissionsGranted() {
        return isLocationPermissionsGranted()
                && PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                && PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                && PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public boolean isWriteSecureSettingsPermissionGranted() {
        return PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS);
    }
}
