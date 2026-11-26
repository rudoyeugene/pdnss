package com.rudyii.pdnss.utils;

import android.widget.Toast;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;

public class NotificationsUtils {
    private final PrivateDnsSwitcherApplication context;

    public NotificationsUtils(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    public void showWarning(String warningMessage) {
        Toast.makeText(context, warningMessage, Toast.LENGTH_SHORT).show();
    }
}