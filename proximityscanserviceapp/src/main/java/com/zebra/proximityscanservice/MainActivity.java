package com.zebra.proximityscanservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

// Copyright Zebra Technologies 2019.
// See included licence for details
//
// Original code: Laurent Trudu
//
//
// The service can be launched using the graphical user interface, intent actions or adb.
//
// If the option "Start on boot" is enabled, the service will be automatically launched when the boot is complete.
//
// Power events occur when the device is connected to a power source (AC/USB/Wireless).
// If the option "Start when charging / Stop when charging" is enabled, the power events will be monitored.
// The ForegroundService will be launched when the device is connected to a power source
//
//
// The service respond to two intent actions (both uses the category: android.intent.category.DEFAULT)
// - "com.zebra.proximityscanservice.startservice" sent on the component "com.zebra.proximityscanservice/com.zebra.proximityscanservice.StartServiceBroadcastReceiver":
//   Start the service.
//   If the device get rebooted the service will start automatically once the reboot is completed.
// - "com.zebra.proximityscanservice.stopservice" sent on the component "com.zebra.proximityscanservice/com.zebra.proximityscanservice.StopServiceBroadcastReceiver":
//   Stop the service.
//   If the device is rebooted, the service will not be started.
//
// The service can be started and stopped manually using the following adb commands:
//  - Start service:
//      adb shell am broadcast -a com.zebra.proximityscanservice.startservice -n com.zebra.proximityscanservice/com.zebra.proximityscanservice.StartServiceBroadcastReceiver
//  - Stop service:
//      adb shell am broadcast -a com.zebra.proximityscanservice.stopservice -n com.zebra.proximityscanservice/com.zebra.proximityscanservice.StopServiceBroadcastReceiver
//  - Setup service
//          The service can be configured using the following intent:
//          adb shell am broadcast -a com.zebra.proximityscanservice.setupservice -n com.zebra.proximityscanservice/com.zebra.proximityscanservice.SetupServiceBroadcastReceiver --es startonboot "true"
//          The command must contain at least one of the extras:
//          - Configure autostart on boot:
//          --es startonboot "true"
//          The extras value can be set to "true" or "1" to enable the option and "false" or "0" to disable the option.
public class MainActivity extends AppCompatActivity {

    private Switch mStartStopServiceSwitch = null;

    public static MainActivity mMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(ZebraDeviceHelper.isZebraDevice(this) == false)
        {
            Toast.makeText(this, "Jacktrigger only runs on Zebra devices.", Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finishAffinity();
            finish();
            System.exit(0);
        }

        setContentView(R.layout.activity_main);


        mStartStopServiceSwitch = (Switch)findViewById(R.id.startStopServiceSwitch);
        mStartStopServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStarted));
                    if(!ForegroundService.isRunning(MainActivity.this))
                        ForegroundService.startService(MainActivity.this);
                }
                else
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStopped));
                    if(ForegroundService.isRunning(MainActivity.this))
                        ForegroundService.stopService(MainActivity.this);
                }
            }
        });

        updateSwitches();
        launchPowerEventsWatcherServiceIfNecessary();
    }

    @Override
    protected void onResume() {
        mMainActivity = this;
        super.onResume();
        updateSwitches();
        launchPowerEventsWatcherServiceIfNecessary();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater findMenuItems = getMenuInflater();
        findMenuItems.inflate(R.menu.mainactivitymenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuitem_setup:
                if(ZebraDeviceHelper.isZebraDevice(this)) {
                    startActivity(new Intent(MainActivity.this, SetupActivity.class));
                }
                else
                {
                    Toast.makeText(this, getString(R.string.text_onlyzebra), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.menuitem_license:
                startActivity(new Intent(MainActivity.this, LicenceActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateSwitches()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(ForegroundService.isRunning(MainActivity.this))
                {
                    setServiceStartedSwitchValues(true, getString(R.string.serviceStarted));
                }
                else
                {
                    setServiceStartedSwitchValues(false, getString(R.string.serviceStopped));
                }
            }
        });

    }

    private void launchPowerEventsWatcherServiceIfNecessary()
    {
        // We need to launch the PowerEventsWatcher Service if necessary
        SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startServiceOnCharging = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, false);
        if(startServiceOnCharging)
        {
            // Launch the service if it was not running
            if(!PowerEventsWatcherService.isRunning(this))
                PowerEventsWatcherService.startService(this);

            // Let's check if we are already connected on power to launch ForegroundService if necessary
            BatteryManager myBatteryManager = (BatteryManager) MainActivity.this.getSystemService(Context.BATTERY_SERVICE);
            if(myBatteryManager.isCharging() && !ForegroundService.isRunning(MainActivity.this))
                ForegroundService.startService(MainActivity.this);
        }
    }

    @Override
    protected void onPause() {
        mMainActivity = null;
        super.onPause();
    }

    private void setServiceStartedSwitchValues(final boolean checked, final String text)
    {
        mStartStopServiceSwitch.setChecked(checked);
        mStartStopServiceSwitch.setText(text);
    }

    public static void updateGUISwitchesIfNecessary()
    {
        // Update GUI if necessary
        if(MainActivity.mMainActivity != null) // The application default activity has been opened
        {
            MainActivity.mMainActivity.updateSwitches();
        }
    }
}
