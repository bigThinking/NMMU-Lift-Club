package za.ac.nmmu.lift_club.web_clients;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * Created by s210036575 on 2015-09-18.
 */
public class ImageResourceClient implements URL {
    private WebTarget webTarget;
    public Client client;
   // private static final String BASE_URI = "http://csdev.nmmu.ac.za/images";
    public static final String TOKENHEADER = "Auth_token";
    private static String authToken;

    public ImageResourceClient(String authToken, Client client) {
      //  if (client == null)
            this.client = javax.ws.rs.client.ClientBuilder.newBuilder().build();//.sslContext(getSSLContext())
       // else this.client = client;

        webTarget = this.client.target(IMAGE_BASE_URI);
        this.authToken = authToken;
    }

    public Bitmap getImage(String imgId) {
        Response resp = webTarget.path(imgId + ".jpg")
                .request()
                .get();

        if(resp.getStatus() == Response.Status.OK.getStatusCode())
        {
            InputStream is = resp.readEntity(InputStream.class);
            byte[] byteArray = new byte[1];
            try {
                byteArray = IOUtils.toByteArray(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
            IOUtils.closeQuietly(is);
            return BitmapFactory.decodeByteArray(byteArray,0, byteArray.length);
        }

        return null;
    }

    public Invocation.Builder addHeaders(Invocation.Builder b) {
        return b.header(TOKENHEADER, authToken);
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

