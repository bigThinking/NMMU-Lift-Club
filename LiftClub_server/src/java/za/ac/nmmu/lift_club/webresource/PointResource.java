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
import javax.naming.NamingException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
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
@Path("point")
public class PointResource {

    @Context
    private UriInfo context;

    @HeaderParam(AuthenticationFilter.TOKENHEADER)
    private String token;//use token in header to get clientinfo and authorise operation
    /**
     * Creates a new instance of PointResource
     */
    public PointResource() {
    }

    /**
     * Retrieves representation of an instance of ac.za.nmmu.matching.web.PointResource
     * @return an instance of java.lang.String
     */
//    @GET
//    @Path("/{pointId}")
//    @Produces("application/json")
//    public String getPoint(@PathParam("pointId")String pointId) throws SQLException, NamingException {
//        String sql = "SELECT pos_gps_point FROM public.positon WHERE pos_id = ?";
//        
//        Connection con = Resource.getDBConnection();
//        PreparedStatement stmt = con.prepareStatement(sql);
//        stmt.setString(1, pointId);
//        
//        ResultSet res = stmt.executeQuery();
//        
//        
//    }

    @GET
    @Produces({"application/json,text/plain"})
    public String getPoints() throws Exception{
        Connection con = null;
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            
            String userId = ci.getUserId();
            
            String sql = "SELECT pos_id, pos_name, pos_gps_point, pos_datetime_created FROM public.position WHERE pos_user_id = ?";
            
            con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, userId);
            ResultSet result = stmt.executeQuery();
            
            StringWriter sw = new StringWriter();
            JsonGeneratorFactory factory = Json.createGeneratorFactory(null);
            JsonGenerator g = factory.createGenerator(sw);
            g.writeStartObject().writeStartArray("points");
            
            while (result.next()) {
                String[] point = result.getObject(3).toString().split(",");
                
                if(point.length != 2){
                    Logger.getLogger(PointResource.class.getName()).log(Level.WARNING, "Error deciphering point: {0}", result.getString(1));
                    throw new Exception();
                }else
                {
                    point[0] = point[0].replace("(", "");
                    point[1] = point[1].replace(")", "");
                }
                
                g.writeStartObject().write("positionId", result.getString(1))
                        .write("name", result.getString(2))
                        .write("lat", point[0])
                        .write("lon", point[1])
                        .write("dateCreated", result.getDate(4).getTime())
                        .writeEnd();
            }
            
            g.writeEnd().writeEnd().close();
            return sw.toString();
        } catch (NamingException ex) {
            Logger.getLogger(PointResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (SQLException ex) {
            Logger.getLogger(PointResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(PointResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    /**
     * PUT method for updating or creating an instance of PointResource
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes({"application/json, text/plain"})
    public Response addPoint(String content) throws NamingException, SQLException{
        Connection con = null;
        try {
            Resource.log("started point resource");
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            
            String userId = ci.getUserId();
            
            String sql = "INSERT INTO public.position(pos_user_id, pos_name, pos_gps_point) VALUES(?,?,?::point) RETURNING pos_id";
            String sql1 = "UPDATE public.user SET user_nr_points_saved = user_nr_points_saved+1";
            
            con = Resource.getDBConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            PreparedStatement stmt2 = con.prepareStatement(sql1);
            stmt.setString(1, userId);
            
            JsonParser parser = Json.createParser(new StringReader(content));
            
            JsonParser.Event event = null;
            StringBuilder sb = new StringBuilder();
            int counter = 0;
            
            while (parser.hasNext()) {
                event = parser.next();
                if (event == JsonParser.Event.KEY_NAME) {
                    switch (parser.getString()) {
                        case "lat": {
                            parser.next();
                            sb.append("(");
                            sb.append(parser.getString());
                            counter++;
                            break;
                        }
                        case "lon": {
                            parser.next();
                            sb.append(",");
                            sb.append(parser.getString());
                            sb.append(")");
                            counter++;
                            break;
                        }
                        case "name":{
                            parser.next();
                            stmt.setString(2,parser.getString());
                            break;
                        }
                    }
                }
            }
            
            if(counter == 2){
                stmt.setString(3, sb.toString());
                ResultSet result = stmt.executeQuery();
                
                if(result.next()){
                    stmt2.executeUpdate();
                    Resource.log("all clear");
                    return Resource.getNoCacheResponseBuilder(Response.Status.OK).entity(result.getString(1)).build();
                }else return Resource.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else return Resource.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(MessageTypes.BADMESSAGE).build();
        } catch (SQLException ex) {
            Logger.getLogger(PointResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (NamingException ex) {
            Logger.getLogger(PointResource.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(PointResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
