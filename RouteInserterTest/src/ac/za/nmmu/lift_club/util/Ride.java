/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.za.nmmu.lift_club.util;

import java.util.Date;
import java.util.UUID;

/**
 *
 * @author Joshua
 */
public class Ride {

    public static final byte MALE = 0;
    public static final byte FEMALE = 1;
    private final String rideId;
    private String driverId, campusId, routeId;
    private byte initialSeatCount, filledSeatCount = 0;
   // private Route route;
    private boolean isToCampus, isNow, hasBoot;
    private Date rideDateTime; // must be set if isNow is false
    private double price;
    private byte onlyGender = -1; // -1 = unset, 0 = males, 1 = females
   // private Match[] passengers = null;
    private String comment;

    public Ride() {
        rideId = UUID.randomUUID().toString();
    }

//    public Ride(String jsonString) {
//        rideId = UUID.randomUUID().toString();
//        JsonParser parser = Json.createParser(new StringReader(jsonString));
//
//        Event event = null;
//
//        while (parser.hasNext()) {
//            event = parser.next();
//
//            if (event == Event.KEY_NAME) {
//                switch (parser.getString()) {
//                    case "driverId": {
//                        parser.next();
//                        driverId = parser.getString();
//                        break;
//                    }
//                    case "routeId": {
//                        parser.next();
//                        routeId = parser.getString();
//                        break;
//                    }
//                    case "noOfSeats": {
//                        parser.next();
//                        initialSeatCount = (byte)parser.getInt();
//                        break;
//                    }
//                    case "isToCampus": {
//                        event = parser.next();
//                        isToCampus = event == event.VALUE_TRUE ? true : false;
//                        break;
//                    }
//                    case "campusId": {
//                        parser.next();
//                        campusId = parser.getString();
//                        break;
//                    }
//                    case "onlyGender": {
//                        parser.next();
//                        onlyGender = parser.getString().equals("male") ? MALE : (parser.getString().equals("female") ? FEMALE : -1);
//                        break;
//                    }
//                    case "isNow": {
//                        event = parser.next();
//                        isNow = event == event.VALUE_TRUE ? true : false;
//                        break;
//                    }
//                    case "hasBoot": {
//                        event = parser.next();
//                        hasBoot = event == event.VALUE_TRUE ? true : false;
//                        break;
//                    }
//                    case "dateTime": {
//                        parser.next();
//                        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
//                        rideDateTime = df.parse(parser.getString());
//                        break;
//                    }
//                    case "price": {
//                        parser.next();
//                        price = Double.valueOf(parser.getString());
//                        break;
//                    }
//                    case "comment": {
//                        parser.next();
//                        comment = parser.getString();
//                        break;
//                    }
//                }
//            }
//        }
//
//    }

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
      //  passengers = new Match[initialSeatCount];
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
}
//    public void addPassenger(Match rideMatch) {
//        if (filledSeatCount < initialSeatCount) {
//            passengers[filledSeatCount] = rideMatch;
//            incFilledSeatCount();
//        }
//    }
//
//    public Route getRoute() {
//        return route;
//    }
//
//    public void setRoute(Route route) {
//        this.route = route;
//    }
//
//    public boolean isToCampus() {
//        return isToCampus;
//    }
//
//    public synchronized void setIsToCampus(boolean isToCampus) {
//        this.isToCampus = isToCampus;
//    }
//
//    public boolean isNow() {
//        return isNow;
//    }
//
//    public synchronized void setIsNow(boolean isNow) {
//        this.isNow = isNow;
//    }
//
//    public Date getRideDateTime() {
//        return rideDateTime;
//    }
//
//    public synchronized boolean setRideDateTime(Date rideDateTime) {
//        if (!isNow) {
//            this.rideDateTime = rideDateTime;
//            return true;
//        }
//
//        return false;
//    }
//
//    public double getPrice() {
//        return price;
//    }
//
//    public synchronized void setPrice(double price) {
//        this.price = price;
//    }
//
//    public byte getOnlyGender() {
//        return onlyGender;
//    }
//
//    public synchronized void setOnlyGender(byte onlyGender) {
//        if (onlyGender == MALE || onlyGender == FEMALE) {
//            this.onlyGender = onlyGender;
//        }
//    }
//
//    public String getRideId() {
//        return rideId;
//    }
//
//    public String getComment() {
//        return comment;
//    }
//
//    public synchronized void setComment(String comment) {
//        this.comment = comment;
//    }
//
//    public Match[] getPassengers() {
//        return passengers;
//    }
//
//    public boolean isHasBoot() {
//        return hasBoot;
//    }
//
//    public synchronized void setHasBoot(boolean hasBoot) {
//        this.hasBoot = hasBoot;
//    }
//
//    private String getGenderString() {
//        return onlyGender == MALE ? "male" : (onlyGender == FEMALE ? "female" : "none");
//    }
//
//    public String getJSON() {
//        StringWriter sw = new StringWriter();
//        JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
//        JsonGenerator g = factory.createGenerator(sw);
//
//        g.writeStartObject().write("rideId", rideId)
//                .write("driverId", driverId)
//                .write("campusId", campusId)
//                .write("routeId", route.getRouteId())
//                .write("noOfSeats", initialSeatCount - filledSeatCount)
//                .write("isTocampus", isToCampus)
//                .write("onlyGender", onlyGender == MALE ? "male" : (onlyGender == FEMALE ? "female" : "none"))
//                .write("isNow", isNow)
//                .write("hasBoot", hasBoot)
//                .write("dateTime", rideDateTime.toString())
//                .write("price", price)
//                .write("comment", comment)
//                .writeEnd()
//                .close();
//
//        return sw.toString();
//
//    }
//    
//    public boolean hasSeat()
//    {
//        return filledSeatCount < initialSeatCount;
//    }
//
//}
