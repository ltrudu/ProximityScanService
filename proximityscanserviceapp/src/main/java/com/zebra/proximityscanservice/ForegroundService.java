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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class ForegroundService extends Service {
    private static final int SERVICE_ID = 1;

    private NotificationManager mNotificationManager = null;
    private Notification mNotification = null;
    private static ProximitySensorModule proximitySensorModule = null;
    //private static DWDistanceScanTrigger dwDistanceScanTrigger = null;

    private static ActionProcessor actionProcessor = null;
    private static DistanceTriggerProcessor distanceTriggerProcessor = null;

    private String mSensorName = null;
    private int mProximityMinDistance = Constants.SHARED_PREFERENCES_PROXIMITYMINDISTANCE_DEFAULTVALUE;
    private ActionProcessor.EActionProcessorType eActionProcessorType = ActionProcessor.EActionProcessorType.TRIGGER_START_STOP;
    private DistanceTriggerProcessor.EDistanceComparator eDistanceComparator = DistanceTriggerProcessor.EDistanceComparator.SUPERIOR_TO_REF;

    public ForegroundService() {
    }

    public IBinder onBind(Intent paramIntent)
    {
        return null;
    }

    public void onCreate()
    {
        LogHelper.logD("onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogHelper.logD("onStartCommand");
        super.onStartCommand(intent, flags, startId);
        startService();
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        LogHelper.logD("onDestroy");
        stopService();
    }

    @SuppressLint({"Wakelock"})
    private void startService()
    {
        LogHelper.logD("startService");
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

            // Do your service stuffs here :)
            SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

            mProximityMinDistance = sharedpreferences.getInt(Constants.SHARED_PREFERENCES_PROXIMITYMINDISTANCE, Constants.SHARED_PREFERENCES_PROXIMITYMINDISTANCE_DEFAULTVALUE);

            String actionType = sharedpreferences.getString(Constants.SHARED_PREFERENCES_ACTION_TYPE, ActionProcessor.EActionProcessorType.TRIGGER_START_STOP.toString());
            eActionProcessorType = ActionProcessor.EActionProcessorType.fromString(actionType);
            if(actionProcessor == null)
            {
                actionProcessor = new ActionProcessor(this, eActionProcessorType);
            }
            else
            {
                actionProcessor.setActionType(eActionProcessorType);
            }

            String triggerType = sharedpreferences.getString(Constants.SHARED_PREFERENCES_TRIGGER_TYPE, DistanceTriggerProcessor.EDistanceComparator.SUPERIOR_TO_REF.toString());
            eDistanceComparator = DistanceTriggerProcessor.EDistanceComparator.fromString(triggerType);
            if(distanceTriggerProcessor == null)
            {
                distanceTriggerProcessor = new DistanceTriggerProcessor(this, (float)mProximityMinDistance, eDistanceComparator, actionProcessor);
            }
            else
            {
                distanceTriggerProcessor.setReferenceDistance((float)mProximityMinDistance);
                distanceTriggerProcessor.setTriggerProcessor(actionProcessor);
                distanceTriggerProcessor.setDistanceComparator(eDistanceComparator);
            }

           mSensorName = sharedpreferences.getString(Constants.SHARED_PREFERENCES_SENSORNAME, Constants.SHARED_PREFERENCES_UNSELECTED);

            if(proximitySensorModule != null)
            {
                // Reset sensor
                proximitySensorModule.stop();
                proximitySensorModule = null;
            }

            if(proximitySensorModule == null)
                proximitySensorModule = new ProximitySensorModule(this, mSensorName, distanceTriggerProcessor);

            proximitySensorModule.start();
            actionProcessor.start();

            LogHelper.logD("startService:Service started without error.");
        }
        catch(Exception e)
        {
            LogHelper.logD("startService:Error while starting service.");
            e.printStackTrace();
        }
    }

    private void stopService()
    {
        try
        {
            LogHelper.logD("stopService.");

            // TODO: Release your stuffs here
            if(mNotificationManager != null)
            {
                mNotificationManager.cancelAll();
                mNotificationManager = null;
            }

            proximitySensorModule.stop();
            actionProcessor.stop();

            stopForeground(true);
            LogHelper.logD("stopService:Service stopped without error.");
        }
        catch(Exception e)
        {
            LogHelper.logD("Error while stopping service.");
            e.printStackTrace();

        }

    }

    protected static ProximitySensorModule getProximitySensorModule()
    {
        return proximitySensorModule;
    }

    protected static DistanceTriggerProcessor getDistanceTriggerProcessor()
    {
        return distanceTriggerProcessor;
    }

    protected static ActionProcessor getActionProcessor()
    {
        return actionProcessor;
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
