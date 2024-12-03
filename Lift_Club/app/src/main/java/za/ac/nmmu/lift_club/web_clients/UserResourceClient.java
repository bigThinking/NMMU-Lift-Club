/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.web_clients;

import android.content.Context;
import android.net.wifi.WifiConfiguration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * Jersey REST client generated for REST resource:UserResource [user]<br>
 * USAGE:
 * <pre>
 *        UserResourceClient client = new UserResourceClient();
 *        Object response = client.XXX(...);
 *        // do whatever with response
 *        client.close();
 * </pre>
 *
 * @author s210036575
 */
public class UserResourceClient implements URL {
    private WebTarget webTarget;
    public Client client;
    //private static final String BASE_URI = "http://honours-21.nmmu.ac.za:8060/lift_club";
//    private static final String BASE_URI = "http://lift_club.csdev.nmmu.ac.za/lift_club";
//    private static final String BASE_URI = "http://csdev.nmmu.ac.za/lift_club";
   // private static final String BASE_URI = "http://192.168.5.125:8060/lift_club";
    public static final String TOKENHEADER = "Auth_token";
    private static String authToken;

    public UserResourceClient(String authToken, Client client) {
       // if(client == null)
        this.client = javax.ws.rs.client.ClientBuilder.newBuilder().build();//.sslContext(getSSLContext())
        //else this.client = client;

        webTarget = this.client.target(BASE_URI).path("user");
        this.authToken = authToken;
    }

    public String getProfile_JSON() throws ClientErrorException {
        WebTarget resource = webTarget;
        Response result = addHeaders(resource.request(javax.ws.rs.core.MediaType.APPLICATION_JSON)).get(Response.class);

        if(result.getStatus() == Response.Status.OK.getStatusCode())
            return result.readEntity(String.class);
        else return "error:" + result.getStatusInfo() +":"+ result.readEntity(String.class);
    }

    public String getProfile_JSON(String userId)
    {
        WebTarget resource = webTarget;
        resource  = resource.path("/" + userId);
        Response result = addHeaders(resource.request(javax.ws.rs.core.MediaType.APPLICATION_JSON)).get(Response.class);

        if(result.getStatus() == Response.Status.OK.getStatusCode())
            return result.readEntity(String.class);
        else return "error:" + result.getStatusInfo() +":"+ result.readEntity(String.class);
    }

    public Response changeProfileInfo_JSON(Object requestEntity) throws ClientErrorException {
        return addHeaders(webTarget.request(javax.ws.rs.core.MediaType.APPLICATION_JSON))
                .post(javax.ws.rs.client.Entity.entity(requestEntity, javax.ws.rs.core.MediaType.APPLICATION_JSON), Response.class);
    }


    public void close() {
        client.close();
    }
    
    public Builder addHeaders(Builder b)
    {
        return b.header(TOKENHEADER, authToken);
    }

    private HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                return true;
            }
        };
    }

    private SSLContext getSSLContext() {
        // for alternative implementation checkout org.glassfish.jersey.SslConfigurator
        javax.net.ssl.TrustManager x509 = new javax.net.ssl.X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {
                return;
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {
                return;
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(null, new javax.net.ssl.TrustManager[]{x509}, null);
        } catch (java.security.GeneralSecurityException ex) {
        }
        return ctx;
    }
    
}
