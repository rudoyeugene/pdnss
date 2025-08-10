package com.rudyii.pdnss.utils;

public class SecurityScoreUtil {
    private final long initTimeMillis = System.currentTimeMillis();
    private long startTimeMillis = 0;
    private long totalEnabledTimeMillis = 0;
    private boolean isEnabled = false;

    public void enabled() {
        if (!isEnabled) {
            isEnabled = true;
            startTimeMillis = System.currentTimeMillis();
        }
    }

    public void disabled() {
        if (isEnabled) {
            long elapsedTime = System.currentTimeMillis() - startTimeMillis;
            totalEnabledTimeMillis += elapsedTime;
            isEnabled = false;
        }
    }

    public long getTotalEnabledTime() {
        if (isEnabled) {
            long currentSessionTime = System.currentTimeMillis() - startTimeMillis;
            return totalEnabledTimeMillis + currentSessionTime;
        } else {
            return totalEnabledTimeMillis;
        }
    }

    public double getSecurityScore() {
        long currentSessionTime = System.currentTimeMillis() - initTimeMillis;
        return ((double) getTotalEnabledTime() / currentSessionTime) * 100;
    }
}

