package za.ac.nmmu.lift_club;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.util.VincentyGeodesy;
import de.hdodenhof.circleimageview.CircleImageView;
import za.ac.nmmu.lift_club.util.Activity;

/**
 * Created by s210036575 on 2015-07-07.
 */
public class ActivitiesFragment extends ListFragment {

    public ArrayList<Activity> activities;
    private ActivitiesAdapter aa;
    private boolean userProfile = true;

    public ActivitiesFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        MainActivity ma = (MainActivity) this.getActivity();
        activities = ma.activities;
        aa = new ActivitiesAdapter(getActivity(), activities);
        setListAdapter(aa);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Activity a = activities.get(position);

        switch (a.activityType) {
            case Activity.TYPE_COMMENT_PENDING: {
                Intent intent = new Intent(getActivity().getApplicationContext(), CommentActivity.class);
                intent.putExtra("za.ac.nmmu.lift_club.activity", a);
                intent.putExtra("za.ac.nmmu.lift_club.token", ((MainActivity) getActivity()).token);

                startActivityForResult(intent, 150);
                break;
            }
            case Activity.TYPE_RECORDING_ROUTE:
            case Activity.TYPE_ROUTE_RECORDED:
            case Activity.TYPE_PRERECORD_BEGIN: {
                ((MainActivity) getActivity()).setCurrentFragment(MainActivity.FRAGMENT_REC_ROUTE);
                break;
            }
        }
    }

    public void update() {
        if(aa != null)
        aa.notifyDataSetChanged();
    }

    class ActivitiesAdapter extends BaseAdapter {
        private List<Activity> data;
        private Context context;
        private LayoutInflater mInflater;

        public ActivitiesAdapter(Context context, List<Activity> objects) {
            super();
            this.data = objects;
            this.context = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getItemViewType(int position) {
            switch (data.get(position).activityType) {
                case Activity.TYPE_COMMENT_PENDING: {
                    return 0;
                }
                case Activity.TYPE_ACK_PICKUP:
                case Activity.TYPE_PASSENGER_FOUND:
                case Activity.TYPE_RIDE_FOUND: {
                    return 1;
                }
                case Activity.TYPE_SAVING_POINT:
                case Activity.TYPE_SEPARATOR: {
                    return 2;
                }
                case Activity.TYPE_SEARCHING_RIDES:
                case Activity.TYPE_ONGOING_RIDE:
                case Activity.TYPE_RECORDING_ROUTE:
                case Activity.TYPE_PRERECORD_BEGIN:
                case Activity.TYPE_ROUTE_RECORDED:
                case Activity.TYPE_SAVING_ROUTE: {
                    return 3;
                }
            }

            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position).toString();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            if (convertView == null) {
                switch (type) {
                    case 2: {//seperator
                        convertView = mInflater.inflate(R.layout.seperator_activities, null);
                        break;
                    }
                    case 3: {
                        convertView = mInflater.inflate(R.layout.route_ride_activities, null);
                        break;
                    }
                    case 0: {
                        convertView = mInflater.inflate(R.layout.comment_pending_activities, null);
                        break;
                    }
                    case 1: {
                        convertView = mInflater.inflate(R.layout.ride_participant_activities, null);
                        break;
                    }
                }
            }

            switch (type) {
                case 0: {
                    TextView txt19 = (TextView) convertView.findViewById(R.id.textView19);
                    TextView txt20 = (TextView) convertView.findViewById(R.id.textView20);
                    CircleImageView imgUser = (CircleImageView) convertView.findViewById(R.id.user_pic3);

                    //set picture
                    if (((MainActivity) getActivity()).imageExist(data.get(position).userInfo.pic_id)) {
                        Bitmap bitmap = ((MainActivity) getActivity()).readImage(data.get(position).userInfo.pic_id);

                        if (bitmap != null) {
                            imgUser.setImageBitmap(bitmap);
                        } else {
                            bitmap = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(), R.drawable.bad_profile_pic_2);
                            imgUser.setImageBitmap(bitmap);
                        }
                    }

                    txt19.setText("Comment pending");
                    txt20.setText("For Ride " + (data.get(position).match.getRide().isToCampus() ? "to " : "from ") + ((MainActivity) getActivity()).getCampusById(data.get(position).match.getRide().getCampusId()).toString());

                    break;
                }
                case 1: {
                    final TextView txt13 = (TextView) convertView.findViewById(R.id.textView13);
                    TextView txt14 = (TextView) convertView.findViewById(R.id.textView14);
                    final TextView txt15 = (TextView) convertView.findViewById(R.id.textView15);
                    final TextView txt16 = (TextView) convertView.findViewById(R.id.textView16);
                    final TextView txt17 = (TextView) convertView.findViewById(R.id.textView17);
                    final CircleImageView imgUser = (CircleImageView) convertView.findViewById(R.id.user_pic4);
                    Button btnAccept = (Button) convertView.findViewById(R.id.btnAccept);
                    txt14.setText("");
                    imgUser.setImageBitmap(BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(), R.drawable.bad_profile_pic_2));

                    if (data.get(position).userInfo != null) {
                        userProfile = true;
                        //set  user picture

                        if (((MainActivity) getActivity()).imageExist(data.get(position).userInfo.pic_id)) {
                            Bitmap bitmap = ((MainActivity) getActivity()).readImage(data.get(position).userInfo.pic_id);

                            if (bitmap != null) {
                                imgUser.setImageBitmap(bitmap);
                            }
                        }

                        txt13.setText(data.get(position).userInfo.fName + " " + data.get(position).userInfo.sName);
                        txt15.setText("Studying: " + data.get(position).userInfo.studying);

                        imgUser.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (userProfile && data.get(position).carInfo != null) {
                                    userProfile = false;
                                    txt13.setText(data.get(position).carInfo.vrn);
                                    txt15.setText("Desription: " +  data.get(position).carInfo.colour + " " + data.get(position).carInfo.manufacturer + " " + data.get(position).carInfo.model);

                                    //set  car picture
                                    if (((MainActivity) getActivity()).imageExist(data.get(position).carInfo.pic_id)) {
                                        Bitmap bitmap = ((MainActivity) getActivity()).readImage(data.get(position).carInfo.pic_id);

                                        if (bitmap != null) {
                                            imgUser.setImageBitmap(bitmap);
                                        }
                                    } else imgUser.setImageBitmap(BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(), R.drawable.scionxdoutline640));
                                } else if (!userProfile && data.get(position).userInfo != null) {
                                    userProfile = true;
                                    if (((MainActivity) getActivity()).imageExist(data.get(position).userInfo.pic_id)) {
                                        //set  car picture
                                        Bitmap bitmap = ((MainActivity) getActivity()).readImage(data.get(position).userInfo.pic_id);

                                        if (bitmap != null) {
                                            imgUser.setImageBitmap(bitmap);
                                        }
                                    } else {
                                        imgUser.setImageBitmap(BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(), R.drawable.bad_profile_pic_2));
                                    }
                                    txt13.setText(data.get(position).userInfo.fName + " " + data.get(position).userInfo.sName);
                                    txt15.setText("Studying: " + data.get(position).userInfo.studying);
                                    txt16.setText("Passenger rating : " + data.get(position).userInfo.currentUserRating);
                                    txt17.setText("System Rating : " + data.get(position).userInfo.driverSystemRating);
                                }else{
                                    Toast.makeText(getActivity(), "Still getting user data", Toast.LENGTH_LONG).show();
                                }
                            }
                        });

                        if (data.get(position).activityType == Activity.TYPE_ACK_PICKUP) {
                            btnAccept.setVisibility(View.VISIBLE);
                            if (data.get(position).match.getRide().getDriverId().compareTo(((MainActivity) ActivitiesFragment.this.getActivity()).currentUser.userId) == 0)//IF CURRENT USER IS DRIVER
                            {
                                txt16.setText("User rating : " + data.get(position).userInfo.currentUserRating);
                                txt17.setVisibility(View.GONE);
                                btnAccept.setText("I picked them up");
                                btnAccept.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ((MainActivity) ActivitiesFragment.this.getActivity()).doAck(true, data.get(position));
                                    }
                                });
                            } else {
                                txt16.setText("Passenger rating : " + data.get(position).userInfo.driverRating);
                                txt17.setText("System Rating : " + data.get(position).userInfo.driverSystemRating);
                                txt17.setVisibility(View.VISIBLE);
                                btnAccept.setText("I have been picked up");
                                btnAccept.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ((MainActivity) ActivitiesFragment.this.getActivity()).doAck(false, data.get(position));
                                    }
                                });
                            }
                            break;
                        }

                        if (data.get(position).activityType == Activity.TYPE_PASSENGER_FOUND) {
                            txt16.setText("User rating : " + data.get(position).userInfo.currentUserRating);
                            txt17.setVisibility(View.GONE);
                            btnAccept.setVisibility(View.GONE);
                            break;
                        }

                        if (data.get(position).activityType == Activity.TYPE_RIDE_FOUND) {
                            txt17.setVisibility(View.VISIBLE);

                            if (data.get(position).match.getRide().isNow()) {
                                WGS84Point driverLoc = GeoHash.fromGeohashString(data.get(position).match.getCurrentDriverLocation()).getPoint();
                                WGS84Point rideLoc = GeoHash.fromGeohashString(data.get(position).match.getRidePoint()).getPoint();
                                double distance = VincentyGeodesy.distanceInMeters(driverLoc, rideLoc) / 1000;
                                txt14.setText("~" + Math.round(distance*100)/100 + " km");
                            }

                            txt16.setText("Passenger rating : " + data.get(position).userInfo.currentUserRating + "  price : R " + data.get(position).match.getRide().getPrice());
                            txt17.setText("System Rating : " + data.get(position).userInfo.driverSystemRating);
                            btnAccept.setText("Select ride");

                            if (data.get(position).isAccepted)
                                btnAccept.setVisibility(View.GONE);
                            else btnAccept.setVisibility(View.VISIBLE);

                            btnAccept.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ((MainActivity) ActivitiesFragment.this.getActivity()).selectRide(data.get(position).match.getId(), data.get(position), MainActivity.FRAGMENT_ACTIVITIES);
                                }
                            });
                            break;
                        }

                        if (data.get(position).activityType == Activity.TYPE_IN_RIDE) {
                            if (data.get(position).match.getRide().getDriverId().equals(((MainActivity) getActivity()).currentUser.userId)) {
                                WGS84Point driverLoc = ((MainActivity) getActivity()).lastLocation;
                                WGS84Point rideLoc = GeoHash.fromGeohashString(data.get(position).match.getRidePoint()).getPoint();
                                double distance = VincentyGeodesy.distanceInMeters(driverLoc, rideLoc) / 1000;
                                txt14.setText("~" + Math.round(distance*100)/100 + " km to dropoff");
                            }

                            txt16.setText("User rating : " + data.get(position).userInfo.currentUserRating);
                            txt17.setVisibility(View.GONE);
                            btnAccept.setVisibility(View.GONE);
                            break;
                        }
                    } else {
                        txt13.setText("getting user data");
                    }
                    break;
                }

                case 2: {
                    if (data.get(position).activityType == Activity.TYPE_SEPARATOR) {
                        TextView txtSeperator = (TextView) convertView.findViewById(R.id.txtSeperator);
                        txtSeperator.setText(data.get(position).seperatorText);
                        break;
                    }

                    if (data.get(position).activityType == Activity.TYPE_SAVING_POINT) {
                        TextView txtSeperator = (TextView) convertView.findViewById(R.id.txtSeperator);
                        txtSeperator.setText("Saving position \"" + data.get(position).pointName + "\"");
                        break;
                    }

                    break;
                }
                case 3: {
                    TextView txt11 = (TextView) convertView.findViewById(R.id.textView11);
                    TextView txt12 = (TextView) convertView.findViewById(R.id.textView12);
                    final Button btnCancel = (Button) convertView.findViewById(R.id.btnCancel2);
                    final Button btnActivate = (Button) convertView.findViewById(R.id.btnActivate);

                    if (data.get(position).activityType == Activity.TYPE_RECORDING_ROUTE) {
                        txt11.setText("Recording route" + (data.get(position).toCampus ? " to " : " from "));
                        txt12.setText(((MainActivity) getActivity()).getCampusById(data.get(position).campusId).toString());
                        btnActivate.setVisibility(View.GONE);
                        btnCancel.setVisibility(View.GONE);
                        break;
                    }

                    if (data.get(position).activityType == Activity.TYPE_PRERECORD_BEGIN) {
                        txt11.setText("Recording route to " + ((MainActivity) getActivity()).getCampusById(data.get(position).campusId).toString());
                        txt12.setText("Waiting for recording to begin");
                        btnActivate.setVisibility(View.GONE);
                        btnCancel.setVisibility(View.GONE);
                        break;
                    }

                    if (data.get(position).activityType == Activity.TYPE_ROUTE_RECORDED) {
                        txt11.setText("Route recorded");
                        txt12.setText("Route " + (data.get(position).toCampus ? " to " : " from ") + ((MainActivity) getActivity()).getCampusById(data.get(position).campusId).toString() + " recorded");
                        btnActivate.setVisibility(View.GONE);
                        btnCancel.setVisibility(View.GONE);
                        break;
                    }

                    if (data.get(position).activityType == Activity.TYPE_SAVING_ROUTE) {
                        txt11.setText("Saving Route");
                        txt12.setText("Route " + (data.get(position).toCampus ? " to " : " from ") + ((MainActivity) getActivity()).getCampusById(data.get(position).campusId).toString());
                        btnActivate.setVisibility(View.GONE);
                        btnCancel.setVisibility(View.GONE);
                        break;
                    }

                    if (data.get(position).activityType == Activity.TYPE_SEARCHING_RIDES) {
                        txt11.setText("Searching for drivers");
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss");
                        txt12.setText((data.get(position).qry.isToCampus() ? "To " : "From ") + ((MainActivity) getActivity()).getCampusById(data.get(position).qry.getCampusId())
                                + (data.get(position).qry.isNow() ? " " : " on ") + sdf.format(data.get(position).qry.getRideDateTime()));

                        btnActivate.setVisibility(View.GONE);
                        btnCancel.setVisibility(View.GONE);
                        break;
                    }

                    if (data.get(position).activityType == Activity.TYPE_ONGOING_RIDE) {
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss");
                        txt11.setText("Searching for passengers");
                        txt12.setText("Ride " + (data.get(position).ride.isToCampus() ? " to " : " from ") + ((MainActivity) getActivity()).getCampusById(data.get(position).ride.getCampusId())
                                + (data.get(position).ride.isNow() ? " " : " on ") + sdf.format(data.get(position).ride.getRideDateTime()));

                        if (data.get(position).activated || data.get(position).ride.isNow())
                            btnActivate.setVisibility(View.INVISIBLE);
                        else btnActivate.setVisibility(View.VISIBLE);

                        btnCancel.setVisibility(View.VISIBLE);

                        btnCancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                btnCancel.setEnabled(false);
                                ((MainActivity) ActivitiesFragment.this.getActivity()).cancelRide(data.get(position).ride.getRideId(), data.get(position));
                            }
                        });

                        btnActivate.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                btnActivate.setEnabled(false);
                                ((MainActivity) ActivitiesFragment.this.getActivity()).activateRide(data.get(position));
                            }
                        });

                        break;
                    }

                }
            }

            convertView.setTag(data.get(position));
            return convertView;
        }

    }
}


