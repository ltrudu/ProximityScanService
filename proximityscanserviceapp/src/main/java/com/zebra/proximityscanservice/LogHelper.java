package com.zebra.proximityscanservice;

import android.util.Log;

public class LogHelper {
    private static boolean forceLogging = false;

    public static void enableLogging()
    {
        forceLogging = true;
    }

    public static void disableLogging()
    {
        forceLogging = false;
    }

    protected static void logV(String message)
    {
        if(BuildConfig.DEBUG || forceLogging)
        {
            Log.v(Constants.TAG, message);
        }
    }

    protected static void logD(String message)
    {
        if(BuildConfig.DEBUG || forceLogging)
        {
            Log.d(Constants.TAG, message);
        }
    }
    protected static void logE(String message)
    {
        if(BuildConfig.DEBUG || forceLogging)
        {
            Log.e(Constants.TAG, message);
        }
    }

}
