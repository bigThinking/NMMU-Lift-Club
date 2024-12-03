/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

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
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import za.ac.nmmu.lift_club.util.Resource;

/**
 * REST Web Service
 *
 * @author s210036575
 */
@Path("campus")
public class CampusResource {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of CampusResource
     */
    public CampusResource() {
    }

    /**
     * Retrieves representation of an instance of za.ac.nmmu.lift_club.webresource.CampusResource
     * @return an instance of java.lang.String
     */
    @GET
    @Produces("application/json")
    public String getJson() throws SQLException, NamingException{
        Connection con = null;
        try {
            String sql = "SELECT campus_id, campus_name, campus_gps_point  FROM public.campus";
            String sql1 = "SELECT entry_point_id, entry_point_gps_point FROM public.entry_point WHERE entry_point_campus_id = ?::uuid";
            
            con = Resource.getDBConnection();
            PreparedStatement stmt1 = con.prepareStatement(sql);
            ResultSet result = stmt1.executeQuery();
            PreparedStatement stmt2 = con.prepareStatement(sql1);
            
            StringWriter sw = new StringWriter();
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator g = factory.createGenerator(sw);
            g.writeStartObject().writeStartArray("campuses");
            
            while (result.next()) {
                Resource.log("Campus resource get: " + result.getObject(1).toString());
                g.writeStartObject();//begin campus
                g.write("Id", result.getObject(1).toString()).write("name", result.getString(2));
                String[] point = result.getString(3).split(",");
                double lat = Double.valueOf(point[0].replace("(", ""));
                double lon = Double.valueOf(point[1].replace(")", ""));
                g.write("lat", lat).write("lon", lon).writeStartArray("entryPoints");
                
                stmt2.setString(1, result.getObject(1).toString());
                ResultSet result2 = stmt2.executeQuery();
                while (result2.next()) {
                    g.writeStartObject().write("Id", result2.getObject(1).toString());//begin entrypoint
                    point = result2.getString(2).split(",");
                    lat = Double.valueOf(point[0].replace("(", ""));
                    lon = Double.valueOf(point[1].replace(")", ""));
                    g.write("lat", lat).write("lon", lon);
                    g.writeEnd();
                }
                g.writeEnd().writeEnd();
            }
            g.writeEnd().writeEnd().close();
            
            return sw.toString();
        } catch (SQLException ex) {
            Logger.getLogger(CampusResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (NamingException ex) {
            Logger.getLogger(CampusResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }catch(Exception ex)
        {
            Logger.getLogger(CampusResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(CampusResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * PUT method for updating or creating an instance of CampusResource
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public void putJson(String content) {
    }
}
