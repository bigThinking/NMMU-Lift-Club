/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routeinsertertest;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ac.za.nmmu.lift_club.util.Cell;
import ac.za.nmmu.lift_club.util.Match;
import ac.za.nmmu.lift_club.util.RideQuery;
import ac.za.nmmu.lift_club.util.Route;
import ac.za.nmmu.lift_club.util.RoutePoints;
import ac.za.nmmu.lift_club.util.StringComparator;
import ghggvg.vgvg.SnapTreeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.NamingException;

/**
 *
 * @author Joshua 187
 */
public class RouteInserterTest {

    SnapTreeMap root = new SnapTreeMap(new StringComparator());
    static boolean toCampus = true;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        GeoHash  g = GeoHash.withCharacterPrecision(-34.00048809,25.66962959, 9);
        System.out.println(g.toBase32());
        
//        ArrayList<String> strings = new ArrayList<>();

//        strings.add("a");
//        strings.add("b");
//        strings.add("c");
//        strings.add("d");
//        strings.add("e");
//        strings.add("f");
//        strings.add("g");
//        strings.add("h");
//        System.out.println(strings.toString());
//        String[] arr = returnArray(strings, new String[1]);
//        // System.out.println(arr.toString());
//        for (String s : arr) {
//            System.out.println(s);
//        }
//        RouteInserterTest rt = null;
//        String input = "";
//        try {
//            input = readFile();
//           rt = new RouteInserterTest(input);
//        } catch (FileNotFoundException ex) {
//            ex.printStackTrace();
//        }
//        
//        WGS84Point point = new WGS84Point(-33.963943,25.625653);
//        Cell cell = rt.getCell(GeoHash.geoHashStringWithCharacterPrecision(point.getLatitude(), point.getLongitude(), 9));
    }

    public static <T> T[] returnArray(ArrayList<T> objects, T[] array) {
        array = objects.toArray(array);
        if (toCampus)//reverse points, so that points go from campus to begin location
        {
            T[] obj1 = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);
            int j = 0;
            for (int i = array.length - 1; i >= 0; i--) {
                obj1[j++] = array[i];
            }
            return obj1;
        } else {
            return array;
        }
    }

    public RouteInserterTest(String input) {
        test(input);
    }

    private void test(String input) {
        Map routes = new HashMap();

        Map cellCache = new HashMap<String, Cell>(100, 1);// a hashmap of all cells encountered when creating the index, to allow quick access

        Route newRoute = new Route("greenacres", "me", "you", 0.0);
        //to do: calculate radial distance, generate sequential geohashes
        int order = 0;
        String path = input;
        String[] gpsPoints = path.split(",");
        WGS84Point point = null;

        if (gpsPoints.length % 2 != 0) {
            System.exit(0);
        } else {

            for (int i = 0; i < gpsPoints.length; i++) {
                gpsPoints[i] = gpsPoints[i].replace("[", "");
                gpsPoints[i] = gpsPoints[i].replace("]", "");
                gpsPoints[i] = gpsPoints[i].replace("(", "");
                gpsPoints[i] = gpsPoints[i].replace(")", "");

                if ((i + 1) % 2 == 0) {
                    point = new WGS84Point(Double.valueOf(gpsPoints[i - 1]), Double.valueOf(gpsPoints[i]));
                    String geoHash = GeoHash.geoHashStringWithCharacterPrecision(point.getLatitude(), point.getLongitude(), 9);

                    Cell cell = null;
                    if (cellCache.containsKey(geoHash)) {
                        cell = (Cell) cellCache.get(geoHash);

                        RoutePoints rp = cell.getRoutePoints("greenacres");

                        if (rp == null) {
                            rp = new RoutePoints(newRoute, cell);
                            rp.setOrder(order);
                            order++;
                            cell.addRoutePoints(rp);
                            newRoute.addRoutePoints(rp);
                        }
                        rp.addPoint(point);
                    } else {
                        if (root.containsKey(geoHash.substring(0, 6))) {
                            Map level8 = (Map) root.get(geoHash.substring(0, 6));

                            if (level8.containsKey(geoHash.substring(6, 8))) {
                                Map level9 = (Map) level8.get(geoHash.substring(6, 8));
                                if (level9.containsKey(geoHash.charAt(8))) {
                                    cell = (Cell) level9.get(geoHash.charAt(8));
                                } else {
                                    cell = new Cell(geoHash);
                                    level9.put(geoHash.charAt(8), cell);
                                }
                            } else {
                                ConcurrentHashMap level9 = new ConcurrentHashMap(8, 1);
                                level8.put(geoHash.substring(6, 8), level9);
                                cell = new Cell(geoHash);
                                level9.put(geoHash.charAt(8), cell);
                            }
                        } else {
                            SnapTreeMap level8 = new SnapTreeMap(new StringComparator());
                            root.put(geoHash.substring(0, 6), level8);
                            ConcurrentHashMap level9 = new ConcurrentHashMap(8, 1);
                            level8.put(geoHash.substring(6, 8), level9);
                            cell = new Cell(geoHash);
                            level9.put(geoHash.charAt(8), cell);
                        }
                        RoutePoints rp = new RoutePoints(newRoute, cell);
                        rp.addPoint(point);
                        rp.setOrder(order);
                        order++;
                        cell.addRoutePoints(rp);
                        newRoute.addRoutePoints(rp);
                        cellCache.put(geoHash, cell);

                        if (i == 305) {
                            fillAdjacents(cell);
                        } else {
                            fillAdjacents(cell);
                        }
                    }
                }
            }
        }

        //newRoute.setRadialDistanceToCampus(VincentyGeodesy.distanceInMeters(campus.getLoc(), point));//calculate radial distance between campus and start of route
        routes.put(newRoute.getRouteId(), newRoute);
        System.out.println(root.toString());
    }

    private Cell getCell(String geoHash) {
//        if (root.containsKey(geoHash.substring(0, 6))) {
//            Map level8 = (Map) root.get(geoHash.substring(0, 6));
//            if (level8.containsKey(geoHash.substring(6, 8))) {
//                Map level9 = (Map) level8.get(geoHash.substring(6, 8));
//                if (level9.containsKey(geoHash.charAt(8))) {
//                    Cell cell = (Cell) level9.get(geoHash.charAt(8));
//                    return cell;
//                }
//            }
//        }
        Map level8;
        if ((level8 = (Map) root.get(geoHash.substring(0, 6))) != null) {
            Map level9;
            if ((level9 = (Map) level8.get(geoHash.substring(6, 8))) != null) {
                Cell cell = (Cell) level9.get(geoHash.charAt(8));
                return cell;
            }
        }
        return null;
    }

    private void fillAdjacents(Cell cell) {//link adjacent cells to each other
        if (cell.adjacentsFilled()) {
            return;
        }

        GeoHash gh = GeoHash.fromGeohashString(cell.getGeoHash());
        GeoHash[] adjacents = gh.getAdjacent();

        for (int i = 0; i < adjacents.length; i++) {
            String curr = adjacents[i].toBase32();

            Cell otherCell = getCell(curr);
            if (otherCell != null) {
                switch (i) {
                    case 0: {

                        cell.setNorth(otherCell);
                        otherCell.setSouth(cell);

                        break;
                    }
                    case 1: {

                        cell.setNorthEast(otherCell);
                        otherCell.setSouthWest(cell);

                        break;
                    }
                    case 2: {

                        cell.setEast(otherCell);
                        otherCell.setWest(cell);

                        break;
                    }
                    case 3: {

                        cell.setSouthEast(otherCell);
                        otherCell.setNorthWest(cell);

                        break;
                    }
                    case 4: {

                        cell.setSouth(otherCell);
                        otherCell.setNorth(cell);

                        break;
                    }
                    case 5: {

                        cell.setSouthWest(otherCell);
                        otherCell.setNorthEast(cell);

                        break;
                    }
                    case 6: {

                        cell.setWest(otherCell);
                        otherCell.setEast(cell);

                        break;
                    }
                    case 7: {

                        cell.setNorthEast(otherCell);
                        otherCell.setSouthWest(cell);

                        break;
                    }
                }
            }
        }

    }

    private static String readFile() throws FileNotFoundException {
        String line = "";
        try {
            Scanner s = new Scanner(new File("greenacres.txt"));

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean moreThan1 = false;
            while (s.hasNext()) {
                line = s.nextLine();

                if (line.compareTo("") == 0) {
                    break;
                }

                if (moreThan1) {
                    sb.append(",");
                }

                moreThan1 = true;
                String[] values = line.split(",");
                sb.append("(");
                sb.append(values[0]);
                sb.append(",");
                sb.append(values[1]);
                sb.append(")");
            }
            sb.append("]");
            return sb.toString();
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println(line);
            ex.printStackTrace();
            System.exit(0);
        }

        return line;
    }
//
//    public Cell[] findRides(WGS84Point point) {
//        Cell cell = getCell(GeoHash.geoHashStringWithCharacterPrecision(point.getLatitude(), point.getLongitude(), 8), null);// get cell with routes
//        Map map = new HashMap(1);//create Hashmap to be returned
//        try {
//            if (cell != null) {
//                map = findRides(cell, point, new HashMap(5), (byte) 0);
//
//            } else {//if no cell found for the points
//                GeoHash[] adjacents = GeoHash.withCharacterPrecision(point.getLatitude(), point.getLongitude(), 8).getAdjacent();//get all cells adjacent to the given cell
//                for (GeoHash adjacent : adjacents) {//for each adjacent cell,get the cell, and find rides
//                    cell = getCell(adjacent.toBase32(), null);
//                    if (cell != null) {
//                        Map tempMap = findRides(cell, point, new HashMap(5), (byte) 0);//call findrides on adjacent cell and sssign result to temp map
//
//                        if (!tempMap.isEmpty()) {
//                            Iterator i = tempMap.keySet().iterator();
//
//                            if (i.hasNext()) {
//                                String key = (String) i.next();
//                                if (!map.containsKey(key)) {// if map does not already contain the ride from tempMap, put it in map
//                                    map.put(key, tempMap.get(key));
//                                }
//                            }
//                        }
//
//                    }
//                }
//            }
//        } catch (NamingException ex) {
//        }
//
//        return (Cell[]) map.entrySet().toArray(new Cell[map.size()]);
//    }
//
//    private Map findRides(Cell cell, WGS84Point query, HashMap map, byte recCount) throws NamingException {
//        Iterator cellRides;
//        if (query.isNow()) {//if is now ride
//            cellRides = cell.getNowRidesIterator();
//            while (cellRides.hasNext()) {
//                if (!map.containsKey(cell.getGeoHash())) {//check driver and passenger position and if ride not already in map, put it in map
//
//                }
//            }
//
//        } else {
//            cellRides = cell.getLaterRidesIterator();
//            while (cellRides.hasNext()) {
//                if (map.containsKey(ride.getRideId()) == false) {
//                    Match res = new Match(query);
//                    res.setRidePoint(cell.getGeoHash());
//                    res.setRide(ride);
//                    res.setPrice(calculatePrice(cell, ride));
//                    map.put(ride.getRideId(), res);
//                }
//            }
//        }
//    
//
//        if (recCount < 2) {// search adjacent cells for rides
//            recCount++;
//            findRides(cell.getEast(), query, map, recCount);
//            findRides(cell.getNorth(), query, map, recCount);
//            findRides(cell.getNorthEast(), query, map, recCount);
//            findRides(cell.getNorthWest(), query, map, recCount);
//            findRides(cell.getSouth(), query, map, recCount);
//            findRides(cell.getSouthEast(), query, map, recCount);
//            findRides(cell.getSouthWest(), query, map, recCount);
//            findRides(cell.getWest(), query, map, recCount);
//        }
//        return map;
//    }

//    private static Cell getCell(String geoHash, String campusId) {
//    
//        
//
//            Map level8 = (Map) root.get(geoHash.substring(0, 6));
//            if (level8 != null) {
//                Map level9 = (Map) level8.get(geoHash.substring(6, 8));
//                if (level9 != null) {
//                    Cell cell = (Cell) level9.get(geoHash.charAt(8));
//                    return cell;
//                }
//            }
//      
//
//        return null;
//    }
}
