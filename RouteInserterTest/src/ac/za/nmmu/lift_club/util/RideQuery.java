/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.za.nmmu.lift_club.util;

import ch.hsr.geohash.GeoHash;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author Joshua
 */
public class RideQuery {

    protected String geohash;//pickup or dropoff point based on value of isToCampus
    protected String userId, id;
    protected double lat, lon;
    protected boolean isToCampus, isNow, needBoot;
    protected String campusId;
    protected Date rideDateTime;
    protected boolean userIsFemale = false;

    public RideQuery()
    {
        id = UUID.randomUUID().toString();
    }
    
    public RideQuery(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        geohash = GeoHash.geoHashStringWithCharacterPrecision(lat, lon, 9);
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public boolean isUserFemale() {
        return userIsFemale;
    }

    public void setUserIsFemale(boolean userIsMale) {
        this.userIsFemale = userIsMale;
    }

    public String getGeohash() {
        return geohash;
    }

    public boolean needsBoot() {
        return needBoot;
    }

    public void setNeedBoot(boolean needBoot) {
        this.needBoot = needBoot;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public boolean isToCampus() {
        return isToCampus;
    }

    public synchronized void setIsToCampus(boolean isToCampus) {
        this.isToCampus = isToCampus;
    }

    public boolean isNow() {
        return isNow;
    }

    public synchronized void setIsNow(boolean isNow) {
        this.isNow = isNow;
    }

    public String getCampusId() {
        return campusId;
    }

    public synchronized void setCampusId(String campusId) {
        this.campusId = campusId;
    }

    public Date getRideDateTime() {
        return rideDateTime;
    }

    public synchronized void setRideDateTime(Date rideDateTime) {
        this.rideDateTime = rideDateTime;
    }

    public String getUserId() {
        return userId;
    }

    public synchronized void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean evaluate(Ride ride) {
        return false;
    }
}
