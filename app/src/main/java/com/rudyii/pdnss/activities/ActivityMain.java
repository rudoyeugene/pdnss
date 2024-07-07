package com.rudyii.pdnss.activities;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.PrivateDnsSwitcherApplication.getContext;
import static com.rudyii.pdnss.common.Constants.PDNS_STATE_CHANGED;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.common.PdnsModeType.GOOGLE;
import static com.rudyii.pdnss.common.PdnsModeType.OFF;
import static com.rudyii.pdnss.common.PdnsModeType.ON;
import static com.rudyii.pdnss.common.Utils.getPDNSState;
import static com.rudyii.pdnss.common.Utils.getSettingsValue;
import static com.rudyii.pdnss.common.Utils.getWifiSsidColorCode;
import static com.rudyii.pdnss.common.Utils.getWifiSsidName;
import static com.rudyii.pdnss.common.Utils.showWarning;
import static com.rudyii.pdnss.common.Utils.trustUntrustSsidName;
import static com.rudyii.pdnss.common.Utils.updateLastPdnsState;
import static com.rudyii.pdnss.common.Utils.updatePdnsModeSettings;
import static com.rudyii.pdnss.common.Utils.updatePdnsUrl;
import static com.rudyii.pdnss.services.QuickTileService.refreshQsTile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.rudyii.pdnss.R;
import com.rudyii.pdnss.common.DnsStateBroadcastReceiver;

public class ActivityMain extends AppCompatActivity {
    private DnsStateBroadcastReceiver dnsStateBroadcastReceiver;
    private TextView txtDnsState;
    private TextView txtCopyrights;
    private TextView txtSsidName;
    private Button btnOn;
    private Button btnOff;
    private Button btnGoogle;
    private Button btnSet;
    private Button btnTrustWiFi;
    private CheckBox cbDisableForVpn;
    private CheckBox cbTrustWiFi;
    private Button btnInstructions;
    private EditText editTxtDnsHost;
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
        LocalBroadcastManager.getInstance(this).registerReceiver(
                dnsStateBroadcastReceiver, filter);
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
        txtDnsState.setText(getString(R.string.dns_state_details, getPDNSState(), getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER)));

        try {
            txtCopyrights.setText(getString(R.string.copyrights,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (Exception ignored) {
        }
    }

    private void initCheckboxes() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.settings_name), Context.MODE_PRIVATE);
        cbDisableForVpn.setChecked(sharedPref.getBoolean(getString(R.string.settings_name_disable_while_vpn), false));
    }

    private void initProps() {
        if (txtDnsState == null) {
            txtDnsState = this.findViewById(R.id.txtDnsState);
        }
        if (txtCopyrights == null) {
            txtCopyrights = this.findViewById(R.id.txtCopyrights);
        }
        if (btnOn == null) {
            btnOn = this.findViewById(R.id.btnOn);
            btnOn.setOnClickListener(v -> {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                updateLastPdnsState(ON);
                updateTexts();
                refreshQsTile();
            });
        }
        if (btnOff == null) {
            btnOff = this.findViewById(R.id.btnOff);
            btnOff.setOnClickListener(v -> {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                updateLastPdnsState(OFF);
                updateTexts();
                refreshQsTile();
            });
        }
        if (btnGoogle == null) {
            btnGoogle = this.findViewById(R.id.btnGoogle);
            btnGoogle.setOnClickListener(v -> {
                updatePdnsModeSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                updateLastPdnsState(GOOGLE);
                updateTexts();
                refreshQsTile();
            });
        }
        if (btnSet == null) {
            btnSet = this.findViewById(R.id.btnSet);
            btnSet.setOnClickListener(v -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                String dnsUrl = editTxtDnsHost.getText().toString();
                updatePdnsUrl(dnsUrl);
                showWarning(getString(R.string.dns_set_host_notification, dnsUrl));
                updateTexts();
                editTxtDnsHost.clearFocus();
                editTxtDnsHost.setText(getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
            });
        }
        if (btnInstructions == null) {
            btnInstructions = this.findViewById(R.id.btnInstructions);
            btnInstructions.setOnClickListener(v -> {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle(getString(R.string.instructions_title));
                alert.setMessage(R.string.instructions);
                alert.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                alert.show();
            });
        }
        if (btnTrustWiFi == null) {
            btnTrustWiFi = this.findViewById(R.id.btnTrustWiFi);
            SharedPreferences sharedPrefForInit = getApplicationContext().getSharedPreferences(
                    getString(R.string.settings_name), Context.MODE_PRIVATE);
            btnTrustWiFi.setEnabled(sharedPrefForInit.getBoolean(getString(R.string.settings_name_trust_wifi), false));
            btnTrustWiFi.setText(getWifiSsidColorCode(getWifiSsidName()) == Color.RED ? getContext().getString(R.string.btn_trust_ssid) : getContext().getString(R.string.btn_untrust_ssid));
            btnTrustWiFi.setOnClickListener(v -> {
                String ssidName = getWifiSsidName();
                SharedPreferences sharedPref = getContext().getSharedPreferences(
                        getContext().getString(R.string.settings_name), Context.MODE_PRIVATE);
                btnTrustWiFi.setEnabled(sharedPref.getBoolean(getContext().getString(R.string.settings_name_trust_wifi), false));
                int colorCode = trustUntrustSsidName(ssidName);
                btnTrustWiFi.setText(colorCode == Color.RED ? getContext().getString(R.string.btn_trust_ssid) : getContext().getString(R.string.btn_untrust_ssid));
                txtSsidName.setTextColor(colorCode);
                showWarning(getContext().getString(R.string.reconnect_wifi_after_trust));
            });
        }
        if (cbDisableForVpn == null) {
            cbDisableForVpn = this.findViewById(R.id.cbDisableForVpn);
            cbDisableForVpn.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                            getString(R.string.settings_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(getString(R.string.settings_name_disable_while_vpn), checked);
                    editor.apply();
                }
            });
        }
        if (cbTrustWiFi == null) {
            cbTrustWiFi = this.findViewById(R.id.cbTrustWiFi);
            SharedPreferences sharedPrefForInit = getApplicationContext().getSharedPreferences(
                    getString(R.string.settings_name), Context.MODE_PRIVATE);
            cbTrustWiFi.setChecked(sharedPrefForInit.getBoolean(getString(R.string.settings_name_trust_wifi), false));
            cbTrustWiFi.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                            getString(R.string.settings_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(getString(R.string.settings_name_trust_wifi), checked);
                    editor.apply();
                    btnTrustWiFi.setEnabled(checked);
                }
            });
        }
        if (editTxtDnsHost == null) {
            editTxtDnsHost = this.findViewById(R.id.editTxtDsnHost);
            editTxtDnsHost.setText(getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
        }
        if (txtSsidName == null) {
            txtSsidName = this.findViewById(R.id.txtSsidName);
            String ssidName = getWifiSsidName();
            txtSsidName.setText(ssidName);
            txtSsidName.setTextColor(getWifiSsidColorCode(ssidName));
        }
        if (dnsStateBroadcastReceiver == null) {
            dnsStateBroadcastReceiver = new DnsStateBroadcastReceiver(txtDnsState);
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
            LocalBroadcastManager.getInstance(this).unregisterReceiver(dnsStateBroadcastReceiver);

        }
    }
}
