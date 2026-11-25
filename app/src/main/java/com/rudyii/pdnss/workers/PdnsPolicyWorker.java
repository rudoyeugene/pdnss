package com.rudyii.pdnss.workers;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.WORK_DATA_REASON;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;

public class PdnsPolicyWorker extends Worker {

    public PdnsPolicyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String reason = getInputData().getString(WORK_DATA_REASON);
        Context appContext = getApplicationContext();
        if (!(appContext instanceof PrivateDnsSwitcherApplication app)) {
            return Result.failure();
        }

        Log.i(APP_NAME, "Worker applying Private DNS policy (reason: " + reason + ")");
        app.getSettingsUtils().updatePdnsSettingsOnNetworkChange(null);
        return Result.success();
    }
}