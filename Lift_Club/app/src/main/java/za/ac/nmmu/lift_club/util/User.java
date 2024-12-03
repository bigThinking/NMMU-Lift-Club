package za.ac.nmmu.lift_club.util;

import android.util.JsonWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by s210036575 on 2015-08-13.
 */
public class User {

    public String userId="", fName="", sName="", initials="", mNames="", studying="", email="", cell="", currentCarVRN="", licenseNo="", pic_id="";
    public boolean isDriver;
    public int nrRidesTaken, nrPointsSaved, nrRatingsRecieved, nrRidesGiven, nrRoutesRecorded;
    public double currentUserRating, driverSystemRating, driverRating;
    public Date userCreated, driverCreated;
    public char gender;

    public User(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);

        userId = jsonObject.getString("userId");
        initials = jsonObject.getString("initials");
        fName = jsonObject.getString("firstname");
        sName = jsonObject.getString("surname");
        mNames = jsonObject.getString("middlenames");
        studying = jsonObject.getString("studying");
        email = jsonObject.getString("email");
        cell = jsonObject.getString("cell");
        isDriver = jsonObject.getBoolean("is_driver");
        nrRidesTaken = jsonObject.getInt("nr_rides_taken");
        nrPointsSaved = jsonObject.getInt("nr_points_saved");
        nrRatingsRecieved = jsonObject.getInt("nr_ratings_recieved");
        currentUserRating = jsonObject.getDouble("current_rating");
        //DateFormat df = new SimpleDateFormat();
        //userCreated = df.parse(jsonObject.getString("datetime_created"));
        userCreated = new Date(jsonObject.getLong("datetime_created"));
        gender = jsonObject.getString("gender").toCharArray()[0];
        pic_id = jsonObject.getString("pic_id");

        if(isDriver)
        {
            licenseNo = jsonObject.getString("licenseNo");
            currentCarVRN = jsonObject.getString("current_car_vrn");
            nrRidesGiven = jsonObject.getInt("nr_rides");
            driverSystemRating = jsonObject.getDouble("driver_system_rating");
            driverRating = jsonObject.getDouble("driver_user_rating");
            //driverCreated = df.parse(jsonObject.getString("driver_datetime_created"));
            driverCreated = new Date(jsonObject.getLong("driver_datetime_created"));

            try {
                nrRoutesRecorded = jsonObject.getInt("nr_routes_saved");
            }catch (Exception e)
            {

            }
        }
    }

    public String toJson()
    {
        StringWriter sw =  new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        try {
            writer.beginObject().name("userId").value(userId).name("initials").value(initials).name("firstname").value(fName).name("surname").value(sName)
                    .name("middlenames").value(mNames).name("studying").value(studying).name("email").value(email).name("cell").value(cell)
                    .name("is_driver").value(isDriver).name("nr_rides_taken").value(nrRidesTaken).name("nr_points_saved").value(nrPointsSaved)
                    .name("nr_ratings_recieved").value(nrRatingsRecieved).name("current_rating").value(currentUserRating).name("datetime_created").value(userCreated.getTime())
                    .name("pic_id").value(pic_id).name("gender").value(gender);

            if(isDriver)
            {
                writer.name("licenseNo").value(licenseNo).name("current_car_vrn").value(currentCarVRN).name("nr_rides").value(nrRidesGiven).name("driver_system_rating").value(driverSystemRating)
                        .name("driver_user_rating").value(driverRating).name("driver_datetime_created").value(driverCreated.getTime()).name("nr_routes_saved").value(nrRoutesRecorded);
            }
            writer.endObject().flush();
            writer.close();
            return sw.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }
}
