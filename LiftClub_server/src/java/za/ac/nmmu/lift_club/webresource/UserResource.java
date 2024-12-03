/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

import za.ac.nmmu.lift_club.ejb.ClientInfoHandlerLocal;
import za.ac.nmmu.lift_club.util.ClientInfo;
import za.ac.nmmu.lift_club.util.Resource;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
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
import javax.naming.NamingException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author Joshua
 */
@Path("user")
public class UserResource {

    @Context
    private UriInfo context;

    @HeaderParam(AuthenticationFilter.TOKENHEADER)
    private String token;//use token in header to get clientinfo and authorise operation

    /**
     * Creates a new instance of User
     */
    public UserResource() {
    }

    @GET
    @Path("/{userId}")
    @Produces({"application/json,text/plain"})
    public String getProfile(@PathParam("userId") String userId) throws NamingException, SQLException{
        Connection con = null;
        try {
            String sql = "SELECT user_initials, user_firstname, user_middlenames,user_surname, user_studying, user_email, user_cell, "
                    + "user_is_driver, user_nr_rides_taken, user_nr_points_saved, user_nr_ratings_recieved, (user_ratings_sum/user_nr_ratings_recieved) AS \"user_current_rating\","
                    + "user_datetime_created, user_gender, user_pic_id FROM public.user WHERE user_id = ?";
            
            con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, userId);
            ResultSet result = stmt.executeQuery();
            
            StringWriter sw = new StringWriter();
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator g = factory.createGenerator(sw);
            
            if (result.next()) {
                g.writeStartObject().write("userId", userId)
                        .write("initials", result.getString(1))
                        .write("firstname", result.getString(2))
                        .write("middlenames", result.getString(3))
                        .write("surname", result.getString(4))
                        .write("studying", result.getString(5))
                        .write("email", result.getString(6))
                        .write("cell", result.getString(7))
                        .write("is_driver", result.getBoolean(8))
                        .write("nr_rides_taken", result.getInt(9))
                        .write("nr_points_saved", result.getInt(10))
                        .write("nr_ratings_recieved", result.getInt(11))
                        .write("current_rating", result.getDouble(12))
                        .write("datetime_created", result.getDate(13).getTime())
                        .write("gender", result.getString(14))
                        .write("pic_id", result.getString(15));
                
                
                if (result.getBoolean(8)) {
                    sql = "SELECT driver_license_no, driver_current_car_vrn, driver_nr_rides, driver_datetime_created, driver_system_rating_sum/driver_nr_system_ratings AS \"driver_system_rating\""
                            + ",  (driver_user_ratings_sum/driver_nr_ratings_recieved) AS \"driver_user_rating\" FROM public.driver WHERE driver_user_id = ?";
                    stmt = con.prepareStatement(sql);
                    stmt.setString(1, userId);
                    result = stmt.executeQuery();
                    
                    if(result.next())
                    {
                        g.write("licenseNo", result.getString(1))
                                .write("current_car_vrn", result.getString(2))
                                .write("nr_rides", result.getInt(3))
                                .write("driver_datetime_created", result.getDate(4).getTime())
                                .write("driver_system_rating", result.getDouble(5))
                                .write("driver_user_rating", result.getDouble(6));
                    }
                }
                
                g.writeEnd().close();
            }
            
            return sw.toString();
        } catch (NamingException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (SQLException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Retrieves representation of an instance of
     * ac.za.nmmu.matching.web.UserResource
     *
     * @return an instance of java.lang.String
     */
    @GET
    @Produces({"application/json,text/plain"})
    public String getProfile() throws SQLException, NamingException{
        Connection con = null;
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            
            String userId = ci.getUserId();
            
            String sql = "SELECT user_initials, user_firstname, user_middlenames,user_surname, user_studying, user_email, user_cell, "
                    + "user_is_driver, user_nr_rides_taken, user_nr_points_saved, user_nr_ratings_recieved, (user_ratings_sum/user_nr_ratings_recieved) AS \"user_current_rating\","
                    + "user_datetime_created, user_gender, user_pic_id FROM public.user WHERE user_id = ?";
            
            con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, userId);
            ResultSet result = stmt.executeQuery();
            
            StringWriter sw = new StringWriter();
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator g = factory.createGenerator(sw);
            
            if (result.next()) {
                g.writeStartObject().write("userId", userId)
                        .write("initials", result.getString(1))
                        .write("firstname", result.getString(2))
                        .write("middlenames", result.getString(3))
                        .write("surname", result.getString(4))
                        .write("studying", result.getString(5))
                        .write("email", result.getString(6))
                        .write("cell", result.getString(7))
                        .write("is_driver", result.getBoolean(8))
                        .write("nr_rides_taken", result.getInt(9))
                        .write("nr_points_saved", result.getInt(10))
                        .write("nr_ratings_recieved", result.getInt(11))
                        .write("current_rating", result.getDouble(12))
                        .write("datetime_created", result.getDate(13).getTime())
                        .write("gender", result.getString(14))
                        .write("pic_id", result.getString(15));
                
                
                if (result.getBoolean(8)) {
                    sql = "SELECT driver_license_no, driver_current_car_vrn, driver_nr_rides, driver_datetime_created, driver_system_rating_sum/driver_nr_system_ratings AS \"driver_system_rating\""
                            + ",(driver_user_ratings_sum/driver_nr_ratings_recieved) AS \"driver_user_rating\", driver_no_routes_recorded FROM public.driver WHERE driver_user_id = ?";
                    stmt = con.prepareStatement(sql);
                    stmt.setString(1, userId);
                    result = stmt.executeQuery();
                    
                    if(result.next())
                    {
                        g.write("licenseNo", result.getString(1))
                                .write("current_car_vrn", result.getString(2))
                                .write("nr_rides", result.getInt(3))
                                .write("driver_datetime_created", result.getDate(4).getTime())
                                .write("driver_system_rating", result.getDouble(5))
                                .write("driver_user_rating", result.getDouble(6))
                                .write("nr_routes_saved", result.getDouble(7));
                    }
                }
                
                g.writeEnd().close();
            }
            
            return sw.toString();
        } catch (SQLException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (NamingException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * PUT method for updating or creating an instance of UserResource
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes({"application/json,text/plain"})
    public Response changeProfileInfo(String content) throws NamingException, SQLException {

        ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
        ClientInfo ci = ch.getUserInfoByToken(token);

        String userId = ci.getUserId();

        String sql = "UPDATE public.user SET ? WHERE user_id = ?";

        Connection con = null;
        try {
            con = Resource.getDBConnection();

            con.setAutoCommit(false);
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(2, userId);

            JsonParser parser = Json.createParser(new StringReader(content));
            JsonParser.Event event = null;

            while (parser.hasNext()) {
                event = parser.next();
                if (event == JsonParser.Event.KEY_NAME) {
                    switch (parser.getString()) {
                        case "initials": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;
                        }
                        case "firstname": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;
                        }
                        case "middlenames": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;
                        }
                        case "surname": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;
                        }
                        case "studying": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;
                        }
                        case "email": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;

                        }
                        case "cell": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;

                        }
//                        case "is_driver": {
//                            stmt.setString(1, "user_" + parser.getString());
//                            parser.next();
//                            stmt.setString(2, parser.getString());
//                            stmt.executeUpdate();
//                            break;
//                        }
//                        case "nr_rides_taken": {
//                            stmt.setString(1, "user_" + parser.getString());
//                            parser.next();
//                            stmt.setString(2, parser.getString());
//                            stmt.executeUpdate();
//                            break;
//                        }
                        case "nr_points_saved": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;
                        }
                        case "nr_ratings_recieved": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;
                        }
                        case "current_rating": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;
                        }
                        case "datetime_created": {
                            stmt.setString(1, "user_" + parser.getString());
                            parser.next();
                            stmt.setString(2, parser.getString());
                            stmt.executeUpdate();
                            break;
                        }
                    }
                }
            }
            con.commit();
            return Resource.getNoCacheResponseBuilder(Response.Status.OK).build();
        } catch (SQLException ex) {
            try {
                con.rollback();
                Resource.log("problem updating user profile" + ex.getMessage());
            } catch (SQLException ex1) {
                Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } finally {
            con.setAutoCommit(true);
            con.close();
            return Resource.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
