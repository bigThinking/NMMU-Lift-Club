package za.ac.nmmu.lift_club;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Locale;

import za.ac.nmmu.lift_club.util.Activity;
import za.ac.nmmu.lift_club.util.Car;
import za.ac.nmmu.lift_club.util.Ride;
import za.ac.nmmu.lift_club.util.Route;
import za.ac.nmmu.lift_club.util.User;

/**
 * Created by s210036575 on 2015-07-07.
 */
public class OfferLiftFragment extends Fragment implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    TextView txtDate, txtTime, txtPrice, txtSeats;
    boolean isNow = true, isToCampus = true,onlyGender = false;
    Calendar cal = Calendar.getInstance();
    int price = 0, availableNrOfSeats, maxSeats, dateTimeSet = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.offer_lift, container, false);
        final RadioButton rbtNow = (RadioButton)view.findViewById(R.id.rbtNow1);
        final RadioButton rbtLater = (RadioButton)view.findViewById(R.id.rbtLater1);
        final View dt = view.findViewById(R.id.dt1);
        txtDate = (TextView)view.findViewById(R.id.txtDate);
        txtTime = (TextView)view.findViewById(R.id.txtTime);
        Button btnOffer = (Button)view.findViewById(R.id.btnOffer);
        final RadioGroup rgpTime = (RadioGroup)view.findViewById(R.id.rgpTime2);
        final Spinner spnRoute = (Spinner)view.findViewById(R.id.spnRoute);
        final RadioGroup rgpDirr = (RadioGroup)view.findViewById(R.id.rgpCampusDirr3);
        txtPrice = (TextView)view.findViewById(R.id.txtPrice);
        txtSeats = (TextView)view.findViewById(R.id.txtAvailSeats);
        Car currentCar = ((MainActivity)getActivity()).currentCar;
        final User currentUser = ((MainActivity)getActivity()).currentUser;
        availableNrOfSeats = currentCar.nrSeats;
        maxSeats = currentCar.nrSeats;
        final CheckBox cbxGender = (CheckBox)view.findViewById(R.id.cbxOnlyGender2);
        cbxGender.setText("Only " + (currentUser.gender == 'm' ? "male passengers" : "female passengers"));
        final EditText edtComments = (EditText)view.findViewById(R.id.edtComments);

        ArrayAdapter<Route> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, ((MainActivity) this.getActivity()).routes);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item );
        spnRoute.setAdapter(spinnerArrayAdapter);

        txtSeats.setText(String.valueOf(maxSeats));

        btnOffer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ride ride = new Ride();
                ride.setPrice(price);
                ride.setInitialSeatCount((byte) availableNrOfSeats);
                ride.setIsNow(isNow);
                ride.setIsToCampus(isToCampus);
                ride.setDriverId(((MainActivity) getActivity()).currentUser.userId);

                if (cbxGender.isChecked())
                    ride.setOnlyGender(currentUser.gender == 'm' ? Ride.MALE : Ride.FEMALE);

                if(!isNow) {
                    if (dateTimeSet >= 2)
                        ride.setRideDateTime(cal.getTime());
                    else{
                        showErrorDialog("Missing values", "Please enter a date and time");
                        return;
                    }
                }

                ride.setComment(edtComments.getText().toString());

                ride.setRouteId(((Route) spnRoute.getSelectedItem()).routeId);
                ride.setCampusId(((Route)spnRoute.getSelectedItem()).campusId);
                Activity a = new Activity(Activity.TYPE_ONGOING_RIDE);
                a.ride = ride;

                if(!ride.isNow()) {//alarm to remind driver of ride
                    Intent intent = new Intent(OfferLiftFragment.this.getActivity().getApplicationContext(), MainActivity.class);
                    intent.putExtra("za.ac.nmmu.lift_club.type",25);
                    PendingIntent sender = PendingIntent.getActivity(OfferLiftFragment.this.getActivity().getApplicationContext(), 25, intent, PendingIntent.FLAG_ONE_SHOT);

                    // Schedule the alarm!
                    AlarmManager am = (AlarmManager) OfferLiftFragment.this.getActivity().getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
                }

                ((MainActivity)getActivity()).offerRide(a);
            }
        });

        rgpTime.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbtNow1) {
                    isNow = true;
                    dt.setVisibility(View.GONE);
                } else {
                    isNow = false;
                    dt.setVisibility(View.VISIBLE);
                }
            }
        });

        rgpDirr.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.rbtnFrom3) {
                    rbtNow.setVisibility(View.GONE);
                    rbtLater.setChecked(true);
                    isToCampus = false;
                }
                else {
                    rbtNow.setVisibility(View.VISIBLE);
                    rbtNow.setChecked(true);
                    isToCampus = true;
                }
            }
        });

        txtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
               /* DatePickerDialogFragment dpdf = new DatePickerDialogFragment();
                dpdf.listener = OfferLiftFragment.this;
                dpdf.show(getFragmentManager(), "");*/
            }
        });

        txtTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
              /*  TimePickerDialogFragment tpdf = new TimePickerDialogFragment();
                tpdf.listener = OfferLiftFragment.this;
                tpdf.show(getFragmentManager(), "");*/
            }
        });

        ImageButton btnIncPrice = (ImageButton) view.findViewById(R.id.imgbtnPlus1);
        ImageButton btnIncSeats = (ImageButton) view.findViewById(R.id.imgbtnPlus2);
        ImageButton btnDecPrice = (ImageButton) view.findViewById(R.id.imgbtnMinus1);
        ImageButton btnDecSeats = (ImageButton) view.findViewById(R.id.imgbtnMinus2);

        btnIncPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incPrice();
            }
        });

        btnIncSeats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incSeats();
            }
        });

        btnDecPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decPrice();
            }
        });

        btnDecSeats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decSeats();
            }
        });

        if(savedInstanceState != null)
        {

        }else{
            dt.setVisibility(View.GONE);
        }

        decPrice();
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

    private void decSeats()
    {
        if(availableNrOfSeats > 1)
            availableNrOfSeats--;
        txtSeats.setText(String.valueOf(availableNrOfSeats));
    }

    private void incSeats()
    {
        if(availableNrOfSeats < maxSeats)
            availableNrOfSeats++;
        txtSeats.setText(String.valueOf(availableNrOfSeats));
    }

    private void decPrice()
    {
        if(price > 0)
            price--;

        if(price == 0)
        {
            txtPrice.setText("Free");
            return;
        }

        txtPrice.setText("R " + String.valueOf(price));
    }

    private void incPrice()
    {
        price++;
        txtPrice.setText("R " + String.valueOf(price));
    }

    public void showErrorDialog(String title, String message)
    {
        ErrorDialogFragment edf = new ErrorDialogFragment();
        edf.title = title;
        edf.errorMessage = message;
        edf.show(getActivity().getSupportFragmentManager(), "");
    }
}
