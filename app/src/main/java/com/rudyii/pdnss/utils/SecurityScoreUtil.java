package com.rudyii.pdnss.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.rudyii.pdnss.R;

public class SecurityScoreUtil {
    private static final String KEY_INIT_TIME = "score_init_time_ms";
    private static final String KEY_START_TIME = "score_start_time_ms";
    private static final String KEY_TOTAL_ENABLED = "score_total_enabled_ms";
    private static final String KEY_IS_ENABLED = "score_is_enabled";

    private final SharedPreferences prefs;
    private final long initTimeMillis;
    private long startTimeMillis;
    private long totalEnabledTimeMillis;
    private boolean isEnabled;

    public SecurityScoreUtil(Context context) {
        this.prefs = context.getSharedPreferences(
                context.getString(R.string.settings_name), Context.MODE_PRIVATE);

        long now = System.currentTimeMillis();
        long savedInit = prefs.getLong(KEY_INIT_TIME, 0L);
        if (savedInit <= 0L) {
            this.initTimeMillis = now;
            prefs.edit().putLong(KEY_INIT_TIME, now).apply();
        } else {
            this.initTimeMillis = savedInit;
        }

        this.startTimeMillis = prefs.getLong(KEY_START_TIME, 0L);
        this.totalEnabledTimeMillis = prefs.getLong(KEY_TOTAL_ENABLED, 0L);
        this.isEnabled = prefs.getBoolean(KEY_IS_ENABLED, false);
    }

    public synchronized void enabled() {
        if (!isEnabled) {
            isEnabled = true;
            startTimeMillis = System.currentTimeMillis();
            persist();
        }
    }

    public synchronized void disabled() {
        if (isEnabled) {
            long elapsedTime = Math.max(0L, System.currentTimeMillis() - startTimeMillis);
            totalEnabledTimeMillis += elapsedTime;
            startTimeMillis = 0L;
            isEnabled = false;
            persist();
        }
    }

    public synchronized long getTotalEnabledTime() {
        if (isEnabled && startTimeMillis > 0L) {
            long currentSessionTime = Math.max(0L, System.currentTimeMillis() - startTimeMillis);
            return totalEnabledTimeMillis + currentSessionTime;
        }
        return totalEnabledTimeMillis;
    }

    public synchronized double getSecurityScore() {
        long currentSessionTime = System.currentTimeMillis() - initTimeMillis;
        if (currentSessionTime <= 0L) {
            return isEnabled ? 100.0 : 0.0;
        }
        double rawScore = ((double) getTotalEnabledTime() / currentSessionTime) * 100.0;
        return Math.clamp(rawScore, 0.0, 100.0);
    }

    private void persist() {
        prefs.edit()
                .putLong(KEY_START_TIME, startTimeMillis)
                .putLong(KEY_TOTAL_ENABLED, totalEnabledTimeMillis)
                .putBoolean(KEY_IS_ENABLED, isEnabled)
                .apply();
    }
}