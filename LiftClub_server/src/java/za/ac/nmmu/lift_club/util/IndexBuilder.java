/*
 * should have stored ponts array as json array for convenience
 * 
 * 
 */
package za.ac.nmmu.lift_club.util;

import za.ac.nmmu.lift_club.ejb.RideHandler;
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
 * @author s210036575
 */
public class IndexBuilder implements Runnable {

    private final Campus campus;
    private final Map root, routes;
    private final RideHandler creator;

    public IndexBuilder(Campus campus, Map root, RideHandler creator, Map routes) {
        this.campus = campus;
        this.root = root;
        this.routes = routes;
        this.creator = creator;
    }

    @Override
    public void run() {
         Connection con = null;
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/Lift_Club");
            con = ds.getConnection();

            Map cellCache = new HashMap<String, Cell>(1000, 1);// a hashmap of all cells encountered when creating the index, to allow quick access
            String sql = "SELECT route_id, route_driver_user_id, route_gps_points, "
                    + "route_price FROM public.route WHERE route_is_usable = 'true' AND route_campus_id = ?::uuid ";

            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, campus.getCampusId());
            ResultSet result = stmt.executeQuery();

            while (result.next()) {
                Resource.log("one route");
                Route newRoute = new Route(result.getObject(1).toString(), result.getString(2), campus.getCampusId(), result.getDouble(4));

                int order = 0;
                String path = result.getString(3);
                String[] gpsPoints = path.split(",");
                WGS84Point point = null;

                if (gpsPoints.length % 2 != 0) {
                    Logger.getLogger(IndexBuilder.class.getName()).log(Level.WARNING, "{0} could not be indexed", result.getObject(1).toString());
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

                                RoutePoints rp = cell.getRoutePoints(result.getObject(1).toString());

                                if (rp == null) {
                                    rp = new RoutePoints(newRoute, cell);
                                    rp.setOrder(order);
                                    order++;
                                    cell.addRoutePoints(rp);
                                    newRoute.addRoutePoints(rp);
                                }

                                rp.addPoint(point);
                            } else {
                                Map level9 = null;
                                if (root.containsKey(geoHash.substring(0, 6))) {
                                    Map level8 = (Map) root.get(geoHash.substring(0, 6));

                                    if (level8.containsKey(geoHash.substring(6, 8))) {
                                        level9 = (Map) level8.get(geoHash.substring(6, 8));
                                    } else {
                                        level9 = new ConcurrentHashMap(8, 1);
                                        level8.put(geoHash.substring(6, 8), level9);
                                    }
                                } else {
                                    SnapTreeMap level8 = new SnapTreeMap(new StringComparator());
                                    root.put(geoHash.substring(0, 6), level8);
                                    level9 = new ConcurrentHashMap(8, 1);
                                    level8.put(geoHash.substring(6, 8), level9);
                                }
                                cell = new Cell(geoHash);
                                level9.put(geoHash.charAt(8), cell);
                                RoutePoints rp = new RoutePoints(newRoute, cell);
                                rp.addPoint(point);
                                rp.setOrder(order);
                                order++;
                                cell.addRoutePoints(rp);
                                newRoute.addRoutePoints(rp);
                                cellCache.put(geoHash, cell);
                                fillAdjacents(cell, cellCache);
                            }
                        }
                    }
                }
                
                newRoute.setRadialDistanceToCampus(VincentyGeodesy.distanceInMeters(campus.getLoc(), point));//calculate radial distance between campus and start of route
                routes.put(newRoute.getRouteId(), newRoute);
            }
            
            Logger.getLogger(IndexBuilder.class.getName()).log(Level.INFO, root.toString());
        } catch (NamingException | SQLException | IllegalArgumentException ex) {
            Logger.getLogger(IndexBuilder.class.getName()).log(Level.SEVERE, "IndexBuilder: ", ex);
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                 Logger.getLogger(IndexBuilder.class.getName()).log(Level.SEVERE, "IndexBuilder: ", ex);
            }
            creator.threadDone();
        }
    }

    private void fillAdjacents(Cell cell, Map cells) {

        if (cell.adjacentsFilled()) {
            return;
        }

        GeoHash gh = GeoHash.fromGeohashString(cell.getGeoHash());

        GeoHash[] adjacents = gh.getAdjacent();

        for (int i = 0; i < adjacents.length; i++) {
            String curr = adjacents[i].toBase32();

            Cell otherCell = (Cell) cells.get(curr);
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
