/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ac.za.nmmu.lift_club.util;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author Joshua
 */
public class ClientInfo {
    
    private final String userId;
    private String authToken, webSockId;
    private Date expiry;
    private double lat, lon;
    private boolean onRoute = true;
    
    public ClientInfo(String userId)
    {
        this.userId = userId;
        authToken = UUID.randomUUID().toString();
        renewExpiry();
    }

    public String getUserId() {
        return userId;
    }

    public boolean isOnRoute() {
        return onRoute;
    }

    public void setOnRoute(boolean onRoute) {
        this.onRoute = onRoute;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getWebSockId() {
        return webSockId;
    }

    public void setWebSockId(String webSockId) {
        this.webSockId = webSockId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Date getExpiry() {
        return expiry;
    }

    public final void renewExpiry() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        expiry = cal.getTime();
    }   
    
}
