/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.ejb;

import za.ac.nmmu.lift_club.util.ClientInfo;
import za.ac.nmmu.lift_club.util.Match;
import za.ac.nmmu.lift_club.util.Ride;
import za.ac.nmmu.lift_club.webresource.AsyncComm;
import java.util.concurrent.ConcurrentHashMap;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import za.ac.nmmu.lift_club.util.Resource;

/**
 *
 * @author Joshua
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ClientInfoHandler implements ClientInfoHandlerLocal {

    private final ConcurrentHashMap<String, ClientInfo> clientInfoTokenMap = new ConcurrentHashMap<String, ClientInfo>(100);
    private final ConcurrentHashMap<String, ClientInfo> clientInfoIdMap = new ConcurrentHashMap<String, ClientInfo>(100);

    @Override
    public void addClientInfo(ClientInfo uci) {
        clientInfoTokenMap.put(uci.getAuthToken(), uci);
        clientInfoIdMap.put(uci.getUserId(), uci);
    }

    @Override
    public ClientInfo getUserInfoByToken(String authToken) {
        return clientInfoTokenMap.get(authToken);
    }

    @Override
    public ClientInfo getUserInfoById(String userId) {
        return clientInfoIdMap.get(userId);
    }

    @Override
    public void removeClientInfo(String authToken) {
        ClientInfo ci = clientInfoTokenMap.remove(authToken);
        if (ci != null) {
            clientInfoIdMap.remove(ci.getUserId());
        }
        
        Resource.log("removing " + ci.toString());
    }

    @Override
    public void setWebSockId(String authToken, String Id) {
        ClientInfo ci = clientInfoTokenMap.get(authToken);

        if (ci != null) {
            ci.setWebSockId(Id);
        }
    }

    @Override
    public void addMatch(String authToken, Match match) {
        ClientInfo ci = clientInfoTokenMap.get(authToken);

        if (ci != null) {
            ci.getMatches().put(match.getId(), match);
        }
        
         Resource.log("ch - mATCH added length: " + String.valueOf(ci.getMatches().size()) + ci.getMatches().toString());       
    }

    @Override
    public void updateClientPosition(String authToken, double lat, double lon, boolean onRoute) {
        ClientInfo ci = clientInfoTokenMap.get(authToken);
        Resource.log("ch - updating driver pos lat:" + String.valueOf(lat) + " lon:" + String.valueOf(lon) + " onRoute:" + String.valueOf(onRoute));
        if (ci != null) {
            ci.setLat(lat);
            ci.setLon(lon);
            ci.setOnRoute(onRoute);
        }
        
        Resource.log("ch - driver pos updated lat:" + String.valueOf(ci.getLat()) + " lon:" + String.valueOf(ci.getLon()) + " onRoute:" + String.valueOf(ci.isOnRoute()));
    }

    @Override
    public void clearMatchs(String authToken) {
        ClientInfo ci = clientInfoTokenMap.get(authToken);

        if (ci != null) {
            ci.getMatches().clear();
        }
    }

    @Override
    public boolean sendMessage(String userId, String message) {
        ClientInfo ci = clientInfoIdMap.get(userId);
        boolean result = false;
        if (ci != null) {
            result = true;
            while (!ci.getMessages().isEmpty()) {
                result = AsyncComm.sendMessage(ci.getWebSockId(), (String) ci.getMessages().peek());
                if (result) {
                    ci.getMessages().poll();
                } else {
                    break;
                }
            }
            if (result) {
                result = AsyncComm.sendMessage(ci.getWebSockId(), message);
                if (!result) {
                    result = ci.getMessages().offer(message);
                }
            }

        }
        return result;
    }

    @Override
    public void removeListedRide(String userId, String rideId) {
        ClientInfo ci = clientInfoIdMap.get(userId);

        for (int i = 0; i < ci.getListedRides().size(); i++) {
            Ride r = ci.getListedRides().get(i);
            if (r.getRideId().compareTo(rideId) == 0) {
                ci.getListedRides().remove(i);
                break;
            }
        }
        
         Resource.log("ch - ride removed length: " + String.valueOf(ci.getListedRides().size()) + ci.getListedRides().toString());       
    }

    @Override
    public void addListedRide(String userId, Ride ride) {
        ClientInfo ci = clientInfoIdMap.get(userId);

        if (ci != null) {
            ci.getListedRides().add(ride);
            Resource.log("ch - ride added length: " + String.valueOf(ci.getListedRides().size()) + ci.getListedRides().toString());       
        }
    }

    @Override
    public boolean renewExpiry(String userId) {
        ClientInfo ci = clientInfoIdMap.get(userId);
        if (ci != null) {
            ci.renewExpiry();
            return true;
        }
        
        return false;
    }

    @Override
    public String hello() {
        return "hello from ch";
    }
}
