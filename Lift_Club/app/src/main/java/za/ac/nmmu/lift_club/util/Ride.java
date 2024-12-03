/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 *
 * @author Joshua
 */
public class Ride {

    public static final byte MALE = 0;
    public static final byte FEMALE = 1;
    private String rideId = "";
    private String driverId = "", campusId = "", routeId = "";
    private byte initialSeatCount = 0, filledSeatCount = 0;
    private Route route;
    private boolean isToCampus, isNow, hasBoot, dbRecorded;
    private Date rideDateTime = new Date(), rideDateTimeBegin; // rideDateTime must be set if isNow is false
    private double price;
    private byte onlyGender = -1; // -1 = unset, 0 = males, 1 = females
    private Match[] passengers = null;
    private String comment = "";

    public Ride() {
        rideId = UUID.randomUUID().toString();
    }

    public Ride(String jsonString) throws BadMessageException {
        //rideId = UUID.randomUUID().toString();
        JsonParser parser = Json.createParser(new StringReader(jsonString));

        Event event = null;

        while (parser.hasNext()) {
            event = parser.next();

            if (event == Event.KEY_NAME) {
                switch (parser.getString()) {
                    case "rideId":{
                        parser.next();
                        rideId = parser.getString();
                        break;
                    }
                    case "driverId": {
                        parser.next();
                        driverId = parser.getString();
                        break;
                    }
                    case "routeId": {
                        parser.next();
                        routeId = parser.getString();
                        break;
                    }
                    case "noOfSeats": {
                        parser.next();
                        initialSeatCount = (byte) parser.getInt();
                        break;
                    }
                    case "isToCampus": {
                        event = parser.next();
                        isToCampus = event == Event.VALUE_TRUE;
                        break;
                    }
                    case "campusId": {
                        parser.next();
                        campusId = parser.getString();
                        break;
                    }
                    case "onlyGender": {
                        parser.next();
                        onlyGender = parser.getString().equals("male") ? MALE : (parser.getString().equals("female") ? FEMALE : -1);
                        break;
                    }
                    case "isNow": {
                        event = parser.next();
                        isNow = event == Event.VALUE_TRUE;
                        break;
                    }
                    case "hasBoot": {
                        event = parser.next();
                        hasBoot = event == Event.VALUE_TRUE;
                        break;
                    }
                    case "dateTime": {
                        parser.next();
                        rideDateTime = new Date(parser.getLong());
                        break;
                    }
                    case "price": {
                        parser.next();
                        price = Double.valueOf(parser.getString());
                        break;
                    }
                    case "comment": {
                        parser.next();
                        comment = parser.getString();
                        break;
                    }
                }
            }
        }

        if (driverId == null || campusId == null || routeId == null || initialSeatCount == 0) {
            throw new BadMessageException();
        }
        
        if (!isNow && rideDateTime == null) {
            throw new BadMessageException();
        }
    }

    public void setRouteId(String routeId)
    {
        this.routeId = routeId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getCampusId() {
        return campusId;
    }

    public synchronized void setCampusId(String campusId) {
        this.campusId = campusId;
    }

    public byte getInitialSeatCount() {
        return initialSeatCount;
    }

    public synchronized void setInitialSeatCount(byte initialSeatCount) {
        this.initialSeatCount = initialSeatCount;
        passengers = new Match[initialSeatCount];
    }

    public byte getFilledSeatCount() {
        return filledSeatCount;
    }

    private synchronized void incFilledSeatCount() {
        this.filledSeatCount++;
    }

    private synchronized void decFilledSeatCount() {
        this.filledSeatCount--;
    }

    public void addPassenger(Match rideMatch) {
        if (filledSeatCount < initialSeatCount) {
            passengers[filledSeatCount] = rideMatch;
            incFilledSeatCount();
        }
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
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

    public Date getRideDateTime() {
        return rideDateTime;
    }

    public synchronized boolean setRideDateTime(Date rideDateTime) {
        if (!isNow) {
            this.rideDateTime = rideDateTime;
            return true;
        }

        return false;
    }

    public double getPrice() {
        return price;
    }

    public synchronized void setPrice(double price) {
        this.price = price;
    }

    public byte getOnlyGender() {
        return onlyGender;
    }

    public synchronized void setOnlyGender(byte onlyGender) {
        if (onlyGender == MALE || onlyGender == FEMALE) {
            this.onlyGender = onlyGender;
        }
    }

    public String getRideId() {
        return rideId;
    }

    public String getComment() {
        return comment;
    }

    public synchronized void setComment(String comment) {
        this.comment = comment;
    }

    public Match[] getPassengers() {
        return passengers;
    }

    public boolean isHasBoot() {
        return hasBoot;
    }

    public synchronized void setHasBoot(boolean hasBoot) {
        this.hasBoot = hasBoot;
    }

    private String getGenderString() {
        return onlyGender == MALE ? "male" : (onlyGender == FEMALE ? "female" : "none");
    }

    public String getJSON() {
        StringWriter sw = new StringWriter();
        JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
        JsonGenerator g = factory.createGenerator(sw);

        g.writeStartObject().write("rideId", rideId)
                .write("driverId", driverId)
                .write("campusId", campusId)
                .write("routeId", routeId)
                .write("noOfSeats", initialSeatCount - filledSeatCount)
                .write("isToCampus", isToCampus)
                .write("onlyGender", onlyGender == MALE ? "male" : (onlyGender == FEMALE ? "female" : "none"))
                .write("isNow", isNow)
                .write("hasBoot", hasBoot)
                .write("dateTime", rideDateTime.getTime())
                .write("price", price)
                .write("comment", comment)
                .writeEnd()
                .close();

        return sw.toString();

    }

    public boolean hasSeat() {
        return filledSeatCount < initialSeatCount;
    }

    public Match[] removePassenger(String qryId) {
        for (int i = 0; i < passengers.length; i++) {
            if (passengers[i].getId().compareTo(qryId) == 0) {
                if (i != passengers.length - 1) {
                    System.arraycopy(passengers, i + 1, passengers, i, passengers.length - 1 - i);
                }
                break;
            }
        }
        filledSeatCount--;
        return passengers;
    }
    
    public boolean isDbRecorded() {
        return dbRecorded;
    }

    public void setDbRecorded(boolean dbRecorded) {
        this.dbRecorded = dbRecorded;
    }

    public Date getRideDateTimeBegin() {
        return rideDateTimeBegin;
    }

    public void setRideDateTimeBegin(Date rideDateTimeBegin) {
        this.rideDateTimeBegin = rideDateTimeBegin;
    }
}
