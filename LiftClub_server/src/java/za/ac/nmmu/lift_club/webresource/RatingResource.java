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
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.naming.NamingException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author Joshua
 */
@Path("rating")
public class RatingResource {

    @Context
    private UriInfo context;

    @HeaderParam(AuthenticationFilter.TOKENHEADER)
    private String token;//use token in header to get clientinfo and authorise operation

    /**
     * Creates a new instance of RatingResource
     */
    public RatingResource() {
    }

    /**
     * Retrieves representation of an instance of
     * ac.za.nmmu.matching.web.RatingResource
     *
     * @param rideId
     * @return an instance of java.lang.String
     */
    @GET
    @Path("/ride/{rideId}")
    @Produces({"application/json, text/plain"})
    public String getRideRating(@PathParam("rideId") String rideId) throws NamingException, SQLException {
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            
            String userId = ci.getUserId();
            
            String sql = "SELECT ride_system_rating FROM public.ride WHERE ride_id = ? and ride_driver_user_id = ?";
            
            Connection con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, rideId);
            stmt.setString(2, userId);
            ResultSet result = stmt.executeQuery();
            
            StringWriter sw = new StringWriter();
            if (result.next()) {
                JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
                JsonGenerator g = factory.createGenerator(sw);
                g.writeStartObject();
                g.write("systemRating", result.getString(1));
                
                sql = "SELECT ur_passenger_id, ur_rating, ur_comment, ur_datetime_given FROM public.userrating WHERE ur_ride_id = ? and ur_rating_type = 'd'";
                
                stmt = con.prepareStatement(sql);
                stmt.setString(1, rideId);
                result = stmt.executeQuery();
                g.writeStartArray("userRatings");
                while (result.next()) {
                    g.writeStartObject()
                            .write("userId", result.getString(1))
                            .write("rating", result.getInt(2))
                            .write("comment", result.getString(3))
                            .write("dateTime", result.getDate(4).getTime())
                            .writeEnd();
                }
                g.writeEnd().writeEnd().close();
                return sw.toString();
            } else {
                Resource.log("Problem in getRideRating");
                return MessageTypes.NOSUCHELEMENT;
            }
        } catch (NamingException ex) {
            Logger.getLogger(RatingResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (SQLException ex) {
            Logger.getLogger(RatingResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    @GET
    @Path("/user/{userId}")
    @Produces({"application/json, text/plain"})
    public String getUserRating(@PathParam("userId") String userId) throws SQLException, NamingException{
        try {
            String sql = "SELECT user_nr_ratings_recieved, (user_ratings_sum/user_nr_ratings_recieved) AS \"user_rating\" FROM public.user WHERE user_id = ?";
            
            Connection con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, userId);
            ResultSet result = stmt.executeQuery();
            
            StringWriter sw = new StringWriter();
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator g = factory.createGenerator(sw);
            g.writeStartObject();
            
            if (result.next()) {
                g.write("rating", result.getString(2))
                        .write("nrRatings", result.getString(1));
            }
            g.writeEnd().close();
            return sw.toString();
        } catch (SQLException ex) {
            Logger.getLogger(RatingResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (NamingException ex) {
            Logger.getLogger(RatingResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    @GET
    @Path("/driver/{driverId}")
    @Produces({"application/json, text/plain"})
    public String getDriverRating(@PathParam("userId") String driverId) throws SQLException, NamingException {
        String sql = "SELECT driver_nr_ratings_recieved, driver_nr_system_ratings, driver_nr_rides,"
                + " (driver_user_ratings_sum/driver_nr_ratings_recieved) AS \"driver_passenger_rating\", (driver_system_rating_sum/driver_nr_system_ratings)"
                + " AS \"driver_system_rating\" FROM public.user WHERE driver_user_id = ?";

        Connection con = Resource.getDBConnection();
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setString(1, driverId);
        ResultSet result = stmt.executeQuery();

        StringWriter sw = new StringWriter();
        JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
        JsonGenerator g = factory.createGenerator(sw);
        g.writeStartObject();

        if (result.next()) {
            g.write("passengerRating", result.getString(4))
                    .write("systemRating", result.getString(5))
                    .write("nrPassengerRatings", result.getString(1))
                    .write("nrSystemRating", result.getString(2))
                    .write("nrRides", result.getString(3));
        }
        g.writeEnd().close();
        return sw.toString();
    }

    /**
     * PUT method for updating or creating an instance of RatingResource
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Path("/ride/")
    @Consumes("application/json")
    public Response giveRideRating(String content) throws NamingException, SQLException {
         Connection con = null;
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);

            String userId = ci.getUserId();

            String sql = "INSERT INTO public.ride_passenger(ur_driver_user_id, ur_passenger_id, ur_ride_id, ur_rating, ur_comment, ur_rating_type) VALUES(?,?,?::uuid,?,?,?)";

            con = Resource.getDBConnection();
            con.setAutoCommit(false);
            PreparedStatement stmt = con.prepareStatement(sql);
            JsonParser parser = Json.createParser(new StringReader(content));

            JsonParser.Event event = null;
            int counter = 0;
            String driverId = "", passengerId = "", rideId = "", comment = "", type = "";
            int rating = 0;
            while (parser.hasNext()) {
                event = parser.next();
                if (event == JsonParser.Event.KEY_NAME) {
                    switch (parser.getString()) {
                        case "driverId": {
                            parser.next();
                            driverId = parser.getString();
                            counter++;
                            break;
                        }
                        case "passengerId": {
                            parser.next();
                            passengerId = parser.getString();
                            counter++;
                            break;
                        }
                        case "rideId": {
                            parser.next();
                            rideId = parser.getString();
                            counter++;
                            break;
                        }
                        case "rating": {
                            parser.next();
                            rating = parser.getInt();
                            counter++;
                            break;
                        }
                        case "comment": {
                            parser.next();
                            comment = parser.getString();
                            counter++;
                            break;
                        }
                    }
                }
            }

            if (counter >= 4) {
                if (driverId.compareTo(userId) == 0) {
                    type = "p";
                } else if (passengerId.compareTo(userId) == 0) {
                    type = "d";
                } else {
                    Resource.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(MessageTypes.BADMESSAGE).build();
                }

                stmt.setString(1, driverId);
                stmt.setString(2, passengerId);
                stmt.setString(3, rideId);
                stmt.setInt(4, rating);
                stmt.setString(5, comment);
                stmt.setString(6, type);
                stmt.executeUpdate();

                if (type.compareTo("d") == 0) {
                    sql = "UPDATE public.driver SET driver_user_ratings_sum = driver_user_ratings_sum + ? AND "
                            + "driver_nr_ratings_recieved = driver_nr_ratings_recieved + 1 WHERE driver_user_id = ?";
                    PreparedStatement stmt1 = con.prepareStatement(sql);
                    stmt1.setInt(1, rating);
                    stmt1.setString(2, driverId);
                    stmt1.executeUpdate();
                } else if (type.compareTo("p") == 0) {
                    sql = "UPDATE public.user SET user_ratings_sum = user_ratings_sum + ? AND"
                            + " user_nr_ratings_recieved = user_nr_ratings_recieved +1 WHERE user_id = ?";
                    PreparedStatement stmt1 = con.prepareStatement(sql);
                    stmt1.setInt(1, rating);
                    stmt1.setString(2, userId);
                    stmt1.executeUpdate();
                }

                con.commit();
            }
        } catch (SQLException ex) {
            if(con != null)
                con.rollback();
        }finally{
            con.setAutoCommit(true);
        }
        
        return Resource.getNoCacheResponseBuilder(Response.Status.OK).build();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////  
    private ExecutorService executorService = java.util.concurrent.Executors.newCachedThreadPool();

    @POST
    @Path("/ride/{rideId}/{rating}")
    @Consumes("application/json")
    public void updateSystemRating(@Suspended final AsyncResponse asyncResponse, @PathParam(value = "rideId")
            final String rideId, @PathParam(value = "rating") final double rating) {
        executorService.submit(new Runnable() {
            public void run() {
                doUpdateSystemRating(rideId, rating);
                asyncResponse.resume(javax.ws.rs.core.Response.ok().build());
            }
        });

    }

    private void doUpdateSystemRating(@PathParam("rideId") String rideId, @PathParam("rating") double rating) {
        Resource.log("updating system rating");
        Connection con = null;
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);

            String userId = ci.getUserId();
            String sql = "UPDATE public.ride SET ride_system_rating = ? WHERE ride_id = ?::uuid";

            con = Resource.getDBConnection();
            con.setAutoCommit(false);
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setDouble(1, rating);
            stmt.setString(2, rideId);
            sql = "UPDATE public.driver SET driver_nr_system_ratings = driver_nr_system_ratings + 1, driver_system_rating_sum = driver_system_rating_sum + ?"
                    + " WHERE driver_user_id = ?";
            PreparedStatement stmt1 = con.prepareStatement(sql);
            stmt1.setDouble(1, rating);
            stmt1.setString(2, userId);
            int result = stmt.executeUpdate();
            int result1 = stmt1.executeUpdate();

            if (result != 1 || result1 != 1) {
                con.rollback();
                Resource.log("Problem with doUpdateSystemRating");
            } else {
                con.commit();
            }

            con.setAutoCommit(true);
        } catch (SQLException ex) {
            Logger.getLogger(RatingResource.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex1) {
                Logger.getLogger(RatingResource.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (NamingException ex) {
            Logger.getLogger(RatingResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
