/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

import za.ac.nmmu.lift_club.ejb.ClientInfoHandlerLocal;
import za.ac.nmmu.lift_club.util.ClientInfo;
import za.ac.nmmu.lift_club.util.Resource;
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
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author Joshua
 */
@Path("car")
public class CarResource {

    @Context
    private UriInfo context;
    
    @HeaderParam(AuthenticationFilter.TOKENHEADER)
    private String token;//use token in header to get clientinfo and authorise operation

    /**
     * Creates a new instance of CarResource
     */
    public CarResource() {
    }

    /**
     * Retrieves representation of an instance of
     * ac.za.nmmu.matching.web.CarResource
     *
     * @return an instance of java.lang.String
     */
    @GET
    @Produces({"application/json,text/plain"})
    public String getCars() throws SQLException, NamingException{
        Connection con = null;
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            
            String userId = ci.getUserId();
            String sql = "SELECT car_vrn, car_manufacturer, car_model, car_pic_id FROM public.car, public.drivercar, public.driver "
                    + "WHERE driver_user_id = ? AND dc_car_vrn = car_vrn AND dc_driver_user_id = driver_user_id";
            con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, userId);
            ResultSet result = stmt.executeQuery();
            
            StringWriter sw = new StringWriter();
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator g = factory.createGenerator(sw);
            g.writeStartObject().writeStartArray("cars");
            
            while (result.next()) {
                g.writeStartObject().write("vrn", result.getString(1))
                        .write("manufaturer", result.getString(2))
                        .write("model", result.getString(3))
                        .write("pic_id", result.getString(4))
                        .writeEnd();
            }
            
            g.writeEnd().writeEnd().close();
            return sw.toString();
        } catch (SQLException ex) {
            Logger.getLogger(CarResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (NamingException ex) {
            Logger.getLogger(CarResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(CarResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @GET
    @Path("/{carVRN}")
    @Produces({"application/json,text/plain"})
    public String getCar(@PathParam("carVRN") String carVRN) throws NamingException, SQLException{
        Connection con = null;
        try {
//            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
//            ClientInfo ci = ch.getUserInfoByToken(token);
            
          //  String userId = ci.getUserId();
            String sql = "SELECT car_vrn, car_manufacturer, car_model, car_has_boot, car_max_nr_seats, car_colour, car_pic_id "
                    + "FROM public.car, public.drivercar, public.driver "
                    + "WHERE car_vrn = ?";//AND dc_car_vrn = car_vrn AND driver_user_id = ? AND dc_driver_user_id = driver_user_id";
            
            con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, carVRN);
          //  stmt.setString(2, userId);
            ResultSet result = stmt.executeQuery();
            
            StringWriter sw = new StringWriter();
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator g = factory.createGenerator(sw);
            g.writeStartObject();
            
            if(result.next())
            {
                g.write("vrn", result.getString(1))
                        .write("manufacturer", result.getString(2))
                        .write("model", result.getString(3))
                        .write("hasBoot", result.getBoolean(4))
                        .write("maxSeats", result.getString(5))
                        .write("colour", result.getString(6))
                        .write("pic_id", result.getString(7))
                        .writeEnd();
            }
            
            g.close();
            
            Resource.log("car : " + sw.toString());
            return sw.toString();
        } catch (NamingException ex) {
            Logger.getLogger(CarResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (SQLException ex) {
            Logger.getLogger(CarResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(CarResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @POST
    @Path("/{carVRN}")
    public Response setCurrentCar(@PathParam("carVRN") String carVRN) throws SQLException, NamingException
    {
        Connection con = null;
        try{
        ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
        ClientInfo ci = ch.getUserInfoByToken(token);

        String userId = ci.getUserId();
        String sql = "UPDATE public.driver SET driver_current_car_vrn = ? WHERE driver_user_id = ?";
        
        con = Resource.getDBConnection();
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setString(1, carVRN);
        stmt.setString(2, userId);
        stmt.executeUpdate();
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(CarResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return Resource.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
    
    /**
     * PUT method for updating or creating an instance of CarResource
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response addCar(String content) {
        throw new UnsupportedOperationException();
    }
}
