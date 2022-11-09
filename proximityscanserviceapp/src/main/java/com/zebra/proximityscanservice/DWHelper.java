package com.zebra.proximityscanservice;

import android.content.Context;

import com.zebra.datawedgeprofileintents.DWProfileBaseSettings;
import com.zebra.datawedgeprofileintents.DWProfileCommandBase;
import com.zebra.datawedgeprofileintents.DWScannerStartScan;
import com.zebra.datawedgeprofileintents.DWScannerStopScan;

public class DWHelper {
    public static void StartScan(Context context) {
        DWScannerStartScan startScan = new DWScannerStartScan(context);
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

    public static void StopScan(Context context) {
        DWScannerStopScan stopScan = new DWScannerStopScan(context);
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
