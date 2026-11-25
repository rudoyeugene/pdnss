package com.rudyii.pdnss.activities;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.MAX_ACTION_DELAY_MS;
import static com.rudyii.pdnss.types.Constants.MIN_ACTION_DELAY_MS;
import static com.rudyii.pdnss.types.Constants.SETTINGS_ACTION_DELAY_MS;
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
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.text.Html;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;
import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.types.ConnectionType;
import com.rudyii.pdnss.utils.PdnsPolicyScheduler;

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
    private MaterialTextView scorePercentText;
    private MaterialTextView activeDnsHostText;
    private MaterialTextView sliderValueText;
    private Slider slider;
    private MaterialButton btnInstructions;
    private MaterialButton btnPermissions;
    private MaterialButton btnUpdatePdns;
    private MaterialButton btnApList;
    private MaterialSwitch switchTrustAp;
    private MaterialSwitch switchDisableForVpn;
    private MaterialSwitch switchEnableForCellular;
    private MaterialSwitch switchTrustWiFiMode;
    private RadioGroup selector;
    private boolean activityInitInProgress;
    private boolean skipRadioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.txt_app_name);
        setContentView(R.layout.activity_main);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                renderNetworkAndDnsState();
            }
        };
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        getAppContext().registerNetworkCallback();
        initAll();
        refreshControlsAvailability();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, new IntentFilter(APP_NAME));
        getAppContext().getAppState().getPdnsState().observe(this, s -> renderNetworkAndDnsState());
        renderNetworkAndDnsState();
        getAppContext().getSettingsUtils().syncExternalStateChanges();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseResources();
    }

    private void refreshControlsAvailability() {
        boolean hasWriteSecure = getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted();
        boolean hasAllLocation = getAppContext().getPermissionsUtils().isAllNeededLocationPermissionsGranted();
        boolean trustModeEnabled = getAppContext().getSettingsUtils().trustedWiFiModeOn();
        boolean isWifi = ConnectionType.WIFI.equals(getAppContext().getSettingsUtils().getConnectionType());

        if (btnInstructions != null) {
            btnInstructions.setEnabled(!hasWriteSecure);
        }
        if (btnPermissions != null) {
            btnPermissions.setEnabled(!hasAllLocation);
        }
        if (btnApList != null) {
            btnApList.setEnabled(hasAllLocation);
            btnApList.setActivated(isWifi);
        }
        if (selector != null) {
            selector.setEnabled(hasWriteSecure);
        }
        if (switchDisableForVpn != null) {
            switchDisableForVpn.setEnabled(hasWriteSecure);
        }
        if (switchEnableForCellular != null) {
            switchEnableForCellular.setEnabled(hasWriteSecure);
        }
        if (switchTrustWiFiMode != null) {
            switchTrustWiFiMode.setEnabled(hasAllLocation);
        }
        if (switchTrustAp != null) {
            switchTrustAp.setActivated(isWifi);
            switchTrustAp.setEnabled(hasAllLocation && isWifi && trustModeEnabled);
        }
    }

    private void renderNetworkAndDnsState() {
        refreshControlsAvailability();

        if (activeDnsHostText != null) {
            String specifier = getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER);
            String modeName = getAppContext().getSettingsUtils().getPDNSState().name();
            activeDnsHostText.setText(modeName + " • " + specifier);
        }

        if (switchTrustAp != null) {
            String apName = getAppContext().getSettingsUtils().getWifiApName();
            switchTrustAp.setChecked(getAppContext().getSettingsUtils().itTrustedWiFiAp(apName));
            switch (getAppContext().getSettingsUtils().getConnectionType()) {
                case WIFI ->
                        switchTrustAp.setText(getString(R.string.txt_connected_ap_name, apName));
                case CELLULAR ->
                        switchTrustAp.setText(getString(R.string.txt_trusted_ap_mode_unsupported,
                                getString(R.string.txt_notification_state_body_on_cellular)));
                case VPN ->
                        switchTrustAp.setText(getString(R.string.txt_trusted_ap_mode_unsupported,
                                getString(R.string.txt_notification_state_body_on_vpn)));
                case UNKNOWN -> switchTrustAp.setText(getString(R.string.txt_ap_name));
            }
        }
        updateRadioButtonsStates();
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
        refreshControlsAvailability();
        renderNetworkAndDnsState();
    }

    private void initAll() {
        activityInitializationStarted();

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
        if (activeDnsHostText == null) {
            activeDnsHostText = this.findViewById(R.id.activeDnsHostText);
        }
        if (scorePercentText == null) {
            scorePercentText = this.findViewById(R.id.scorePercentText);
        }
        if (score == null) {
            score = this.findViewById(R.id.score);
            try {
                int safeScore = (int) getAppContext().getSecurityScoreUtil().getSecurityScore();
                animateProgressBar(safeScore);
            } catch (Exception ignored) {
            }
        }
        if (sliderValueText == null) {
            sliderValueText = findViewById(R.id.sliderValueText);
            int currentDelay = PdnsPolicyScheduler.getConfiguredDelayMs(this);
            sliderValueText.setText(String.valueOf(currentDelay));
        }
    }

    private void initCheckboxes() {
        if (switchDisableForVpn == null) {
            switchDisableForVpn = this.findViewById(R.id.switchDisableForVpn);
            switchDisableForVpn.setEnabled(getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted());
            switchDisableForVpn.setChecked(getAppContext().getSettingsUtils().getSharedPrefs()
                    .getBoolean(getString(R.string.settings_name_disable_while_vpn), false));
            switchDisableForVpn.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    compoundButton.performHapticFeedback(VibrationEffect.EFFECT_CLICK);

                    SharedPreferences.Editor editor = getAppContext().getSettingsUtils().getSharedPrefsEditor();
                    editor.putBoolean(getString(R.string.settings_name_disable_while_vpn), checked);
                    editor.apply();
                }
            });
        }
        if (switchEnableForCellular == null) {
            switchEnableForCellular = this.findViewById(R.id.switchEnableForCellular);
            switchEnableForCellular.setEnabled(getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted());
            switchEnableForCellular.setChecked(getAppContext().getSettingsUtils().getSharedPrefs()
                    .getBoolean(getString(R.string.settings_name_enable_while_cellular), false));
            switchEnableForCellular.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (!activityInitInProgress) {
                    compoundButton.performHapticFeedback(VibrationEffect.EFFECT_CLICK);

                    SharedPreferences.Editor editor = getAppContext().getSettingsUtils().getSharedPrefsEditor();
                    editor.putBoolean(getString(R.string.settings_name_enable_while_cellular), checked);
                    editor.apply();
                }
            });
        }
    }

    private boolean isIgnoringBatteryOptimizations() {
        return ((PowerManager) getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName());
    }

    private void initButtons() {
        if (selector == null) {
            selector = this.findViewById(R.id.selector);
            updateRadioButtonsStates();

            selector.setEnabled(getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted());
            selector.setOnCheckedChangeListener((group, checkedId) -> {
                if (!skipRadioButton) {
                    selector.performHapticFeedback(VibrationEffect.EFFECT_CLICK);
                    if (checkedId == R.id.radioOff) {
                        if (getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted()) {
                            getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                            getAppContext().getSettingsUtils().updateLastPdnsState(OFF);
                            updateRadioButtonsStates();
                            getAppContext().refreshQuickTile();
                            getAppContext().getSecurityScoreUtil().disabled();
                            getAppContext().getAppState().updatePdnsState(SystemClock.elapsedRealtime());
                        }
                    } else if (checkedId == R.id.radioOn) {
                        if (getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted()) {
                            getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                            getAppContext().getSettingsUtils().updateLastPdnsState(ON);
                            updateRadioButtonsStates();
                            getAppContext().refreshQuickTile();
                            getAppContext().getSecurityScoreUtil().enabled();
                            getAppContext().getAppState().updatePdnsState(SystemClock.elapsedRealtime());
                        }
                    } else {
                        if (getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted()) {
                            getAppContext().getSettingsUtils().updatePdnsModeSettings(PRIVATE_DNS_MODE_OPPORTUNISTIC);
                            getAppContext().getSettingsUtils().updateLastPdnsState(GOOGLE);
                            updateRadioButtonsStates();
                            getAppContext().refreshQuickTile();
                            getAppContext().getSecurityScoreUtil().disabled();
                            getAppContext().getAppState().updatePdnsState(SystemClock.elapsedRealtime());
                        }
                    }
                }
            });
        }
        if (btnInstructions == null) {
            btnInstructions = this.findViewById(R.id.btnInstructions);

            btnInstructions.setEnabled(!getAppContext().getPermissionsUtils().isWriteSecureSettingsPermissionGranted());

            btnInstructions.setOnClickListener(v -> {
                v.performHapticFeedback(VibrationEffect.EFFECT_CLICK);
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle(getString(R.string.txt_instructions_title));
                alert.setMessage(Html.fromHtml(getString(R.string.txt_instructions), Html.FROM_HTML_MODE_LEGACY));
                alert.setPositiveButton(getString(R.string.txt_ok), (dialog, which) -> dialog.dismiss());
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.txt_update_pdns_host));
                    builder.setView(editTxtDnsHost);
                    builder.setPositiveButton(getText(R.string.txt_save), (dialogInterface, i) -> {
                        String dnsUrl = editTxtDnsHost.getText().toString().trim();
                        getAppContext().getSettingsUtils().updatePdnsUrl(dnsUrl);
                        getAppContext().getNotificationsUtils().showWarning(getString(R.string.txt_dns_set_host_notification, dnsUrl));
                        editTxtDnsHost.clearFocus();
                        editTxtDnsHost.setText(getAppContext().getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER));
                        getAppContext().refreshQuickTile();
                        renderNetworkAndDnsState();
                    });
                    builder.setNegativeButton(getText(R.string.txt_cancel), (dialogInterface, i) -> dialogInterface.cancel());
                    builder.show();
                });
            } else {
                btnUpdatePdns.setOnClickListener(v -> {
                    v.performHapticFeedback(VibrationEffect.EFFECT_TICK);
                    getAppContext().getNotificationsUtils().showWarning(getString(R.string.txt_missing_permissions_warning));
                });
            }
        }
        if (slider == null) {
            slider = findViewById(R.id.delaySlider);
            slider.setValueFrom(MIN_ACTION_DELAY_MS);
            slider.setValueTo(MAX_ACTION_DELAY_MS);
            if (slider.getStepSize() <= 0f) {
                slider.setStepSize(1000f);
            }

            int clampedDelay = PdnsPolicyScheduler.getConfiguredDelayMs(this);
            float step = slider.getStepSize();
            int snappedDelay = Math.round(clampedDelay / step) * (int) step;
            snappedDelay = PdnsPolicyScheduler.clampDelayMs(snappedDelay);

            getAppContext().getSettingsUtils().getSharedPrefs()
                    .edit()
                    .putInt(SETTINGS_ACTION_DELAY_MS, snappedDelay)
                    .apply();

            slider.setValue(snappedDelay);
            slider.addOnChangeListener((s, value, fromUser) -> {
                if (sliderValueText != null) {
                    sliderValueText.setText(String.valueOf((int) value));
                }
            });
            slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider s) {
                    s.performHapticFeedback(HapticFeedbackConstants.GESTURE_START);
                }

                @Override
                public void onStopTrackingTouch(@NonNull Slider s) {
                    s.performHapticFeedback(HapticFeedbackConstants.GESTURE_END);
                    int finalValue = PdnsPolicyScheduler.clampDelayMs((int) s.getValue());
                    getAppContext().getSettingsUtils().getSharedPrefs()
                            .edit()
                            .putInt(SETTINGS_ACTION_DELAY_MS, finalValue)
                            .apply();

                    Log.i(APP_NAME, "Delay saved: " + finalValue + " ms");
                }
            });
        }
    }

    private void updateRadioButtonsStates() {
        if (selector != null) {
            skipRadioButton = true;
            switch (getAppContext().getSettingsUtils().getPDNSState()) {
                case ON:
                    selector.check(R.id.radioOn);
                    break;
                case OFF:
                    selector.check(R.id.radioOff);
                    break;
                default:
                    selector.check(R.id.radioAuto);
            }
            skipRadioButton = false;
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

                        SharedPreferences.Editor editor = getAppContext().getSettingsUtils().getSharedPrefsEditor();
                        editor.putBoolean(getString(R.string.settings_name_trust_wifi), checked);
                        editor.apply();
                        if (switchTrustAp != null) {
                            switchTrustAp.setEnabled(checked && ConnectionType.WIFI.equals(getAppContext().getSettingsUtils().getConnectionType()));
                        }
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
                switchTrustAp.setEnabled(ConnectionType.WIFI.equals(getAppContext().getSettingsUtils().getConnectionType())
                        && sharedPrefForInit.getBoolean(getString(R.string.settings_name_trust_wifi), false));
                switchTrustAp.setText(getString(R.string.txt_connected_ap_name, apName));
                switchTrustAp.setOnClickListener(v -> {
                    v.performHapticFeedback(VibrationEffect.EFFECT_CLICK);

                    String currentApName = getAppContext().getSettingsUtils().getWifiApName();
                    boolean trustResult = getAppContext().getSettingsUtils().trustUntrustApByName(currentApName);
                    getAppContext().getNotificationsUtils().showWarning(trustResult
                            ? getString(R.string.txt_connected_ap_trusted, currentApName)
                            : getString(R.string.txt_connected_ap_untrusted, currentApName));
                    getAppContext().getSettingsUtils().updatePdnsSettingsOnNetworkChange(null);
                    renderNetworkAndDnsState();
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
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.txt_click_to_remove_ap));
                        String[] apsSimple = apsCopy.toArray(new String[0]);
                        builder.setItems(apsSimple, (dialog, which) -> {
                            apsCopy.remove(apsSimple[which]);
                            SharedPreferences.Editor editor = getAppContext().getSettingsUtils().getSharedPrefsEditor();
                            editor.putStringSet(getString(R.string.settings_name_trust_wifi_ap_set), apsCopy);
                            editor.apply();
                            getAppContext().getSettingsUtils().updatePdnsSettingsOnNetworkChange(null);
                            renderNetworkAndDnsState();
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
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkBackgroundLocation();
        } else {
            showLocationPermissionDialog();
        }
    }

    private void showLocationPermissionDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder
                .setCancelable(false)
                .setTitle(R.string.txt_location_disclosure_title)
                .setMessage(Html.fromHtml(getString(R.string.txt_location_disclosure_agreement), Html.FROM_HTML_MODE_LEGACY))
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
        if (scorePercentText != null) {
            scorePercentText.setText(targetProgress + "%");
        }
        ValueAnimator animator = ValueAnimator.ofInt(score.getProgress(), targetProgress);
        animator.setDuration(1500L);
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