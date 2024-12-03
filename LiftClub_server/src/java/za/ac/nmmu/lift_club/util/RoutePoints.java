/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.util;

import ch.hsr.geohash.WGS84Point;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author Joshua
 */
public class RoutePoints{    
    private final Route route;
    private final Cell cell;
    private final ArrayList<WGS84Point> points = new ArrayList<WGS84Point>();
    private int order;
    private RoutePoints pred,succ;

    public RoutePoints getPred() {
        return pred;
    }

    public void setPred(RoutePoints pred) {
        this.pred = pred;
    }

    public RoutePoints getSucc() {
        return succ;
    }

    public void setSucc(RoutePoints succ) {
        this.succ = succ;
    }
    
    public RoutePoints(Route route, Cell cell)
    {
        this.route = route;
        this.cell = cell;
    }

    public int getOrder() {
        return order;
    }

    
    public synchronized void setOrder(int order) {
        this.order = order;
    }
    
    public synchronized void addPoint(WGS84Point point)
    {
        points.add(point);
    }

    public Route getRoute() {
        return route;
    }

    public Cell getCell() {
        return cell;
    }
    
    public Iterator getPointsIterator()
    {
       return points.iterator();
    }
}

//class Point{
//    private double lat, lon;
//
//    public Point(double lat, double lon)
//    {
//        this.lat = lat;
//        this.lon = lon;
//    }
//      
//    public double getLat() {
//        return lat;
//    }
//
//    public double getLon() {
//        return lon;
//    }
//
//    public void setLat(double lat) {
//        this.lat = lat;
//    }
//
//    public void setLon(double lon) {
//        this.lon = lon;
//    }
//    
//}
