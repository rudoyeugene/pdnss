package com.rudyii.pdnss.receivers;

import static com.rudyii.pdnss.types.Constants.APP_NAME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.types.NetworkSnapshot;
import com.rudyii.pdnss.utils.PdnsPolicyScheduler;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static volatile int lastBroadcastTransportMask = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = intent.getParcelableExtra(
                ConnectivityManager.EXTRA_NETWORK,
                Network.class
        );

        NetworkCapabilities caps = (cm != null && network != null)
                ? cm.getNetworkCapabilities(network)
                : null;

        int transportMask = 0;
        if (caps != null) {
            transportMask = (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ? 1 : 0)
                    | (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? 2 : 0)
                    | (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? 4 : 0);
        }

        if (caps != null && transportMask == lastBroadcastTransportMask) {
            return;
        }
        lastBroadcastTransportMask = transportMask;

        String fallbackSsid = null;
        if (context.getApplicationContext() instanceof PrivateDnsSwitcherApplication app) {
            fallbackSsid = app.getSettingsUtils().getCachedWifiSsid();
        }

        NetworkSnapshot snapshot = (caps != null)
                ? NetworkSnapshot.fromCapabilities(network, caps, fallbackSsid)
                : null;

        Log.i(APP_NAME, "System network PendingIntent triggered (transportMask=" + transportMask + ")");
        PendingResult pendingResult = goAsync();
        PdnsPolicyScheduler.scheduleEvaluation(context, "pendingIntent", snapshot, pendingResult);
    }
}