package com.rudyii.pdnss.activities;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.PDNS_STATE_CHANGED;
import static com.rudyii.pdnss.types.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.types.PdnsModeType.GOOGLE;
import static com.rudyii.pdnss.types.PdnsModeType.OFF;
import static com.rudyii.pdnss.types.PdnsModeType.ON;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import android.os.VibrationEffect;
import android.provider.Settings;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.services.NetworkMonitor;
import com.rudyii.pdnss.types.ConnectionType;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ActivityMain extends AppCompatActivity {
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1122;
    public static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1133;
    private BroadcastReceiver broadcastReceiver;
    private TextView txtCopyrights;
    private ProgressBar score;
    private Button btnSet;
    private Button btnInstructions;
    private Button btnPermissions;
    private Button btnUpdatePdns;
    private Button btnApList;
    private MaterialSwitch switchTrustAp;
    private MaterialSwitch switchDisableForVpn;
    private MaterialSwitch switchEnableForCellular;
    private MaterialSwitch switchTrustWiFiMode;
    private Slider slider;
    private boolean activityInitInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.txt_app_name);
        setContentView(R.layout.activity_main);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (switchTrustAp != null) {
                    String apName = getAppContext().getSettingsUtils().getWifiApName();
                    switchTrustAp.setEnabled(ConnectionType.WIFI.equals(getAppContext().getSettingsUtils().getConnectionType()) && getAppContext().getSettingsUtils().trustedWiFiModeOn());
                    switchTrustAp.setChecked(getAppContext().getSettingsUtils().itTrustedWiFiAp(apName));
                    if (ConnectionType.WIFI.equals(getAppContext().getSettingsUtils().getConnectionType())) {
                        switchTrustAp.setText(getString(R.string.txt_connected_ap_name, apName));
                    }
                }
            }
        };
        try {
            Intent service = new Intent(getApplicationContext(), NetworkMonitor.class);
            getApplicationContext().startForegroundService(service);
        } catch (Exception e) {
            Log.w(APP_NAME, "NetworkMonitor Service will start later...");
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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
        initCheckboxes();
        initSensitiveControls();

        activityInitializationCompleted();
    }

    private void initTexts() {
        if (txtCopyrights == null) {
            txtCopyrights = this.findViewById(R.id.txtCopyrights);
            try {
                txtCopyrights.setText(getString(R.string.txt_copyrights,
                        LocalDate.now().getYear(),
                        getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
            } catch (Exception ignored) {
            }
        }
        if (score == null) {
            score = this.findViewById(R.id.score);
            try {
                int safeScore = (int) getAppContext().getSecurityScoreUtil().getSecurityScore();
                animateProgressBar(safeScore);
            } catch (Exception ignored) {
            }
        }
    }

    private void initCheckboxes() {
        if (switchDisableForVpn == null) {
            switchDisableForVpn = this.findViewById(R.id.switchDisableForVpn);

            switchDisableForVpn.setEnabled(getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted());

            switchDisableForVpn.setChecked(getAppContext().getSettingsUtils().getSharedPrefs().getBoolean(getString(R.string.settings_name_disable_while_vpn), false));
            switchDisableForVpn = this.findViewById(R.id.switchDisableForVpn);
            switchDisableForVpn.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    compoundButton.performHapticFeedback(VibrationEffect.EFFECT_CLICK);
                    showDozeModeWarning();

                    SharedPreferences.Editor editor = getAppContext().getSettingsUtils().getSharedPrefsEditor();
                    editor.putBoolean(getString(R.string.settings_name_disable_while_vpn), checked);
                    editor.apply();
                }
            });
        }
        if (switchEnableForCellular == null) {
            switchEnableForCellular = this.findViewById(R.id.switchEnableForCellular);

            switchEnableForCellular.setEnabled(getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted());

            switchEnableForCellular.setChecked(getAppContext().getSettingsUtils().getSharedPrefs().getBoolean(getString(R.string.settings_name_enable_while_cellular), false));
            switchEnableForCellular = this.findViewById(R.id.switchEnableForCellular);
            switchEnableForCellular.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    compoundButton.performHapticFeedback(VibrationEffect.EFFECT_CLICK);
                    showDozeModeWarning();

                    SharedPreferences.Editor editor = getAppContext().getSettingsUtils().getSharedPrefsEditor();
                    editor.putBoolean(getString(R.string.settings_name_enable_while_cellular), checked);
                    editor.apply();
                }
            });
        }
    }

    private void showDozeModeWarning() {
        if (!isIgnoringBatteryOptimizations()) {
            getAppContext().getNotificationsUtils().showWarning(getString(R.string.txt_battery_optimization_enabled));
        }
    }

    private boolean isIgnoringBatteryOptimizations() {
        return ((PowerManager) getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName());
    }

    private void initButtons() {
        if (slider == null) {
            slider = this.findViewById(R.id.slider);

            slider.setEnabled(getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted());
            slider.setValue(getAppContext().getSettingsUtils().getPDNSStateInFloat());

            slider.setLabelFormatter(value -> {
                switch (String.valueOf(slider.getValue())) {
                    case "2.0":
                        return getString(R.string.txt_dns_state_google);
                    case "3.0":
                        return getString(R.string.txt_dns_state_on);
                    default:
                        return getString(R.string.txt_dns_state_off);
                }
            });

            slider.addOnChangeListener((slider1, value, fromUser) ->
                    slider.performHapticFeedback(VibrationEffect.EFFECT_CLICK));

            slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider slider) {

                }

                @Override
                public void onStopTrackingTouch(@NonNull Slider slider) {
                    switch (String.valueOf(slider.getValue())) {
                        case "1.0":
                            if (getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted()) {
                                getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                                getAppContext().getSettingsUtils().updateLastPdnsState(OFF);
                                updateControlButtonsStates();
                                getAppContext().refreshQuickTile();
                                getAppContext().getSecurityScoreUtil().disabled();
                            }
                            break;
                        case "2.0":
                            if (getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted()) {
                                getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                                getAppContext().getSettingsUtils().updateLastPdnsState(GOOGLE);
                                updateControlButtonsStates();
                                getAppContext().refreshQuickTile();
                                getAppContext().getSecurityScoreUtil().disabled();
                            }
                            break;
                        case "3.0":
                            if (getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted()) {
                                getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                                getAppContext().getSettingsUtils().updateLastPdnsState(ON);
                                updateControlButtonsStates();
                                getAppContext().refreshQuickTile();
                                getAppContext().getSecurityScoreUtil().enabled();
                            }
                            break;
                    }
                }
            });
        }
        if (btnInstructions == null) {
            btnInstructions = this.findViewById(R.id.btnInstructions);

            btnInstructions.setEnabled(!getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted() || !isIgnoringBatteryOptimizations());

            btnInstructions.setOnClickListener(v -> {
                v.performHapticFeedback(VibrationEffect.EFFECT_CLICK);
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

            btnPermissions.setEnabled(!getAppContext().getPermissionsUtils().isAllNeededLocationPermissionsGranted());
            btnPermissions.setOnClickListener(v -> {
                v.performHapticFeedback(VibrationEffect.EFFECT_TICK);
                checkPermissions();
            });
        }
        if (btnUpdatePdns == null) {
            btnUpdatePdns = this.findViewById(R.id.btnUpdatePdns);
            if (getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted()) {
                btnUpdatePdns.setOnClickListener(v -> {
                    v.performHapticFeedback(VibrationEffect.EFFECT_TICK);
                    EditText editTxtDnsHost = new EditText(this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    editTxtDnsHost.setLayoutParams(lp);
                    editTxtDnsHost.setText(getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, getAppContext().getSettingsUtils().isLightTheme()
                            ? android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert);
                    builder.setTitle(getString(R.string.txt_update_pdns_host));
                    builder.setView(editTxtDnsHost);
                    builder.setPositiveButton(getText(R.string.txt_save), (dialogInterface, i) -> {
                        String dnsUrl = editTxtDnsHost.getText().toString();
                        getAppContext().getSettingsUtils().updatePdnsUrl(dnsUrl);
                        getAppContext().getNotificationsUtils().showWarning(getString(R.string.txt_dns_set_host_notification, dnsUrl));
                        editTxtDnsHost.clearFocus();
                        editTxtDnsHost.setText(getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));

                    });
                    builder.setNegativeButton(getText(R.string.txt_cancel), (dialogInterface, i) -> {
                        dialogInterface.cancel();
                    });
                    builder.show();
                });
            } else {
                btnUpdatePdns.setOnClickListener(v -> {
                    v.performHapticFeedback(VibrationEffect.EFFECT_TICK);
                    getAppContext().getNotificationsUtils().showWarning(getString(R.string.txt_missing_permissions_warning));
                });
            }
        }
    }

    private void updateControlButtonsStates() {
        if (slider != null) {
            slider.setValue(getAppContext().getSettingsUtils().getPDNSStateInFloat());
        }
    }

    public void initSensitiveControls() {
        if (switchTrustWiFiMode == null) {
            switchTrustWiFiMode = this.findViewById(R.id.switchTrustWiFiMode);

            switchTrustWiFiMode.setEnabled(getAppContext().getPermissionsUtils().isAllNeededLocationPermissionsGranted());

            if (getAppContext().getPermissionsUtils().isAllNeededLocationPermissionsGranted()) {
                switchTrustWiFiMode.setChecked(getAppContext().getSettingsUtils().getSharedPrefs().getBoolean(getString(R.string.settings_name_trust_wifi), false));

                switchTrustWiFiMode.setOnCheckedChangeListener((compoundButton, checked) -> {
                    if (!activityInitInProgress) {
                        compoundButton.performHapticFeedback(VibrationEffect.EFFECT_CLICK);
                        showDozeModeWarning();

                        SharedPreferences.Editor editor = getAppContext().getSettingsUtils().getSharedPrefsEditor();
                        editor.putBoolean(getString(R.string.settings_name_trust_wifi), checked);
                        editor.apply();
                        switchTrustAp.setEnabled(checked && ConnectionType.WIFI.equals(getAppContext().getSettingsUtils().getConnectionType()));
                    }
                });
            }
        }
        if (switchTrustAp == null) {
            switchTrustAp = this.findViewById(R.id.switchTrustWiFi);

            switchTrustAp.setEnabled(getAppContext().getPermissionsUtils().isAllNeededLocationPermissionsGranted());

            if (getAppContext().getPermissionsUtils().isAllNeededLocationPermissionsGranted()) {
                switchTrustAp.setActivated(ConnectionType.WIFI.equals(getAppContext().getSettingsUtils().getConnectionType()));
                String apName = getAppContext().getSettingsUtils().getWifiApName();
                switchTrustAp.setChecked(getAppContext().getSettingsUtils().itTrustedWiFiAp(apName));

                SharedPreferences sharedPrefForInit = getAppContext().getSettingsUtils().getSharedPrefs();
                switchTrustAp.setEnabled(ConnectionType.WIFI.equals(getAppContext().getSettingsUtils().getConnectionType()) && sharedPrefForInit.getBoolean(getString(R.string.settings_name_trust_wifi), false));
                switchTrustAp.setText(getString(R.string.txt_connected_ap_name, apName));
                switchTrustAp.setOnClickListener(v -> {
                    v.performHapticFeedback(VibrationEffect.EFFECT_CLICK);
                    showDozeModeWarning();

                    switchTrustAp.setText(getString(R.string.txt_connected_ap_name, apName));
                    boolean trustResult = getAppContext().getSettingsUtils().trustUntrustApByName(apName);
                    switchTrustAp.setChecked(trustResult);
                    getAppContext().getNotificationsUtils().showWarning(trustResult ? getString(R.string.txt_connected_ap_trusted, apName) : getString(R.string.txt_connected_ap_untrusted, apName));
                    getAppContext().getSettingsUtils().updatePdnsSettingsOnNetworkChange(null);
                    slider.setValue(getAppContext().getSettingsUtils().getPDNSStateInFloat());
                });
            }
        }
        if (btnApList == null) {
            btnApList = this.findViewById(R.id.btnApList);

            btnApList.setEnabled(getAppContext().getPermissionsUtils().isAllNeededLocationPermissionsGranted());

            if (getAppContext().getPermissionsUtils().isLocationPermissionsGranted()) {
                btnApList.setActivated(ConnectionType.WIFI.equals(getAppContext().getSettingsUtils().getConnectionType()));

                btnApList.setOnClickListener(v -> {
                    v.performHapticFeedback(VibrationEffect.EFFECT_CLICK);
                    Set<String> apsCopy = new HashSet<>(getAppContext().getSettingsUtils().getSharedPrefs().getStringSet(getString(R.string.settings_name_trust_wifi_ap_set), Collections.emptySet()));
                    if (apsCopy.isEmpty()) {
                        getAppContext().getNotificationsUtils().showWarning(getString(R.string.txt_empty_ap_list));
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this, getAppContext().getSettingsUtils().isLightTheme()
                                ? android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert);
                        builder.setTitle(getString(R.string.txt_click_to_remove_ap));
                        String[] apsSimple = apsCopy.toArray(new String[0]);
                        builder.setItems(apsSimple, (dialog, which) -> {
                            apsCopy.remove(apsSimple[which]);
                            SharedPreferences.Editor editor = getAppContext().getSettingsUtils().getSharedPrefsEditor();
                            editor.putStringSet(getString(R.string.settings_name_trust_wifi_ap_set), apsCopy);
                            editor.apply();
                            getAppContext().getSettingsUtils().updatePdnsSettingsOnNetworkChange(null);
                            slider.setValue(getAppContext().getSettingsUtils().getPDNSStateInFloat());
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
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
        getAppContext().getSettingsUtils().getSharedPrefsEditor().putBoolean(getString(R.string.settings_location_permissions_granted), granted).apply();
    }

    private void animateProgressBar(int targetProgress) {
        ValueAnimator animator = ValueAnimator.ofInt(score.getProgress(), targetProgress);
        animator.setDuration(3000L);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            score.setProgress(animatedValue);
        });
        animator.start();
    }

    private PrivateDnsSwitcherApplication getAppContext() {
        return (PrivateDnsSwitcherApplication) getApplicationContext();
    }
}
