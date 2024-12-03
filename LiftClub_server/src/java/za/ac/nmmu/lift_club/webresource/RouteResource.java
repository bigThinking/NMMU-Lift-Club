/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

import za.ac.nmmu.lift_club.ejb.ClientInfoHandlerLocal;
import za.ac.nmmu.lift_club.util.ClientInfo;
import za.ac.nmmu.lift_club.util.MessageTypes;
import za.ac.nmmu.lift_club.util.Resource;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.naming.NamingException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author Joshua
 */
@Path("route")
public class RouteResource {
    
    @Context
    private UriInfo context;
    
    @HeaderParam(AuthenticationFilter.TOKENHEADER)
    private String token;//use token in header to get clientinfo and authorise operation

    /**
     * Creates a new instance of Route
     */
    public RouteResource() {
    }

    /**
     * Retrieves representation of an instance of
     * ac.za.nmmu.matching.web.RouteResource
     *
     * @return an instance of java.lang.String
     */
    @GET
    @Produces({"application/json,text/plain"})
    public String getRoutes() throws SQLException, NamingException{
        Connection con = null;
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            
            String userId = ci.getUserId();
            
            String sql = "SELECT route_id, route_name, route_datetime_created, route_price, route_campus_id, route_geohashes FROM public.route WHERE route_driver_user_id = ? and route_is_usable = true";
            
            con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, userId);
            ResultSet result = stmt.executeQuery();
            
            StringWriter sw = new StringWriter();
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator g = factory.createGenerator(sw);
            g.writeStartObject().writeStartArray("routes");
            
            while (result.next()) {
                g.writeStartObject().write("routeId", result.getObject(1).toString())
                        .write("name", result.getString(2))
                        .write("dateCreated", result.getDate(3).getTime())
                        .write("price", result.getString(4))
                        .write("campusId", result.getString(5));
                
                         String geohashes = result.getString(6);
                         geohashes = geohashes.replace("[", "");
                         geohashes = geohashes.replace("]", "");
                         String[] hashes = geohashes.split(",");
                         g.writeStartArray("geohashes");
                         
                         for(String s : hashes)
                         g.write(s);
                        
                        g.writeEnd().writeEnd();
            }
            g.writeEnd().writeEnd().close();
            return sw.toString();
        } catch (SQLException ex) {
            Logger.getLogger(RouteResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (NamingException ex) {
            Logger.getLogger(RouteResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(RouteResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @GET
    @Path("/{routeId}")
    @Produces({"application/json,text/plain"})
    public String getRoutePoints(@PathParam("routeId") String routeId) throws NamingException, SQLException{
        Connection con = null;
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            
            String userId = ci.getUserId();
            
            String sql = "SELECT route_gps_points, route_geohashes FROM public.route WHERE route_driver_user_id = ? and route_id = ?::uuid";
            
            con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, userId);
            stmt.setString(2, routeId);
            ResultSet result = stmt.executeQuery();
            
            StringWriter sw = new StringWriter();
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator g = factory.createGenerator(sw);
            
            if (result.next()) {
                g.writeStartObject().write("points", result.getObject(1).toString()).write("hashes", result.getString(2)).writeEnd().close();
                return sw.toString();
            } else {
                return MessageTypes.NOSUCHELEMENT;//no route with this id for this user
            }
        } catch (NamingException ex) {
            Logger.getLogger(RouteResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (SQLException ex) {
            Logger.getLogger(RouteResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(RouteResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * PUT method for updating or creating an instance of RouteResource
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response addRoute(String content) throws NamingException, SQLException{
        Connection con = null;
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            String userId = ci.getUserId();
            JsonParser parser = Json.createParser(new StringReader(content));
            JsonParser.Event event = null;
            int counter = 0;
            String name = "", campusId = "", points = "", hashes = "", entrypoint = "";
            double price = 0;
            while (parser.hasNext()) {
                event = parser.next();
                
                if (event == Event.KEY_NAME) {
                    switch (parser.getString()) {
                        case "points": {
                            parser.next();
                            points = parser.getString();
                            counter++;
                            break;
                        }
                        case "name": {
                            parser.next();
                            name = parser.getString();
                            counter++;
                            break;
                        }
                        case "price": {
                            parser.next();
                            price = Double.valueOf(parser.getString());
                            counter++;
                            break;
                        }
                        case "campusId": {
                            parser.next();
                            campusId = parser.getString();
                            counter++;
                            break;
                        }
                        case "hashes": {
                            parser.next();
                            hashes = parser.getString();
                            counter++;
                            break;
                        }
                        case "entryPoint": {
                            parser.next();
                            entrypoint = parser.getString();
                            counter++;
                            break;
                        }
                    }
                }
            }   if (counter >= 5) {
                String sql = "INSERT INTO public.route(route_driver_user_id, route_name,route_gps_points,route_price,route_campus_id, route_geohashes, route_entrypoint) VALUES(?,?,?::path,?,?::uuid,?,?::uuid) RETURNING route_id";
                String sql1 = "UPDATE public.driver SET driver_no_routes_recorded = driver_no_routes_recorded+1";
                
                con = Resource.getDBConnection();
                PreparedStatement stmt = con.prepareStatement(sql);
                stmt.setString(1, userId);
                stmt.setString(2, name);
                stmt.setString(3, points);
                stmt.setDouble(4, price);
                stmt.setString(5, campusId);
                stmt.setString(6, hashes);
                stmt.setString(7, entrypoint);
                ResultSet result = stmt.executeQuery();
                
                if (result.next()) {
                    PreparedStatement stmt1 = con.prepareStatement(sql1);
                    stmt1.execute();
                    Resource.getRideHandlerLocal().addRoute(result.getString(1), campusId);
                return Resource.getNoCacheResponseBuilder(Response.Status.OK).entity(result.getString(1)).build();
            }
        }   return Resource.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(MessageTypes.BADMESSAGE).build();
        } catch (NamingException ex) {
            Logger.getLogger(RouteResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (SQLException ex) {
            Logger.getLogger(RouteResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(RouteResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @DELETE
    @Path("/{routeId}")
    public Response removeRoute(@PathParam("routeId") String routeId) throws NamingException, SQLException {
        
        ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
        ClientInfo ci = ch.getUserInfoByToken(token);
        
        String userId = ci.getUserId();
        
        String sql = "DELETE FROM public.route WHERE route_driver_user_id = ? and route_id = ?";
        
        Connection con = Resource.getDBConnection();
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setString(1, userId);
        stmt.setString(2, routeId);
        int res = stmt.executeUpdate();
        
        if (res != 1) {
            Resource.log("error in removeRoute");
        }
        
        return Resource.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
}
