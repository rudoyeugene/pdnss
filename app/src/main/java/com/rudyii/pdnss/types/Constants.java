package com.rudyii.pdnss.types;

public class Constants {
    public static final String APP_NAME = "com.rudyii.pdnss";
    public static final String SETTINGS_PRIVATE_DNS_MODE = "private_dns_mode";
    public static final String VALUE_PRIVATE_DNS_MODE_OFF_STRING = "off";
    public static final String VALUE_PRIVATE_DNS_MODE_GOOGLE_STRING = "opportunistic";
    public static final String VALUE_PRIVATE_DNS_MODE_ON_STRING = "hostname";
    public static final String SETTINGS_PRIVATE_DNS_SPECIFIER = "private_dns_specifier";
    public static final String SETTINGS_ACTION_DELAY_MS = "action_delay_ms";
    public static final int MIN_ACTION_DELAY_MS = 0;
    public static final int MAX_ACTION_DELAY_MS = 10000;
    public static final int DEFAULT_ACTION_DELAY_MS = 0;
    public static final String PDNSS_GOOGLE = "com.rudyii.pdnss.GOOGLE";
    public static final String PDNSS_ON = "com.rudyii.pdnss.ON";
    public static final String PDNSS_OFF = "com.rudyii.pdnss.OFF";
    public static final String QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON";
    public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String STATE_NOTIFICATION_NAME = "STATE_NOTIFICATIONS";
    public static final String UNSAFE_DNS_NOTIFICATION_NAME = "UNSAFE_DNS_NOTIFICATIONS";
    public static final int STATE_NOTIFICATION_ID = 112211;
    public static final String WORK_NAME_DNS_EVALUATION = "pdnss_dns_evaluation_work";
    public static final String WORK_DATA_DELAY_MS = "work_data_delay_ms";
    public static final String WORK_DATA_REASON = "work_data_reason";
}