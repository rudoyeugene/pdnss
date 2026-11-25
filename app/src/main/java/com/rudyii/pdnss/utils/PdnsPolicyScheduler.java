package com.rudyii.pdnss.utils;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.DEFAULT_ACTION_DELAY_MS;
import static com.rudyii.pdnss.types.Constants.MAX_ACTION_DELAY_MS;
import static com.rudyii.pdnss.types.Constants.MIN_ACTION_DELAY_MS;
import static com.rudyii.pdnss.types.Constants.SETTINGS_ACTION_DELAY_MS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.types.NetworkSnapshot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class PdnsPolicyScheduler {
    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pdnss-policy-scheduler");
                t.setDaemon(true);
                return t;
            });

    private static final long ZERO_DELAY_COALESCE_MS = 200L;
    private static final long MAX_BROADCAST_HOLD_MS = 8500L;

    private static ScheduledFuture<?> pendingTask;
    private static BroadcastReceiver.PendingResult activePendingResult;

    private PdnsPolicyScheduler() {
    }

    public static int clampDelayMs(int rawDelayMs) {
        return Math.clamp(rawDelayMs, MIN_ACTION_DELAY_MS, MAX_ACTION_DELAY_MS);
    }

    public static int getConfiguredDelayMs(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext instanceof PrivateDnsSwitcherApplication app) {
            int raw = app.getSettingsUtils()
                    .getSharedPrefs()
                    .getInt(SETTINGS_ACTION_DELAY_MS, DEFAULT_ACTION_DELAY_MS);
            return clampDelayMs(raw);
        }
        return DEFAULT_ACTION_DELAY_MS;
    }

    public static synchronized void scheduleEvaluation(
            @NonNull Context context,
            @NonNull String reason,
            @Nullable NetworkSnapshot snapshot,
            @Nullable BroadcastReceiver.PendingResult incomingPendingResult) {

        Context appContext = context.getApplicationContext();
        if (!(appContext instanceof PrivateDnsSwitcherApplication app)) {
            if (incomingPendingResult != null) {
                incomingPendingResult.finish();
            }
            return;
        }

        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
        }

        // Only replace activePendingResult if a new broadcast PendingResult arrived;
        // otherwise keep holding the existing broadcast lock until the rescheduled task finishes.
        if (incomingPendingResult != null) {
            finishActivePendingResult();
            activePendingResult = incomingPendingResult;
        }

        int configuredDelayMs = getConfiguredDelayMs(app);
        long effectiveDelayMs = configuredDelayMs == 0 ? ZERO_DELAY_COALESCE_MS : configuredDelayMs;
        if (activePendingResult != null && effectiveDelayMs > MAX_BROADCAST_HOLD_MS) {
            effectiveDelayMs = MAX_BROADCAST_HOLD_MS;
        }

        final long scheduledDelay = effectiveDelayMs;
        pendingTask = EXECUTOR.schedule(() -> {
            try {
                Log.i(APP_NAME, "Executing Private DNS policy (reason: " + reason + ")");
                app.getSettingsUtils().updatePdnsSettingsOnNetworkChange(snapshot);
            } catch (Exception e) {
                Log.e(APP_NAME, "Error applying Private DNS policy", e);
            } finally {
                finishActivePendingResult();
            }
        }, scheduledDelay, TimeUnit.MILLISECONDS);

        Log.i(APP_NAME, "Scheduled policy evaluation (" + reason + ") in " + scheduledDelay + " ms");
    }

    private static synchronized void finishActivePendingResult() {
        if (activePendingResult != null) {
            try {
                activePendingResult.finish();
            } catch (IllegalStateException ignored) {
            }
            activePendingResult = null;
        }
    }
}