/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

import za.ac.nmmu.lift_club.ejb.ClientInfoHandlerLocal;
import za.ac.nmmu.lift_club.ejb.RideHandlerLocal;
import za.ac.nmmu.lift_club.util.Resource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

/**
 * REST Web Service
 *
 * @author Joshua
 */
@Path("test")
public class TestResource {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of TestResource
     */
    public TestResource() {
    }

    /**
     * Retrieves representation of an instance of ac.za.nmmu.lift_club.webresource.TestResource
     * @return an instance of java.lang.String
     */
    @GET
    @Produces("text/plain")
    public String getText() throws NamingException, SQLException {
        Connection con = Resource.getDBConnection();
        RideHandlerLocal rh = Resource.getRideHandlerLocal();
        ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
        
        String output = "";
        
        if(con != null)
            output = output + "con goodassssssssssssssss fuck nigga";
        
        if(rh != null)
            output = output + rh.hello() + "/n";
        
        if(ch != null)
            output = output + ch.hello() + "/n";
        
        return output;
    }

    /**
     * PUT method for updating or creating an instance of TestResource
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("text/plain")
    public void putText(String content) {
    }
    
     public RideHandlerLocal getRideHandlerLocal() throws NamingException {
        try {
            InitialContext ic = new InitialContext();
            RideHandlerLocal rh = (RideHandlerLocal) ic.lookup("java:global/LiftClub_server/RideHandler");
            return rh;
        } catch (NamingException ex) {
            Logger.getLogger(Resource.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            throw ex;
        }
    }
     
      public ClientInfoHandlerLocal getClientInfoHandlerLocal() throws NamingException {
        try {
            InitialContext ic = new InitialContext();
            ClientInfoHandlerLocal ch = (ClientInfoHandlerLocal) ic.lookup("java:global/LiftClub_server/ClientInfoHandler");
            return ch;
        } catch (NamingException ex) {
            Logger.getLogger(Resource.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            throw ex;
        }
    }

    public Connection getDBConnection() throws NamingException, SQLException {
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/Lift_Club");
            Connection con = ds.getConnection();
            return con;
        } catch (NamingException | SQLException ex) {
            Logger.getLogger(Resource.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            throw ex;
        }
    }
}
