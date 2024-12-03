/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.util;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.util.VincentyGeodesy;
import edu.stanford.ppl.concurrent.SnapTreeMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author Joshua
 */
public class RouteInserter implements Runnable {

    private final Campus campus;
    private final String routeId;
    private final Map root, routes;

    public RouteInserter(Campus campus, String routeId, Map root, Map routes) {
        this.campus = campus;
        this.root = root;
        this.routes = routes;
        this.routeId = routeId;
    }

    @Override
    public void run() {
        Connection con = null;
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/Lift_Club");
            con = ds.getConnection();

            Map cellCache = new HashMap<String, Cell>(100, 1);// a hashmap of all cells encountered when creating the index, to allow quick access
            String sql = "SELECT route_driver_user_id, route_gps_points, "
                    + "route_price FROM public.route WHERE route_is_usable = 'true' AND route_id = ?::uuid ";

            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, routeId);
            ResultSet result = stmt.executeQuery();

            if (result.next()) {
                Route newRoute = new Route(routeId, result.getString(1), campus.getCampusId(), result.getDouble(3));
                //to do: calculate radial distance, generate sequential geohashes
                int order = 0;
                String path = result.getString(2);
                String[] gpsPoints = path.split(",");
                WGS84Point point = null;

                if (gpsPoints.length % 2 != 0) {
                    Logger.getLogger(RouteInserter.class.getName()).log(Level.WARNING, "{0} could not be indexed", routeId);
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

                                RoutePoints rp = cell.getRoutePoints(routeId);

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
                                fillAdjacents(cell);
                            }
                        }
                    }
                }

                newRoute.setRadialDistanceToCampus(VincentyGeodesy.distanceInMeters(campus.getLoc(), point));//calculate radial distance between campus and start of route
                routes.put(newRoute.getRouteId(), newRoute);
            }

            if (result.next()) {
                Logger.getLogger(RouteInserter.class.getName()).log(Level.SEVERE, "multiple routes with route_id {0}", routeId);
            }

            Logger.getLogger(RouteInserter.class.getName()).log(Level.INFO, root.toString());
        } catch (NamingException | SQLException | IllegalArgumentException ex) {
            Logger.getLogger(RouteInserter.class.getName()).log(Level.SEVERE, "RouteInserter: ",  ex);
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(RouteInserter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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

    private void fillAdjacents(Cell cell) {
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
}
