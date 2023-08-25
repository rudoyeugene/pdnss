package com.rudyii.pdnss;

import android.content.Context;
import android.content.IntentFilter;

import com.rudyii.pdnss.services.ConnectionStateMonitor;

public class Application extends android.app.Application {
    private static Application instance;

    public static Application getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(new ConnectionStateMonitor(), filter);
    }
}
