package com.zebra.proximityscanservice;

import android.content.Context;

import com.zebra.datawedgeprofileenums.SC_S_SCANNER_STATUS;
import com.zebra.datawedgeprofileintents.DWStatusScanner;
import com.zebra.datawedgeprofileintents.DWStatusScannerCallback;
import com.zebra.datawedgeprofileintents.DWStatusScannerSettings;
import com.zebra.datawedgeprofileintents.DataWedgeConstants;

public class DWDistanceScanTrigger implements ProximitySensorModule.ProximitySensorModuleCallback {

    private Context context;
    private float distance_to_trigger_event = 100;
    private SC_S_SCANNER_STATUS eStatus = SC_S_SCANNER_STATUS.DISABLED;

    protected interface DWDistanceScanTriggerDebugInterface
    {
        void onTriggerStart();
        void onTriggerStop();
    }
    protected static DWDistanceScanTriggerDebugInterface debugInterfaceCallback = null;
    /*
    Scanner status checker
     */
    DWStatusScanner mScannerStatusChecker = null;

    public DWDistanceScanTrigger(Context context, float distance_to_trigger_event)
    {
        this.context = context;
        this.distance_to_trigger_event = distance_to_trigger_event;
    }

    public void setMinDistance(float distance_to_trigger_event)
    {
        this.distance_to_trigger_event = distance_to_trigger_event;
    }

    public void start()
    {
        setupScannerStatusChecker();
    }

    public void stop()
    {
        if(mScannerStatusChecker != null)
        {
            mScannerStatusChecker.stop();
            mScannerStatusChecker = null;
        }
    }

    @Override
    public void onDistance(float distance, float sensorMaximumRange) {
        if(distance > distance_to_trigger_event ) {
            if(debugInterfaceCallback != null)
            {
                debugInterfaceCallback.onTriggerStart();
            }
            if(eStatus != SC_S_SCANNER_STATUS.SCANNING)
                DWHelper.StartScan(context);
        }
        else if(distance < distance_to_trigger_event)
        {
            if(debugInterfaceCallback != null)
            {
                debugInterfaceCallback.onTriggerStop();
            }
            if(eStatus == SC_S_SCANNER_STATUS.SCANNING)
                DWHelper.StopScan(context);
        }
    }

    private void setupScannerStatusChecker()
    {
        if(mScannerStatusChecker == null) {
            DWStatusScannerSettings profileStatusSettings = new DWStatusScannerSettings() {{
                mPackageName = context.getPackageName();
                mScannerCallback = new DWStatusScannerCallback() {
                    @Override
                    public void result(String status) {
                        eStatus = SC_S_SCANNER_STATUS.valueOf(status);
                        switch (status) {
                            case DataWedgeConstants.SCAN_STATUS_CONNECTED:
                                LogHelper.logD("Scanner is connected.");
                                break;
                            case DataWedgeConstants.SCAN_STATUS_DISABLED:
                                LogHelper.logD("Scanner is disabled.");
                                break;
                            case DataWedgeConstants.SCAN_STATUS_DISCONNECTED:
                                LogHelper.logD("Scanner is disconnected.");
                                break;
                            case DataWedgeConstants.SCAN_STATUS_SCANNING:
                                LogHelper.logD("Scanner is scanning.");
                                break;
                            case DataWedgeConstants.SCAN_STATUS_WAITING:
                                LogHelper.logD("Scanner is waiting.");
                                break;
                        }
                    }
                };
            }};

            LogHelper.logD("Setting up scanner status checking on package : " + profileStatusSettings.mPackageName + ".");

            mScannerStatusChecker = new DWStatusScanner(context, profileStatusSettings);
            mScannerStatusChecker.start();
        }
        else
        {
            mScannerStatusChecker.start();
        }
    }
}
