/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.util;

import java.io.StringWriter;
import java.util.Date;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

/**
 *
 * @author Joshua
 */
public class Match extends RideQuery{
    private String ridePoint = "";//geohash
    private double price = 0;
    private Ride ride;
    private boolean passengerAcked, driverAcked, dbRecorded;
    private Date rideDateTimePickedUp;
    private String currentDriverLocation = "";//geohash
   
    public Match(RideQuery qry)
    {
        super(qry.getLat(),qry.getLon());
        this.id = qry.id;
        this.isToCampus = qry.isToCampus;
        this.isNow = qry.isNow;
        this.needBoot = qry.needBoot;
        this.campusId = qry.campusId;
        this.rideDateTime = qry.rideDateTime;
        this.userIsFemale = qry.userIsFemale;
        this.userId = qry.userId;
    }
    
    public String getOriginalRidePoint() {
        return this.geohash;
    }

    public String getRidePoint() {
        return ridePoint;
    }

    public void setRidePoint(String pickUpPoint) {
        this.ridePoint = pickUpPoint;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }
    
    @Override
    public String getJson() {
        StringWriter sw = new StringWriter();
        JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
        JsonGenerator g = factory.createGenerator(sw);

        g.writeStartObject().write("Id", id)
                .write("userId", userId)
                .write("lat", lat)
                .write("lon", lon)
                .write("campusId", campusId)
                .write("isTocampus", isToCampus)
                .write("isNow", isNow)
                .write("needBoot", needBoot)
                .write("dateTime", rideDateTime.getTime())
                .write("ridePoint",ridePoint)
                .write("price", price)
                .write("ride",ride.getJSON())
                .write("currentDriverLocation", currentDriverLocation)
                .writeEnd()
                .close();

        return sw.toString();
    }

    public boolean isPassengerAcked() {
        return passengerAcked;
    }

    public void setPassengerAcked(boolean passengerAcked) {
        this.passengerAcked = passengerAcked;
    }

    public boolean isDriverAcked() {
        return driverAcked;
    }

    public void setDriverAcked(boolean driverAcked) {
        this.driverAcked = driverAcked;
    }

    public boolean isDbRecorded() {
        return dbRecorded;
    }

    public void setDbRecorded(boolean dbRecorded) {
        this.dbRecorded = dbRecorded;
    }

    public Date getRideDateTimePickedUp() {
        return rideDateTimePickedUp;
    }

    public void setRideDateTimePickedUp(Date rideDateTimePickeUp) {
        this.rideDateTimePickedUp = rideDateTimePickeUp;
    }

    public String getCurrentDriverLocation() {
        return currentDriverLocation;
    }

    public void setCurrentDriverLocation(String currentDriverLocation) {
        this.currentDriverLocation = currentDriverLocation;
    }
 }
