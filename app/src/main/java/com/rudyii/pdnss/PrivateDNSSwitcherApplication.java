package com.rudyii.pdnss;

import android.app.Application;
import android.content.Context;

public class PrivateDNSSwitcherApplication extends Application {
    private static PrivateDNSSwitcherApplication instance;

    public static PrivateDNSSwitcherApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
    }
}
