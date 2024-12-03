package za.ac.nmmu.lift_club.services;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import com.philippheckel.service.AbstractService;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.util.VincentyGeodesy;
import za.ac.nmmu.lift_club.util.Campus;


public class GpsService extends AbstractService implements LocationListener, Listener {

    //RADIUS_FROM_ENTRYPOINT = 100,
    private static final float ACCURACY_OF_POINTS = 50, RADIUS_FROM_ENTRYPOINT = 50, FIRST_PASSENGER_ALERT = 200, SECOND_PASSENGER_ALERT = 50;
    private static final int SPEED_LIMIT = 60;
    public static final String ENTRY_POINT_INTENT = "za.ac.nmmu.lift_club.ENTRY_POINT";
    public static final String PASSENGER_PICKUP_INTENT = "za.ac.nmmu.lift_club.PASSENGER_PICKUP_INTENT";
    public static final String ENDPOINT_INTENT = "za.ac.nmmu.lift_club.ENDPOINT";
    public static final String PASEENGER_DROPOFF_INTENT = "za.ac.nmmu.lift_club.PASEENGER_DROPOFF_INTENT";
    public LocationManager service = null;
    public byte activeMethod;
    public ArrayList<WGS84Point> routePoints = new ArrayList<>();
    public ArrayList<String> geoHashes  = new ArrayList<>();
    private int sumOfSpeed = 0, count = 0, nrBelowSpeedLimit = 0;//for calculating average speed
    public boolean toCampus, isOnRoute = true, begun = false;
    public ArrayList<PendingIntent> proximityAlerts  = new ArrayList<>();
    // --Commented out by Inspection (2015-09-20 04:32 PM):private Date started;
    private BroadcastReceiver reciever;
    private WGS84Point lastKnownLocation;
    private  int waiting = 2;
    private Random rand = new Random();

    public GpsService() {
    }

    @Override
    public void onStartService() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ENTRY_POINT_INTENT);
        intentFilter.addAction(PASSENGER_PICKUP_INTENT);
        intentFilter.addAction(ENDPOINT_INTENT);
        intentFilter.addAction(PASEENGER_DROPOFF_INTENT);
        reciever = new Reciever(this);
        registerReceiver(reciever, intentFilter);//register reciever
    }

    @Override
    public void onStopService() {
        cancelProximityAlerts();
        unregisterReceiver(reciever);//de-register reciever
    }

    @Override
    public void onReceiveMessage(Message msg) {
        switch (msg.what) {
            case 0: {
                checkGps();
                activeMethod = 0;
                getPoint();
                break;
            }
            case 1: {
                checkGps();
                activeMethod = 1;
                recordRoute(msg.getData().getBoolean("toCampus"), (Campus) msg.getData().getSerializable("campus"));
                break;
            }
            case 2: {
                checkGps();
                activeMethod = 2;
                geoHashes = (ArrayList<String>) msg.getData().getSerializable("geohashes");
                toCampus = msg.getData().getBoolean("toCampus");
                Campus c = null;
                if (toCampus)
                    c = (Campus) msg.getData().get("campus");

                beginRide(toCampus, c, geoHashes.get(geoHashes.size() - 1));
                break;
            }
            case 3: {
                checkGps();
                String userId = msg.getData().getString("userId");
                WGS84Point point = (WGS84Point) msg.getData().getSerializable("point");

                addPotentialPassengers(userId, point);
                break;
            }
            case 4: {
                checkGps();
                isOnRoute();
                break;
            }
            case 5: {
                getRoute();
                break;
            }
            case 6: {
                getRideStats();
                break;
            }
            case 7: {
                getLastKnownLocation();
                break;
            }
            case 8: {
                cancelActiveMethod();
                break;
            }
            case 9: {
                destinationReached(null);
                break;
            }
            case 10:{
                setPassengerDropoffPoint(msg.getData().getString("userId"), (WGS84Point)msg.getData().getSerializable("point"));
            }
        }
    }

    public void passengerDropoffAhead(Intent intent)
    {
        Message msg = new Message();
        msg.what = 10;
        msg.getData().putString("user", intent.getExtras().getString("user"));
        send(msg);
    }

    private void setPassengerDropoffPoint(String userId, WGS84Point point) {
        Intent intent = new Intent(ENTRY_POINT_INTENT);
        intent.putExtra("ep", "endpoint");
        intent.putExtra("user", userId);
        PendingIntent pIntent = PendingIntent.getBroadcast(this.getApplicationContext(), rand.nextInt(), intent, PendingIntent.FLAG_ONE_SHOT);
        service.addProximityAlert(point.getLatitude(), point.getLongitude(), RADIUS_FROM_ENTRYPOINT, -1, pIntent);
        proximityAlerts.add(pIntent);
    }

    public void passengerAhead(Intent intent)
    {
        Message msg = new Message();
        msg.what = 2;
        msg.arg1 = 16;
        msg.getData().putString("user", intent.getExtras().getString("pp"));
        msg.getData().putInt("distance", intent.getExtras().getInt("which") == 1 ? (int) FIRST_PASSENGER_ALERT : (int) SECOND_PASSENGER_ALERT);
        send(msg);
    }

    private void recordRoute(boolean toCampus, Campus campus) {//msg will have entrypoints,tocampus arg1 = toCampus, arg2 = entrypoint count
        routePoints = new ArrayList<>();
        proximityAlerts = new ArrayList<>();
        geoHashes = new ArrayList<>();
        service.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 4, this);
        this.toCampus = toCampus;

        Iterator<Map.Entry<String, WGS84Point>> entryPoints = campus.getEntryPointsIterator();

        while (entryPoints.hasNext()) {
            Map.Entry<String, WGS84Point> entry = entryPoints.next();
            Intent intent = new Intent(ENTRY_POINT_INTENT);
            intent.putExtra("ep", entry.getKey());
            PendingIntent pIntent = PendingIntent.getBroadcast(this, rand.nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            service.addProximityAlert(entry.getValue().getLatitude(), entry.getValue().getLongitude(), RADIUS_FROM_ENTRYPOINT, -1, pIntent);
            proximityAlerts.add(pIntent);
        }

        if (toCampus) {
            begun = true;
            Message msg = new Message();
            msg.what = 1;
            msg.arg1 = 12;
            send(msg);
        }
    }

    public void begin(Intent intent)//start recording or ride, if from campus
    {
        Log.i("begininng recording", " begining");
        if (activeMethod == 1) {//notify activity that route recording begun
            begun = true;
            Message msg = new Message();
            msg.what = 1;
            msg.arg1 = 12;

            if (intent != null)
                msg.getData().putString("ep", intent.getExtras().getString("ep"));

            send(msg);
            return;
        }

//        if(activeMethod == 2)//notify activity that ride has begun
//        {
//            Message msg = new Message();
//            msg.what = 2;
//            msg.arg1 = 12;
//            send(msg);
//            return;
//        }
    }

    private void getRoute()//get route for mapping
    {
        if(routePoints.size() > 0) {
            Message msg = new Message();
            msg.what = 5;
            Bundle data = new Bundle();
            data.putSerializable("points", returnArray(routePoints, new WGS84Point[1]));
            msg.setData(data);
            send(msg);
        }
    }

    public void destinationReached(Intent intent) {
        Log.i("campus reached: ", "campus reached");
        service.removeUpdates(this);//stop receiving updates from location service

        if (activeMethod == 1) {
            Message msg = new Message();
            msg.what = 1;
            msg.arg1 = 14;

            if(routePoints.size() > 0) {
                msg.getData().putBoolean("hasPoints", true);
                msg.getData().putSerializable("points", returnArray(routePoints, new WGS84Point[1]));
                msg.getData().putSerializable("geohashes", returnArray(geoHashes, new String[1]));
                msg.getData().putDouble("radialDistance", VincentyGeodesy.distanceInMeters(routePoints.get(0), routePoints.get(routePoints.size() - 1)));
            }else msg.getData().putBoolean("hasPoints", false);

            if (intent != null)
                msg.getData().putString("ep", intent.getExtras().getString("ep"));

            send(msg);
            begun = false;
            service.removeUpdates(this);
            cancelProximityAlerts();
            stopSelf();
        }

        if (activeMethod == 2) {
            Message msg = new Message();
            msg.what = 2;
            msg.arg1 = 14;

            msg.getData().putInt("avgSpeed", count > 0 ? sumOfSpeed / count : 0);
            msg.getData().putInt("rating", count > 0 ? (int) ((nrBelowSpeedLimit / count) * 100) : 100);

            if (intent != null)
                msg.getData().putString("ep", intent.getExtras().getString("ep"));

            send(msg);
            begun = false;
            service.removeUpdates(this);
            cancelProximityAlerts();
            stopSelf();
        }
    }

    private void getLastKnownLocation() {
        if (service == null)
            service = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(lastKnownLocation == null) {
            Location loc = service.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(loc != null) {
                Message msg = new Message();
                msg.what = 7;
                msg.getData().putSerializable("point", new WGS84Point(loc.getLatitude(), loc.getLongitude()));
                send(msg);
            }
        } else
        {
            Message msg = new Message();
            msg.what = 7;
            msg.getData().putSerializable("point", lastKnownLocation);
            send(msg);
        }
    }

    private void getPoint()//get a point
    {
        service.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
    }

    private void isOnRoute()//send isOnRoute
    {
        Message msg = new Message();
        msg.what = 4;
        msg.getData().putBoolean("onRoute", isOnRoute);
        msg.getData().putSerializable("point", lastKnownLocation);

        send(msg);
    }

    private void monitorSpeed(int speed)//calculate ride rating
    {
        sumOfSpeed = sumOfSpeed + speed;
        count++;

        if (speed <= SPEED_LIMIT)
            nrBelowSpeedLimit++;
    }

    private void beginRide(boolean toCampus, Campus campus, String geohash)// set entrypoint proximity if to campus and route end proximity if from campus
    {
        proximityAlerts = new ArrayList<>();
        service.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 100, this);

        if (toCampus) {
            Iterator<Map.Entry<String, WGS84Point>> entryPoints = campus.getEntryPointsIterator();

            while (entryPoints.hasNext()) {
                Map.Entry<String, WGS84Point> entry = entryPoints.next();
                Intent intent = new Intent(ENTRY_POINT_INTENT);
                intent.putExtra("ep", entry.getKey());
                PendingIntent pIntent = PendingIntent.getBroadcast(this.getApplicationContext(), rand.nextInt(), intent, PendingIntent.FLAG_ONE_SHOT);
                service.addProximityAlert(entry.getValue().getLatitude(), entry.getValue().getLongitude(), RADIUS_FROM_ENTRYPOINT, -1, pIntent);
                proximityAlerts.add(pIntent);
            }
        } else {
            GeoHash g = GeoHash.fromGeohashString(geohash);

            Intent intent = new Intent(ENDPOINT_INTENT);
            intent.putExtra("ep", "endpoint");
            PendingIntent pIntent = PendingIntent.getBroadcast(this.getApplicationContext(), rand.nextInt(), intent, PendingIntent.FLAG_ONE_SHOT);
            service.addProximityAlert(g.getPoint().getLatitude(), g.getPoint().getLongitude(), RADIUS_FROM_ENTRYPOINT, -1, pIntent);
            proximityAlerts.add(pIntent);
        }
    }

    private void getRideStats()//send ride rating
    {
        Message msg = new Message();
        msg.what = 6;
        msg.getData().putInt("avgSpeed", sumOfSpeed / count);
        msg.getData().putInt("rating", (int) ((nrBelowSpeedLimit / count) * 100));
        send(msg);
    }

    private void addPotentialPassengers(String userId, WGS84Point point)//add point with passenger to watch for proximity
    {
        if (activeMethod == 2) {
            if (proximityAlerts == null)
                proximityAlerts = new ArrayList<>();

            Intent intent = new Intent(PASSENGER_PICKUP_INTENT);
            intent.putExtra("which", 1);
            intent.putExtra("pp", userId);//get the userId of passenger passed in bundle as pp{index}_id
            PendingIntent pIntent = PendingIntent.getBroadcast(this.getApplicationContext(), rand.nextInt(), intent, PendingIntent.FLAG_ONE_SHOT);
            service.addProximityAlert(point.getLatitude(), point.getLongitude(), FIRST_PASSENGER_ALERT, -1, pIntent);
            proximityAlerts.add(pIntent);

            intent = new Intent(PASSENGER_PICKUP_INTENT);
            intent.putExtra("which", 2);
            intent.putExtra("pp", userId);//get the userId of passenger passed in bundle as pp{index}_id
            pIntent = PendingIntent.getBroadcast(this.getApplicationContext(), rand.nextInt(), intent, PendingIntent.FLAG_ONE_SHOT);
            service.addProximityAlert(point.getLatitude(), point.getLongitude(), SECOND_PASSENGER_ALERT, -1, pIntent);
            proximityAlerts.add(pIntent);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        switch (activeMethod) {
            case 0: {
                if (location.getAccuracy() < ACCURACY_OF_POINTS) {
                    lastKnownLocation = new WGS84Point(location.getLatitude(), location.getLongitude());
                    Message msg = new Message();
                    msg.what = 0;
                    msg.getData().putSerializable("point", new WGS84Point(location.getLatitude(), location.getLongitude()));
                    send(msg);
                    stopSelf();
                } else getPoint();
                break;
            }
            case 1: {
                if (location.getAccuracy() < ACCURACY_OF_POINTS && begun) {
                    routePoints.add(new WGS84Point(location.getLatitude(), location.getLongitude()));
                    monitorSpeed((int) location.getSpeed());
                    String geohash = GeoHash.geoHashStringWithCharacterPrecision(location.getLatitude(), location.getLongitude(), 7);
                    lastKnownLocation = new WGS84Point(location.getLatitude(), location.getLongitude());

                    int size = geoHashes.size();
                    if (size > 0) {
                        if (geoHashes.get(size-1).compareTo(geohash) != 0)
                            geoHashes.add(geohash);
                    } else geoHashes.add(geohash);
                }

                break;
            }
            case 2: {
                monitorSpeed((int) location.getSpeed());
                String geohash = GeoHash.geoHashStringWithCharacterPrecision(location.getLatitude(), location.getLongitude(), 7);
                lastKnownLocation = new WGS84Point(location.getLatitude(), location.getLongitude());

                for (String s : geoHashes) {
                    if (s.compareTo(geohash) == 0) {
                        isOnRoute = true;
                        return;
                    }
                }

                isOnRoute = false;

                break;
            }
        }
    }

    private void checkGps() {
        if (service == null)
            service = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(intent);
        }
    }

    private void cancelActiveMethod() {
        cancelProximityAlerts();
        service.removeUpdates(this);
        stopSelf();
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    public void cancelProximityAlerts() {
        if (proximityAlerts != null && proximityAlerts.size() != 0)//cancel all proximity alerts
        {
            for (PendingIntent p : proximityAlerts) {
                p.cancel();
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        if (provider.compareTo(LocationManager.GPS_PROVIDER) == 0) {
            if (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                Message msg = new Message();
                msg.what = 101;//error
                send(msg);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.compareTo(LocationManager.GPS_PROVIDER) == 0) {
            checkGps();
        }
    }

    public <T> T[] returnArray(ArrayList<T> objects, T[] array) {
        array = objects.toArray(array);
        if (toCampus)//reverse points, so that points go from campus to begin location
        {
            T[] obj1 = (T[])Array.newInstance(array.getClass().getComponentType(), array.length);
            int j = 0;
            for (int i = array.length - 1; i >= 0; i--) {
                obj1[j++] = array[i];
            }
            return obj1;
        } else return array;
    }

    @Override
    public void onGpsStatusChanged(int event) {
        if(event == GpsStatus.GPS_EVENT_FIRST_FIX)
        {

        }
    }
}
