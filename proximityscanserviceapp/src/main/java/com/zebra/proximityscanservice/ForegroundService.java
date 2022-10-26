package com.zebra.proximityscanservice;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.media.AudioTrack;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.util.Log;

import com.zebra.datawedgeprofileintents.DWProfileBaseSettings;
import com.zebra.datawedgeprofileintents.DWProfileCommandBase;
import com.zebra.datawedgeprofileintents.DWScannerStartScan;
import com.zebra.datawedgeprofileintents.DWScannerStopScan;

import java.util.List;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class ForegroundService extends Service {
    private static final int SERVICE_ID = 1;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private SensorManager sensorManager = null;
    private Sensor proximitySensor = null;
    private SensorEventCallback sensorEventCallback = null;

    public ForegroundService() {
    }

    public IBinder onBind(Intent paramIntent)
    {
        return null;
    }

    public void onCreate()
    {
        logD("onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logD("onStartCommand");
        super.onStartCommand(intent, flags, startId);
        startService();
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        logD("onDestroy");
        stopService();
    }

    @SuppressLint({"Wakelock"})
    private void startService()
    {
        logD("startService");
        try
        {
            if(mNotificationManager == null)
                mNotificationManager = ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE));

            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    0,
                    mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Create the Foreground Service Notification
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, createNotificationChannel(mNotificationManager));
            mNotification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_jacktrigger)
                    .setContentTitle(getString(R.string.foreground_service_notification_title))
                    .setContentText(getString(R.string.foreground_service_notification_text))
                    .setTicker(getString(R.string.foreground_service_notification_tickle))
                    .setPriority(PRIORITY_MIN)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .build();

            TaskStackBuilder localTaskStackBuilder = TaskStackBuilder.create(this);
            localTaskStackBuilder.addParentStack(MainActivity.class);
            localTaskStackBuilder.addNextIntent(mainActivityIntent);
            notificationBuilder.setContentIntent(localTaskStackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT));

            // Start foreground service
            startForeground(SERVICE_ID, mNotification);

            // Initialize functional things here
            if(sensorManager == null)
                sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            if(sensorManager != null){

                if(proximitySensor == null) {
                    List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_ALL);
                    for(Sensor sensor : list)
                    {
                        if(sensor.getName().equalsIgnoreCase("LONGDISTANCEPROX"))
                        {
                            proximitySensor = sensor;
                        }
                    }
                }

                if (proximitySensor != null) {

                    if (sensorEventCallback == null) {

                        sensorEventCallback = new SensorEventCallback() {
                            @Override
                            public void onSensorChanged(SensorEvent event) {
                                super.onSensorChanged(event);
                                processSensorEvent(event);
                            }
                        };
                    }
                    sensorManager.registerListener(sensorEventCallback, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }

            logD("startService:Service started without error.");
        }
        catch(Exception e)
        {
            logD("startService:Error while starting service.");
            e.printStackTrace();
        }
    }

    private void processSensorEvent(SensorEvent event) {
        float distance = event.values[0];
        logD("Distance: " + distance);

        if(distance > Constants.MIN_DISTANCE_TO_TRIGGER_SCAN ) {
            DWScannerStartScan startScan = new DWScannerStartScan(ForegroundService.this);
            DWProfileBaseSettings baseSettings = new DWProfileBaseSettings();
            startScan.execute(baseSettings, new DWProfileCommandBase.onProfileCommandResult() {
                @Override
                public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                }

                @Override
                public void timeout(String profileName) {
                }
            });
        }
        else if(distance < Constants.MIN_DISTANCE_TO_TRIGGER_SCAN)
        {
            DWScannerStopScan stopScan = new DWScannerStopScan(ForegroundService.this);
            DWProfileBaseSettings baseSettings = new DWProfileBaseSettings();
            stopScan.execute(baseSettings, new DWProfileCommandBase.onProfileCommandResult() {
                @Override
                public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                }

                @Override
                public void timeout(String profileName) {
                }
            });
        }
    }

    private void stopService()
    {
        try
        {
            logD("stopService.");

            // TODO: Release your stuffs here
            if(mNotificationManager != null)
            {
                mNotificationManager.cancelAll();
                mNotificationManager = null;
            }

            if(sensorManager != null && sensorEventCallback != null)
            {
                sensorManager.unregisterListener(sensorEventCallback);
                proximitySensor = null;
                sensorManager = null;
            }

            stopForeground(true);
            logD("stopService:Service stopped without error.");
        }
        catch(Exception e)
        {
            logD("Error while stopping service.");
            e.printStackTrace();

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        NotificationChannel channel = new NotificationChannel(getString(R.string.foreground_service_channel_id), getString(R.string.foreground_service_channel_name), NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return getString(R.string.foreground_service_channel_id);
    }

    private void logD(String message)
    {
        Log.d(Constants.TAG, message);
    }

    public static void startService(Context context)
    {
        Intent myIntent = new Intent(context, ForegroundService.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            // Use start foreground service to prevent the runtime error:
            // "not allowed to start service intent app is in background"
            // to happen when running on OS >= Oreo
            context.startForegroundService(myIntent);
        }
        else
        {
            context.startService(myIntent);
        }
    }

    public static void stopService(Context context)
    {
        Intent myIntent = new Intent(context, ForegroundService.class);
        context.stopService(myIntent);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
