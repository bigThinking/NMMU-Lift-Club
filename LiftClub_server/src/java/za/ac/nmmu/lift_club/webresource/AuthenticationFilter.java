/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

import za.ac.nmmu.lift_club.ejb.ClientInfoHandlerLocal;
import za.ac.nmmu.lift_club.util.ClientInfo;
import za.ac.nmmu.lift_club.util.Resource;
import com.sun.xml.messaging.saaj.util.Base64;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author Joshua
 */
@PreMatching
@Provider
public class AuthenticationFilter implements ContainerRequestFilter {

    public static final String TOKENHEADER = "Auth_token";
    public static final String AUTHORISATIONHEADER = "Authorisation";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getRequest().getMethod().equals("OPTIONS") || requestContext.getRequest().getMethod().equals("UPGRADE")) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        
        //Resource.log(path);
        String header;
        Connection con = null;
        try {
            if (path.compareTo("login") == 0) {
                header = requestContext.getHeaderString(AUTHORISATIONHEADER);

                if (header != null || header.startsWith("Basic") != true) {
                    header = header.replace("Basic ", "");
                    String value = Base64.base64Decode(header);
                    int seperatorIndex = value.indexOf(':');
                    String userId = "", password = "";
                    
                    if (seperatorIndex != -2) {
                        userId = value.substring(0, seperatorIndex);
                        password = value.substring(seperatorIndex + 1);
                    

                    con = Resource.getDBConnection();

                    Resource.log(userId + "," + password);
                    String sql = "SELECT user_password = crypt(?,user_password) FROM public.user WHERE user_id = ?";

                    PreparedStatement stmt = con.prepareStatement(sql);
                    stmt.setString(1, password);
                    stmt.setString(2, userId);
                    ResultSet res = stmt.executeQuery();

                    if (res.next()) {
                        if (res.getBoolean(1)) {
                            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                            ClientInfo ci = ch.getUserInfoById(userId);
                            
                            if (ci != null) {
                                ch.renewExpiry(userId);
                            } else {
                                ci = new ClientInfo(userId);
                                ch.addClientInfo(ci);
                            }
                            
                            Resource.log(ci.toString());
                            con.close();
                            requestContext.abortWith(Resource.getNoCacheResponseBuilder(Response.Status.OK).header(TOKENHEADER, ci.getAuthToken()).build());
                            return;
                        }
                    }
                    }
                    
                    requestContext.abortWith(Resource.getNoCacheResponseBuilder(Response.Status.FORBIDDEN).entity("30100").build());//invalid username or password
                }
            } else if (path.compareTo("logout") == 0) {
                header = requestContext.getHeaderString(TOKENHEADER);
                if (header != null) {
                        ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                        ClientInfo ci = ch.getUserInfoByToken(header);
                        
                        if (ci != null)
                        {
                            Resource.log("logging out, " + ci.toString());
                            if(ci.getListedRides().size() == 0 && ci.getMatches().size() == 0)//check for rides or ridesearches, if none remove
                            ch.removeClientInfo(header);
                        }
                }
                requestContext.abortWith(Resource.getNoCacheResponseBuilder(Response.Status.OK).build());
            } else {
                header = requestContext.getHeaderString(TOKENHEADER);
                if (header != null) {
                    ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                    ClientInfo ci = ch.getUserInfoByToken(header);

                    if (ci == null || ci.getExpiry().before(new Date()))//if expiry is before current datetime
                    {
                        requestContext.abortWith(Resource.getNoCacheResponseBuilder(Response.Status.FORBIDDEN).entity("1").build());
                        return;
                    }
                } else {
                    requestContext.abortWith(Resource.getNoCacheResponseBuilder(Response.Status.FORBIDDEN).entity("2").build());
                }

            }
        } catch (NamingException | SQLException ex) {
            Logger.getLogger(AuthenticationFilter.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            requestContext.abortWith(Resource.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).build());
        }finally{
            if(con != null)
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(AuthenticationFilter.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

}
