package za.ac.nmmu.lift_club.util;

import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import ch.hsr.geohash.WGS84Point;

/**
 * Created by s210036575 on 2015-08-13.
 */
public class Position {

    public String Id, name;
    public WGS84Point point;
    public Date dateCreated;

    public static ArrayList<Position> parse(String json) throws JSONException, ParseException {
        ArrayList<Position> positions = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(json);
        JSONArray pointsArray = jsonObject.getJSONArray("points");
       // SimpleDateFormat sdf = new SimpleDateFormat();

        for(int i = 0; i < pointsArray.length(); i++)
        {
            JSONObject positionObject = pointsArray.getJSONObject(i);
            positions.add(new Position(positionObject.getString("positionId"), positionObject.getString("name"),
                    new Date(positionObject.getLong("dateCreated")), new WGS84Point(positionObject.getDouble("lat"), positionObject.getDouble("lon"))));
        }

        return positions;
    }

    public Position(String Id, String name, Date dateCreated, WGS84Point point)
    {
        this.point = point;
        this.Id = Id;
        this.name = name;
        this.dateCreated = dateCreated;
    }

    public String toString()
    {
        return name;
    }

    public String toJson() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        writer.beginObject().name("positionId").value(Id).name("name").value(name).name("dateCreated")
                    .value(dateCreated.getTime()).name("lat").value(point.getLatitude()).name("lon").value(point.getLongitude()).endObject().flush();
        writer.close();


        return sw.toString();
    }
}
