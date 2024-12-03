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
public class Route {
    public String routeId, name, campusId;
    public double price = 0;
    public Date dateCreated;
    public String[] geohashes;

    public static ArrayList<Route> parse(String json) throws JSONException {
        ArrayList<Route> routes = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(json);
        JSONArray routesArray = jsonObject.getJSONArray("routes");

        for(int i = 0; i < routesArray.length(); i++)
        {
            JSONObject routeObject = routesArray.getJSONObject(i);
            routes.add(new Route(routeObject.getString("routeId"), routeObject.getString("name"), routeObject.getString("campusId")
            , routeObject.getDouble("price"), new Date(routeObject.getLong("dateCreated"))));

            JSONArray array = routeObject.getJSONArray("geohashes");
            if(array != null) {
                String[] geohashes = new String[array.length()];

                for (int j = 0; j < geohashes.length; j++) {
                    geohashes[j] = array.getString(j);
                }

                routes.get(routes.size()-1).geohashes = geohashes;
            }
        }
        return routes;
    }

    public Route(String routeId, String name, String campusId, double price, Date dateCreated)
    {
        this.routeId = routeId;
        this.name = name;
        this.campusId = campusId;
        this.price = price;
        this.dateCreated = dateCreated;
    }

    public String toString()
    {
        return  name;
    }

    public String toJson() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        writer.beginObject().name("routeId").value(routeId).name("name").value(name).name("dateCreated")
                    .value(dateCreated.getTime()).name("campusId").value(campusId).name("price").value(price).name("geohashes").beginArray();

        if(geohashes != null) {
            for (int i = 0; i < geohashes.length; i++) {
                writer.value(geohashes[i]);
            }
        }

        writer.endArray().endObject();
        writer.flush();
        writer.close();

        return sw.toString();
    }

}
