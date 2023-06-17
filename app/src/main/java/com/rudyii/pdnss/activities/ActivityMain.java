package com.rudyii.pdnss.activities;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_OFF_STRING;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_OPPORTUNISTIC_STRING;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;
import static com.rudyii.pdnss.common.Utils.showWarning;
import static com.rudyii.pdnss.common.Utils.updatePdnsModeSettings;
import static com.rudyii.pdnss.common.Utils.updatePdnsUrl;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rudyii.pdnss.R;

public class ActivityMain extends AppCompatActivity {
    private TextView dnsStateText;
    private Button on;
    private Button off;
    private Button auto;
    private Button set;
    private Button instructions;
    private EditText dnsHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        initProps();
        setText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(R.string.app_name);
        initProps();
        setText();
    }

    public void setText() {
        dnsStateText.setText(getString(R.string.private_dns_state_string, getPDNSState(), getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER)));
    }

    private String getSettingsValue(String name) {
        String result = Settings.Global.getString(getApplicationContext().getContentResolver(), name);
        return result == null ? getString(R.string.none) : result;
    }

    private void initProps() {
        if (dnsStateText == null) {
            dnsStateText = this.findViewById(R.id.dnsStateText);
        }
        if (on == null) {
            on = this.findViewById(R.id.on);
            on.setOnClickListener(v -> {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                setText();
            });
        }
        if (off == null) {
            off = this.findViewById(R.id.off);
            off.setOnClickListener(v -> {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                setText();
            });
        }
        if (auto == null) {
            auto = this.findViewById(R.id.auto);
            auto.setOnClickListener(v -> {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                setText();
            });
        }
        if (set == null) {
            set = this.findViewById(R.id.set);
            set.setOnClickListener(v -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                String dnsUrl = dnsHost.getText().toString();
                updatePdnsUrl(dnsUrl);
                showWarning(String.format("Updated PDNS URL to %s", dnsUrl));
                setText();
                dnsHost.clearFocus();
                dnsHost.setText(getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
            });
        }
        if (instructions == null) {
            instructions = this.findViewById(R.id.instructions);
            instructions.setOnClickListener(v -> {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Permissions granting instructions");
                alert.setMessage(R.string.pm_grant_write_security_settings);
                alert.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                alert.show();
            });
        }
        if (dnsHost == null) {
            dnsHost = this.findViewById(R.id.dsnHost);
            dnsHost.setText(getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
        }
    }

    private String getPDNSState() {
        String PDNSState = getSettingsValue(SETTINGS_PRIVATE_DNS_MODE);
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