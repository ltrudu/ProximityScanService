package com.zebra.proximityscanservice;

import android.content.Context;

import com.zebra.datawedgeprofileintents.DWDataWedgeDisable;
import com.zebra.datawedgeprofileintents.DWDataWedgeEnable;
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

    public static void EnableDataWedge(Context context) {
        DWDataWedgeEnable dwDataWedgeEnable = new DWDataWedgeEnable(context);
        DWProfileBaseSettings baseSettings = new DWProfileBaseSettings();
        dwDataWedgeEnable.execute(baseSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
            }

            @Override
            public void timeout(String profileName) {
            }
        });
    }

    public static void DisableDataWedge(Context context) {
        DWDataWedgeDisable dwDataWedgeDisable = new DWDataWedgeDisable(context);
        DWProfileBaseSettings baseSettings = new DWProfileBaseSettings();
        dwDataWedgeDisable.execute(baseSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
            }

            @Override
            public void timeout(String profileName) {
            }
        });
    }

}
