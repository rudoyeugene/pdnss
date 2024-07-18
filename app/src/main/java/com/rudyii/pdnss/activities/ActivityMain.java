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
import static com.rudyii.pdnss.common.Utils.getWifiApName;
import static com.rudyii.pdnss.common.Utils.isAllNeededLocationPermissionsGranted;
import static com.rudyii.pdnss.common.Utils.isLocationPermissionsGranted;
import static com.rudyii.pdnss.common.Utils.itTrustedWiFiAp;
import static com.rudyii.pdnss.common.Utils.showWarning;
import static com.rudyii.pdnss.common.Utils.trustUntrustApByName;
import static com.rudyii.pdnss.common.Utils.trustedWiFiModeOn;
import static com.rudyii.pdnss.common.Utils.updateLastPdnsState;
import static com.rudyii.pdnss.common.Utils.updatePdnsModeSettings;
import static com.rudyii.pdnss.common.Utils.updatePdnsSettingsOnNetworkChange;
import static com.rudyii.pdnss.common.Utils.updatePdnsUrl;
import static com.rudyii.pdnss.services.QuickTile.refreshQsTile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ActivityMain extends AppCompatActivity {
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1122;
    public static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1133;
    private BroadcastReceiver broadcastReceiver;
    private TextView txtDnsState;
    private TextView txtCopyrights;
    private Button btnOn;
    private Button btnOff;
    private Button btnGoogle;
    private Button btnSet;
    private Button btnInstructions;
    private Button btnPermissions;
    private Button btnApList;
    private MaterialSwitch swchTrustAp;
    private MaterialSwitch swchDisableForVpn;
    private MaterialSwitch swchEnableForCellular;
    private MaterialSwitch swchTrustWiFi;
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
                if (swchTrustAp != null) {
                    String apName = getWifiApName();
                    swchTrustAp.setEnabled(ConnectionType.WIFI.equals(getConnectionType()) && trustedWiFiModeOn());
                    swchTrustAp.setChecked(itTrustedWiFiAp(apName));
                    if (ConnectionType.WIFI.equals(getConnectionType())) {
                        swchTrustAp.setText(getString(R.string.txt_connected_ap_name, apName));
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
        if (swchDisableForVpn == null) {
            swchDisableForVpn = this.findViewById(R.id.cbDisableForVpn);

            swchDisableForVpn.setChecked(getSharedPrefs().getBoolean(getString(R.string.settings_name_disable_while_vpn), false));
            swchDisableForVpn = this.findViewById(R.id.cbDisableForVpn);
            swchDisableForVpn.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    showDozeModeWarning();

                    SharedPreferences.Editor editor = getSharedPrefsEditor();
                    editor.putBoolean(getString(R.string.settings_name_disable_while_vpn), checked);
                    editor.apply();
                }
            });
        }
        if (swchEnableForCellular == null) {
            swchEnableForCellular = this.findViewById(R.id.cbEnableForCellular);

            swchEnableForCellular.setChecked(getSharedPrefs().getBoolean(getString(R.string.settings_name_enable_while_cellular), false));
            swchEnableForCellular = this.findViewById(R.id.cbEnableForCellular);
            swchEnableForCellular.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    showDozeModeWarning();

                    SharedPreferences.Editor editor = getSharedPrefsEditor();
                    editor.putBoolean(getString(R.string.settings_name_enable_while_cellular), checked);
                    editor.apply();
                }
            });
        }
    }

    private void showDozeModeWarning() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            showWarning(getString(R.string.txt_battery_optimization_enabled));
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
                alert.setPositiveButton(getString(R.string.txt_ok), (dialog, which) -> dialog.dismiss());
                alert.setNeutralButton(getString(R.string.txt_disable_battery_optimizations), (dialog, which) -> {
                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                        startActivity(new Intent().setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    }
                });
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
        if (swchTrustWiFi == null) {
            swchTrustWiFi = this.findViewById(R.id.cbTrustWiFi);

            if (isLocationPermissionsGranted()) {
                swchTrustWiFi.setVisibility(View.VISIBLE);
                swchTrustWiFi.setEnabled(isAllNeededLocationPermissionsGranted());
                swchTrustWiFi.setChecked(getSharedPrefs().getBoolean(getString(R.string.settings_name_trust_wifi), false));

                swchTrustWiFi.setOnCheckedChangeListener((compoundButton, checked) -> {
                    if (!activityInitInProgress) {
                        showDozeModeWarning();

                        SharedPreferences.Editor editor = getSharedPrefsEditor();
                        editor.putBoolean(getString(R.string.settings_name_trust_wifi), checked);
                        editor.apply();
                        swchTrustAp.setEnabled(checked && ConnectionType.WIFI.equals(getConnectionType()));
                    }
                });
            } else {
                swchTrustWiFi.setVisibility(View.INVISIBLE);
            }
        }
        if (swchTrustAp == null) {
            swchTrustAp = this.findViewById(R.id.btnTrustWiFi);

            if (isLocationPermissionsGranted()) {
                swchTrustAp.setVisibility(View.VISIBLE);
                swchTrustAp.setActivated(ConnectionType.WIFI.equals(getConnectionType()));
                String apName = getWifiApName();
                swchTrustAp.setChecked(itTrustedWiFiAp(apName));

                SharedPreferences sharedPrefForInit = getSharedPrefs();
                swchTrustAp.setEnabled(ConnectionType.WIFI.equals(getConnectionType()) && sharedPrefForInit.getBoolean(getString(R.string.settings_name_trust_wifi), false));
                swchTrustAp.setText(getString(R.string.txt_connected_ap_name, apName));
                swchTrustAp.setOnClickListener(v -> {
                    showDozeModeWarning();

                    swchTrustAp.setText(getString(R.string.txt_connected_ap_name, apName));
                    boolean trustResult = trustUntrustApByName(apName);
                    swchTrustAp.setChecked(trustResult);
                    showWarning(trustResult ? getString(R.string.txt_connected_ap_trusted, apName) : getString(R.string.txt_connected_ap_untrusted, apName));
                    updatePdnsSettingsOnNetworkChange();
                });
            } else {
                swchTrustAp.setVisibility(View.INVISIBLE);
            }
        }
        if (btnApList == null) {
            btnApList = this.findViewById(R.id.btnApList);

            if (isLocationPermissionsGranted()) {
                btnApList.setVisibility(View.VISIBLE);
                btnApList.setActivated(ConnectionType.WIFI.equals(getConnectionType()));


                btnApList.setOnClickListener(v -> {
                    Set<String> apsCopy = new HashSet<>(getSharedPrefs().getStringSet(getContext().getString(R.string.settings_name_trust_wifi_ap_set), Collections.emptySet()));
                    if (apsCopy.isEmpty()) {
                        showWarning(getString(R.string.txt_empty_ap_list));
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.txt_click_to_remove_ap));
                        String[] apsSimple = apsCopy.toArray(new String[0]);
                        builder.setItems(apsSimple, (dialog, which) -> {
                            apsCopy.remove(apsSimple[which]);
                            SharedPreferences.Editor editor = getSharedPrefsEditor();
                            editor.putStringSet(getContext().getString(R.string.settings_name_trust_wifi_ap_set), apsCopy);
                            editor.apply();
                            updatePdnsSettingsOnNetworkChange();
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
            } else {
                btnApList.setVisibility(View.INVISIBLE);
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
                .setTitle(R.string.txt_location_disclosure_agreement)
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
