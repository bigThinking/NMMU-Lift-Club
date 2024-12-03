/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.za.nmmu;

import java.sql.Connection;
import java.sql.Statement;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;

/**
 * REST Web Service
 *
 * @author s210036575
 */
@Path("point")
public class Point {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of Point
     */
    public Point() {
    }

    /**
     * Retrieves representation of an instance of ac.za.nmmu.Point
     * @param name
     * @param lat
     * @return an instance of java.lang.String
     */
    @GET
    @Produces("text/plain")
    public String getPoint(@QueryParam("name") String name, @QueryParam("lat") String lat, @QueryParam("long") String lon){
         String sql = "INSERT INTO public.temppoints(id, name, point, time) VALUES(uuid_generate_v4(),'" + name + "'," + "'(" + lat + "," + lon + ")'"+",LOCALTIMESTAMP)";
         
        try{
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource)ctx.lookup("jdbc/Lift_Club");
        Connection con = ds.getConnection();
        
        Statement stmt = con.createStatement();
        stmt.execute(sql);
        int result = stmt.getUpdateCount();
        
        if(result == 1)
           return "202";
        else return "201";
        }catch(Exception e)
        { 
            return e.getMessage() + sql;
        }
    }

    /**
     * PUT method for updating or creating an instance of Point
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public void putJson(String content) {
    }
}
