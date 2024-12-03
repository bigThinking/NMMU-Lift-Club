package za.ac.nmmu.lift_club.util;

import android.animation.FloatArrayEvaluator;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;

import ch.hsr.geohash.WGS84Point;
import za.ac.nmmu.lift_club.MainActivity;

/**
 * Created by s210036575 on 2015-08-11.
 */
public class Activity implements Serializable{

    public static final int TYPE_SEPARATOR = 0;//has layout: done

    public static final int TYPE_RECORDING_ROUTE = 1;//has layout : done
    public static final int TYPE_ROUTE_RECORDED = 7;//done
    public static final int TYPE_PRERECORD_BEGIN = 8;//done
    public static final int TYPE_SAVING_ROUTE = 9;//done

    public static final int TYPE_SAVING_POINT = 10;

    public static final int TYPE_ONGOING_RIDE = 6;//has layout-used by driver

    public static final int TYPE_SEARCHING_RIDES = 11;//has layout

    public static final int TYPE_PASSENGER_FOUND = 3;//has layout
    public static final int TYPE_RIDE_FOUND = 4;//has layout
    public static final int TYPE_ACK_PICKUP = 5;
    public static final int TYPE_IN_RIDE = 12;

    public static final int TYPE_COMMENT_PENDING = 2;//has layout



    public static final int TYPE_COUNT = 13;//count of the number of types
    public int activityType;

    //used by CreateRouteFragment
    public int fragmentState = 0;
    public boolean mapHidden = false, toCampus, gottenRouteDetails;
    public String campusId = "", routeName = "", entryPoint = "";
    public WGS84Point[] recordedRoute = new WGS84Point[1];
    public String[] geohashes = new String[1];
    public double radialDistance = 0;

    //used by seperator
    public String seperatorText;

    //used by saving point
    public WGS84Point point;
    public String pointName = "";

    //used for ongoing ride
    public Ride ride;
    public boolean activated = false;

    //used for passenger found && driver found
    public Match match;
    public User userInfo;
    public Car carInfo;
    public boolean isAccepted = false;

    //used for searching for rides
    public RideQuery qry;

    public static ArrayList<Activity> parse(String json) throws JSONException {
        ArrayList<Activity> activities = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(json);

        JSONArray jsonActivities = jsonObject.getJSONArray("activities");

        for(int i = 0; i < jsonActivities.length(); i++)
        {
            JSONObject activity = jsonActivities.getJSONObject(i);
            Activity a = new Activity(activity.toString());
            activities.add(a);
        }

        return activities;
    }


    public Activity(int type)
    {
        activityType = type;
    }

    public String toJson() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginObject().name("type").value(activityType);

        switch (activityType)
        {
            case TYPE_SEPARATOR:{
                writer.name("seperatorText").value(seperatorText);
                break;
            }
            case TYPE_RECORDING_ROUTE:
            case TYPE_ROUTE_RECORDED:
            case TYPE_PRERECORD_BEGIN:
            case TYPE_SAVING_ROUTE:{
                writer.name("fragmentState").value(fragmentState).name("mapHidden").value(mapHidden)
                        .name("toCampus").value(toCampus).name("gottenRouteDetails").value(gottenRouteDetails)
                        .name("campus").value(campusId).name("radialDistance").value(radialDistance)
                        .name("routeName").value(routeName).name("entryPoint").value(entryPoint);

                if(recordedRoute != null && recordedRoute.length > 1)
                {
                    writer.name("points").beginArray();
                    for(WGS84Point p : recordedRoute )
                    {
                        writer.value(p.toString());
                    }
                    writer.endArray();
                }

                if(geohashes != null)
                {
                    writer.name("geohashes").beginArray();
                    for(String p : geohashes)
                    {
                        writer.value(p);
                    }
                    writer.endArray();
                }


                break;
            }
            case TYPE_SAVING_POINT:{
                writer.name("pointName").value(pointName).name("lat").value(point.getLatitude()).name("lon").value(point.getLongitude());
                break;
            }
            case TYPE_ONGOING_RIDE:{
                writer.name("ride").value(ride.getJSON());
                break;
            }
            case TYPE_SEARCHING_RIDES:{
                writer.name("rideQuery").value(qry.getJson());
                break;
            }
            case TYPE_PASSENGER_FOUND:
            case TYPE_RIDE_FOUND:
            case TYPE_ACK_PICKUP:
            case TYPE_IN_RIDE:{
                writer.name("match").value(match==null ? " " : match.getJson()).name("userInfo").value(userInfo == null ? " " : userInfo.toJson())
                        .name("carInfo").value(carInfo == null ? " " : carInfo.toJson()).name("isAccepted").value(isAccepted);
            }
        }
        writer.endObject().flush();
        writer.close();
        return sw.toString();
    }

    public Activity(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);

        activityType = jsonObject.getInt("type");

        switch (activityType)
        {
            case TYPE_SEPARATOR:{
                seperatorText = jsonObject.getString("seperatorText");
                break;
            }
            case TYPE_RECORDING_ROUTE:
            case TYPE_ROUTE_RECORDED:
            case TYPE_PRERECORD_BEGIN:
            case TYPE_SAVING_ROUTE:{
                fragmentState = jsonObject.getInt("fragmentState");
                mapHidden = jsonObject.getBoolean("mapHidden");
                toCampus = jsonObject.getBoolean("toCampus");
                gottenRouteDetails = jsonObject.getBoolean("gottenRouteDetails");
                campusId = jsonObject.getString("campus");
                radialDistance = jsonObject.getDouble("radialDistance");
                routeName = jsonObject.getString("routeName");
                entryPoint = jsonObject.getString("entryPoint");

                JSONArray arr = jsonObject.getJSONArray("points");
                if(arr != null)
                {
                    recordedRoute = new WGS84Point[arr.length()];
                    for(int i = 0; i < arr.length(); i++)
                    {
                        String element = arr.getString(i);
                        String[] parts = element.split(",");

                        if(parts.length == 2)
                        recordedRoute[i] = new WGS84Point(Double.valueOf(parts[0].replace("(", "")),Double.valueOf(parts[1].replace(")", "")));
                    }
                }

                arr = jsonObject.getJSONArray("geohashes");
                if(arr != null)
                {
                    geohashes= new String[arr.length()];
                    for(int i = 0; i < arr.length(); i++)
                    {
                        String element = arr.getString(i);
                        geohashes[i] = element;
                    }
                }

                break;
            }
            case TYPE_SAVING_POINT:{
                pointName = jsonObject.getString("pointName");
                double lat = jsonObject.getDouble("lat");
                double lon = jsonObject.getDouble("lon");
                point = new WGS84Point(lat,lon);
                break;
            }
            case TYPE_ONGOING_RIDE:{
                try {
                    ride = new Ride(jsonObject.getString("ride"));
                } catch (BadMessageException e) {
                    e.printStackTrace();
                    Log.i("bad ride", "bad ride");
                }
                break;
            }
            case TYPE_SEARCHING_RIDES:{
                try {
                    qry = new RideQuery(jsonObject.getString("rideQuery"));
                } catch (BadMessageException e) {
                    e.printStackTrace();
                    Log.i("bad qry", "bad qry");
                }
                break;
            }
            case TYPE_IN_RIDE:
            case TYPE_RIDE_FOUND:
            case TYPE_ACK_PICKUP:
            case TYPE_COMMENT_PENDING:
            case TYPE_PASSENGER_FOUND: {
                Match match = null;
                try {
                    String matchString = jsonObject.getString("match");
                    match = new Match(new RideQuery());

                    JSONObject obj = new JSONObject(matchString);
                    match.setRidePoint(obj.getString("ridePoint"));
                    match.setPrice(obj.getDouble("price"));
                    match.setCurrentDriverLocation("currentDriverLocation");
                    match.setRide(new Ride(obj.getString("ride")));

                    userInfo = new User(jsonObject.getString("userInfo"));
                    isAccepted = jsonObject.getBoolean("isAccepted");

                    if(match.getRide().getDriverId().equals(userInfo.userId))
                    carInfo = new Car(jsonObject.getString("carInfo"));
                } catch (BadMessageException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
