package za.ac.nmmu.lift_club;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import ch.hsr.geohash.WGS84Point;
import za.ac.nmmu.lift_club.util.Activity;
import za.ac.nmmu.lift_club.util.Campus;
import za.ac.nmmu.lift_club.util.Route;

/**
 * Created by s210036575 on 2015-07-07.
 */
public class CreateRouteFragment extends Fragment implements OnMapReadyCallback {

    public static final int NOT_RECORDING_STATE = 0;
    public static final int RECORDING_STATE = 1;
    public static final int RECORDED_STATE = 2;
    private int currentState = NOT_RECORDING_STATE;
    private GoogleMap map;
    private View mapHolder, recRo, saveRo, newRo;
    public WGS84Point initialLoc;
    public Activity activity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View layout =  inflater.inflate(R.layout.create_route, container, false);

        //setup google map
        GoogleMapOptions options = new GoogleMapOptions();
        options.mapType(GoogleMap.MAP_TYPE_NORMAL).tiltGesturesEnabled(false).compassEnabled(false).rotateGesturesEnabled(false).zoomControlsEnabled(true);

        SupportMapFragment map = SupportMapFragment.newInstance(options);
        map.getMapAsync(this);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.map1_container, map);
        fragmentTransaction.commit();

        //set initial state and onclicklisteners
        mapHolder = layout.findViewById(R.id.map1_container);
        saveRo = layout.findViewById(R.id.saveRo);
        recRo = layout.findViewById(R.id.recRo);
        newRo = layout.findViewById(R.id.newRo);
        final CheckBox hideMap = (CheckBox)layout.findViewById(R.id.cbxHideMap);
        final CheckBox hideMap1 = (CheckBox)layout.findViewById(R.id.cbxHideMap1);
        Button btnSave = (Button)layout.findViewById(R.id.btnSave2);
        Button btnDiscard = (Button)layout.findViewById(R.id.btnDiscard);
        Button btnCancel = (Button)layout.findViewById(R.id.btnCancel1);
        Button btnStart = (Button)layout.findViewById(R.id.btnStart);
        Button btnStop = (Button)layout.findViewById(R.id.btnStop);
        Spinner spnCampus = (Spinner)layout.findViewById(R.id.spnCampus1);

        ArrayAdapter<Campus> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, ((MainActivity) this.getActivity()).campuses);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCampus.setAdapter(spinnerArrayAdapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRoute();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRecording();
            }
        });

        btnDiscard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discardRoute();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        hideMap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setMapHidden(isChecked);
            }
        });

        hideMap1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setMapHidden(isChecked);
            }
        });

        if(activity != null)
        {
            currentState = activity.fragmentState;
            setMapHidden(activity.mapHidden);
        }else{
            btnSave.setEnabled(false);
            btnDiscard.setEnabled(false);
            activity = new Activity(Activity.TYPE_PRERECORD_BEGIN);
        }

        setState(currentState, layout);
        return layout;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        if(initialLoc != null)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(initialLoc.getLatitude(),initialLoc.getLongitude()), 15));
        else map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-34.000451, 25.669540), 16));

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        });
    }

    public void setState(int state, View layout)
    {
        switch(state)
        {
            case NOT_RECORDING_STATE:{
                newRo.setVisibility(View.VISIBLE);
                saveRo.setVisibility(View.GONE);
                recRo.setVisibility(View.GONE);

                if(activity != null && activity.toCampus)
                {
                    Button btnStop = (Button)layout.findViewById(R.id.btnStop);
                    btnStop.setVisibility(View.VISIBLE);
                    Button btnCancel = (Button)layout.findViewById(R.id.btnCancel1);
                    ((LinearLayout.LayoutParams)btnCancel.getLayoutParams()).setMargins(5,0,5,0);
                }

                break;
            }
            case RECORDING_STATE:{
                newRo.setVisibility(View.GONE);
                saveRo.setVisibility(View.GONE);
                recRo.setVisibility(View.VISIBLE);

                TextView currently = (TextView)layout.findViewById(R.id.txtCurrently);
                String currentlyText;

                if(activity.activityType == Activity.TYPE_PRERECORD_BEGIN)
                {
                    currentlyText = "Waiting for recording to begin";
                    currently.setText(currentlyText);
                    break;
                }

                currentlyText = "Recording route ";
                if(activity.toCampus)
                {
                    Button btnStop = (Button)layout.findViewById(R.id.btnStop);
                    btnStop.setVisibility(View.GONE);
                    Button btnCancel = (Button)layout.findViewById(R.id.btnCancel1);
                    ((LinearLayout.LayoutParams)btnCancel.getLayoutParams()).setMargins(150,0,150,0);
                    currentlyText = currentlyText + "to " + getCampusById(activity.campusId).toString();
                }else{
                    currentlyText = currentlyText + "from " + getCampusById(activity.campusId).toString();
                }

                currently.setText(currentlyText);
                break;
            }
            case RECORDED_STATE:{
                newRo.setVisibility(View.GONE);
                saveRo.setVisibility(View.VISIBLE);
                recRo.setVisibility(View.GONE);
                mapHolder.setVisibility(View.VISIBLE);

                TextView routeInfo = (TextView) layout.findViewById(R.id.txtRoute1);
                if(activity.gottenRouteDetails) {
                    String text = "Route " + (activity.toCampus ? "to " : "from ") + getCampusById(activity.campusId).toString() + " recorded";
                    routeInfo.setText(text);
                    Button btnSave = (Button)layout.findViewById(R.id.btnSave2);
                    Button btnDiscard = (Button)layout.findViewById(R.id.btnDiscard);
                    btnSave.setEnabled(true);
                    btnDiscard.setEnabled(true);
                }else{
                    routeInfo.setText("Obtaining route details");
                }
            }
        }

        currentState = state;
        activity.fragmentState = state;
    }
    
    public void setState(int state)
    {
        switch(state)
        {
            case NOT_RECORDING_STATE:{
                newRo.setVisibility(View.VISIBLE);
                saveRo.setVisibility(View.GONE);
                recRo.setVisibility(View.GONE);

                if(activity != null && activity.toCampus)
                {
                    Button btnStop = (Button)getView().findViewById(R.id.btnStop);
                    btnStop.setVisibility(View.VISIBLE);
                    Button btnCancel = (Button)getView().findViewById(R.id.btnCancel1);
                    ((LinearLayout.LayoutParams)btnCancel.getLayoutParams()).setMargins(5,0,5,0);
                }

                break;
            }
            case RECORDING_STATE:{
                newRo.setVisibility(View.GONE);
                saveRo.setVisibility(View.GONE);
                recRo.setVisibility(View.VISIBLE);

                TextView currently = (TextView)getView().findViewById(R.id.txtCurrently);
                String currentlyText;

                if(activity.activityType == Activity.TYPE_PRERECORD_BEGIN)
                {
                    currentlyText = "Waiting for recording to begin";
                    currently.setText(currentlyText);
                    break;
                }

                currentlyText = "Recording route ";
                if(activity.toCampus)
                {
                    Button btnStop = (Button)getView().findViewById(R.id.btnStop);
                    btnStop.setVisibility(View.GONE);
                    Button btnCancel = (Button)getView().findViewById(R.id.btnCancel1);
                    ((LinearLayout.LayoutParams)btnCancel.getLayoutParams()).setMargins(150,0,150,0);
                    currentlyText = currentlyText + "to " + getCampusById(activity.campusId).toString();
                }else{
                    currentlyText = currentlyText + "from " + getCampusById(activity.campusId).toString();
                }

                currently.setText(currentlyText);
                break;
            }
            case RECORDED_STATE:{
                newRo.setVisibility(View.GONE);
                saveRo.setVisibility(View.VISIBLE);
                recRo.setVisibility(View.GONE);
                mapHolder.setVisibility(View.VISIBLE);

                TextView routeInfo = (TextView) getView().findViewById(R.id.txtRoute1);
                if(activity.gottenRouteDetails) {
                    String text = "Route " + (activity.toCampus ? "to " : "from ") + getCampusById(activity.campusId).toString() + " recorded";
                    routeInfo.setText(text);
                    Button btnSave = (Button)getView().findViewById(R.id.btnSave2);
                    Button btnDiscard = (Button)getView().findViewById(R.id.btnDiscard);
                    btnSave.setEnabled(true);
                    btnDiscard.setEnabled(true);
                }else{
                    routeInfo.setText("Obtaining route details");
                }
            }
        }

        currentState = state;
        activity.fragmentState = state;
    }

    private void saveRoute(){
        EditText edtRouteName = (EditText)getView().findViewById(R.id.edtRouteName);
        String routeName = edtRouteName.getText().toString();

        if(routeName.trim().compareTo("") != 0) {
            edtRouteName.setText("");

            for(Route r : ((MainActivity)this.getActivity()).routes)
            {
                if(r.name.compareTo(routeName) == 0) {
                    showErrorDialog("Invalid entry","A route with the given name already exist");
                    return;
                }
            }

            this.setState(NOT_RECORDING_STATE);
             activity.routeName = routeName;
            ((MainActivity) this.getActivity()).saveCurrentRouteRecording(activity);
            activity.activityType = Activity.TYPE_SAVING_ROUTE;
        }else{
           showErrorDialog("Invalid entry","Please enter a name for the route");
        }
    }

    public void showErrorDialog(String title, String message)
    {
        ErrorDialogFragment edf = new ErrorDialogFragment();
        edf.title = title;
        edf.errorMessage = message;
        edf.show(getActivity().getSupportFragmentManager(),"");
    }

    private void stopRecording()
    {
        if(activity.toCampus)
        {
            cancelRecording();
        }else{
            ((MainActivity) this.getActivity()).stopRecording(activity);
            this.setState(RECORDED_STATE);
        }
    }

    private void startRecording(){
        activity = new Activity(Activity.TYPE_PRERECORD_BEGIN);
        RadioGroup rgpDirr = (RadioGroup)getView().findViewById(R.id.rgpCampusDirr1);
        activity.toCampus = rgpDirr.getCheckedRadioButtonId() == R.id.rbtnTo1;
        activity.campusId = ((Campus)((Spinner)getView().findViewById(R.id.spnCampus1)).getSelectedItem()).getCampusId();
        activity.mapHidden = ((CheckBox)getView().findViewById(R.id.cbxHideMap1)).isChecked();
        this.setState(RECORDING_STATE);
       // placeMapMarker(activity.campus.getLoc(),true, activity.campus.getCampusName());
        ((MainActivity) this.getActivity()).startRecording(activity);
    }

    private void discardRoute(){
        this.setState(NOT_RECORDING_STATE);
        ((MainActivity) this.getActivity()).removeActivity(activity);
        ((MainActivity) this.getActivity()).recordingRoute = false;
        activity = null;
    }

    private void cancelRecording(){
        this.setState(NOT_RECORDING_STATE);
        ((MainActivity) this.getActivity()).cancelRouteRecording(activity);
        activity = null;
    }

    public void setMapHidden(boolean hidden)
    {
        if(activity.mapHidden)
        {
            if(!hidden)
            mapHolder.setVisibility(View.VISIBLE);
        }else{
            if(hidden)
            mapHolder.setVisibility(View.INVISIBLE);
        }
        activity.mapHidden = hidden;
    }

    public void clearMap()
    {
        map.clear();
    }

    public void placeMapMarker(WGS84Point point, boolean isCampus, String infoWindowText, boolean moveCamera)
    {
        Marker currentMarker = null;
        if(isCampus)
        {
            float[] hsv = new float[3];
            Color.colorToHSV(getResources().getColor(R.color.nmmu_blue), hsv);
            currentMarker = map.addMarker(new MarkerOptions().position(new LatLng(point.getLatitude(), point.getLongitude())).icon(BitmapDescriptorFactory.defaultMarker(hsv[0])).snippet(infoWindowText));
        }else{
            if(point != null) {
                if (infoWindowText == null || infoWindowText.equals(""))
                  currentMarker =  map.addMarker(new MarkerOptions().position(new LatLng(point.getLatitude(), point.getLongitude())).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                else
                   currentMarker = map.addMarker(new MarkerOptions().position(new LatLng(point.getLatitude(), point.getLongitude())).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).snippet(infoWindowText));
            }
        }

        if (moveCamera)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(point.getLatitude(), point.getLongitude()), 16));

        if(currentMarker != null)
            currentMarker.showInfoWindow();
    }

    public void drawPath(WGS84Point[] points)
    {
        if(points != null) {
            for (int i = 0; i < points.length - 1; i++) {
                map.addPolyline(new PolylineOptions()
                        .add(new LatLng(points[i].getLatitude(), points[i].getLongitude()), new LatLng(points[i + 1].getLatitude(), points[i + 1].getLongitude()))
                        .color(getResources().getColor(R.color.nmmu_blue)));
            }

            if (points.length > 5)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(points[points.length - 1].getLatitude(), points[points.length - 1].getLongitude()), 16));
        }
    }

    public void update(Activity activity)
    {
        if(activity.activityType == Activity.TYPE_RECORDING_ROUTE)//after
        {
            this.activity = activity;
            setState(activity.fragmentState);

            if(mapHolder.getVisibility() == View.VISIBLE) {
                clearMap();
                drawPath(activity.recordedRoute);
                placeMapMarker(getCampusById(activity.campusId).getLoc(),true, getCampusById(activity.campusId).getCampusName(),
                        !activity.toCampus && activity.recordedRoute != null && activity.recordedRoute.length < 5 ? true : false);

                if(activity.toCampus && activity.recordedRoute.length > 1)
                placeMapMarker(activity.recordedRoute[activity.recordedRoute.length-1], false, "Begin",
                        activity.recordedRoute != null && activity.recordedRoute.length < 5 ? true : false);

               // drawPath(activity.recordedRoute);
            }

            return;
        }

        if(activity.activityType == Activity.TYPE_ROUTE_RECORDED)//on route recording stop
        {
            this.activity = activity;
            setState(activity.fragmentState);

            if(mapHolder.getVisibility() == View.VISIBLE) {
                clearMap();
                drawPath(activity.recordedRoute);
                placeMapMarker(getCampusById(activity.campusId).getLoc(),true, getCampusById(activity.campusId).getCampusName(), false);
                placeMapMarker(activity.recordedRoute[activity.recordedRoute.length-1], false, activity.toCampus ? "Begin" : "End", false);
            }
        }
    }

    public Campus getCampusById(String id)
    {
        return ((MainActivity)getActivity()).getCampusById(id);
    }
}
