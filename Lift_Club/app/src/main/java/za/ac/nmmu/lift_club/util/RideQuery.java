/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.util;

import ch.hsr.geohash.GeoHash;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;

/**
 *
 * @author Joshua
 */
public class RideQuery {

    protected String geohash = "";//pickup or dropoff point based on value of isToCampus
    protected String userId = "", id = "";
    protected double lat, lon;
    protected boolean isToCampus, isNow, needBoot;
    protected String campusId = "";
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

    public RideQuery(String jsonString) throws BadMessageException {
        JsonParser parser = Json.createParser(new StringReader(jsonString));
        JsonParser.Event event = null;

        while (parser.hasNext()) {
            event = parser.next();

            if (event == JsonParser.Event.KEY_NAME) {
                switch (parser.getString()) {
                    case "Id":{
                        parser.next();
                        id = parser.getString();
                        break;
                    }
                    case "userId": {
                        parser.next();
                        userId = parser.getString();
                        break;
                    }
                    case "lat": {
                        parser.next();
                        lat = Double.valueOf(parser.getString());
                        break;
                    }
                    case "lon": {
                        parser.next();
                        lon = Double.valueOf(parser.getString());
                        break;
                    }
                    case "isToCampus": {
                        event = parser.next();
                        isToCampus = event == JsonParser.Event.VALUE_TRUE ? true : false;
                        break;
                    }
                    case "campusId": {
                        parser.next();
                        campusId = parser.getString();
                        break;
                    }
                    case "isNow": {
                        event = parser.next();
                        isNow = event == JsonParser.Event.VALUE_TRUE ? true : false;
                        break;
                    }
                    case "needBoot": {
                        event = parser.next();
                        needBoot = event == JsonParser.Event.VALUE_TRUE ? true : false;
                        break;
                    }
                    case "dateTime": {
                            parser.next();
                            rideDateTime = new Date(parser.getLong());
                            break;
                    }
                }
            }
        }

        if (userId == null || campusId == null) {
            throw new BadMessageException();
        }

        if (!isNow && rideDateTime == null) {
            throw new BadMessageException();
        }

        geohash = GeoHash.geoHashStringWithCharacterPrecision(lat, lon, 9);
    }

    public void setLocation(double lat, double lon)
    {
        this.lat = lat;
        this.lon = lon;
        geohash = GeoHash.geoHashStringWithCharacterPrecision(lat, lon, 9);
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

    public synchronized void setIsNow(boolean isNow) { this.isNow = isNow; }

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

    public String getJson() {
        StringWriter sw = new StringWriter();
        JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
        JsonGenerator g = factory.createGenerator(sw);

        g.writeStartObject().write("Id", id)
                .write("userId", userId)
                .write("lat", String.valueOf(lat))
                .write("lon", String.valueOf(lon))
                .write("campusId", campusId)
                .write("isToCampus", isToCampus)
                .write("isNow", isNow)
                .write("needBoot", needBoot)
                .write("dateTime", rideDateTime.getTime())
                .writeEnd()
                .close();

        return sw.toString();
    }
}
