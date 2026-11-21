package com.rudyii.pdnss.utils;

import android.Manifest;
import android.content.pm.PackageManager;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;

public class PermissionsUtils {
    private final PrivateDnsSwitcherApplication context;

    public PermissionsUtils(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    public boolean isLocationPermissionsGranted() {
        return PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                || PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public boolean isAllNeededLocationPermissionsGranted() {
        return PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                && PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                && PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public boolean isWriteSecureSettingsPermissionGranted() {
        return PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS);
    }
}