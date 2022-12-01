package com.zebra.proximityscanservice;

import android.content.Context;

import com.zebra.datawedgeprofileenums.SC_S_SCANNER_STATUS;
import com.zebra.datawedgeprofileintents.DWStatusScanner;
import com.zebra.datawedgeprofileintents.DWStatusScannerCallback;
import com.zebra.datawedgeprofileintents.DWStatusScannerSettings;
import com.zebra.datawedgeprofileintents.DataWedgeConstants;

public class ActionProcessorDataWedge implements DistanceTriggerProcessor.IDistanceTriggerProcessorInterface {

    protected enum EDatawedgeAction
    {
        TRIGGER_START_STOP("Start Scan / Stop Scan"),
        ENABLE_DISABLE_DATAWEDGE("Enable Datawedge / Disable Datawedge");

        String asString = "";
        EDatawedgeAction(String asString)
        {
            this.asString = asString;
        }

        @Override
        public String toString() {
            return asString;
        }

        public static EDatawedgeAction fromString(String asAString)
        {
            if(asAString.equalsIgnoreCase(ENABLE_DISABLE_DATAWEDGE.toString())){
                return ENABLE_DISABLE_DATAWEDGE;
            }
            else
            {
                return TRIGGER_START_STOP;
            }
        }
    }

    private EDatawedgeAction eDatawedgeAction = EDatawedgeAction.TRIGGER_START_STOP;
    /*
    Scanner status checker
    */
    private SC_S_SCANNER_STATUS eStatus = SC_S_SCANNER_STATUS.DISABLED;
    private DWStatusScanner mScannerStatusChecker = null;
    private Context context = null;

    protected static DistanceTriggerProcessor.DistanceTriggerDebugInterface debugInterfaceCallback = null;

    public ActionProcessorDataWedge(Context context, EDatawedgeAction eDatawedgeAction)
    {
        this.context = context;
        this.eDatawedgeAction = eDatawedgeAction;
    }

    @Override
    public void onInsideZone() {
        switch(eDatawedgeAction)
        {
            case ENABLE_DISABLE_DATAWEDGE:
                DWHelper.EnableDataWedge(context);
                break;
            case TRIGGER_START_STOP:
                if(eStatus != SC_S_SCANNER_STATUS.SCANNING)
                    DWHelper.StartScan(context);
        }
    }

    @Override
    public void onOutsideZone() {
        switch(eDatawedgeAction)
        {
            case ENABLE_DISABLE_DATAWEDGE:
                DWHelper.DisableDataWedge(context);
                break;
            case TRIGGER_START_STOP:
                if(eStatus == SC_S_SCANNER_STATUS.SCANNING)
                    DWHelper.StopScan(context);
        }
    }

    public void start() {
        if(eDatawedgeAction == EDatawedgeAction.TRIGGER_START_STOP)
            setupScannerStatusChecker();
    }

    public void stop() {
        releaseScannerStatusChecker();
    }

    public void setDatawedgeAction(EDatawedgeAction eDatawedgeAction)
    {
        if(this.eDatawedgeAction == EDatawedgeAction.TRIGGER_START_STOP && eDatawedgeAction != EDatawedgeAction.TRIGGER_START_STOP)
        {
            releaseScannerStatusChecker();
        }
        this.eDatawedgeAction = eDatawedgeAction;
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
