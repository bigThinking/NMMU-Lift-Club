/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

import za.ac.nmmu.lift_club.ejb.ClientInfoHandlerLocal;
import za.ac.nmmu.lift_club.util.ClientInfo;
import za.ac.nmmu.lift_club.util.Resource;
import za.ac.nmmu.lift_club.util.Ride;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import za.ac.nmmu.lift_club.util.Match;

/**
 * REST Web Service
 *
 * @author Joshua
 */
@Path("ride")
public class RideResource {

    @Context
    private UriInfo context;

    @HeaderParam(AuthenticationFilter.TOKENHEADER)
    private String token;
    
    /**
     * Creates a new instance of Ride
     */
    public RideResource() {
    }

    /**
     * Look for rides by creating a ridequery or by rideid
     * @return an instance of java.lang.String
     */
    @GET
    @Path("rides")
    @Produces("application/json")
    public String getRides(@Context final HttpServletResponse response) throws NamingException {
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            ArrayList<Ride> listedRides = ci.getListedRides();
            StringBuilder sb = new StringBuilder();
            sb.append("{\"rides\" : [");
            
            for(Ride r : listedRides)
            {
                sb.append(r.getJSON());
                sb.append(",");
            }
           if(listedRides.size() > 0) sb.deleteCharAt(sb.length()-1);
           sb.append("]}");
           return sb.toString();
           
        } catch (NamingException ex) {
            Logger.getLogger(RideResource.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            response.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            throw ex;
        }
    }
    
    @GET
    @Path("matches")
    @Produces("application/json")
    public String getMatchs(@Context final HttpServletResponse response) throws NamingException {
        try {
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            ClientInfo ci = ch.getUserInfoByToken(token);
            Match[] matchs = new Match[ci.getMatches().entrySet().size()];
            ci.getMatches().values().toArray(matchs);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"matches\" : [");
            
            for(Match m : matchs)
            {
                sb.append(m.getJson());
                sb.append(",");
            }
            
           if(ci.getMatches().entrySet().size() > 0) sb.deleteCharAt(sb.length()-1);
           sb.append("]}");
           return sb.toString();
           
        } catch (NamingException ex) {
            Logger.getLogger(RideResource.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            response.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            throw ex;
        }
    }
}
