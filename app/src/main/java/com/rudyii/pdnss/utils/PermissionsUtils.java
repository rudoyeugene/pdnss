package com.rudyii.pdnss.utils;


import android.Manifest;
import android.content.pm.PackageManager;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;

public class PermissionsUtils {
    private final PrivateDnsSwitcherApplication context;

    public PermissionsUtils(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    public boolean isLocationPermissionsGranted() {
        return context.getSettingsUtils().getSharedPrefs().getBoolean(context.getString(R.string.settings_location_permissions_granted), false);
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
