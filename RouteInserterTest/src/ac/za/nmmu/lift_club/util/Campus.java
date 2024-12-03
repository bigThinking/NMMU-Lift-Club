/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.za.nmmu.lift_club.util;

import ch.hsr.geohash.WGS84Point;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Joshua
 */
public class Campus {
    
    private final String campusId;
    private final WGS84Point loc;
    private final ConcurrentHashMap<String,WGS84Point> entryPoints = new ConcurrentHashMap<String,WGS84Point>(5);
    
     public Campus(String campusId, double lat, double lon)
    {
        this.loc = new WGS84Point(lat,lon);
        this.campusId = campusId;
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
    
     public WGS84Point getEntryPoint(String id)
     {
         return entryPoints.get(id);
     }
    
}
