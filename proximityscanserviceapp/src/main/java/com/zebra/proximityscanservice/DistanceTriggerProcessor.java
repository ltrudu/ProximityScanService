package com.zebra.proximityscanservice;

import android.content.Context;

public class DistanceTriggerProcessor implements ProximitySensorModule.ProximitySensorModuleCallback {


    protected interface DistanceTriggerDebugInterface {
        void onInsideZone(String triggerType);
        void onOutsideZone(String triggerType);
    }

    protected enum EDistanceComparator
    {
        SUPERIOR_TO_REF("Current Distance > Reference Distance"),
        INFERIOR_TO_REF("Current Distance < Reference Distance");

        String asString = "";
        EDistanceComparator(String asString)
        {
            this.asString = asString;
        }

        @Override
        public String toString() {
            return asString;
        }

        public static EDistanceComparator fromString(String asAString)
        {
            if(asAString.equalsIgnoreCase(INFERIOR_TO_REF.toString())){
                return INFERIOR_TO_REF;
            }
            else
            {
                return SUPERIOR_TO_REF;
            }
        }
    }

    protected Context context;
    protected float reference_distance = 100;
    protected EDistanceComparator eDistanceComparator = EDistanceComparator.SUPERIOR_TO_REF;
    protected static DistanceTriggerDebugInterface debugInterfaceCallback = null;

    protected interface IDistanceTriggerProcessorInterface
    {
        void onInsideZone();
        void onOutsideZone();
    }

    private IDistanceTriggerProcessorInterface iDistanceTriggerProcessorInterface = null;

    public DistanceTriggerProcessor(Context context, float reference_distance, EDistanceComparator distanceComparator, IDistanceTriggerProcessorInterface iDistanceTriggerProcessorInterface)
    {
        this.context = context;
        this.reference_distance = reference_distance;
        this.eDistanceComparator = distanceComparator;
        this.iDistanceTriggerProcessorInterface = iDistanceTriggerProcessorInterface;
    }

    public void setReferenceDistance(float distance_to_trigger_event)
    {
        this.reference_distance = distance_to_trigger_event;
    }

    public void setDistanceComparator(EDistanceComparator eDistanceComparator)
    {
        this.eDistanceComparator = eDistanceComparator;
    }

    public void setTriggerProcessor(IDistanceTriggerProcessorInterface iDistanceTriggerProcessorInterface)
    {
        this.iDistanceTriggerProcessorInterface = iDistanceTriggerProcessorInterface;
    }

    @Override
    public void onDistance(float distance, float sensorMaximumRange) {
        switch (eDistanceComparator)
        {
            case SUPERIOR_TO_REF:
                if(distance > reference_distance) {
                    if(debugInterfaceCallback != null)
                    {
                        debugInterfaceCallback.onInsideZone(DistanceTriggerProcessor.class.getTypeName());
                    }
                    if(iDistanceTriggerProcessorInterface != null)
                    {
                        iDistanceTriggerProcessorInterface.onInsideZone();
                    }
                }
                else if(distance < reference_distance)
                {
                    if(debugInterfaceCallback != null)
                    {
                        debugInterfaceCallback.onOutsideZone(DistanceTriggerProcessor.class.getTypeName());
                    }
                    if(iDistanceTriggerProcessorInterface != null)
                    {
                        iDistanceTriggerProcessorInterface.onOutsideZone();
                    }
                }
                break;
            case INFERIOR_TO_REF:
                if(distance < reference_distance) {
                    if(debugInterfaceCallback != null)
                    {
                        debugInterfaceCallback.onInsideZone(DistanceTriggerProcessor.class.getTypeName());
                    }
                    if(iDistanceTriggerProcessorInterface != null)
                    {
                        iDistanceTriggerProcessorInterface.onInsideZone();
                    }                }
                else if(distance > reference_distance)
                {
                    if(debugInterfaceCallback != null)
                    {
                        debugInterfaceCallback.onOutsideZone(DistanceTriggerProcessor.class.getTypeName());
                    }
                    if(iDistanceTriggerProcessorInterface != null)
                    {
                        iDistanceTriggerProcessorInterface.onOutsideZone();
                    }
                }
                break;
        }

    }
}
