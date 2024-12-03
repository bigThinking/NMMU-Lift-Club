/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.ejb;

import za.ac.nmmu.lift_club.util.Campus;
import za.ac.nmmu.lift_club.util.IndexBuilder;
import za.ac.nmmu.lift_club.util.Cell;
import za.ac.nmmu.lift_club.util.ClientInfo;
import za.ac.nmmu.lift_club.util.Match;
import za.ac.nmmu.lift_club.util.MessageTypes;
import za.ac.nmmu.lift_club.util.Resource;
import za.ac.nmmu.lift_club.util.Route;
import za.ac.nmmu.lift_club.util.Ride;
import za.ac.nmmu.lift_club.util.RideQuery;
import za.ac.nmmu.lift_club.util.RouteInserter;
import za.ac.nmmu.lift_club.util.RoutePoints;
import za.ac.nmmu.lift_club.util.StringComparator;
import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.util.VincentyGeodesy;
import edu.stanford.ppl.concurrent.SnapTreeMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.NamingException;

/**
 *
 * @author Joshua
 */
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
public class RideHandler implements RideHandlerLocal {

    private final ConcurrentHashMap<String, SnapTreeMap> trees = new ConcurrentHashMap<>(5, (float) 1.2); //holds a tree for each campus
    private final ConcurrentHashMap<String, Ride> rides = new ConcurrentHashMap<>(100);//holds each ride, which is listed in the tree
    private final ConcurrentHashMap<String, Route> routes = new ConcurrentHashMap<>(100);//holds each route which is listed in a tree
    private final ConcurrentHashMap<String, RideQuery> unMatchedLaterRideQueries = new ConcurrentHashMap<>(20);//create a map for unmatched later queries and try to match it whenever a new later ride is added
    private final ConcurrentHashMap<String, Campus> campuses = new ConcurrentHashMap<>(5);
    private boolean ready = false;
    private byte indexBuilderCount = 0;
    private static final long MILLISECONDS_IN_AN_HOUR = 3600000;
    private static final byte MAX_REC_COUNT = 2;//maximum recursive call that can be made

    @PostConstruct
    private void buildIndex() {
        try {
            Campus[] campuses = getCampuses();

            for (Campus c : campuses) {
                SnapTreeMap campusIndex = new SnapTreeMap(new StringComparator());
                trees.put(c.getCampusId(), campusIndex);

                this.campuses.put(c.getCampusId(), c);
                Thread builder = new Thread(new IndexBuilder(c, campusIndex, this, routes));
                builder.setPriority(Thread.MAX_PRIORITY);
                builder.start();
                indexBuilderCount++;
            }
        } catch (Exception ex) {
            Logger.getLogger(RideHandler.class.getName()).log(Level.SEVERE, "Exception while building index: {0}", ex);
        }
    }

    @Override
    public Match[] findRides(RideQuery query) {
        Cell cell = getCell(query.getGeohash(), query.getCampusId());// get cell with routes
        Map map = new HashMap(1);//create Hashmap to be returned
        try {
            if (cell != null) {
                Resource.log(cell.getGeoHash() + ", " + cell.laterRides.toString() + ";  " + cell.nowRides.toString());
                map = findRides(cell, query, new HashMap(5), (byte) 0);
                if (!query.isNow()) {//if ride is a later ride put it in unMatchedLaterRides
                    unMatchedLaterRideQueries.put(query.getId(), query);
                    Resource.log("unmatchedlaterqueries: " + unMatchedLaterRideQueries.toString());
                }
            } else {//if no cell found for the points
                GeoHash[] adjacents = GeoHash.fromGeohashString(query.getGeohash()).getAdjacent();//get all cells adjacent to the given cell
                for (GeoHash adjacent : adjacents) {//for each adjacent cell,get the cell, and find rides
                    cell = getCell(adjacent.toBase32(), query.getCampusId());
                    
                    if (cell != null) {
                        Resource.log(cell.getGeoHash() + ", " + cell.laterRides.toString() + ";  " + cell.nowRides.toString());
                        Map tempMap = findRides(cell, query, new HashMap(5), (byte) 0);//call findrides on adjacent cell and sssign result to temp map

                        if (!tempMap.isEmpty()) {
                            Iterator i = tempMap.keySet().iterator();

                            if (i.hasNext()) {
                                String key = (String) i.next();
                                if (!map.containsKey(key)) {// if map does not already contain the ride from tempMap, put it in map
                                    map.put(key, tempMap.get(key));
                                }
                            }
                        }
                        if (!query.isNow()) {//if ride is a later ride put it in unMatchedLaterRides
                            unMatchedLaterRideQueries.put(query.getId(), query);
                            Resource.log("unmatchedlaterqueries: " + unMatchedLaterRideQueries.toString());
                        }
                    }
                }
            }
        } catch (NamingException ex) {
        }

        
        return (Match[]) map.values().toArray(new Match[map.size()]);
    }

    private Map findRides(Cell cell, RideQuery query, HashMap map, byte recCount) throws NamingException {
        if(cell != null){
        Iterator<Map.Entry<String, Ride>> cellRides;
        if (query.isNow()) {//if is now ride
            cellRides = cell.getNowRidesIterator();
            Resource.log("isNow");
            while (cellRides.hasNext()) {
                Ride ride = (Ride) cellRides.next().getValue();
            //    if (ride.getOnlyGender() == (query.isUserFemale() ? Ride.FEMALE : (Ride.MALE))) {//consider onlygender requirement
                    if (ride.isToCampus() == query.isToCampus()) {//is ride to campus
                        if (!map.containsKey(ride.getRideId())) {//check driver and passenger position and if ride not already in map, put it in map

                            try {//check driver and passenger position
                                ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                                ClientInfo ci = ch.getUserInfoById(ride.getDriverId());//get driver info

                                if (ci != null && ci.isOnRoute()) {
                                    String driverGeoHash = GeoHash.geoHashStringWithCharacterPrecision(ci.getLat(), ci.getLon(), 8);
                                    Iterator<RoutePoints> i = ride.getRoute().getRoutePointsIterator();
                                    RoutePoints rp;
                                    while (i.hasNext()) {
                                        rp = (RoutePoints) i.next();
                                        //if driver position encountered first, then driver is past passenger(remember points recorded in order away from campus)
                                        if (rp.getCell().getGeoHash().substring(0, 7).compareTo(driverGeoHash) == 0) {
                                            break;
                                        } else if (rp.getCell().getGeoHash().substring(0, 7).compareTo(query.getGeohash().substring(0, 7)) == 0) { //if passenger position encountered first
                                            Match res = new Match(query);
                                            res.setCurrentDriverLocation(driverGeoHash);
                                            res.setRidePoint(cell.getGeoHash());
                                            res.setRide(ride);
                                            res.setPrice(calculatePrice(cell, ride));
                                            map.put(ride.getRideId(), res);
                                            break;
                                        }
                                    }
                                }
                            } catch (NamingException ex) {
                                throw ex;
                            }
                        }
                    }

                //}
            }
        } else {
            Resource.log("isLater");
            cellRides = cell.getLaterRidesIterator();
            while (cellRides.hasNext()) {
                Resource.log("hasRides");
                Ride ride = cellRides.next().getValue();
          //      if (ride.getOnlyGender() == (query.isUserFemale() ? Ride.FEMALE : Ride.MALE)) {//consider onlygender requirement
                    if (ride.isToCampus() == query.isToCampus()) {
                        Resource.log("istocampus same");
                        if ((ride.getRideDateTime().getTime() - query.getRideDateTime().getTime()) <= MILLISECONDS_IN_AN_HOUR) {
                            Resource.log("ridedatetime difference less than an hour");
                            if (map.containsKey(ride.getRideId()) == false) {
                                Match res = new Match(query);
                                res.setRidePoint(cell.getGeoHash());
                                res.setRide(ride);
                                res.setPrice(calculatePrice(cell, ride));
                                map.put(ride.getRideId(), res);
                            }
                        }
                    }
               // }
            }
        }

        if (recCount < MAX_REC_COUNT) {// search adjacent cells for rides
            recCount++;
            findRides(cell.getEast(), query, map, recCount);
            findRides(cell.getNorth(), query, map, recCount);
            findRides(cell.getNorthEast(), query, map, recCount);
            findRides(cell.getNorthWest(), query, map, recCount);
            findRides(cell.getSouth(), query, map, recCount);
            findRides(cell.getSouthEast(), query, map, recCount);
            findRides(cell.getSouthWest(), query, map, recCount);
            findRides(cell.getWest(), query, map, recCount);
        }
        }
        
        Resource.log(map.toString());
        return map;
    }

    private Cell getCell(String geoHash, String campusId) {
        Map level6;
        if ((level6 = trees.get(campusId)) != null) {

            Map level8;
            if ((level8 = (Map) level6.get(geoHash.substring(0, 6))) != null) {
                Map level9;
                if ((level9 = (Map) level8.get(geoHash.substring(6, 8))) != null) {
                    Cell cell = (Cell) level9.get(geoHash.charAt(8));
                    return cell;
                }
            }
        }

        return null;
    }

    @Override
    public Ride getRide(String rideId) {
        if (ready) {
            return rides.get(rideId);
        }
        return null;
    }

    @Override
    public boolean removeRide(String rideId) {
        if (ready) {
            Resource.log("rh - rides before remove" + rides.toString());
            final Ride ride = rides.get(rideId);
            Route route = ride.getRoute();
            route.removeRide(ride.getRideId());
            rides.remove(rideId);
            Resource.log("rh - ride removed " + rides.toString());
            //remove previously matched rides and inform passengers about canccellation
            if (ride.getFilledSeatCount() != 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();

                            RideQuery[] passengers = ride.getPassengers();

                            StringBuilder sb = new StringBuilder();
                            sb.append(MessageTypes.RIDECANCELLED);
                            sb.append(MessageTypes.DELIMITER);
                            sb.append("{\"rideId\":\"");
                            sb.append(ride.getRideId());
                            sb.append("\"");

                            Resource.log("rh - cancellation message " + sb.toString());
                            for (int i = 0; i < ride.getFilledSeatCount(); i++) {
                                if (ride.isNow() == false)//make matched later rides unmatched,
                                {
                                    unMatchedLaterRideQueries.put(passengers[i].getId(), passengers[i]);
                                }

                                ch.sendMessage(passengers[i].getUserId(), sb.toString());//inform passengers
                            }
                        } catch (NamingException ex) {
                            Logger.getLogger(RideHandler.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                        }
                    }
                }).start();
            }
        }

        return ready;
    }

    @Override
    public boolean addRide(final Ride ride) {
        Resource.log("rh ready - " + String.valueOf(ready));
        if (ready) {
            //add a ride to a route
            Resource.log("rh - rides before " + rides.toString());
            Route route = routes.get(ride.getRouteId());
            route.addRide(ride);
            ride.setRoute(route);
            rides.put(ride.getRideId(), ride);
            Resource.log("rh - ride added " + rides.toString());
            try {
                ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                ch.addListedRide(ride.getDriverId(), ride);
            } catch (NamingException ex) {
                Logger.getLogger(RideHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

            //match later ride
            if (ride.isNow() == false) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();

                            Iterator<Map.Entry<String, RideQuery>> i = unMatchedLaterRideQueries.entrySet().iterator();
                            while (i.hasNext()) {
                                RideQuery query = (RideQuery) i.next().getValue();
                                Cell cell = getCell(query.getGeohash(), query.getCampusId());
                                Match res = matchRide(cell, query, (byte) 0);

                                ClientInfo ci = ch.getUserInfoById(query.getUserId());

                                if (res != null && unMatchedLaterRideQueries.containsKey(query.getId()))//check that query is unmatched before sending ridematch because of long running proccess
                                {
                                    ch.addMatch(ci.getAuthToken(), res);//store match in clientinfo 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(MessageTypes.RIDEMATCH);
                                    sb.append(MessageTypes.DELIMITER);
                                    sb.append("{\"matches\" : [");
                                    sb.append(res.getJson());
                                    sb.append("]}");
                                    ch.sendMessage(query.getUserId(), sb.toString());//inform passenger, 
                                }
                            }
                        } catch (NamingException ex) {
                        }
                    }

                    private Match matchRide(Cell cell, RideQuery query, byte recCount) {
                        if (cell != null && cell.LaterRidesContains(ride.getRideId())) {//check cell info
                           // if (ride.getOnlyGender() == (query.isUserFemale() ? Ride.FEMALE : Ride.MALE)) {//consider onlygender requirement
                                if (ride.isToCampus() == query.isToCampus()) {
                                    if ((ride.getRideDateTime().getTime() - query.getRideDateTime().getTime()) <= MILLISECONDS_IN_AN_HOUR) {
                                        Match res = new Match(query);
                                        res.setRidePoint(cell.getGeoHash());
                                        res.setRide(ride);
                                        res.setPrice(calculatePrice(cell, ride));
                                        return res;
                                    }
                                }
                           // }
                        } else {//check adjacent cells
                            if (recCount < MAX_REC_COUNT) {// search adjacent cells for rides
                                recCount++;
                                GeoHash g = GeoHash.fromGeohashString(query.getGeohash());
                                GeoHash[] adj = g.getAdjacent();
                                Cell[] cells = {getCell(adj[0].toBase32(), query.getCampusId()), getCell(adj[1].toBase32(), query.getCampusId()), getCell(adj[2].toBase32(), query.getCampusId()), getCell(adj[3].toBase32(), query.getCampusId()), getCell(adj[4].toBase32(), query.getCampusId()),
                                    getCell(adj[5].toBase32(), query.getCampusId()), getCell(adj[6].toBase32(), query.getCampusId()), getCell(adj[7].toBase32(), query.getCampusId())};

                                for (int i = 0; i < 8; i++) {
                                    Match res;
                                    if ((res = matchRide(cells[i], query, recCount)) != null) {
                                        return res;
                                    }
                                }
                            }
                        }
                        return null;
                    }
                }).start();

            }

//            else {
//                recordRide(ride);
//            }
        }

        return ready;
    }

    public synchronized void threadDone() {
        indexBuilderCount--;

        if (indexBuilderCount == 0) {
            ready = true;
        }
    }

    @Override
    public boolean addRoute(String routeId, String campusId) {
        if (ready) {
            if (!routes.containsKey(routeId)) {
                new Thread(new RouteInserter(campuses.get(campusId), routeId, trees.get(campusId), routes)).start();
            }
        }
        return ready;
    }

    @Override
    public void removeRoute(String routeId) {
        Route route = routes.remove(routeId);

        if (route != null) {
            Set<String> rideKeys = route.getRideKeySet();
            Iterator<RoutePoints> i = route.getRoutePointsIterator();

            while (i.hasNext()) {
                RoutePoints rp = i.next();
                rp.getCell().removeRides(rideKeys.iterator());
                rp.getCell().removeRoutePoints(routeId);
            }

            Iterator<String> j = rideKeys.iterator();

            while (j.hasNext()) {
                String key = j.next();
                rides.remove(key);
            }
        }
    }

    @Override
    public void activateRide(String rideId) {
        Ride ride = rides.get(rideId);

        if (ride != null) {
            try {
                Route route = ride.getRoute();
                Iterator<RoutePoints> i = route.getRoutePointsIterator();

                while (i.hasNext()) {
                    RoutePoints rp = i.next();
                    rp.getCell().activateRide(rideId);
                }

                ride.setRideDateTimeBegin(new Date());
                Resource.log("ride activated:" + rideId);
                ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();

                if (ride.getPassengers() != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(MessageTypes.RIDEBEGUN);
                    sb.append(MessageTypes.DELIMITER);
                    sb.append("{\"rideId\":\"");
                    sb.append(ride.getRideId());
                    Date now = new Date();
                    sb.append("\",\"dateTime\":");
                    sb.append(String.valueOf(now.getTime()));
                    sb.append("}");
                    
                    Resource.log("Activating ride activationMessage:" + sb.toString());
                    Match[] matches = ride.getPassengers();
                    for(byte j = 0; j < ride.getFilledSeatCount(); j++){
                        Match m = matches[j];                  
                        ch.sendMessage(m.getUserId(), sb.toString());
                    }
                    
                }
//                
//                if(!ride.isToCampus())
//                recordRide(ride);
            } catch (NamingException ex) {
                Logger.getLogger(RideHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public String selectNowRide(Match match) {
        String msg = "";

        Ride ride = rides.get(match.getRide().getRideId());
        if (ride != null) {
            try {
                ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();

                ClientInfo ci = ch.getUserInfoById(ride.getDriverId());

                if (ci != null && ci.isOnRoute()) {
                    String driverGeoHash = GeoHash.geoHashStringWithCharacterPrecision(ci.getLat(), ci.getLon(), 8);
                    Iterator<RoutePoints> i = ride.getRoute().getRoutePointsIterator();

                    RoutePoints rp;
                    while (i.hasNext()) {
                        rp = i.next();

                        if (rp.getCell().getGeoHash().substring(0, 7).compareTo(driverGeoHash) == 0) { //if driver position encountered first
                            return "false:driver not within range";
                        } else if (rp.getCell().getGeoHash().substring(0, 7).compareTo(match.getGeohash().substring(0, 7)) == 0) { //if passenger position encountered first
                            ch.addMatch(ci.getAuthToken(), match);
                            StringBuilder sb = new StringBuilder();
                            sb.append(MessageTypes.PASSENGERAHEAD);
                            sb.append(MessageTypes.DELIMITER);
                            sb.append("{\"match\":");
                            sb.append(match.getJson());
                            sb.append("}");
                            ch.sendMessage(ride.getDriverId(), sb.toString());//send message to driver
                            ride.addPassenger(match);
                            return "true";
                        }
                    }
                } else {
                    return "false:Could not contact driver:" + match.getId();
                }

            } catch (Exception ex) {
                return "false:internal error:" + match.getId();
            }
        }
        return "false:Ride does not exist:" + match.getId();
    }

    @Override
    public String selectLaterRide(Match match) {
        String msg = "";

        Ride ride = rides.get(match.getRide().getRideId());
        if (ride != null) {
            try {
                ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();

                ClientInfo ci = ch.getUserInfoById(ride.getDriverId());

                if (ci != null) {
                    ch.addMatch(ci.getAuthToken(), match);
                    unMatchedLaterRideQueries.remove(match.getId());
                    StringBuilder sb = new StringBuilder();
                    sb.append(MessageTypes.LATERPASSENGER);
                    sb.append(MessageTypes.DELIMITER);
                    sb.append("{\"match\":");
                    sb.append(match.getJson());
                    sb.append("}");
                    ch.sendMessage(ride.getDriverId(), sb.toString());//send message to driver
                    ride.addPassenger(match);
                    return "true";
                } else {
                    return "false:Could not contact driver";
                }

            } catch (Exception ex) {
                if (ex.getClass() != NamingException.class) {
                    Logger.getLogger(RideHandler.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                }
                return "false:internal error";
            }
        }
        return "false:Ride does not exist";
    }

    private double calculatePrice(Cell cell, Ride ride) {

        GeoHash gh = GeoHash.fromGeohashString(cell.getGeoHash());
        WGS84Point center = gh.getBoundingBoxCenterPoint();
        Campus c = campuses.get(ride.getCampusId());
        double distance = VincentyGeodesy.distanceInMeters(center, c.getLoc());
        return (distance / ride.getRoute().getRadialDistanceToCampus()) * ride.getPrice();
    }

    private Campus[] getCampuses() throws Exception {
        String sql = "SELECT campus_id, campus_gps_point FROM public.campus";
        String sql1 = "SELECT entry_point_id, entry_point_gps_point FROM public.entry_point WHERE entry_point_campus_id = ?::uuid";

        ArrayList<Campus> campuses = new ArrayList<>();
        try {
            Connection con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            ResultSet result = stmt.executeQuery();

            PreparedStatement stmt2 = con.prepareStatement(sql1);

            while (result.next()) {
                String[] point = result.getObject(2).toString().split(",");
                double lat = Double.valueOf(point[0].replace("(", ""));
                double lon = Double.valueOf(point[1].replace(")", ""));
                Campus c = new Campus(result.getObject(1).toString(), lat, lon);
                stmt2.setString(1, result.getObject(1).toString());
                ResultSet result2 = stmt2.executeQuery();
                while (result2.next()) {
                    point = result2.getObject(2).toString().split(",");
                    lat = Double.valueOf(point[0].replace("(", ""));
                    lon = Double.valueOf(point[1].replace(")", ""));
                    c.addEntryPoint(result2.getObject(1).toString(), lat, lon);
                }
                campuses.add(c);
            }

            Resource.log("Campuses : " + String.valueOf(campuses.size()));
            Campus[] campusArray = new Campus[campuses.size()];
            return campuses.toArray(campusArray);
        } catch (NamingException | SQLException | NumberFormatException ex) {
            Logger.getLogger(RideHandler.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    @Override
    public void passengerAcknowledgePickup(String rideId, Match match) {
        Ride r = rides.get(rideId);

        if (r != null) {
            Match[] passengers = r.getPassengers();
            boolean found = false;
            for (Match m : passengers) {
                if (m.getId().compareTo(match.getId()) == 0) {
                    m.setPassengerAcked(true);

                    if (!m.isDriverAcked()) {
                        m.setRideDateTimePickedUp(new Date());
                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                r.addPassenger(match);
                match.setPassengerAcked(true);
                match.setRideDateTimePickedUp(new Date());
            }
        }
        //    }
    }

    @Override
    public void driverAcknowledgePickup(String rideId, Match match) {
        Ride r = rides.get(rideId);

        if (r != null) {
            Match[] passengers = r.getPassengers();

            boolean found = false;
            for (Match m : passengers) {
                if (m.getId().compareTo(match.getId()) == 0) {
                    m.setDriverAcked(true);
                    if (!m.isPassengerAcked()) {
                        m.setRideDateTimePickedUp(new Date());
                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                r.addPassenger(match);
                match.setDriverAcked(true);
                match.setRideDateTimePickedUp(new Date());
            }

        }
    }

    @Override
    public void rideDone(String rideId, String qryId) {
        Ride r = rides.get(rideId);

        if (r != null) {
            Match[] passengers = r.getPassengers();

            if (!r.isDbRecorded()) {
                recordRide(r);
            }

            if (r.isDbRecorded()) {
                for (int i = 0; i < passengers.length; i++) {
                    try {
                        if (passengers[i].getId().equals(qryId)) {
                            Connection con = Resource.getDBConnection();
                            String sql = "INSERT INTO public.ride_passenger(rp_ride_id, rp_user_id, rp_datetime_picked_up, rp_datetime_dropped_off, rp_point_picked_up, rp_point_dropped_off point, rp_successful) VALUES(?::uuid,?,?, now(),?::point,?::point,?)";
                            PreparedStatement stmt = con.prepareStatement(sql);
                            stmt.setString(1, rideId);
                            stmt.setString(2, passengers[i].getUserId());
                            stmt.setTimestamp(3, new Timestamp(passengers[i].getRideDateTimePickedUp().getTime()));

                            if (r.isToCampus()) {
                                stmt.setString(4, GeoHash.fromGeohashString(passengers[i].getRidePoint()).getPoint().toString());
                                stmt.setString(5, campuses.get(r.getCampusId()).getLoc().toString());
                            } else {
                                stmt.setString(5, GeoHash.fromGeohashString(passengers[i].getRidePoint()).getPoint().toString());
                                stmt.setString(4, campuses.get(r.getCampusId()).getLoc().toString());
                            }

                            if (passengers[i].isDriverAcked() && passengers[i].isPassengerAcked()) {//to do record in db
                                stmt.setBoolean(6, true);
                            } else {
                                stmt.setBoolean(6, false);
                            }

                            if (stmt.execute()) {
                                sql = "UPDATE public.ride SET ride_nr_passengers = ride_nr_passengers + 1 WHERE ride_id = ?::uuid";
                                stmt = con.prepareStatement(sql);
                                stmt.setString(1, r.getRideId());
                            }

                            if (stmt.execute()) {
                                sql = "UPDATE public.user SET user_nr_rides_taken = user_nr_rides_taken + 1 WHERE user_id = ?";
                                stmt = con.prepareStatement(sql);
                                stmt.setString(1, qryId);
                            }

                            r.removePassenger(qryId);

                            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                            ch.sendMessage(passengers[i].getUserId(), MessageTypes.ACKDROPOFF + MessageTypes.DELIMITER + rideId);
                            break;
                        }
                    } catch (NamingException | SQLException ex) {
                        Logger.getLogger(RideHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    //event class to allow storing of current activities
    @Override
    public String hello() {
        return "hello from rh";
    }

    private void recordRide(Ride ride) {
        try {
            Connection con = Resource.getDBConnection();
            String sql = "INSERT INTO public.ride(ride_driver_user_id, ride_datetime_begin, ride_is_to_campus, ride_commment, ride_price, ride_route_id, ride_only_gender, ride_car_vrn) VALUES(?,now(),?,?,?,?::uuid,?,"
                    + "SELECT driver_current_car_vrn FROM public.driver WHERE driver_user_id = ?)";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, ride.getDriverId());
            stmt.setBoolean(2, ride.isToCampus());
            stmt.setString(3, ride.getComment());
            stmt.setDouble(4, ride.getPrice());
            stmt.setString(5, ride.getRouteId());
            stmt.setString(6, ride.getOnlyGender() == Ride.MALE ? "m" : (ride.getOnlyGender() == Ride.FEMALE ? "f" : ""));
            stmt.setString(7, ride.getDriverId());

            if (stmt.execute()) {
                ride.setDbRecorded(true);
                sql = "UPDATE public.route SET route_no_times_used = route_no_times_used + 1 WHERE route_driver_user_id = ?";
                stmt = con.prepareStatement(sql);
                stmt.setString(1, ride.getRideId());
            }

            if (stmt.execute()) {
                sql = "UPDATE public.driver SET driver_nr_rides = driver_nr_rides + 1 WHERE ride_driver_user_id = ?";
                stmt = con.prepareStatement(sql);
                stmt.setString(1, ride.getDriverId());
                stmt.execute();
            }
        } catch (SQLException | NamingException ex) {
            Logger.getLogger(RideHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

//Cell[] cells = {cell.getEast(),cell.getNorth(),cell.getNorthEast(),cell.getNorthWest(),cell.getSouth(),
//                                cell.getSouthEast(),cell.getSouthWest(),cell.getWest()};
//                                matchRide(cell.getEast(), query, recCount);
//                                matchRide(cell.getNorth(), query, recCount);
//                                matchRide(cell.getNorthEast(), query, recCount);
//                                matchRide(cell.getNorthWest(), query, recCount);
//                                matchRide(cell.getSouth(), query,recCount);
//                                matchRide(cell.getSouthEast(), query, recCount);
//                                matchRide(cell.getSouthWest(), query,recCount);
//                                matchRide(cell.getWest(), query,recCount);
