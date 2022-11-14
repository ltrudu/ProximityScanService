package com.zebra.proximityscanservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;

import java.util.List;

import static android.content.Context.SENSOR_SERVICE;

public class ProximitySensorModule {

    private Context context = null;

    private SensorManager sensorManager = null;
    private Sensor proximitySensor = null;
    private SensorEventCallback sensorEventCallback = null;
    private ProximitySensorModuleCallback proximitySensorModuleCallback = null;
    protected static ProximitySensorModuleCallback debugInterfaceCallback = null;

    private float sensorMaximumRange = 1.0f;

    private String sensorName = "";

    public interface ProximitySensorModuleCallback
    {
        void onDistance(float distance, float sensorMaximumRange);
    }

    public ProximitySensorModule(Context context, String sensorName, ProximitySensorModuleCallback proximitySensorModuleCallback)
    {
        this.context = context;
        this.sensorName = sensorName;
        this.proximitySensorModuleCallback = proximitySensorModuleCallback;
        initialize();
    }

    public void initialize()
    {
        // Initialize functional things here
        if(sensorManager == null)
            sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);

        if(sensorManager != null){

            if(proximitySensor == null) {
                proximitySensor = getSensor();
            }

            if (proximitySensor != null) {

                sensorMaximumRange = proximitySensor.getMaximumRange();

                if (sensorEventCallback == null) {

                    sensorEventCallback = new SensorEventCallback() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            super.onSensorChanged(event);
                            processSensorEvent(event);
                        }
                    };
                }
            }
        }

    }

    public void start()
    {
        if(sensorManager != null && sensorEventCallback != null && proximitySensor != null)
            sensorManager.registerListener(sensorEventCallback, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        if(sensorManager != null && sensorEventCallback != null)
        {
            sensorManager.unregisterListener(sensorEventCallback);
            proximitySensor = null;
            sensorManager = null;
        }
    }

    public static int getSensorMaxRange(Context context, String sensorName)
    {
        // Initialize functional things here
        SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_PROXIMITY);
        for(Sensor sensor : list)
        {
            if(sensor.getName().equalsIgnoreCase(sensorName))
            {
                return (int)sensor.getMaximumRange();
            }
        }
        return Integer.MAX_VALUE;
    }

    private Sensor getSensor()
    {
        List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_ALL);
        SharedPreferences sharedpreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String sensorName = sharedpreferences.getString(Constants.SHARED_PREFERENCES_SENSORNAME, Constants.SHARED_PREFERENCES_UNSELECTED);

        for(Sensor sensor : list)
        {
            if(sensor.getName().equalsIgnoreCase(sensorName))
            {
                return sensor;
            }
        }
        return list.get(0);
    }

    private void processSensorEvent(SensorEvent event) {
        float distance = event.values[0];
        LogHelper.logD("Distance: " + distance);

        if(proximitySensorModuleCallback != null)
        {
            proximitySensorModuleCallback.onDistance(distance, sensorMaximumRange);
        }
        if(debugInterfaceCallback != null)
        {
            debugInterfaceCallback.onDistance(distance, sensorMaximumRange);
        }
    }

    public float getSensorMaximumRange()
    {
        return sensorMaximumRange;
    }

    public String getSensorName()
    {
        return sensorName;
    }

}
