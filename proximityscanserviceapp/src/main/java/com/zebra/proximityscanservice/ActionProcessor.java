package com.zebra.proximityscanservice;

import android.content.Context;
import android.content.Intent;

import com.zebra.datawedgeprofileenums.SC_S_SCANNER_STATUS;
import com.zebra.datawedgeprofileintents.DWStatusScanner;
import com.zebra.datawedgeprofileintents.DWStatusScannerCallback;
import com.zebra.datawedgeprofileintents.DWStatusScannerSettings;
import com.zebra.datawedgeprofileintents.DataWedgeConstants;

import static com.zebra.proximityscanservice.Constants.INTENT_ACTION_INSIDEZONE;
import static com.zebra.proximityscanservice.Constants.INTENT_ACTION_OUTSIDEZONE;

public class ActionProcessor implements DistanceTriggerProcessor.IDistanceTriggerProcessorInterface {

    protected enum EActionProcessorType
    {
        TRIGGER_START_STOP("Start Scan / Stop Scan"),
        ENABLE_DISABLE_DATAWEDGE("Enable Datawedge / Disable Datawedge"),
        SEND_INTENT("Send inside zone intent / Send outside zone intent");

        String asString = "";
        EActionProcessorType(String asString)
        {
            this.asString = asString;
        }

        @Override
        public String toString() {
            return asString;
        }

        public static EActionProcessorType fromString(String asAString)
        {
            switch(asAString)
            {
                case "Start Scan / Stop Scan":
                    return TRIGGER_START_STOP;
                case "Enable Datawedge / Disable Datawedge":
                    return ENABLE_DISABLE_DATAWEDGE;
                default:
                    return SEND_INTENT;
            }
        }
    }

    private EActionProcessorType eActionProcessorType = EActionProcessorType.TRIGGER_START_STOP;
    /*
    Scanner status checker
    */
    private SC_S_SCANNER_STATUS eStatus = SC_S_SCANNER_STATUS.DISABLED;
    private DWStatusScanner mScannerStatusChecker = null;
    private Context context = null;

    protected static DistanceTriggerProcessor.DistanceTriggerDebugInterface debugInterfaceCallback = null;

    public ActionProcessor(Context context, EActionProcessorType eActionProcessorType)
    {
        this.context = context;
        this.eActionProcessorType = eActionProcessorType;
    }

    @Override
    public void onInsideZone() {
        switch(eActionProcessorType)
        {
            case ENABLE_DISABLE_DATAWEDGE:
                DWHelper.EnableDataWedge(context);
                break;
            case TRIGGER_START_STOP:
                if(eStatus != SC_S_SCANNER_STATUS.SCANNING)
                    DWHelper.StartScan(context);
                break;
            case SEND_INTENT:
                Intent intent = new Intent(INTENT_ACTION_INSIDEZONE);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                context.sendBroadcast(intent);
                break;
        }
    }

    @Override
    public void onOutsideZone() {
        switch(eActionProcessorType)
        {
            case ENABLE_DISABLE_DATAWEDGE:
                DWHelper.DisableDataWedge(context);
                break;
            case TRIGGER_START_STOP:
                if(eStatus == SC_S_SCANNER_STATUS.SCANNING)
                    DWHelper.StopScan(context);
                break;
            case SEND_INTENT:
                Intent intent = new Intent(INTENT_ACTION_OUTSIDEZONE);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                context.sendBroadcast(intent);
                break;
        }
    }

    public void start() {
        if(eActionProcessorType == EActionProcessorType.TRIGGER_START_STOP)
            setupScannerStatusChecker();
    }

    public void stop() {
        releaseScannerStatusChecker();
    }

    public void setActionType(EActionProcessorType eActionProcessorType)
    {
        if(this.eActionProcessorType == EActionProcessorType.TRIGGER_START_STOP && eActionProcessorType != EActionProcessorType.TRIGGER_START_STOP)
        {
            releaseScannerStatusChecker();
        }
        this.eActionProcessorType = eActionProcessorType;
    }

    private void releaseScannerStatusChecker()
    {
        if(mScannerStatusChecker != null)
        {
            mScannerStatusChecker.stop();
            mScannerStatusChecker = null;
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
