package za.ac.nmmu.lift_club;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.os.Bundle;

import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.philippheckel.service.AbstractService;
import com.philippheckel.service.ServiceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import de.hdodenhof.circleimageview.CircleImageView;
import za.ac.nmmu.lift_club.services.GpsService;
import za.ac.nmmu.lift_club.services.HttpCommService;
import za.ac.nmmu.lift_club.services.SockCommService;
import za.ac.nmmu.lift_club.util.Activity;
import za.ac.nmmu.lift_club.util.BadMessageException;
import za.ac.nmmu.lift_club.util.Campus;
import za.ac.nmmu.lift_club.util.Car;
import za.ac.nmmu.lift_club.util.Match;
import za.ac.nmmu.lift_club.util.MessageTypes;
import za.ac.nmmu.lift_club.util.Position;
import za.ac.nmmu.lift_club.util.Ride;
import za.ac.nmmu.lift_club.util.RideQuery;
import za.ac.nmmu.lift_club.util.Route;
import za.ac.nmmu.lift_club.util.User;

public class MainActivity extends AppCompatActivity {

    public static final String PREF_NAME = "za.ac.nmmu.lift_club";
    public static final byte FRAGMENT_ACTIVITIES = 0;
    public static final byte FRAGMENT_CURR_POS = 1;
    public static final byte FRAGMENT_REC_ROUTE = 2;
    public static final byte FRAGMENT_OFFER = 3;
    public static final byte FRAGMENT_FIND = 4;
    public static final byte FRAGMENT_SETTINGS = 5;

    public ArrayList<Activity> activities = new ArrayList<>();
    public ArrayList<Campus> campuses = new ArrayList<>();
    public ArrayList<Position> points = new ArrayList<>();
    public ArrayList<Route> routes = new ArrayList<>();

    private DrawerLayout mDrawerLayout;
    public boolean recordingRoute = false;
    public boolean rideOngoing = false;
    public ServiceManager gps, http, sock;
    private Fragment currentFragment = null;
    public WGS84Point lastLocation;
    public byte currentFragmentId;
    public String token;
    public Pull pull;
    public User currentUser;
    public Car currentCar;
    private int nowActivityPos = 0, laterActivityPos = 1, otherActivityPos = 2;
    private boolean haveShownNotConnectedMessage = false, sockAuthorised = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initHttp();
        initGps();
        //Log.i("cache directory: ", getApplicationContext().getCacheDir().getAbsolutePath());
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        token = preferences.getString("token", null);
        String userInfo = preferences.getString("user", null);
        String campusInfo = preferences.getString("campus", null);
        String points = preferences.getString("points", null);
        String activities = preferences.getString("activities", null);
        boolean justLoggedIn = preferences.getBoolean("justLoggedIn", true);
        sockAuthorised = preferences.getBoolean("sockAuthorised", false);

        if (token == null || userInfo == null || campusInfo == null) {//try getting data, if don't exist, go to login activity
            Intent intent = new Intent(this.getApplicationContext(), LoginActivity.class);
            finish();
            startActivity(intent);
        } else {
            try {
                initSock();
                this.currentUser = new User(userInfo);
                if (this.currentUser.nrPointsSaved > 0 && points == null) {
                    showDialog("Invalid data", "Some data was not initialised correctly. Please try logging in again");
                    doLogout();
                } else if (this.currentUser.nrPointsSaved > 0) {
                    this.points = Position.parse(points);
                }

                if (!imageExist(this.currentUser.pic_id)) {
                    Message msg1 = new Message();
                    msg1.what = 18;
                    msg1.getData().putString("imageId", this.currentUser.pic_id);
                    httpSend(msg1);
                } else {
                    Log.i("image: ", "Setting image");
                    Bitmap bitmap = readImage(this.currentUser.pic_id);
                    CircleImageView img = (CircleImageView) findViewById(R.id.pic);
                    img.setImageBitmap(bitmap);
                }

                //get ride matchs
                if(justLoggedIn) {
                    Message msg = new Message();
                    msg.what = 19;
                    msg.getData().putString("token", token);
                    httpSend(msg);
                }


                if (this.currentUser.isDriver) {
                    String currentCar = preferences.getString("currentCar", null);

                    if (currentCar == null) {
                        showDialog("Invalid data", "Some data was not intialised correctly. Please try logging in again");
                        doLogout();
                    } else {
                        this.currentCar = new Car(currentCar);

                        if (!imageExist(this.currentCar.pic_id)) {
                            Message msg1 = new Message();
                            msg1.what = 18;
                            msg1.getData().putString("imageId", this.currentCar.pic_id);
                            httpSend(msg1);
                        } else {
                            Bitmap bitmap = readImage(this.currentCar.pic_id);
                            CircleImageView img = (CircleImageView) findViewById(R.id.car_pic);
                            img.setImageBitmap(bitmap);
                        }
                    }

                    //todo other cars details,  should only be obtained on edit car details
                    String routes = preferences.getString("routes", null);
                    if (this.currentUser.nrRoutesRecorded > 0 && routes == null) {
                        showDialog("Invalid data", "Some data was not intialised correctly. Please try logging in again");
                        doLogout();
                    } else if (this.currentUser.nrRoutesRecorded > 0) {
                        this.routes = Route.parse(routes);
                    }

                    if (justLoggedIn) {
                        //get current rides
                        Message msg1 = new Message();
                        msg1.what = 7;
                        msg1.getData().putString("token", token);
                        try {
                            httpSend(msg1);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("justLoggedIn", false);
                        editor.apply();
                    }
                }else{
                    CircleImageView imgCar = (CircleImageView)findViewById(R.id.car_pic);
                    imgCar.setVisibility(View.GONE);
                }

                campuses = Campus.parse(campusInfo);
                if (justLoggedIn || activities == null) {
                    Activity activity = new Activity(Activity.TYPE_SEPARATOR);
                    activity.seperatorText = "Now";
                    Activity activity1 = new Activity(Activity.TYPE_SEPARATOR);
                    activity1.seperatorText = "Later";
                    Activity activity2 = new Activity(Activity.TYPE_SEPARATOR);
                    activity2.seperatorText = "Other";

                    this.activities.add(activity);
                    this.activities.add(activity1);
                    this.activities.add(activity2);
                } else {
                    this.activities = Activity.parse(activities);
                    nowActivityPos = preferences.getInt("nowActivityIndex", nowActivityPos);
                    laterActivityPos = preferences.getInt("laterActivityIndex", laterActivityPos);
                    otherActivityPos = preferences.getInt("otherActivityIndex", otherActivityPos);
                }

                if (!justLoggedIn) {
                    recordingRoute = preferences.getBoolean("recordingRoute", false);
                    rideOngoing = preferences.getBoolean("rideOngoing", false);

                    double lat, lon;
                    lat = preferences.getFloat("lastLocLat", 0);
                    lon = preferences.getFloat("lastLocLon", 0);

                    if (!(lat == 0 && lon == 0))
                        lastLocation = new WGS84Point(lat, lon);
                }

                Intent intent = getIntent();
                if (intent.getIntExtra("za.ac.nmmu.lift_club.type", 0) == 25) {
                    // indicate to driver that its time to begin ride
                    Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(500);
                    showDialog("Time to begin ride", "Click activate to begin ride");
                }
            } catch (JSONException | ParseException e) {
                showDialog("Error occurred", "LiftClub will now exit");
                doLogout();
                e.printStackTrace();
            } catch (RemoteException e) {
                showDialog("Error occurred", "LiftClub will now exit");
                doLogout();
                e.printStackTrace();
            }

            Message msg = new Message();
            msg.what = 7;//get last known location
            try {
                gps.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            final ActionBar ab = getSupportActionBar();
            ab.setHomeAsUpIndicator(R.drawable.ic_action_menu);
            ab.setDisplayHomeAsUpEnabled(true);

            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            if (navigationView != null) {
                setupDrawerContent(navigationView);
            }

            if (savedInstanceState == null) {
                setCurrentFragment(FRAGMENT_ACTIVITIES);
                setTitle("Activities");
                // navigationView.getMenu().getItem(0).setChecked(true);
            } /*else {
            setCurrentFragment(savedInstanceState.getByte("currentFragmentId"));
        }*/
        }
    }

    private void setChecked()
    {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        switch (currentFragmentId)
        {
            case FRAGMENT_ACTIVITIES:{
                navigationView.getMenu().getItem(0).setChecked(true);
                setTitle(navigationView.getMenu().getItem(0).getTitle());
                break;
            }
            case FRAGMENT_CURR_POS:{
                navigationView.getMenu().getItem(4).setChecked(true);
                setTitle(navigationView.getMenu().getItem(4).getTitle());
                break;
            }
            case FRAGMENT_REC_ROUTE:{
                navigationView.getMenu().getItem(3).setChecked(true);
                setTitle(navigationView.getMenu().getItem(3).getTitle());
                break;
            }
            case FRAGMENT_OFFER:{
                navigationView.getMenu().getItem(2).setChecked(true);
                setTitle(navigationView.getMenu().getItem(2).getTitle());
                break;
            }
            case FRAGMENT_SETTINGS:{
                break;
            }
            case FRAGMENT_FIND:{
                navigationView.getMenu().getItem(1).setChecked(true);
                setTitle(navigationView.getMenu().getItem(1).getTitle());
                break;
            }
        }
    }

    public void startRecording(Activity activity) {
        initGps();
        putActivity(activity);
        Message msg = new Message();
        msg.what = 1;
        msg.getData().putBoolean("toCampus", activity.toCampus);
        msg.getData().putSerializable("campus", getCampusById(activity.campusId));

        try {
            gps.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        recordingRoute = true;

        pull = new Pull(0);
        pull.start();
    }

    @Override
    protected void onDestroy ()
    {
        if(gps != null)
            gps.unbind();

        if(sock != null)
            sock.unbind();

        if(http != null)
            http.unbind();

        super.onDestroy();
    }

    public void saveCurrentRouteRecording(Activity activity) {
        initHttp();
        Message msg = new Message();

        msg.what = 10;
        msg.getData().putString("token", token);
        msg.getData().putString("name", activity.routeName);

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (WGS84Point p : activity.recordedRoute) {
            sb.append(p.toString());
            sb.append(",");
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");

        msg.getData().putString("points", sb.toString());
        msg.getData().putDouble("radialDistance", activity.radialDistance);

        sb = new StringBuilder();
        // sb.append("[\"");//send to server to be stored as json array
        sb.append("[");

        for (String s : activity.geohashes) {
            sb.append(s);
            sb.append(",");
            // sb.append("\",\"");
        }

        sb.deleteCharAt(sb.length() - 1);
        //sb.delete(sb.length() - 2, sb.length()-1);
        sb.append("]");

        msg.getData().putString("geoHashes", sb.toString());
        msg.getData().putString("entryPoint", activity.entryPoint);
        msg.getData().putString("campus", activity.campusId);
        //todo route price(leaving it out for now)

        try {
            httpSend(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelRouteRecording(Activity activity) {
        pull.doStop();
        removeActivity(activity);
        Message msg = new Message();
        msg.what = 8;

        try {
            gps.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        recordingRoute = false;
    }

    private void doLogout() {
        initHttp();
        Message msg = new Message();
        msg.what = 1;
        msg.getData().putString("token", token);
        try {
            httpSend(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            showDialog("Error logging out", "Could not contact server");
            return;
        }

        if (gps != null)
            gps.stop();

        if (sock != null)
            sock.stop();
        //http stops itself in order to allow it perform pending task

        token = null;
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("token");
        editor.remove("user");
        editor.remove("campus");
        editor.remove("activities");
        editor.remove("points");
        editor.remove("routes");
        editor.remove("currentCar");
        editor.remove("activities");
        editor.remove("currentFragment");
        editor.remove("recordingRoute");
        editor.remove("rideOngoing");
        editor.remove("nowActivityIndex");
        editor.remove("laterActivityIndex");
        editor.remove("otherActivityIndex");
        editor.remove("lastLocLat");
        editor.remove("lastLocLon");
        editor.remove("justLoggedIn");
        editor.remove("sockAuthorised");
        editor.commit();

        finish();
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
    }

    private void pointReceived(WGS84Point point) {
        lastLocation = point;

        if (currentFragmentId == FRAGMENT_CURR_POS) {
            Activity a = new Activity(Activity.TYPE_SAVING_POINT);
            a.point = point;
            ((SavePositionFragment) currentFragment).update(a);
        }

        if(currentFragmentId == FRAGMENT_FIND)
        {
            ((FindLiftFragment)currentFragment).pointReady();
        }
    }

    private void routeRecordingFinished(WGS84Point[] points, String[] geohashes, double radialDistance, String entryPoint) {
        if (pull != null)
            pull.doStop();

        Activity a = null;
        for (int i = 0; i < activities.size(); i++) {
            a = activities.get(i);
            if (a.activityType == Activity.TYPE_PRERECORD_BEGIN || a.activityType == Activity.TYPE_RECORDING_ROUTE || a.activityType == Activity.TYPE_ROUTE_RECORDED) {
                break;
            }
        }

        a.recordedRoute = points;
        a.radialDistance = radialDistance;
        a.geohashes = geohashes;
        a.activityType = Activity.TYPE_ROUTE_RECORDED;
        a.fragmentState = CreateRouteFragment.RECORDED_STATE;
        a.gottenRouteDetails = true;

        if(entryPoint != null)
            a.entryPoint = entryPoint;

        if (currentFragmentId == FRAGMENT_REC_ROUTE)
            ((CreateRouteFragment) currentFragment).update(a);
        else if (currentFragmentId == FRAGMENT_ACTIVITIES)
            updateActivitiesFragment();

        recordingRoute = false;
    }

    private void routeRecordingBegun(String entryPoint) {
        Activity a = null;
        for (int i = 0; i < activities.size(); i++) {
            a = activities.get(i);
            if (a.activityType == Activity.TYPE_PRERECORD_BEGIN || a.activityType == Activity.TYPE_RECORDING_ROUTE) {
                break;
            }
        }

        if(a != null) {
            a.activityType = Activity.TYPE_RECORDING_ROUTE;
            a.entryPoint = entryPoint;
            if (currentFragmentId == FRAGMENT_REC_ROUTE)
                ((CreateRouteFragment) currentFragment).update(a);
            else if (currentFragmentId == FRAGMENT_ACTIVITIES)
                updateActivitiesFragment();

            recordingRoute = true;
        }
    }

    private void updateRoute(WGS84Point[] route) {
        if(route != null) {
            Activity a = null;
            for (int i = 0; i < activities.size(); i++) {
                a = activities.get(i);
                if (a.activityType == Activity.TYPE_PRERECORD_BEGIN || a.activityType == Activity.TYPE_RECORDING_ROUTE || a.activityType == Activity.TYPE_ROUTE_RECORDED) {
                    break;
                }
            }

            if(a != null) {
                a.recordedRoute = route;

                if (currentFragmentId == FRAGMENT_REC_ROUTE)
                    ((CreateRouteFragment) currentFragment).update(a);
                else if (currentFragmentId == FRAGMENT_ACTIVITIES)
                    updateActivitiesFragment();
            }
        }
    }

    public void stopRecording(Activity activity) {
        pull.doStop();
        Message msg = new Message();
        msg.what = 9;
        try {
            gps.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        recordingRoute = false;
    }

    public void putActivity(Activity activity) {
        //// place activity under right seperator
        switch (activity.activityType) {
            case Activity.TYPE_SAVING_POINT:
            case Activity.TYPE_RECORDING_ROUTE:
            case Activity.TYPE_ROUTE_RECORDED:
            case Activity.TYPE_PRERECORD_BEGIN:
            case Activity.TYPE_SAVING_ROUTE: {
                activities.add(nowActivityPos + 1, activity);
                laterActivityPos++;
                otherActivityPos++;
                break;
            }
            case Activity.TYPE_SEARCHING_RIDES:
            case Activity.TYPE_IN_RIDE:
            case Activity.TYPE_ACK_PICKUP: {
                activities.add(nowActivityPos + 1, activity);
                laterActivityPos++;
                otherActivityPos++;
                break;
            }
            case Activity.TYPE_PASSENGER_FOUND: {
                activities.add(laterActivityPos + 1, activity);
                otherActivityPos++;
                break;
            }
            case Activity.TYPE_ONGOING_RIDE: {
                if (activity.ride.isNow()) {
                    activities.add(nowActivityPos + 1, activity);
                    laterActivityPos++;
                    otherActivityPos++;
                } else {
                    activities.add(laterActivityPos + 1, activity);
                    otherActivityPos++;
                }
                break;
            }
            case Activity.TYPE_COMMENT_PENDING:
            case Activity.TYPE_RIDE_FOUND: {
                activities.add(laterActivityPos + 1, activity);
                otherActivityPos++;
                break;
            }
        }
    }

    public void removeActivity(Activity activity) {
        switch (activity.activityType) {
            case Activity.TYPE_SAVING_POINT:
            case Activity.TYPE_RECORDING_ROUTE:
            case Activity.TYPE_ROUTE_RECORDED:
            case Activity.TYPE_PRERECORD_BEGIN:
            case Activity.TYPE_SAVING_ROUTE: {
                laterActivityPos--;
                otherActivityPos--;
                break;
            }
            case Activity.TYPE_SEARCHING_RIDES:
            case Activity.TYPE_IN_RIDE:
            case Activity.TYPE_ACK_PICKUP: {
                laterActivityPos--;
                otherActivityPos--;
                break;
            }
            case Activity.TYPE_PASSENGER_FOUND: {
                otherActivityPos--;
                break;
            }
            case Activity.TYPE_ONGOING_RIDE: {
                if (activity.ride.isNow()) {
                    laterActivityPos--;
                    otherActivityPos--;
                } else {
                    otherActivityPos--;
                }
                break;
            }
            case Activity.TYPE_COMMENT_PENDING:
            case Activity.TYPE_RIDE_FOUND: {
                otherActivityPos--;
                break;
            }
        }
        activities.remove(activity);
        updateActivitiesFragment();
    }

    private void updateActivitiesFragment() {
        if (currentFragmentId == FRAGMENT_ACTIVITIES)
            ((ActivitiesFragment) currentFragment).update();
    }

    public void saveCurrentPosition(Activity a) {
        initHttp();

        Message msg = new Message();
        msg.what = 8;
        msg.getData().putString("token", token);
        msg.getData().putString("name", a.pointName);
        msg.getData().putSerializable("point", a.point);

        putActivity(a);
        a.activityType = Activity.TYPE_SAVING_POINT;

        try {
            if (isConnected())
                httpSend(msg);
            Toast.makeText(getApplicationContext(), "Sending data", Toast.LENGTH_LONG).show();
            setCurrentFragment(FRAGMENT_ACTIVITIES);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void offerRide(Activity a) {
        initSock();
        initGps();

        if (!recordingRoute) {
            Message msg = new Message();
            msg.what = 1;

            msg.getData().putString("ride", a.ride.getJSON());

            try {
                if (a.ride.isNow()) {
                    if (isConnected())
                        sock.send(msg);
                    else {
                        showDialog("Request not sent", "Couldn't send message, due to bad internet connection. Please try again later");
                        return;
                    }
                } else if (!a.ride.isNow()) {
                    boolean res = sockSend(msg);
                    if (!res)
                        showDialog("Request deferred", "Request will be sent on connecting to internet");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                showDialog("Request not sent", "Couldn't send message, please try again later");
                return;
            }

            if (a.ride.isNow()) {
                rideOngoing = true;
                a.activated = true;
                Message msg1 = new Message();
                msg1.what = 2;
                msg1.getData().putSerializable("geohashes", new ArrayList<>(Arrays.asList(getRouteById(a.ride.getRouteId()).geohashes)));
                msg1.getData().putBoolean("toCampus", a.ride.isToCampus());
                msg1.getData().putSerializable("campus", getCampusById(a.ride.getCampusId()));
                try {
                    gps.send(msg1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                pull = new Pull(1);
                pull.start();
            }
            putActivity(a);
            setCurrentFragment(FRAGMENT_ACTIVITIES);
        } else showDialog("GPS in use", "GPS in use by another operation");
    }

    private Route getRouteById(String routeId) {
        for (Route r : routes) {
            if (r.routeId.equals(routeId))
                return r;
        }

        return null;
    }

    public void selectRide(String matchId, Activity a, byte sender) {
        Message msg = new Message();
        msg.what = 5;
        msg.getData().putString("matchId", matchId);

        try {
            if (isConnected())
                sock.send(msg);
            else {
                Toast.makeText(getApplicationContext(), "Request not sent", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Request not sent", Toast.LENGTH_LONG).show();
            return;
        }

        if (sender == FRAGMENT_ACTIVITIES) {
            if (a != null && a.activityType == Activity.TYPE_RIDE_FOUND) {
                a.isAccepted = true;

                for (int i = laterActivityPos + 1; i < otherActivityPos; i++)//remove all unselected matches
                {
                    if ((activities.get(i).activityType == Activity.TYPE_RIDE_FOUND && !activities.get(i).isAccepted) || activities.get(i).activityType == Activity.TYPE_SEARCHING_RIDES)
                        activities.remove(i);
                }

                updateActivitiesFragment();
            }
        } else if (sender == FRAGMENT_FIND) {
            a.activityType = Activity.TYPE_ACK_PICKUP;
            putActivity(a);
            updateActivitiesFragment();
        }
    }

    public void searchForRide(Activity a) {
        initSock();

        Message msg = new Message();
        msg.what = 2;

        msg.getData().putString("rideQuery", a.qry.getJson());

        try {
            if (a.qry.isNow()) {
                if (isConnected())
                    sock.send(msg);
            } else if (!a.qry.isNow()) {
                sockSend(msg);
                showDialog("Ride query queued", "Your ride query will be sent when internet is enabled");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            showDialog("Error sending query", "Couldn't send message");
            setCurrentFragment(FRAGMENT_ACTIVITIES);
            return;
        }

        if(!a.qry.isNow()) {
            setCurrentFragment(FRAGMENT_ACTIVITIES);
            putActivity(a);
            updateActivitiesFragment();
        }
    }

    public void activateRide(Activity a) {
        if (!recordingRoute) {
            initGps();
            Message msg = Message.obtain();
            msg.what = 7;
            msg.getData().putString("rideId", a.ride.getRideId());

            try {
                if (isConnected())
                    sockSend(msg);
                else return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Message msg1 = Message.obtain();
            msg1.what = 2;
            msg1.getData().putSerializable("geohashes", new ArrayList<>(Arrays.asList(getRouteById(a.ride.getRouteId()).geohashes)));
            msg1.getData().putBoolean("toCampus", a.ride.isToCampus());
            msg1.getData().putSerializable("campus", getCampusById(a.ride.getCampusId()));
            try {
                gps.send(msg1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            pull = new Pull(1);

            for (int i = laterActivityPos + 1; i < otherActivityPos; i++) {
                if (activities.get(i).activityType == Activity.TYPE_ONGOING_RIDE) {
                    a = activities.remove(i);
                    a.activated = true;
                    putActivity(a);
                }

                if (a.ride.isToCampus() && activities.get(i).activityType == Activity.TYPE_PASSENGER_FOUND) {
                    Activity b = activities.remove(i);
                    b.activityType = Activity.TYPE_ACK_PICKUP;
                   putActivity(b);
                    addPotentialPassenger(b.match);
                }
            }
            a.ride.setIsNow(true);
            rideOngoing = true;
            updateActivitiesFragment();
        } else showDialog("GPS in use", "GPS in use by another operation");
    }

    public void doAck(boolean isSenderDriver, Activity a) {
        Message msg = new Message();
        msg.what = 3;
        msg.getData().putBoolean("isDriver", isSenderDriver);
        msg.getData().putString("qryId", a.match.getId());
        msg.getData().putString("rideId", a.match.getRide().getRideId());

        try {
            if (isConnected())
                sockSend(msg);
            else return;
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (isSenderDriver && !a.match.isToCampus()) {
            Message msg1 = new Message();
            msg1.what = 10;
            msg1.getData().putString("userId", a.match.getUserId());
            msg1.getData().putSerializable("point", GeoHash.fromGeohashString(a.match.getRidePoint()).getPoint());
            try {
                gps.send(msg1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        a.activityType = Activity.TYPE_IN_RIDE; //should not remove activity to use to send driver notifications and keep track of passengers
        updateActivitiesFragment();
    }

    public void cancelRide(String rideId, Activity a) {
        Message msg = Message.obtain();
        msg.what = 8;//lucky coincidence that both cancels are 8 :)
        msg.getData().putString("rideId", rideId);
        try {
            if (isConnected())
                sockSend(msg);
            else return;
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (a != null) {
            if (a.ride.isNow()) {
                if(pull != null)
                    pull.doStop();

                try {
                    Message msg1 = Message.obtain();
                    msg1.what = 8;
                    gps.send(msg1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                for (int i = laterActivityPos + 1; i < otherActivityPos; i++) {
                    if (activities.get(i).activityType == Activity.TYPE_PASSENGER_FOUND)
                        activities.remove(i);
                }
            }
            removeActivity(a);
        }

        rideOngoing = false;
    }

    private void getDriverCarProfile(String currentCarVRN, String matchId, boolean forNowRide) {
        initHttp();
        Message msg = Message.obtain();
        msg.getData().putString("token", token);
        msg.getData().putString("vrn", currentCarVRN);
        msg.getData().putString("matchId", matchId);
        msg.getData().putBoolean("forNowRide", forNowRide);

        try {
            httpSend(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void getUserProfile(Match match) {
        initHttp();
        Message msg = Message.obtain();
        msg.what = 13;
        msg.getData().putString("token", token);
        msg.getData().putString("userId", match.getUserId().equals(currentUser.userId) ? match.getRide().getDriverId() : match.getUserId());
        msg.getData().putString("matchId", match.getId());
        msg.getData().putBoolean("isDriver", match.getUserId().equals(currentUser.userId));
        msg.getData().putBoolean("forNowRide", match.isNow());

        try {
            httpSend(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void addPotentialPassenger(Match m) {
        Message msg = Message.obtain();
        msg.what = 3;
        msg.getData().putString("userId", m.getUserId());
        msg.getData().putSerializable("point", GeoHash.fromGeohashString(m.getRidePoint()).getPoint());

        try {
            gps.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void rideFinished(int avgSpeed, int rating) {
        initHttp();
        Activity a = null;
        for (int i = nowActivityPos + 1; i < laterActivityPos; i++) {
            if (activities.get(i).activityType == Activity.TYPE_ONGOING_RIDE)
                a = activities.get(i);
        }

        if (a != null) {
            for (int i = nowActivityPos; i < laterActivityPos; i++) {
                if (activities.get(i).activityType == Activity.TYPE_IN_RIDE) {
                    Message msg1 = Message.obtain();
                    msg1.what = 6;
                    msg1.getData().putString("qryId", a.match.getId());
                    msg1.getData().putString("rideId", a.match.getRide().getRideId());

                    //send Ackdropoff
                    try {
                        sockSend(msg1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    //remove activity and add comment pending
                    Activity passenger = activities.remove(i);
                    passenger.activityType = Activity.TYPE_COMMENT_PENDING;
                    putActivity(passenger);
                    updateActivitiesFragment();
                }
            }


            cancelRide(a.ride.getRideId(), a);

            //send system rating via http
            Message msg = Message.obtain();
            msg.what = 16;
            msg.getData().putString("token", token);
            msg.getData().putString("rideId", a.ride.getRideId());
            msg.getData().putString("rating", String.valueOf(rating));
            try {
                httpSend(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        rideOngoing = false;
    }

    private void showDialog(String title, String message) {
        try {
            // String[] message = response.split(":");
            ErrorDialogFragment edf = new ErrorDialogFragment();
            edf.errorMessage = message;
            edf.title = title;
            edf.show(getSupportFragmentManager(), "");
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    class Pull extends Thread {

        public int method, count = 0;
        public boolean stop;

        public Pull(int method) {
            this.method = method;
        }

        public void doStop() {
            this.stop = true;
        }

        @Override
        public void run() {
            while (!stop) {
                switch (method) {
                    case 0: {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        pullRecordedPoints();
                        break;
                    }
                    case 1: {
                        if(count == 0) {
                            pullCurrentPoint();
                            count++;
                        }

                        try {
                            Thread.sleep(6000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        pullCurrentPoint();
                    }
                }
            }
        }

        public void pullRecordedPoints() {
            Message msg = Message.obtain();
            msg.what = 5;

            try {
                gps.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void pullCurrentPoint() {
            Message msg = Message.obtain();
            msg.what = 4;
            try {
                Log.i("Puller :", "requesting current point");
                gps.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    class AsynCommHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:{
                    if(msg.arg1 == 10){
                        sockAuthorised = false;
                        Message msg1 = new Message();
                        msg1.what = 0;
                        msg1.getData().putString("token", token);
                        try {
                            sock.send(msg1);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    sockAuthorised = msg.getData().getBoolean("successful", false);
                    break;
                }
                case 20: {
                    if (msg.arg1 == 0) {
                        //todo check for errors, important authorisation error
                        //  String[] parts = msg.getData().getString("message").split(MessageTypes.DELIMITER);
                        Log.i("Sock error: ", msg.getData().getString("message"));
                    } else if (msg.arg1 == 1) {
                        String[] parts = msg.getData().getString("message").split(MessageTypes.DELIMITER);

                        switch (parts[0]) {
                            case MessageTypes.SELECTRIDERESPONSE: {
                                if (parts[1].startsWith("false")) {
                                    String[] parts1 = parts[1].split(":");

                                    for (int i = nowActivityPos + 1; i < otherActivityPos; i++) {
                                        Activity a = activities.get(i);
                                        if (a.activityType == Activity.TYPE_RIDE_FOUND && a.isAccepted && a.match.getId().equals(parts1[2])) {
                                            removeActivity(a);
                                            break;
                                        }
                                    }

                                    showDialog("Error selecting ride", parts1[1]);
                                } else {
                                    String[] parts1 = parts[1].split(":");
                                    for (int i = nowActivityPos + 1; i < laterActivityPos; i++) {
                                        Activity a = activities.get(i);
                                        if (a.activityType == Activity.TYPE_RIDE_FOUND && a.isAccepted && a.match.getId().equals(parts1[1]) && a.match.isNow()) {
                                            a.activityType = Activity.TYPE_ACK_PICKUP;
                                            updateActivitiesFragment();
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                            case MessageTypes.LATERPASSENGER: {
                                try {
                                    Match match = new Match(new RideQuery(parts[1]));

                                    try {
                                        JSONObject obj = new JSONObject(parts[1]);
                                        JSONObject mObj = obj.getJSONObject("match");
                                        match.setRidePoint(mObj.getString("ridePoint"));
                                        match.setPrice(mObj.getDouble("price"));
                                        match.setCurrentDriverLocation("currentDriverLocation");
                                        match.setRide(new Ride(mObj.getString("ride")));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        break;
                                    }

                                    Activity a = new Activity(Activity.TYPE_PASSENGER_FOUND);
                                    a.match = match;
                                    putActivity(a);
                                    updateActivitiesFragment();
                                    getUserProfile(a.match);
                                } catch (BadMessageException e) {
                                    e.printStackTrace();
                                    return;
                                }
                                break;
                            }
                            case MessageTypes.PASSENGERAHEAD: {
                                try {
                                    Match match = new Match(new RideQuery(parts[1]));

                                    try {
                                        JSONObject obj = new JSONObject(parts[1]);
                                        match.setRidePoint(obj.getString("ridePoint"));
                                        match.setPrice(obj.getDouble("price"));
                                        match.setCurrentDriverLocation("currentDriverLocation");
                                        match.setRide(new Ride(obj.getString("ride")));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        break;
                                    }

                                    Activity a = new Activity(Activity.TYPE_ACK_PICKUP);
                                    a.match = match;
                                    putActivity(a);

                                    if (match.isNow()) {
                                        addPotentialPassenger(match);
                                    }

                                    updateActivitiesFragment();
                                    getUserProfile(a.match);
                                } catch (BadMessageException e) {
                                    e.printStackTrace();
                                    return;
                                }
                                break;
                            }
                            case MessageTypes.RIDEBEGUN: {
                                JSONObject obj;
                                String rideId;
                                Date date;
                                try {
                                    obj = new JSONObject(parts[1]);
                                    rideId = obj.getString("rideId");
                                    date = new Date(obj.getLong("dateTime"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    break;
                                }

                                //move ride from later to now on passenger phone
                                for (int i = laterActivityPos + 1; i < otherActivityPos; i++) {
                                    if (activities.get(i).activityType == Activity.TYPE_RIDE_FOUND && activities.get(i).isAccepted && activities.get(i).match.getRide().getRideId().equals(rideId)) {
                                        Activity a = activities.remove(i);
                                        a.activityType = Activity.TYPE_ACK_PICKUP;
                                        putActivity(a);
                                        break;
                                    }
                                }
                                updateActivitiesFragment();
                                break;
                            }
                            case MessageTypes.RIDEMATCH: {
                                //Put ride under later, if later ride, else if now update fragment
                                try {
                                    JSONObject obj = new JSONObject(parts[1]);
                                    JSONArray arr = obj.getJSONArray("matches");

                                    Match[] matches = null;

                                    if(arr.length() > 0) {
                                        matches = new Match[arr.length()];

                                        for (int i = 0; i < arr.length(); i++) {
                                            JSONObject jsonObject = arr.getJSONObject(i);
                                            Match match = new Match(new RideQuery(jsonObject.toString()));
                                            match.setRidePoint(jsonObject.getString("ridePoint"));
                                            match.setPrice(jsonObject.getDouble("price"));
                                            match.setCurrentDriverLocation(jsonObject.getString("currentDriverLocation"));
                                            match.setRide(new Ride(jsonObject.getString("ride")));

                                            if (match.isNow() && currentFragmentId == FRAGMENT_FIND) {
                                                matches[i] = match;
                                                getUserProfile(match);
                                                Log.i("match length: ", String.valueOf(matches.length));
                                            } else if (match.isNow() && currentFragmentId != FRAGMENT_FIND) {
                                                return;
                                            } else if (!match.isNow()) {
                                                Activity a = new Activity(Activity.TYPE_RIDE_FOUND);
                                                a.match = match;
                                                putActivity(a);
                                                updateActivitiesFragment();
                                                getUserProfile(a.match);
                                            }
                                        }
                                    } else{
                                        Toast.makeText(getApplicationContext(), "No matches found", Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    if (currentFragmentId == FRAGMENT_FIND) {
                                        ((FindLiftFragment) currentFragment).update(matches);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    break;
                                } catch (BadMessageException e) {
                                    e.printStackTrace();
                                }

                                break;
                            }
                            case MessageTypes.RIDECANCELLED: {
                                //remove ride
                                JSONObject obj;
                                String rideId;
                                try {
                                    obj = new JSONObject(parts[1]);
                                    rideId = obj.getString("rideId");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    break;
                                }

                                for (int i = nowActivityPos + 1; i < otherActivityPos; i++) {
                                    Activity a = activities.get(i);
                                    if (a.activityType == Activity.TYPE_RIDE_FOUND)
                                        if (a.match.getRide().getRideId().equals(rideId))
                                            removeActivity(a);
                                    //show notification informing user
                                    Ride r = a.match.getRide();
                                    showDialog("Ride cancelled by driver", "Ride " + (r.isToCampus() ? "to" : "from") + " cancelled by driver");
                                }
                                break;
                            }
                            case MessageTypes.ACKDROPOFF: {
                                String rideId = parts[1];

                                for (int i = nowActivityPos + 1; i < laterActivityPos; i++) {
                                    if (activities.get(i).activityType == Activity.TYPE_IN_RIDE && activities.get(i).match.getRide().getRideId().equals(rideId)) {
                                        Activity a = activities.remove(i);
                                        a.activityType = Activity.TYPE_COMMENT_PENDING;
                                        putActivity(a);
                                    }
                                }
                                updateActivitiesFragment();
                            }
                        }
                    }
                }
            }
        }
    }

    class GPSServiceHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 0: {
                    pointReceived((WGS84Point) (msg.getData().getSerializable("point")));
                    break;
                }
                case 1: {
                    if (msg.arg1 == 14) {
                        if (msg.getData().getBoolean("hasPoints", false))
                            routeRecordingFinished((WGS84Point[]) msg.getData().getSerializable("points"), (String[]) msg.getData().getSerializable("geohashes"), msg.getData().getDouble("radialDistance"), msg.getData().getString("ep"));
                        else {
                            Activity a = null;
                            for (int i = 0; i < activities.size(); i++) {
                                a = activities.get(i);
                                if (a.activityType == Activity.TYPE_PRERECORD_BEGIN || a.activityType == Activity.TYPE_RECORDING_ROUTE || a.activityType == Activity.TYPE_ROUTE_RECORDED) {
                                    break;
                                }
                            }

                            removeActivity(a);
                            setCurrentFragment(FRAGMENT_ACTIVITIES);
                            Toast.makeText(getApplicationContext(), "No points were recorded", Toast.LENGTH_LONG).show();
                        }
                    }else if (msg.arg1 == 12)
                        routeRecordingBegun(msg.getData().getString("ep"));
                    break;
                }
                case 2: {
                    if (msg.arg1 == 14)
                        rideFinished(msg.getData().getInt("avgSpeed"), msg.getData().getInt("rating"));

                    if (msg.arg1 == 16) {
                        //perform action to inform driver of passenger
                        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.beep_01);
                        mp.start();
                        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(1000);
                    }
                    break;
                }
                case 4: {
                    initSock();
                    //   boolean onroute = msg.getData().getBoolean("onRoute");
                    //  WGS84Point point = (WGS84Point) msg.getData().getSerializable("point");

                    Message msg1 = new Message();
                    msg1.what = 4;// update server concerning driver position
                    msg1.setData(msg.getData());
                    try {
                        sockSend(msg1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    updateActivitiesFragment();
                    break;
                }
                case 5: {
                    updateRoute((WGS84Point[]) msg.getData().getSerializable("points"));
                    break;
                }
                case 6: {
                    //can recieve ride stats here and do whatever with it
                    break;
                }
                case 7: {
                    lastLocation = (WGS84Point) msg.getData().get("point");
                    break;
                }
                case 10: {
                    String userId = msg.getData().getString("user");
                    Activity a = null;
                    for (int i = nowActivityPos + 1; i < laterActivityPos; i++) {
                        if (activities.get(i).match.getUserId().equals(userId)) {
                            a = activities.remove(i);
                            break;
                        }
                    }

                    Message msg1 = new Message();
                    msg1.what = 6;
                    msg1.getData().putString("qryId", a.match.getId());
                    msg1.getData().putString("rideId", a.match.getRide().getRideId());

                    //send Ackdropoff
                    try {
                        sockSend(msg1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    //inform driver of passenger dropoff

                    MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.beep_01);
                    mp.start();
                    Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(1000);
                    // Vibrate for 500 milliseconds
                    //add comment pending
                    a.activityType = Activity.TYPE_COMMENT_PENDING;
                    putActivity(a);
                    updateActivitiesFragment();
                }
            }
        }

    }

    class HttpCommHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 7: {
                    String response = msg.getData().getString("response");
                    if (!response.startsWith("error")) {
                        // add listed rides
                        try {
                            JSONObject obj = new JSONObject(response);
                            JSONArray arr = obj.getJSONArray("rides");

                            Activity a = null;
                            for(int i = 0; i < arr.length(); i++)
                            {
                                Ride r = new Ride(arr.getJSONObject(i).toString());
                                a = new Activity(Activity.TYPE_ONGOING_RIDE);
                                a.ride = r;
                                putActivity(a);

//                                if (a != null && a.ride.isNow() && !recordingRoute && !rideOngoing) {
//                                    rideOngoing = true;
//                                    a.activated = true;
//                                    Message msg1 = new Message();
//                                    msg1.what = 2;
//                                    msg1.getData().putSerializable("geohashes", new ArrayList<>(Arrays.asList(getRouteById(a.ride.getRouteId()).geohashes)));
//                                    msg1.getData().putBoolean("toCampus", a.ride.isToCampus());
//                                    msg1.getData().putSerializable("campus", getCampusById(a.ride.getCampusId()));
//                                    try {
//                                        gps.send(msg1);
//                                    } catch (RemoteException e) {
//                                        e.printStackTrace();
//                                    }
//                                    pull = new Pull(1);
//                                }
                            }

                            updateActivitiesFragment();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (BadMessageException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                }
                case 8: {//save point response
                    String response = msg.getData().getString("response");
                    String name = msg.getData().getString("name");
                    if (!response.startsWith("error")) {
                        Toast.makeText(getApplicationContext(), "Position saved", Toast.LENGTH_LONG);
                        Activity a = null;

                        for (int i = 0; i < activities.size(); i++) {
                            a = activities.get(i);

                            if (a.pointName.equals(name)) {//remove activity using name of positon
                                a = activities.remove(i);
                                break;
                            }
                        }

                        //save point to device, to allow immediate use
                        if (a != null) {
                            Position newPoint = new Position(response, name, new Date(), a.point);
                            points.add(newPoint);

                            StringBuilder sb = new StringBuilder();
                            sb.append("{\"points\":[");

                            for (Position p : points) {
                                try {
                                    sb.append(p.toJson());
                                } catch (IOException e) {
                                    showDialog("Problem while saving point", "You will need to logout and login in order to access your new position");
                                    return;
                                }
                                sb.append(",");
                            }

                            sb.deleteCharAt(sb.length() - 1);
                            sb.append("]}");

                            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("points", sb.toString());
                            editor.apply();
                            Toast.makeText(getApplicationContext(),"point \"" + name + "\" saved", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        for (Activity a : activities) {
                            if (a.pointName.equals(name)) {//remove activity using name of positon
                                activities.remove(a);
                                break;
                            }
                        }

                        String[] parts = response.split(":");
                        showDialog("Problem while saving position", parts[1]);
                    }
                    break;
                }
                case 10: {//save route response
                    String response = msg.getData().getString("response");
                    String name = msg.getData().getString("name");

                    if (!response.startsWith("error")) {
                        Toast.makeText(getApplicationContext(), "Route saved", Toast.LENGTH_LONG);
                        Activity a = null;

                        for (int i = 0; i < activities.size(); i++) {
                            a = activities.get(i);

                            if (a.routeName.equals(name)) {//remove activity using name of route
                                a = activities.remove(i);
                                break;
                            }
                        }

                        if (a != null) {
                            Route newRoute = new Route(response, name, a.campusId, 0, new Date());
                            routes.add(newRoute);

                            //edit and save route on device
                            StringBuilder sb = new StringBuilder();
                            sb.append("{\"routes\":[");

                            for (Route r : routes) {
                                try {
                                    sb.append(r.toJson());
                                } catch (IOException e) {
                                    showDialog("Problem while saving route", "You will need to logout and login in order to access your new route");
                                    return;
                                }
                                sb.append(",");
                            }

                            sb.deleteCharAt(sb.length() - 1);
                            sb.append("]}");

                            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("routes", sb.toString());
                            editor.apply();
                        }
                    } else {
                        for (Activity a : activities) {
                            if (a.routeName.equals(name)) {//remove activity using name of positon
                                activities.remove(a);
                                break;
                            }
                        }
                        String[] parts = response.split(":");
                        showDialog("Problem while saving route", parts[1]);
                    }
                    break;
                }
                case 13: {// on receiving user info
                    String response = msg.getData().getString("response");
                    if (!response.startsWith("error")) {

                        try {
                            User user = new User(response);

                            if (msg.getData().getBoolean("forNowRide") && currentFragmentId == FRAGMENT_FIND)
                                ((FindLiftFragment) currentFragment).update(user, msg.getData().getString("matchId", ""));
                            else {
                                for (int i = nowActivityPos + 1; i < otherActivityPos; i++) {
                                    if (activities.get(i).match != null && activities.get(i).match.getId().equals(msg.getData().getString("matchId")))
                                        activities.get(i).userInfo = user;
                                }
                            }

                            if (msg.getData().getBoolean("isDriver", false)/* && msg.getData().getBoolean("forNowRide", false)&& user.isDriver*/) {
                                Message msg1 = new Message();
                                msg1.what = 15;//get car profile
                                msg1.getData().putString("token", token);
                                msg1.getData().putString("vrn", user.currentCarVRN);
                                msg1.getData().putString("matchId", msg.getData().getString("matchId"));
                                msg1.getData().putBoolean("forNowRide", msg.getData().getBoolean("forNowRide", false));
                                httpSend(msg1);
                            }

                            if (!imageExist(user.pic_id)) {
                                Message msg1 = new Message();
                                msg1.what = 18;
                                msg1.getData().putString("imageId", user.pic_id);
                                httpSend(msg1);
                            }

                            updateActivitiesFragment();
                        } catch (JSONException | RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        String[] error = response.split(":");
                        Toast.makeText(getApplicationContext(), error[1], Toast.LENGTH_LONG).show();
                        for (int i = nowActivityPos + 1; i < otherActivityPos; i++) {
                            if (activities.get(i).match != null && activities.get(i).match.getId().equals(msg.getData().getString("matchId"))) {
                                //doqueue to try and get user info
                                getUserProfile(activities.get(i).match);
                            }
                        }
                    }
                    break;
                }
                case 15: {//on recieving car details
                    String response = msg.getData().getString("response");
                    if (!response.startsWith("error")) {
                        try {
                            Car car = new Car(response);

                            if (msg.getData().getBoolean("forNowRide") && currentFragmentId == FRAGMENT_FIND)
                                ((FindLiftFragment) currentFragment).updateCar(car, msg.getData().getString("matchId", ""));
                            else {
                                if (activities.size() > 0) {
                                    for (int i = nowActivityPos + 1; i < otherActivityPos; i++) {
                                        if (activities.get(i).match != null && activities.get(i).match.getId().equals(msg.getData().getString("matchId")))
                                            activities.get(i).carInfo = car;
                                    }
                                }

                                updateActivitiesFragment();
                            }

                            if (!imageExist(car.pic_id)) {
                                Message msg1 = new Message();
                                msg1.what = 18;
                                msg1.getData().putString("imageId", car.pic_id);
                                httpSend(msg1);
                            }

                        } catch (JSONException | RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        String[] error = response.split(":");
                        Toast.makeText(getApplicationContext(), error[1], Toast.LENGTH_LONG).show();
                        for (int i = nowActivityPos + 1; i < otherActivityPos; i++) {
                            if (activities.get(i).match != null && activities.get(i).match.getId().equals(msg.getData().getString("matchId"))) {
                                //doqueue to try and get car info
                                getDriverCarProfile(msg.getData().getString("vrn", ""), msg.getData().getString("matchId"), msg.getData().getBoolean("forNowRide", false));
                            }
                        }
                    }
                    break;
                }
                case 18: {//on receiving image
                    //store image
                    File file;
                    try {
                        String fileName = msg.getData().getString("imageId");
                        file = File.createTempFile(fileName, null, getApplicationContext().getCacheDir());
                        storeThumbnail((Bitmap) msg.getData().getParcelable("response"), file);

                        if (fileName.equals(MainActivity.this.currentUser.pic_id)) {
                            Bitmap bitmap = readImage(MainActivity.this.currentUser.pic_id);
                            CircleImageView img = (CircleImageView) findViewById(R.id.pic);
                            img.setImageBitmap(bitmap);
                        } else if (fileName.equals(MainActivity.this.currentCar.pic_id)) {
                            Bitmap bitmap = readImage(MainActivity.this.currentCar.pic_id);
                            CircleImageView img = (CircleImageView) findViewById(R.id.car_pic);
                            img.setImageBitmap(bitmap);
                        }

                        updateActivitiesFragment();
                    } catch (IOException e) {
                        // Error while creating file
                        Log.i("image error: ", "error while storing image");
                        e.printStackTrace();
                    }
                    break;
                }
                case 19: {//ride matches
                    String response = msg.getData().getString("response");
                    if (!response.startsWith("error")) {
                        // add listed rides
                        try {
                            JSONObject obj = new JSONObject(response);
                            JSONArray arr = obj.getJSONArray("matches");

                            for(int i = 0; i < arr.length(); i++)
                            {
                                Match m = new Match(new RideQuery(arr.getJSONObject(i).toString()));
                                m.setRidePoint(arr.getJSONObject(i).getString("ridePoint"));
                                m.setPrice(arr.getJSONObject(i).getDouble("price"));
                                m.setCurrentDriverLocation(arr.getJSONObject(i).getString("currentDriverLocation"));
                                m.setRide(new Ride(arr.getJSONObject(i).getString("ride")));

                                Activity a = new Activity(currentUser.userId.equals(m.getRide().getDriverId()) ? Activity.TYPE_PASSENGER_FOUND : Activity.TYPE_RIDE_FOUND);
                                a.match = m;

                             if (!m.isNow()) {
                                 getUserProfile(a.match);
                                 putActivity(a);
                                 updateActivitiesFragment();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (BadMessageException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                }
            }
        }
    }

    public Campus getCampusById(String id) {
        for (Campus c : campuses) {
            if (c.getCampusId().equals(id))
                return c;
        }

        return null;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    private void initGps() {
        if (gps == null) {
            gps = new ServiceManager(this.getApplicationContext(), GpsService.class, new GPSServiceHandler());
            gps.start();
        }
    }

    private void initHttp() {
        if (http == null) {
            http = new ServiceManager(this.getApplicationContext(), HttpCommService.class, new HttpCommHandler());
            http.start();
        }
    }

    private void initSock() {
        if (sock == null) {
            sock = new ServiceManager(this.getApplicationContext(), SockCommService.class, new AsynCommHandler());
            sock.start();

//            if(!sockAuthorised) {
//                Thread init = new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Message msg = new Message();
//                        msg.what = 0;
//                        msg.getData().putString("token", token);
//
//                        while (!sock.isRunning()) {
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        try {
//                            sock.send(msg);
//                        } catch (RemoteException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//                init.start();
            }
        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"activities\":[");

        for (Activity a : activities) {
            try {
                sb.append(a.toJson());
                sb.append(",");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]}");

        if(token != null) {
            editor.putString("activities", sb.toString());//store activities in sharedpreferences
            // editor.putInt("currentFragment", currentFragmentId);
            editor.putBoolean("recordingRoute", recordingRoute);
            editor.putBoolean("rideOngoing", rideOngoing);
            editor.putInt("nowActivityIndex", nowActivityPos);
            editor.putInt("laterActivityIndex", laterActivityPos);
            editor.putInt("otherActivityIndex", otherActivityPos);
            editor.putFloat("lastLocLat", (float) (lastLocation != null ? lastLocation.getLatitude() : 0));
            editor.putFloat("lastLocLon", (float) (lastLocation != null ? lastLocation.getLongitude() : 0));
            editor.putBoolean("sockAuthorised", sockAuthorised);
            editor.commit();
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    private void selectDrawerItem(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_find: {
                if (recordingRoute || rideOngoing) {
                    showDialog("GPS currently in use", "You are already performing an operation that requires the GPS. Try again later");
                    return;
                }

                initGps();

                Message msg = new Message();
                msg.what = 0;//get current position
                try {
                    gps.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                setCurrentFragment(FRAGMENT_FIND);
                break;
            }
            case R.id.nav_create_route: {
                if (!currentUser.isDriver) {
                    showDialog("Not accessible", "You can't record a route, if you aren't registered as a driver");
                    return;
                }

                if (rideOngoing) {
                    showDialog("GPS currently in use", "You are already performing an operation that requires the GPS. Try again later");
                    return;
                }

                initGps();
                setCurrentFragment(FRAGMENT_REC_ROUTE);
                break;
            }
            case R.id.nav_offer: {
                if (!currentUser.isDriver) {
                    showDialog("Not accessible", "You can't offer a ride, if you aren't registered as a driver");
                    return;
                }

                if (recordingRoute || rideOngoing) {
                    showDialog("GPS currently in use", "You are already performing an operation that requires the GPS. Try again later");
                   return;
                }

                initGps();
                setCurrentFragment(FRAGMENT_OFFER);
                break;
            }
            case R.id.nav_save: {
                if (recordingRoute || rideOngoing) {
                    showDialog("GPS currently in use", "You are already performing an operation that requires the GPS. Try again later");
                    return;
                }

                initGps();
                Message msg = new Message();
                msg.what = 0;//get current position
                try {
                    gps.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                setCurrentFragment(FRAGMENT_CURR_POS);
                break;
            }
            case R.id.nav_settings:
                showDialog("Coming soon in 20..Who knows", "This functionality is yet to be implemented");
                return;
              //  setCurrentFragment(FRAGMENT_SETTINGS);
              //  break;
            case R.id.nav_logout:
                doLogout();
                break;
            default:
                setCurrentFragment(FRAGMENT_ACTIVITIES);
        }

        // Highlight the selected item, update the title, and close the drawer
        menuItem.setChecked(true);
        setTitle(menuItem.getTitle());
        mDrawerLayout.closeDrawers();
    }

    public void setCurrentFragment(byte fragment) {
        Class fragmentClass;

        switch (fragment) {
            case FRAGMENT_FIND: {
                fragmentClass = (FindLiftFragment.class);
                currentFragmentId = fragment;
                break;
            }
            case FRAGMENT_REC_ROUTE: {
                fragmentClass = CreateRouteFragment.class;
                currentFragmentId = fragment;
                break;
            }
            case FRAGMENT_OFFER: {
                fragmentClass = OfferLiftFragment.class;
                currentFragmentId = fragment;
                break;
            }
            case FRAGMENT_CURR_POS: {
                fragmentClass = SavePositionFragment.class;
                currentFragmentId = fragment;
                break;
            }
            case FRAGMENT_SETTINGS:
                fragmentClass = SettingsFragment.class;
                currentFragmentId = fragment;
                break;
            default:
                fragmentClass = ActivitiesFragment.class;
                currentFragmentId = FRAGMENT_ACTIVITIES;
        }

        try {
            currentFragment = (Fragment) fragmentClass.newInstance();

            if (fragment == FRAGMENT_REC_ROUTE) {
                for (Activity a : activities) {
                    if (a.activityType == Activity.TYPE_PRERECORD_BEGIN || a.activityType == Activity.TYPE_RECORDING_ROUTE || a.activityType == Activity.TYPE_ROUTE_RECORDED) {
                        ((CreateRouteFragment) currentFragment).activity = a;
                        pull = new Pull(0);
                        pull.start();
                    }
                }
                ((CreateRouteFragment) currentFragment).initialLoc = lastLocation;
            } else if (pull != null) pull.doStop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragment_container, currentFragment).commit();
        setChecked();
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected && !haveShownNotConnectedMessage) {
            showDialog("Poor internet connection", "Please enable your internet connection and try again");
            haveShownNotConnectedMessage = true;
        } else if (isConnected) haveShownNotConnectedMessage = false;

        return isConnected;
    }

    private boolean sockSend(Message msg) throws RemoteException {
        initSock();
        if (isConnected()  /* && sockAuthorised*/) {
            sock.send(msg);
            return true;
        } else {
            //doqueue
            Message msg1 = new Message();
            msg1.what = AbstractService.ADD_PROCESSING_QUEUE;
            msg1.getData().putParcelable("msg", msg);
            sock.send(msg1);
        }
        return false;
    }

    private void httpSend(Message msg) throws RemoteException {
        initHttp();
        if (isConnected()) {
            http.send(msg);
        } else {
            //doqueue
            Message msg1 = new Message();
            msg1.what = AbstractService.ADD_PROCESSING_QUEUE;
            msg1.getData().putParcelable("msg", msg);
            http.send(msg1);
        }
    }

    private void storeThumbnail(Bitmap image, File pictureFile) {
        if (pictureFile == null) {
            Log.d("Save Thumbnail",
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            Matrix m = new Matrix();
            //m.postRotate(90);
            Bitmap i = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), m, true);
            Log.i("Saving file: ", i.compress(Bitmap.CompressFormat.JPEG, 50, fos) ? "successful" : "failed");
            fos.close();
        } catch (FileNotFoundException e) {
            Log.i("FNF", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.i("Error accessing file",
                    "Error accessing file: " + e.getMessage());
        }
    }

    public Bitmap readImage(String id) {
        try {
            File dir = getCacheDir();
            File file = null;
            if (dir.exists()) {
                for (File f : dir.listFiles()) {
                    if(f.getName().startsWith(id))
                        file = f;
                }
            }

          //  File file = new File(getApplicationContext().getCacheDir(), id + ".tmp");
            if(file != null) {
                FileInputStream fis = new FileInputStream(file);

                byte[] data = new byte[(int) file.length()];
                int res = fis.read(data);
                fis.close();

                if (res > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    return bitmap;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean imageExist(String id) {
        //File file = new File(getApplicationContext().getCacheDir(), id + ".tmp");

        File dir = getCacheDir();
        File file = null;
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if(f.getName().startsWith(id))
                    return true;
            }
        }

        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 150:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "Rating has been sent", Toast.LENGTH_LONG).show();
                    Activity a = (Activity) data.getSerializableExtra("za.ac.nmmu.lift_club.activity");
                    removeActivity(a);
                } else if (resultCode == 36)
                    Toast.makeText(getApplicationContext(), "Request could not be made", Toast.LENGTH_LONG).show();
                break;
        }
    }

//    @Override
//    public void onSaveInstanceState(Bundle savedInstanceState) {
//
//        if(currentFragmentId == FRAGMENT_ACTIVITIES) {
//            savedInstanceState.putBoolean("restore", false);
//            return;
//        }
//
//        super.onSaveInstanceState(savedInstanceState);
//    }
//
//    public void onRestoreInstanceState(Bundle savedInstanceState) {
//        if(!savedInstanceState.getBoolean("restore", true)) {
//            super.onRestoreInstanceState(null);
//            return;
//        }
//
//        super.onRestoreInstanceState(savedInstanceState);
//    }

}

