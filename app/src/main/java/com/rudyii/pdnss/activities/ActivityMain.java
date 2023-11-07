package com.rudyii.pdnss.activities;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.common.Constants.PDNS_STATE_CHANGED;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.common.Utils.getPDNSState;
import static com.rudyii.pdnss.common.Utils.getSettingsValue;
import static com.rudyii.pdnss.common.Utils.showWarning;
import static com.rudyii.pdnss.common.Utils.updateLastPdnsState;
import static com.rudyii.pdnss.common.Utils.updatePdnsModeSettings;
import static com.rudyii.pdnss.common.Utils.updatePdnsUrl;
import static com.rudyii.pdnss.services.QuickTileService.refreshQsTile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rudyii.pdnss.R;
import com.rudyii.pdnss.common.DnsStateBroadcastReceiver;

public class ActivityMain extends AppCompatActivity {
    private DnsStateBroadcastReceiver dnsStateBroadcastReceiver;
    private TextView dnsStateText;
    private TextView copyrights;
    private Button on;
    private Button off;
    private Button auto;
    private Button set;
    private CheckBox disableForVpn;
    private Button instructions;
    private EditText dnsHost;
    private boolean activityInitInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initAll();
        IntentFilter filter = new IntentFilter(PDNS_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dnsStateBroadcastReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dnsStateBroadcastReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseResources();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void initAll() {
        activityInitializationStarted();

        initProps();
        updateTexts();
        initCheckboxes();

        activityInitializationCompleted();
    }

    public void updateTexts() {
        dnsStateText.setText(getString(R.string.dns_state_details, getPDNSState(), getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER)));

        try {
            copyrights.setText(getString(R.string.copyrights,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (Exception ignored) {
        }
    }

    private void initCheckboxes() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.settings_name), Context.MODE_PRIVATE);
        disableForVpn.setChecked(sharedPref.getBoolean(getString(R.string.settings_name_disable_while_vpn), false));
    }

    private void initProps() {
        if (dnsStateText == null) {
            dnsStateText = this.findViewById(R.id.dnsStateText);
        }
        if (copyrights == null) {
            copyrights = this.findViewById(R.id.copyrights);
        }
        if (on == null) {
            on = this.findViewById(R.id.on);
            on.setOnClickListener(v -> {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                updateLastPdnsState(true);
                updateTexts();
                refreshQsTile();
            });
        }
        if (off == null) {
            off = this.findViewById(R.id.off);
            off.setOnClickListener(v -> {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                updateLastPdnsState(false);
                updateTexts();
                refreshQsTile();
            });
        }
        if (auto == null) {
            auto = this.findViewById(R.id.auto);
            auto.setOnClickListener(v -> {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                updateLastPdnsState(false);
                updateTexts();
                refreshQsTile();
            });
        }
        if (set == null) {
            set = this.findViewById(R.id.set);
            set.setOnClickListener(v -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                String dnsUrl = dnsHost.getText().toString();
                updatePdnsUrl(dnsUrl);
                showWarning(getString(R.string.dns_set_host_notification, dnsUrl));
                updateTexts();
                dnsHost.clearFocus();
                dnsHost.setText(getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
            });
        }
        if (instructions == null) {
            instructions = this.findViewById(R.id.instructions);
            instructions.setOnClickListener(v -> {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle(getString(R.string.instructions_title));
                alert.setMessage(R.string.instructions);
                alert.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                alert.show();
            });
        }
        if (disableForVpn == null) {
            disableForVpn = this.findViewById(R.id.disableForVpn);
            disableForVpn.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                            getString(R.string.settings_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(getString(R.string.settings_name_disable_while_vpn), checked);
                    editor.apply();
                }
            });
        }
        if (dnsHost == null) {
            dnsHost = this.findViewById(R.id.dsnHost);
            dnsHost.setText(getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
        }
        if (dnsStateBroadcastReceiver == null) {
            dnsStateBroadcastReceiver = new DnsStateBroadcastReceiver(dnsStateText);
        }
    }

    private void activityInitializationStarted() {
        activityInitInProgress = true;
    }

    private void activityInitializationCompleted() {
        activityInitInProgress = false;
    }

    private void releaseResources() {
        if (dnsStateBroadcastReceiver != null) {
            unregisterReceiver(dnsStateBroadcastReceiver);
        }
    }
}
