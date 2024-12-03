/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.hsr.geohash.WGS84Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Joshua
 */
public class Campus implements Serializable {
    
    private String campusId, campusName;
    private WGS84Point loc;
    private final HashMap<String,WGS84Point> entryPoints = new HashMap<>(5);

    public static ArrayList<Campus> parse(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        JSONArray campusesArray = jsonObject.getJSONArray("campuses");

        ArrayList<Campus> campuses = new ArrayList<>();

        for(int i = 0; i < campusesArray.length(); i++){
            JSONObject campus = campusesArray.getJSONObject(i);
            Campus c = new Campus(campus.getString("Id"), campus.getDouble("lat"), campus.getDouble("lon"), campus.getString("name"));

            JSONArray epArray = campus.getJSONArray("entryPoints");

            for(int j = 0; j < epArray.length(); j++)
            {
                JSONObject entryPoint = epArray.getJSONObject(j);
                c.addEntryPoint(entryPoint.getString("Id"), entryPoint.getDouble("lat"), entryPoint.getDouble("lon"));
            }

            campuses.add(c);
        }

        Log.i("Campus count : ", String.valueOf(campuses.size()));
        return  campuses;
    }

    public Campus(String campusId, double lat, double lon, String campusName)
    {
        this.loc = new WGS84Point(lat,lon);
        this.campusId = campusId;
        this.campusName = campusName;
    }

    public String getCampusName()
    {
        return campusName;
    }
    public String getCampusId() {
        return campusId;
    }

    public WGS84Point getLoc() {
        return loc;
    }
          
     public void addEntryPoint(String id, double lat, double lon)
     {
         entryPoints.put(id, new WGS84Point(lat,lon));
     }
    
     public Iterator<Map.Entry<String, WGS84Point>> getEntryPointsIterator()
     {
         return entryPoints.entrySet().iterator();
     }

    public String toString()
    {
        return campusName;
    }
    
}
