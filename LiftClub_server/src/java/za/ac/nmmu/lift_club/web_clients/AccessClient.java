/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.web_clients;

import java.util.concurrent.Future;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *
 * @author s210036575
 */
public class AccessClient {
    
    private Client client;
    private static final String BASE_URI = "https://localhost:8060/lift_club";
    public static final String TOKENHEADER = "Auth_token";
    public static final String AUTHORISATIONHEADER = "Authorisation";
   
    public AccessClient() {
        client = javax.ws.rs.client.ClientBuilder.newBuilder().sslContext(getSSLContext()).build();
    }
    
    public String login(String userName, String password)
    {
        WebTarget resource = client.target(BASE_URI).path("login");
        Builder b = resource.request();
        String encoded = doBase64Encoding(userName+":"+password);
        b.header(AUTHORISATIONHEADER, "Basic " + encoded);
        
        Response result = b.get(Response.class);       
        
        if(result.getStatus() == Status.OK.getStatusCode())
        {
            return result.getHeaderString(TOKENHEADER);
        }else return "error:" + result.getStatusInfo();
    }
    
    private void logout(String authToken)
    {
        WebTarget resource = client.target(BASE_URI).path("logout");
        Builder b = resource.request();
        b.header(TOKENHEADER, authToken);    
        b.async().get(Response.class);
        client.close();
    }
    
    public String doBase64Encoding(String s)
    {
        return null;
    }
    
    public void close() {
        client.close();
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
