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
import static com.rudyii.pdnss.common.Utils.getConnectionType;
import static com.rudyii.pdnss.common.Utils.getPDNSState;
import static com.rudyii.pdnss.common.Utils.getSettingsValue;
import static com.rudyii.pdnss.common.Utils.getSharedPrefs;
import static com.rudyii.pdnss.common.Utils.getSharedPrefsEditor;
import static com.rudyii.pdnss.common.Utils.getWifiSsidColorCode;
import static com.rudyii.pdnss.common.Utils.getWifiSsidName;
import static com.rudyii.pdnss.common.Utils.isAllNeededLocationPermissionsGranted;
import static com.rudyii.pdnss.common.Utils.isLocationPermissionsGranted;
import static com.rudyii.pdnss.common.Utils.showWarning;
import static com.rudyii.pdnss.common.Utils.trustUntrustSsidName;
import static com.rudyii.pdnss.common.Utils.trustedWiFiModeOn;
import static com.rudyii.pdnss.common.Utils.updateLastPdnsState;
import static com.rudyii.pdnss.common.Utils.updatePdnsModeSettings;
import static com.rudyii.pdnss.common.Utils.updatePdnsSettingsOnNetworkChange;
import static com.rudyii.pdnss.common.Utils.updatePdnsUrl;
import static com.rudyii.pdnss.services.QuickTileService.refreshQsTile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.common.ConnectionType;

public class ActivityMain extends AppCompatActivity {
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1122;
    public static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1133;
    private BroadcastReceiver broadcastReceiver;
    private TextView txtDnsState;
    private TextView txtCopyrights;
    private TextView txtSsidName;
    private Button btnOn;
    private Button btnOff;
    private Button btnGoogle;
    private Button btnSet;
    private Button btnTrustWiFi;
    private Button btnInstructions;
    private Button btnPermissions;
    private MaterialSwitch cbDisableForVpn;
    private MaterialSwitch cbEnableForCellular;
    private MaterialSwitch cbTrustWiFi;
    private EditText editTxtDnsHost;
    private boolean activityInitInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.txt_app_name);
        setContentView(R.layout.activity_main);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (txtDnsState != null) {
                    txtDnsState.setText(getContext().getString(R.string.txt_dns_state_details_text, getPDNSState(), getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER)));
                }
                if (btnTrustWiFi != null && txtSsidName != null) {
                    String ssidName = getWifiSsidName();
                    btnTrustWiFi.setEnabled(ConnectionType.WIFI.equals(getConnectionType()) && trustedWiFiModeOn());
                    int colorCode = getWifiSsidColorCode(ssidName);
                    if (ConnectionType.WIFI.equals(getConnectionType())) {
                        btnTrustWiFi.setText(colorCode == Color.RED ? getContext().getString(R.string.btn_trust_ssid) : getContext().getString(R.string.btn_untrust_ssid));
                        txtSsidName.setText(ssidName);
                        txtSsidName.setTextColor(colorCode);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        initAll();
        IntentFilter filter = new IntentFilter(PDNS_STATE_CHANGED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, filter);
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (LOCATION_PERMISSION_REQUEST_CODE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handlePermissionsGranted(true);
                checkBackgroundLocation();
            } else {
                handlePermissionsGranted(false);
            }
        }
        if (BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE == requestCode) {
            handlePermissionsGranted(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void initAll() {
        activityInitializationStarted();

//        initReceivers();
        initButtons();
        initTexts();
        updateTexts();
        initCheckboxes();
        initSensitiveControls();

        activityInitializationCompleted();
    }

    private void initTexts() {
        if (txtDnsState == null) {
            txtDnsState = this.findViewById(R.id.txtDnsState);
        }
        if (txtCopyrights == null) {
            txtCopyrights = this.findViewById(R.id.txtCopyrights);
        }
        if (editTxtDnsHost == null) {
            editTxtDnsHost = this.findViewById(R.id.editTxtDsnHost);
            editTxtDnsHost.setText(getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
        }
    }

    private void updateTexts() {
        txtDnsState.setText(getString(R.string.txt_dns_state_details_text, getPDNSState(), getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER)));

        try {
            txtCopyrights.setText(getString(R.string.txt_copyrights,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (Exception ignored) {
        }
    }

    private void initCheckboxes() {
        if (cbDisableForVpn == null) {
            cbDisableForVpn = this.findViewById(R.id.cbDisableForVpn);

            cbDisableForVpn.setChecked(getSharedPrefs().getBoolean(getString(R.string.settings_name_disable_while_vpn), false));
            cbDisableForVpn = this.findViewById(R.id.cbDisableForVpn);
            cbDisableForVpn.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    SharedPreferences.Editor editor = getSharedPrefsEditor();
                    editor.putBoolean(getString(R.string.settings_name_disable_while_vpn), checked);
                    editor.apply();
                }
            });
        }
        if (cbEnableForCellular == null) {
            cbEnableForCellular = this.findViewById(R.id.cbEnableForCellular);

            cbEnableForCellular.setChecked(getSharedPrefs().getBoolean(getString(R.string.settings_name_enable_while_cellular), false));
            cbEnableForCellular = this.findViewById(R.id.cbEnableForCellular);
            cbEnableForCellular.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    SharedPreferences.Editor editor = getSharedPrefsEditor();
                    editor.putBoolean(getString(R.string.settings_name_enable_while_cellular), checked);
                    editor.apply();
                }
            });
        }
    }

    private void initButtons() {
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
                showWarning(getString(R.string.txt_dns_set_host_notification, dnsUrl));
                updateTexts();
                editTxtDnsHost.clearFocus();
                editTxtDnsHost.setText(getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
            });
        }
        if (btnInstructions == null) {
            btnInstructions = this.findViewById(R.id.btnInstructions);

            btnInstructions.setOnClickListener(v -> {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle(getString(R.string.txt_instructions_title));
                alert.setMessage(R.string.txt_instructions);
                alert.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                alert.show();
            });
        }
        if (btnPermissions == null) {
            btnPermissions = this.findViewById(R.id.btnPermissions);

            btnPermissions.setOnClickListener(v -> {
                checkPermissions();
            });
        }
    }

    public void initSensitiveControls() {
        if (btnTrustWiFi == null) {
            btnTrustWiFi = this.findViewById(R.id.btnTrustWiFi);

            if (isLocationPermissionsGranted()) {
                btnTrustWiFi.setVisibility(View.VISIBLE);
                btnTrustWiFi.setActivated(ConnectionType.WIFI.equals(getConnectionType()));

                SharedPreferences sharedPrefForInit = getSharedPrefs();
                btnTrustWiFi.setEnabled(ConnectionType.WIFI.equals(getConnectionType()) && sharedPrefForInit.getBoolean(getString(R.string.settings_name_trust_wifi), false));
                btnTrustWiFi.setText(getWifiSsidColorCode(getWifiSsidName()) == Color.RED ? getContext().getString(R.string.btn_trust_ssid) : getContext().getString(R.string.btn_untrust_ssid));
                btnTrustWiFi.setOnClickListener(v -> {
                    String ssidName = getWifiSsidName();
                    int colorCode = trustUntrustSsidName(ssidName);
                    btnTrustWiFi.setText(colorCode == Color.RED ? getContext().getString(R.string.btn_trust_ssid) : getContext().getString(R.string.btn_untrust_ssid));
                    txtSsidName.setTextColor(colorCode);
                    updatePdnsSettingsOnNetworkChange(null);
                });
            } else {
                btnTrustWiFi.setVisibility(View.INVISIBLE);
            }
        }
        if (cbTrustWiFi == null) {
            cbTrustWiFi = this.findViewById(R.id.cbTrustWiFi);

            if (isLocationPermissionsGranted()) {
                cbTrustWiFi.setVisibility(View.VISIBLE);
                cbTrustWiFi.setEnabled(isAllNeededLocationPermissionsGranted());
                cbTrustWiFi.setChecked(getSharedPrefs().getBoolean(getString(R.string.settings_name_trust_wifi), false));

                cbTrustWiFi.setOnCheckedChangeListener((compoundButton, checked) -> {
                    if (!activityInitInProgress) {
                        SharedPreferences.Editor editor = getSharedPrefsEditor();
                        editor.putBoolean(getString(R.string.settings_name_trust_wifi), checked);
                        editor.apply();
                        btnTrustWiFi.setEnabled(checked && ConnectionType.WIFI.equals(getConnectionType()));
                    }
                });
            } else {
                cbTrustWiFi.setVisibility(View.INVISIBLE);
            }
        }
        if (txtSsidName == null) {
            txtSsidName = this.findViewById(R.id.txtSsidName);

            if (isLocationPermissionsGranted()) {
                txtSsidName.setVisibility(View.VISIBLE);
                String ssidName = getWifiSsidName();
                txtSsidName.setText(ssidName);
                txtSsidName.setTextColor(getWifiSsidColorCode(ssidName));
            } else {
                txtSsidName.setVisibility(View.INVISIBLE);
            }
        }

    }

    private void activityInitializationStarted() {
        activityInitInProgress = true;
    }

    private void activityInitializationCompleted() {
        activityInitInProgress = false;
    }

    private void releaseResources() {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            // Location permission already granted
            checkBackgroundLocation();
        } else {
            // Location permission not granted, request it
            showLocationPermissionDialog();
        }
    }

    private void showLocationPermissionDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder
                .setCancelable(false)
                .setTitle(R.string.txt_location_disclosure_agreement_title)
                .setMessage(R.string.txt_location_disclosure_agreement)
                .setPositiveButton("Grant Permissions", (dialog, which) -> requestLocationPermission())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    handlePermissionsGranted(false);
                    dialog.dismiss();
                })
                .show();
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    private void checkBackgroundLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestBackgroundLocationPermission();
        } else {
            handlePermissionsGranted(true);
        }
    }

    private void requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    private void handlePermissionsGranted(boolean granted) {
        getSharedPrefsEditor().putBoolean(getString(R.string.settings_location_permissions_granted), granted).apply();
    }
}
