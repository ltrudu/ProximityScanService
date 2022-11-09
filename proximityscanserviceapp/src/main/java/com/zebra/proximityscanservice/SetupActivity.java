package com.zebra.proximityscanservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class SetupActivity extends AppCompatActivity implements ProximitySensorModule.ProximitySensorModuleCallback, DWDistanceScanTrigger.DWDistanceScanTriggerDebugInterface {

    private Switch mStartStopServiceSwitch = null;
    private Switch mAutoStartServiceOnBootSwitch = null;
    private Switch mAutoStartServiceOnPowerEventSwitch = null;

    private TextView mMinDistanceTextView = null;
    private SeekBar mMinDistanceSeekBar = null;
    private TextView mCurrentDistanceTextView = null;
    private SeekBar mCurrentDistanceSeekBar = null;
    private TextView mProximityStatus = null;
    private Spinner mSensorTypeSpinner = null;
    private ArrayAdapter mAdapter = null;
    private Map<String, Integer> mSensorNames = null;
    private int mSeekBarsMaxRange = 1000;

    protected static SetupActivity mSetupActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSetupActivity = this;

        setContentView(R.layout.activity_setup);

        mAutoStartServiceOnBootSwitch = (Switch) findViewById(R.id.swStartOnBootSwitch);
        mAutoStartServiceOnPowerEventSwitch = (Switch) findViewById(R.id.swStartOnCraddle);
        mStartStopServiceSwitch = (Switch)findViewById(R.id.swStartStopServiceSwitch);

        mMinDistanceTextView = (TextView)findViewById(R.id.txtProximityMinDistance);
        mCurrentDistanceTextView = (TextView)findViewById(R.id.txtProximityCurrentDistance);
        mProximityStatus = (TextView)findViewById(R.id.txtProximityStatus);

        mMinDistanceSeekBar = (SeekBar)findViewById(R.id.sbMinDistance);
        mCurrentDistanceSeekBar = (SeekBar)findViewById(R.id.sbCurrentDistance);

        mMinDistanceTextView = (TextView)findViewById(R.id.txtProximityMinDistance);
        mCurrentDistanceTextView = (TextView)findViewById(R.id.txtProximityCurrentDistance);

        mProximityStatus = (TextView)findViewById(R.id.txtProximityStatus);

        mSensorTypeSpinner = (Spinner)findViewById(R.id.spSensorType);

        populateSensorNames();

        updateGuiInternal();

        mSensorTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // First entry is Unknown
                String sensorName = adapterView.getItemAtPosition(i).toString();
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(Constants.SHARED_PREFERENCES_SENSORNAME, sensorName);
                editor.commit();
                if(i > 0)
                {
                    mSeekBarsMaxRange = ProximitySensorModule.getSensorMaxRange(SetupActivity.this, sensorName);
                    mMinDistanceSeekBar.setMax(mSeekBarsMaxRange);
                    mCurrentDistanceSeekBar.setMax(mSeekBarsMaxRange);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(Constants.SHARED_PREFERENCES_SENSORNAME, Constants.SHARED_PREFERENCES_UNSELECTED_SENSOR_NAME);
                editor.commit();
            }
        });

        mMinDistanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentProgress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                currentProgress = i;
                mMinDistanceTextView.setText("Min distance before action: " + currentProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                currentProgress = 0;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putInt(Constants.SHARED_PREFERENCES_PROXIMITYMINDISTANCE, currentProgress);
                editor.commit();
                if(ForegroundService.isRunning(SetupActivity.this))
                {
                    DWDistanceScanTrigger distanceScanTrigger = ForegroundService.getDwDistanceScanTrigger();
                    if(distanceScanTrigger != null)
                    {
                        distanceScanTrigger.setMinDistance(currentProgress);
                    }
                }
                mMinDistanceTextView.setText("Min distance before action: " + currentProgress);
            }
        });

        mAutoStartServiceOnBootSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mAutoStartServiceOnBootSwitch.setText(getString(R.string.startOnBoot));
                } else {
                    mAutoStartServiceOnBootSwitch.setText(getString(R.string.doNothingOnBoot));
                }
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, isChecked);
                editor.commit();
            }
        });

        mAutoStartServiceOnPowerEventSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mAutoStartServiceOnPowerEventSwitch.setText(getString(R.string.startOnCharging));
                    // Launch the watcher service
                    if(!PowerEventsWatcherService.isRunning(SetupActivity.this))
                        PowerEventsWatcherService.startService(SetupActivity.this);
                    // Let's check if we are already connected on power to launch ForegroundService if necessary
                    BatteryManager myBatteryManager = (BatteryManager) SetupActivity.this.getSystemService(Context.BATTERY_SERVICE);
                    if(myBatteryManager.isCharging() && !ForegroundService.isRunning(SetupActivity.this))
                        ForegroundService.startService(SetupActivity.this);
                } else {
                    mAutoStartServiceOnPowerEventSwitch.setText(getString(R.string.doNothingOnCharging));
                    // Stop the watcher service
                    if(PowerEventsWatcherService.isRunning(SetupActivity.this))
                        PowerEventsWatcherService.stopService(SetupActivity.this);
                }
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, isChecked);
                editor.commit();
            }
        });

        mStartStopServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStarted));
                    if(!ForegroundService.isRunning(SetupActivity.this)) {
                        ForegroundService.startService(SetupActivity.this);
                        setupDebugInterfacesIfServiceIsRunning();
                    }
                }
                else
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStopped));
                    if(ForegroundService.isRunning(SetupActivity.this)) {
                        ForegroundService.stopService(SetupActivity.this);
                        setupDebugInterfacesIfServiceIsRunning();
                    }
                }
                syncGUIwithServiceStatus();
            }
        });
        setupDebugInterfacesIfServiceIsRunning();
    }

    private void setupDebugInterfacesIfServiceIsRunning() {
        boolean isRunning = ForegroundService.isRunning(SetupActivity.this);
        if(isRunning)
        {
            ForegroundService.getProximitySensorModule().debugInterfaceCallback = SetupActivity.this;
            ForegroundService.getDwDistanceScanTrigger().debugInterfaceCallback = SetupActivity.this;
        }
        else
        {
            ForegroundService.getProximitySensorModule().debugInterfaceCallback = null;
            ForegroundService.getDwDistanceScanTrigger().debugInterfaceCallback = null;
        }
    }

    private void populateSensorNames() {
        SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String sensorName = sharedpreferences.getString(Constants.SHARED_PREFERENCES_SENSORNAME, Constants.SHARED_PREFERENCES_UNSELECTED_SENSOR_NAME);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if(sensorManager != null)
        {
            int elementToSelect = 0;
            String sensorNameToSelect = "";
            mSensorNames = new HashMap<>();
            mSensorNames.put(Constants.SHARED_PREFERENCES_UNSELECTED_SENSOR_NAME, 0);
            List<String> orderedSensorNames = new ArrayList<>();
            orderedSensorNames.add(Constants.SHARED_PREFERENCES_UNSELECTED_SENSOR_NAME);
            int index = 1;
            // Populate with proximity sensors
            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_PROXIMITY);
            for (Sensor sensor : sensorList) {
                String currentSensorName = sensor.getName();
                orderedSensorNames.add(currentSensorName);
                mSensorNames.put(currentSensorName, index);
                if (currentSensorName.equalsIgnoreCase(sensorName)) {
                    elementToSelect = index;
                    sensorNameToSelect = sensorName;
                }
                index++;
            }
            // Add Zebra custom proximity sensor if available (CCXX models)
            sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : sensorList) {
                String sensorTypeString = sensor.getStringType();
                if(sensorTypeString.equalsIgnoreCase(Constants.ZEBRA_PROXIMITY_SENSOR_TYPE)) {
                    String currentSensorName = sensor.getName();
                    orderedSensorNames.add(currentSensorName);
                    mSensorNames.put(currentSensorName, index);
                    if (currentSensorName.equalsIgnoreCase(sensorName)) {
                        elementToSelect = index;
                        sensorNameToSelect = sensorName;
                    }
                    index++;
                }
            }
            if(orderedSensorNames.size() > 0) {
                mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, orderedSensorNames);
                mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSensorTypeSpinner.setAdapter(mAdapter);
                mSensorTypeSpinner.setSelection(elementToSelect);
                if(elementToSelect > 0)
                {
                    mSeekBarsMaxRange = ProximitySensorModule.getSensorMaxRange(SetupActivity.this, sensorNameToSelect);
                    mMinDistanceSeekBar.setMax(mSeekBarsMaxRange);
                    mCurrentDistanceSeekBar.setMax(mSeekBarsMaxRange);
                }
            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    private void setAutoStartServiceOnBootSwitch(final boolean checked)
    {
        mAutoStartServiceOnBootSwitch.setChecked(checked);
        mAutoStartServiceOnBootSwitch.setText(checked ? R.string.startOnBoot : R.string.doNothingOnBoot);
    }
    private void setAutoStartServiceOnPowerEventSwitch(final boolean checked)
    {
        mAutoStartServiceOnPowerEventSwitch.setChecked(checked);
        mAutoStartServiceOnPowerEventSwitch.setText(checked ? R.string.startOnCharging : R.string.doNothingOnCharging);
    }

    private void setServiceStartedSwitchValues(final boolean checked, final String text)
    {
        mStartStopServiceSwitch.setChecked(checked);
        mStartStopServiceSwitch.setText(text);
    }

    private void syncGUIwithServiceStatus() {
        boolean bServiceIsRunning = ForegroundService.isRunning(SetupActivity.this);
        if(bServiceIsRunning)
        {
            setServiceStartedSwitchValues(true, getString(R.string.serviceStarted));
        }
        else
        {
            setServiceStartedSwitchValues(false, getString(R.string.serviceStopped));
        }
        mSensorTypeSpinner.setEnabled(!bServiceIsRunning);
        mCurrentDistanceTextView.setVisibility(bServiceIsRunning ? View.VISIBLE : View.GONE);
        mCurrentDistanceSeekBar.setVisibility(bServiceIsRunning ? View.VISIBLE : View.GONE);
        mProximityStatus.setVisibility(bServiceIsRunning ? View.VISIBLE : View.GONE);
    }

    private void selectSensor(String name)
    {
        int sensorIndex = 0;
        try {
            sensorIndex = mSensorNames.get(name);
        }
        catch(Exception e)
        {
            sensorIndex = 0;
        }
        if(sensorIndex < mAdapter.getCount())
        {
            mSensorTypeSpinner.setSelection(sensorIndex);
        }
    }

    private void updateGuiInternal()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                syncGUIwithServiceStatus();
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean startServiceOnBoot = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
                setAutoStartServiceOnBootSwitch(startServiceOnBoot);
                boolean startServicePowerEvents = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, false);
                setAutoStartServiceOnPowerEventSwitch(startServicePowerEvents);
                int minDistance = sharedpreferences.getInt(Constants.SHARED_PREFERENCES_PROXIMITYMINDISTANCE, Constants.SHARED_PREFERENCES_PROXIMITYMINDISTANCE_DEFAULTVALUE);
                mMinDistanceSeekBar.setProgress(minDistance);
                String selectedSensor = sharedpreferences.getString(Constants.SHARED_PREFERENCES_SENSORNAME, Constants.SHARED_PREFERENCES_UNSELECTED_SENSOR_NAME);
                selectSensor(selectedSensor);
            }
        });

    }



    // Update GUI controls only if the activity exists
    public static void updateGUIfNecessary()
    {
        // Update GUI if necessary
        if(SetupActivity.mSetupActivity != null) // The application default activity has been opened
        {
            SetupActivity.mSetupActivity.updateGuiInternal();
        }
    }

    @Override
    public void onTriggerStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update seek bars and stuffs once
                mProximityStatus.setText("Proximity Action Activated");
            }
        });
    }

    @Override
    public void onTriggerStop() {
         runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update seek bars and stuffs once
                mProximityStatus.setText("Proximity Action Stopped");
            }
        });
    }

    @Override
    public void onDistance(float distance, float sensorMaximumRange) {
        if(sensorMaximumRange != mSeekBarsMaxRange)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Update seek bars and stuffs once
                    mSeekBarsMaxRange = (int)sensorMaximumRange;
                    mMinDistanceSeekBar.setMax(mSeekBarsMaxRange);
                    mCurrentDistanceSeekBar.setMax(mSeekBarsMaxRange);
                }
            });
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update seek bars and stuffs once
                mCurrentDistanceSeekBar.setProgress((int)distance, false);
                mCurrentDistanceTextView.setText("Current Distance: " + (int)distance);
            }
        });
    }
}