package com.zebra.proximityscanservice;

public class Constants {
    public static final String TAG  ="ForegroundService";

    // Shared preference keys
    public static final String SHARED_PREFERENCES_NAME = "ForegroundService";
    public static final String SHARED_PREFERENCES_START_SERVICE_ON_BOOT = "startonboot";
    public static final String SHARED_PREFERENCES_START_SERVICE_ON_CHARGING = "startoncharging";
    public static final String SHARED_PREFERENCES_SENSORNAME = "sensorname";
    public static final String SHARED_PREFERENCES_PROXIMITYMINDISTANCE = "proximitymindistance";
    public static final int SHARED_PREFERENCES_PROXIMITYMINDISTANCE_DEFAULTVALUE = 700;
    public static final String SHARED_PREFERENCES_UNSELECTED = "unselected";
    public static final String SHARED_PREFERENCES_TRIGGER_TYPE = "triggertype";
    public static final String SHARED_PREFERENCES_TRIGGER_TYPE_SUPERIOR = "superior";
    public static final String SHARED_PREFERENCES_TRIGGER_TYPE_INFERIOR = "inferior";
    public static final String SHARED_PREFERENCES_ACTION_TYPE = "triggertype";
    public static final String SHARED_PREFERENCES_ACTION_TYPE_TRIGGER_START_STOP = "triggerstartstop";
    public static final String SHARED_PREFERENCES_ACTION_TYPE_ENABLE_DISABLE_DATAWEDGE = "enabledisabledw";

    public static final String EXTRA_CONFIGURATION_START_ON_BOOT = "startonboot";
    public static final String EXTRA_CONFIGURATION_START_ON_CHARGING = "startoncharging";

    public static final String ZEBRA_PROXIMITY_SENSOR_TYPE = "com.symbol.sensor.longdistanceprox";

}
