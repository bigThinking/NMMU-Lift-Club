/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.web_clients;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * Jersey REST client generated for REST resource:RatingResource [rating]<br>
 * USAGE:
 * <pre>
 *        RatingResourceClient client = new RatingResourceClient();
 *        Object response = client.XXX(...);
 *        // do whatever with response
 *        client.close();
 * </pre>
 *
 * @author s210036575
 */
public class RatingResourceClient {
    private WebTarget webTarget;
    private Client client;
    private static final String BASE_URI = "https://localhost:8060/lift_club";
    public static final String TOKENHEADER = "Auth_token";
    private static String authToken;
    
    public RatingResourceClient(String authToken) {
        client = javax.ws.rs.client.ClientBuilder.newBuilder().sslContext(getSSLContext()).build();
        webTarget = client.target(BASE_URI).path("rating");
         authToken = authToken;
    }

    public void updateSystemRating(Object requestEntity, String rideId, String rating) throws ClientErrorException {
        addHeaders(webTarget.path(java.text.MessageFormat.format("ride/{0}/{1}", new Object[]{rideId, rating})).request(javax.ws.rs.core.MediaType.APPLICATION_JSON))
                .put(javax.ws.rs.client.Entity.entity(requestEntity, javax.ws.rs.core.MediaType.APPLICATION_JSON));
    }

    public Response giveRideRating(Object requestEntity) throws ClientErrorException {
        return addHeaders(webTarget.path("ride").request(javax.ws.rs.core.MediaType.APPLICATION_JSON))
                .put(javax.ws.rs.client.Entity.entity(requestEntity, javax.ws.rs.core.MediaType.APPLICATION_JSON), Response.class);
    }

    public String getRideRating_JSON(String rideId) throws ClientErrorException {
        WebTarget resource = webTarget;
        resource = resource.path(java.text.MessageFormat.format("ride/{0}", new Object[]{rideId}));
        return addHeaders(resource.request(javax.ws.rs.core.MediaType.APPLICATION_JSON)).get(String.class);
    }

    public String getRideRating_TEXT(String rideId) throws ClientErrorException {
        WebTarget resource = webTarget;
        resource = resource.path(java.text.MessageFormat.format("ride/{0}", new Object[]{rideId}));
        return addHeaders(resource.request(javax.ws.rs.core.MediaType.TEXT_PLAIN)).get(String.class);
    }

    public String getUserRating_JSON(String userId) throws ClientErrorException {
        WebTarget resource = webTarget;
        resource = resource.path(java.text.MessageFormat.format("user/{0}", new Object[]{userId}));
        return addHeaders(resource.request(javax.ws.rs.core.MediaType.APPLICATION_JSON)).get(String.class);
    }

    public String getUserRating_TEXT(String userId) throws ClientErrorException {
        WebTarget resource = webTarget;
        resource = resource.path(java.text.MessageFormat.format("user/{0}", new Object[]{userId}));
        return addHeaders(resource.request(javax.ws.rs.core.MediaType.TEXT_PLAIN)).get(String.class);
    }

    public String getDriverRating_JSON(String driverId) throws ClientErrorException {
        WebTarget resource = webTarget;
        resource = resource.path(java.text.MessageFormat.format("driver/{0}", new Object[]{driverId}));
        return addHeaders(resource.request(javax.ws.rs.core.MediaType.APPLICATION_JSON)).get(String.class);
    }

    public String getDriverRating_TEXT(String driverId) throws ClientErrorException {
        WebTarget resource = webTarget;
        resource = resource.path(java.text.MessageFormat.format("driver/{0}", new Object[]{driverId}));
        return addHeaders(resource.request(javax.ws.rs.core.MediaType.TEXT_PLAIN)).get(String.class);
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
