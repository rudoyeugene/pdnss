package com.rudyii.pdnss;

import static com.rudyii.pdnss.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.Constants.VALUE_PRIVATE_DNS_MODE_OFF_STRING;
import static com.rudyii.pdnss.Constants.VALUE_PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING;
import static com.rudyii.pdnss.Constants.VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivateDNSSwitcherMainActivity extends AppCompatActivity {
    private TextView beforeUseText;
    private TextView beforeUseCommandText;
    private TextView dnsStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initProps();
        setText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initProps();
        setText();
    }

    public void setText() {
        beforeUseText.setText(R.string.before_all);
        beforeUseCommandText.setText(R.string.pm_grant_write_security_settings);
        dnsStateText.setText(getString(R.string.private_dns_state_string, getPDNSState(), Settings.Global.getString(getApplicationContext().getContentResolver(), SETTINGS_PRIVATE_DNS_SPECIFIER)));
    }

    private void initProps() {
        if (beforeUseText == null) {
            beforeUseText = this.findViewById(R.id.beforeUseText);
        }
        if (beforeUseCommandText == null) {
            beforeUseCommandText = this.findViewById(R.id.beforeUseCommand);
        }
        if (dnsStateText == null) {
            dnsStateText = this.findViewById(R.id.dnsStateText);
        }
    }

    private String getPDNSState() {
        String PDNSState = Settings.Global.getString(getApplicationContext().getContentResolver(), SETTINGS_PRIVATE_DNS_MODE);
        if (PDNSState == null) {
            return getString(R.string.private_dns_state_unknown);
        } else {
            switch (PDNSState) {
                case VALUE_PRIVATE_DNS_MODE_OFF_STRING:
                    return getString(R.string.private_dns_state_off);
                case VALUE_PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING:
                    return getString(R.string.private_dns_state_auto);
                case VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING:
                    return getString(R.string.private_dns_state_on);
                default:
                    return getString(R.string.private_dns_state_unknown);
            }
        }
    }
}