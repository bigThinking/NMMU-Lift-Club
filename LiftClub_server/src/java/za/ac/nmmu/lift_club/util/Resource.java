/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.util;

import za.ac.nmmu.lift_club.ejb.ClientInfoHandlerLocal;
import za.ac.nmmu.lift_club.ejb.RideHandler;
import za.ac.nmmu.lift_club.ejb.RideHandlerLocal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

/**
 *
 * @author Joshua
 */
public class Resource {
    public static ClientInfoHandlerLocal getClientInfoHandlerLocal() throws NamingException {
        try {
            InitialContext ic = new InitialContext();
            ClientInfoHandlerLocal ch = (ClientInfoHandlerLocal) ic.lookup("java:global/LiftClub_server/ClientInfoHandler");
            return ch;
        } catch (NamingException ex) {
            Logger.getLogger(Resource.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            throw ex;
        }
    }

    public static Connection getDBConnection() throws NamingException, SQLException {
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
    
    public static Response.ResponseBuilder getNoCacheResponseBuilder(Response.Status status) {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setMaxAge(-1);
        cc.setMustRevalidate(true);

        return Response.status(status).cacheControl(cc);
    }
    
     public static RideHandlerLocal getRideHandlerLocal() throws NamingException {
        try {
            InitialContext ic = new InitialContext();
            RideHandlerLocal rh = (RideHandlerLocal) ic.lookup("java:global/LiftClub_server/RideHandler");
            return rh;
        } catch (NamingException ex) {
            Logger.getLogger(Resource.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            throw ex;
        }
    }
     
     public static void log(String msg)
     {
         Logger.getLogger(Resource.class.getName()).log(Level.SEVERE, msg);
     }
}
