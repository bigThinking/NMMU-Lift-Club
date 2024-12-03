package za.ac.nmmu.lift_club.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Reciever extends BroadcastReceiver {

    GpsService gps;

    public Reciever(){}

    public Reciever(GpsService gps) {
        this.gps = gps;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().compareTo(GpsService.ENTRY_POINT_INTENT) == 0) {
            gps.cancelProximityAlerts();

            if (gps.toCampus) {
                Log.i("Destination reached : ", " we here yo");
                gps.destinationReached(intent);
            } else {
                Log.i("beginning recording: ", " lets get going");
                gps.begin(intent);
            }

            return;
        }

        if (intent.getAction().compareTo(GpsService.ENDPOINT_INTENT) == 0) {
            gps.cancelProximityAlerts();
            gps.destinationReached(intent);
            return;
        }

        if (intent.getAction().compareTo(GpsService.PASSENGER_PICKUP_INTENT) == 0) {
           gps.passengerAhead(intent);
            return;
        }

        if(intent.getAction().compareTo(GpsService.PASEENGER_DROPOFF_INTENT) == 0)
        {
            gps.passengerDropoffAhead(intent);
            return;
        }
    }
}
