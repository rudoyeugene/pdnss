package com.rudyii.pdnss.common;

import static com.rudyii.pdnss.PrivateDnsSwitcherApplication.getContext;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.common.Utils.getPDNSState;
import static com.rudyii.pdnss.common.Utils.getSettingsValue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import com.rudyii.pdnss.R;

public class DnsStateBroadcastReceiver extends BroadcastReceiver {
    private final TextView dnsStateText;

    public DnsStateBroadcastReceiver(TextView dnsStateText) {
        this.dnsStateText = dnsStateText;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        dnsStateText.setText(getContext().getString(R.string.txt_dns_state_details_text, getPDNSState(), getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER)));
    }
}
