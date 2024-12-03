/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.za.nmmu.lift_club.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * points in order away from campus
 *
 * @author Joshua
 */
public class Route {

    private String routeId, driverId, campusId;
    private final Map rides = new ConcurrentHashMap<String, Ride>(2, (float) 1.1);
    private final ArrayList<RoutePoints> routepoints = new ArrayList<RoutePoints>();
    private double radialDistanceToCampus;//of start point

    public Route() {

    }

    public double getRadialDistanceToCampus() {
        return radialDistanceToCampus;
    }

    public void setRadialDistanceToCampus(double radialDistanceToCampus) {
        this.radialDistanceToCampus = radialDistanceToCampus;
    }

    public Route(String routeId, String driverId, String campusId, double price) {
        this.driverId = driverId;
        this.routeId = routeId;
        this.campusId = campusId;
    }

    public String getCampusId() {
        return campusId;
    }

    public String getRouteId() {
        return routeId;
    }

    public synchronized void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getDriverId() {
        return driverId;
    }

    public synchronized void setDriverId(String driverId) {
        this.driverId = driverId;
    }

//    public void addRide(Ride ride) {
//        rides.put(ride.getRideId(), ride);
//
//        for (RoutePoints r : routepoints) {
//            Cell cell = r.getCell();
//            cell.addRide(ride);
//        }
//    }

    public void removeRide(final String rideId) {
        rides.remove(rideId);
        new Thread(new Runnable() {

            @Override
            public void run() {
                for (RoutePoints r : routepoints) {
                    Cell cell = r.getCell();
                    cell.removeRide(rideId);
                }
            }

        }).start();

    }

    public synchronized void addRoutePoints(RoutePoints r) {
        routepoints.add(r);
    }

    public Iterator getRoutePointsIterator() {
        return routepoints.iterator();
    }

    public Ride getRide(String rideId) {
        return (Ride) rides.get(rideId);
    }

    public Set getRideKeySet() {
        return rides.keySet();
    }
}
