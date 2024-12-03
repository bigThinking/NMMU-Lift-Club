package za.ac.nmmu.lift_club;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.util.VincentyGeodesy;
import de.hdodenhof.circleimageview.CircleImageView;
import za.ac.nmmu.lift_club.util.Activity;
import za.ac.nmmu.lift_club.util.Campus;
import za.ac.nmmu.lift_club.util.Car;
import za.ac.nmmu.lift_club.util.Match;
import za.ac.nmmu.lift_club.util.Position;
import za.ac.nmmu.lift_club.util.RideQuery;
import za.ac.nmmu.lift_club.util.User;

/**
 * Created by s210036575 on 2015-07-07.
 */
public class FindLiftFragment extends Fragment implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    TextView txtDate, txtTime;
    String pointId;
    boolean isNow = true, toCampus = true, pointReady = false;
    Calendar cal = Calendar.getInstance();
    int dateTimeSet = 0;
    Activity[] matches = null;
    ListView lstRides;
    Button btnOk;
    MatchAdapter ma;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.find_lift, container, false);

        final RadioButton rbtLater = (RadioButton) view.findViewById(R.id.rbtLater);
        final RadioButton rbtNow = (RadioButton) view.findViewById(R.id.rbtNow);
        final TextView txtPoint = (TextView) view.findViewById(R.id.textView7);
        final View dt = view.findViewById(R.id.dt2);
        txtDate = (TextView) view.findViewById(R.id.txtDate);
        txtTime = (TextView) view.findViewById(R.id.txtTime);
        btnOk = (Button) view.findViewById(R.id.btnOk1);
        final RadioGroup rgpDirr = (RadioGroup) view.findViewById(R.id.rgpCampusDirr2);
        final RadioGroup rgpTime = (RadioGroup) view.findViewById(R.id.rgpTime1);
        final CheckBox cbxBoot = (CheckBox) view.findViewById(R.id.cbxBoot);
        final Spinner spnCampus = (Spinner) view.findViewById(R.id.spnCampus2);
        final Spinner spnPosition = (Spinner) view.findViewById(R.id.spnPoint);
        lstRides = (ListView)view.findViewById(R.id.lstRides);
        btnOk.setEnabled(false);
        Toast.makeText(getActivity().getApplicationContext(), "Obtaining current GPS point", Toast.LENGTH_LONG).show();

        ArrayAdapter<Campus> spinnerArrayAdapter = new ArrayAdapter<Campus>(getActivity(), android.R.layout.simple_spinner_item, ((MainActivity) this.getActivity()).campuses);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCampus.setAdapter(spinnerArrayAdapter);

        ArrayAdapter<Position> spinnerArrayAdapter1 = new ArrayAdapter<Position>(getActivity(), android.R.layout.simple_spinner_item, ((MainActivity) this.getActivity()).points);
        spinnerArrayAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnPosition.setAdapter(spinnerArrayAdapter1);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity a = null;
                RideQuery r = new RideQuery();

                r.setIsNow(isNow);
                r.setIsToCampus(toCampus);

               /* if (!isNow && dateTimeSet == 2)
                    r.setRideDateTime(cal.getTime());
                else if (!isNow && dateTimeSet != 2) {
                    showErrorDialog("Missing values", "Please enter a date and time");
                    return;
                }*/

                r.setNeedBoot(cbxBoot.isChecked());

                r.setCampusId(((Campus) spnCampus.getSelectedItem()).getCampusId());

                if(!isNow) {
                    r.setLocation(((Position) spnPosition.getSelectedItem()).point.getLatitude(), ((Position) spnPosition.getSelectedItem()).point.getLongitude());

                    if (dateTimeSet >= 2)
                        r.setRideDateTime(cal.getTime());
                    else {
                        showErrorDialog("Missing values", "Please enter a date and time");
                        return;
                    }
                }
                else {
                    WGS84Point currLocation = ((MainActivity) getActivity()).lastLocation;
                    r.setLocation(currLocation.getLatitude(), currLocation.getLongitude());
                    Log.i("geohash: ", r.getGeohash());
                    Log.i("lat: ", String.valueOf(r.getLat()));
                    Log.i("lon: ", String.valueOf(r.getLon()));
                    Date date = new Date();
                    r.setRideDateTime(date);
                }

                r.setUserId(((MainActivity) getActivity()).currentUser.userId);
                a = new Activity(Activity.TYPE_SEARCHING_RIDES);
                a.qry = r;
                ((MainActivity) getActivity()).searchForRide(a);

            }
        });

        rgpDirr.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbtnTo2) {
                    txtPoint.setText("Select pickup point");
                    rbtNow.setVisibility(View.VISIBLE);
                    rbtNow.setChecked(true);
                    toCampus = true;
                } else {
                    txtPoint.setText("Select dropoff point");
                    rbtNow.setVisibility(View.GONE);
                    rbtLater.setChecked(true);
                    toCampus = false;
                }
            }
        });

        rgpTime.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbtNow) {
                    isNow = true;
                    txtPoint.setVisibility(View.GONE);
                    spnPosition.setVisibility(View.GONE);
                    dt.setVisibility(View.GONE);

                    if(!pointReady)
                    {
                        btnOk.setEnabled(false);
                        Toast.makeText(getActivity().getApplicationContext(), "Obtaining current GPS point", Toast.LENGTH_LONG).show();
                    }
                } else {
                    isNow = false;
                    txtPoint.setVisibility(View.VISIBLE);
                    spnPosition.setVisibility(View.VISIBLE);
                    dt.setVisibility(View.VISIBLE);
                    btnOk.setEnabled(true);
                }
            }
        });

        txtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
                /*DatePickerDialogFragment dpdf = new DatePickerDialogFragment();
                dpdf.listener = FindLiftFragment.this;
                dpdf.context = getActivity().getApplicationContext();
                dpdf.show(getActivity().getSupportFragmentManager(), "");*/
            }
        });

        txtTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
                /*TimePickerDialogFragment tpdf = new TimePickerDialogFragment();
                tpdf.listener = FindLiftFragment.this;
                tpdf.context = getActivity().getApplicationContext();
                tpdf.show(getActivity().getSupportFragmentManager(), "");*/
            }
        });

        if (savedInstanceState != null) {

        } else {
            txtPoint.setVisibility(View.GONE);
            spnPosition.setVisibility(View.GONE);
            dt.setVisibility(View.GONE);
        }

        return view;
    }

    private void showDatePickerDialog()
    {
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dpd = new DatePickerDialog(getActivity(), this, year,month, day);
        dpd.show();
    }

    private void showTimePickerDialog()
    {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        TimePickerDialog tpd =  new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
        tpd.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        cal.set(year, monthOfYear, dayOfMonth);
        txtDate.setText(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) + "," + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + " " + cal.get(Calendar.YEAR));
        dateTimeSet++;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        txtTime.setText(cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + " " + (cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM"));
        dateTimeSet++;
    }

    public void showErrorDialog(String title, String message) {
        ErrorDialogFragment edf = new ErrorDialogFragment();
        edf.title = title;
        edf.errorMessage = message;
        edf.show(getActivity().getSupportFragmentManager(), "");
    }

    public void update(Match[] matches) {
       this.matches = new Activity[matches.length];
       for(int i =0; i < matches.length; i++)
       {
           Activity a = new Activity(Activity.TYPE_RIDE_FOUND);
           a.match = matches[i];
           this.matches[i] = a;
       }

        ma = new MatchAdapter(getActivity().getApplicationContext(), this.matches);
        lstRides.setAdapter(ma);
    }

    public void update(User userInfo, String matchId) {

        Log.i("user details recieved: ", matchId);
        for(Activity a : matches)
        {
            if(a.match.getId().equals(matchId)) {
                a.userInfo = userInfo;
                ma.notifyDataSetChanged();
            }
        }
    }

    public void updateCar(Car car, String matchId)
    {
        Log.i("car details recieved: ", matchId);
        for(Activity a : matches)
        {
            if(a.match.getId().equals(matchId)) {
                a.carInfo = car;
                ma.notifyDataSetChanged();
            }
        }
    }

    public void pointReady()
    {
        pointReady = true;
        btnOk.setEnabled(true);
        Toast.makeText(getActivity().getApplicationContext(), "Point obtained", Toast.LENGTH_LONG).show();
    }

    class MatchAdapter extends ArrayAdapter<Activity>{

        Activity[] data;
        boolean userProfile = true;
        LayoutInflater mInflater;

        public MatchAdapter(Context context,  Activity[] objects) {
            super(context, R.layout.ride_participant_activities, objects);
            data = objects;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent)
        {
            userProfile = true;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.ride_participant_activities, null);
            }

            final TextView txt13 = (TextView) convertView.findViewById(R.id.textView13);
            TextView txt14 = (TextView) convertView.findViewById(R.id.textView14);
            final TextView txt15 = (TextView) convertView.findViewById(R.id.textView15);
            final TextView txt16 = (TextView) convertView.findViewById(R.id.textView16);
            final TextView txt17 = (TextView) convertView.findViewById(R.id.textView17);
            final CircleImageView imgUser = (CircleImageView) convertView.findViewById(R.id.user_pic4);
            Button btnAccept = (Button)convertView.findViewById(R.id.btnAccept);
            txt14.setText("");

            if (data[position].activityType == Activity.TYPE_RIDE_FOUND) {
                txt17.setVisibility(View.VISIBLE);

                //set  user picture
                if (data[position].userInfo != null) {
                    Bitmap bitmap = ((MainActivity) getActivity()).readImage(data[position].userInfo.pic_id);

                    if (bitmap != null) {
                        imgUser.setImageBitmap(bitmap);
                    } else {
                        bitmap = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(), R.drawable.bad_profile_pic_2);
                        imgUser.setImageBitmap(bitmap);
                    }

                    txt13.setText(data[position].userInfo.fName + " " + data[position].userInfo.sName);
                    txt15.setText("Studying: " + data[position].userInfo.studying);

                    if (data[position].match.getRide().isNow()) {
                        WGS84Point driverLoc = GeoHash.fromGeohashString(data[position].match.getCurrentDriverLocation()).getPoint();
                        WGS84Point rideLoc = GeoHash.fromGeohashString(data[position].match.getRidePoint()).getPoint();
                        double distance = VincentyGeodesy.distanceInMeters(driverLoc, rideLoc) / 1000;
                        txt14.setText("~" + distance + " km");
                    }

                    txt16.setText("Passenger rating : " + data[position].userInfo.currentUserRating + "\tprice : R " + data[position].match.getRide().getPrice());
                    txt17.setText("System Rating : " + data[position].userInfo.driverSystemRating);
                    btnAccept.setText("Select");
                }else{
                    txt13.setText("Getting user info");
                }

                    imgUser.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (userProfile) {
                                userProfile = false;
                                if (data[position].carInfo != null) {
                                    txt13.setText(data[position].carInfo.vrn);
                                    txt15.setText(data[position].carInfo.colour + " " + data[position].carInfo.manufacturer + " " + data[position].carInfo.model);

                                    //set  car picture
                                    Bitmap bitmap = ((MainActivity) getActivity()).readImage(data[position].carInfo.pic_id);

                                    if (bitmap != null) {
                                        imgUser.setImageBitmap(bitmap);
                                    } else {
                                        bitmap = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(), R.drawable.scionxdoutline640);
                                        imgUser.setImageBitmap(bitmap);
                                    }
                                }else{
                                    Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(), R.drawable.scionxdoutline640);
                                    imgUser.setImageBitmap(bitmap);
                                    txt13.setText("Getting car info");
                                }
                            } else {
                                userProfile = true;
                                if (data[position].userInfo != null) {
                                    Bitmap bitmap = ((MainActivity) getActivity()).readImage(data[position].carInfo.pic_id);

                                    if (bitmap != null) {
                                        imgUser.setImageBitmap(bitmap);
                                    } else {
                                        bitmap = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(), R.drawable.bad_profile_pic_2);
                                        imgUser.setImageBitmap(bitmap);
                                    }

                                    txt13.setText(data[position].userInfo.fName + " " + data[position].userInfo.sName);
                                    txt15.setText("Studying: " + data[position].userInfo.studying);
                                    txt16.setText("Passenger rating : " + data[position].userInfo.currentUserRating);
                                    txt17.setText("System Rating : " + data[position].userInfo.driverSystemRating);
                                }else{
                                    Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(), R.drawable.bad_profile_pic_2);
                                    imgUser.setImageBitmap(bitmap);
                                    txt13.setText("Getting user info");
                                }
                            }
                        }
                    });

                    btnAccept.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((MainActivity) FindLiftFragment.this.getActivity()).selectRide(data[position].match.getId(), data[position], MainActivity.FRAGMENT_FIND);
                            ((MainActivity) FindLiftFragment.this.getActivity()).setCurrentFragment(MainActivity.FRAGMENT_ACTIVITIES);
                        }
                    });
                }

            return convertView;
        }
    }
}
