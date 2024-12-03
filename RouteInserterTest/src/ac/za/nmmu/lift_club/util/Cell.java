/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.za.nmmu.lift_club.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.String;
import java.util.Iterator;

/**
 *
 * @author Joshua
 * Each cell is specified by a geohash(9 characters for my purposes)
 */
public class Cell {
   private final String geohash;
   private Cell north, northEast, northWest, south, southEast, southWest, east, west;
   private int routeCount = 0, rideCount = 0; 
   private final Map routePoints = new ConcurrentHashMap<String,RoutePoints>(20);
   private final Map nowRides = new ConcurrentHashMap<String,Ride>(4, (float) 1.1);
   private final Map laterRides = new ConcurrentHashMap<String,Ride>(4, (float) 1.1);
     
   public Cell(String geohash)
   {
       this.geohash = geohash;
   }
   
    public String getGeoHash() {
        return geohash;
    }

    private synchronized void incRouteCount() {
        this.routeCount++;
    }
    
    private synchronized void decRouteCount() {
        this.routeCount--;
    }

    public int getRideCount() {
        return rideCount;
    }

    private synchronized void incRideCount() {
        this.rideCount++;
    }
    
    private synchronized void decRideCount() {
        this.rideCount--;
    }    
    
//    public void addRide(Ride ride)
//    {
//        if(ride.isNow())
//            nowRides.put(ride.getRideId(),ride);
//        else laterRides.put(ride.getRideId(),ride);
//        incRideCount();
//    }
    
    public void addRoutePoints(RoutePoints r)
    {
        routePoints.put(r.getRoute().getRouteId(), r);
        incRouteCount();
    }
    
//    public void activateRide(String rideId)
//    {
//        if(laterRides.containsKey(rideId))
//        {
//            Ride ride = (Ride)laterRides.remove(rideId);
//            if(ride.isToCampus()){
//            ride.setIsNow(true);
//            nowRides.put(rideId, ride);
//            }
//        }
//    }
    
    public void removeRide(String rideId)
    {
        if(nowRides.containsKey(rideId))
        nowRides.remove(rideId);
        else if(laterRides.containsKey(rideId))
            nowRides.remove(rideId);
    }
    
    public Cell getNorth() {
        return north;
    }

    public Cell getNorthEast() {
        return northEast;
    }

    public Cell getNorthWest() {
        return northWest;
    }

    public Cell getSouth() {
        return south;
    }

    public Cell getSouthEast() {
        return southEast;
    }

    public Cell getSouthWest() {
        return southWest;
    }

    public Cell getEast() {
        return east;
    }

    public Cell getWest() {
        return west;
    }

    public int getRouteCount() {
        return routeCount;
    }

    public void setNorth(Cell north) {
        this.north = north;
    }

    public void setNorthEast(Cell northEast) {
        this.northEast = northEast;
    }

    public void setNorthWest(Cell northWest) {
        this.northWest = northWest;
    }

    public void setSouth(Cell south) {
        this.south = south;
    }

    public void setSouthEast(Cell southEast) {
        this.southEast = southEast;
    }

    public void setSouthWest(Cell southWest) {
        this.southWest = southWest;
    }

    public void setEast(Cell east) {
        this.east = east;
    }

    public void setWest(Cell west) {
        this.west = west;
    }
    
    public void removeRides(Iterator i)
    {
        while(i.hasNext())
        {
            String key = (String)i.next();
            
            if(nowRides.remove(key) == null)
                laterRides.remove(key);
        }
    }
    
    public void removeRoutePoints(String routeId)
    {
        routePoints.remove(routeId);
    }
    
    public RoutePoints getRoutePoints(String routeId)
    {
        return (RoutePoints)routePoints.get(routeId);
    }
    
    public boolean equals(Cell cell)
    {
        return this.geohash.equals(cell.getGeoHash());
    }
    
    public boolean adjacentsFilled()
    {
        if(north != null || northEast != null || northWest != null || south != null || southEast != null || southWest != null || east != null || west != null )
            return true;
        return false;
    }
    
    public Iterator getNowRidesIterator()
    {
        return nowRides.entrySet().iterator();
    }
    
    public Iterator getLaterRidesIterator()
    {
        return laterRides.entrySet().iterator();
    }
    
    public boolean NowRidesContains(String rideId)
    {
        return nowRides.containsKey(rideId);
    }
    
    public boolean LaterRidesContains(String rideId)
    {
        return laterRides.containsKey(rideId);
    }
}
