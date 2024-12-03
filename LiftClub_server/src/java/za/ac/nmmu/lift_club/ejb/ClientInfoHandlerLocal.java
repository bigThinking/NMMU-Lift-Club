/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.ejb;

import za.ac.nmmu.lift_club.util.ClientInfo;
import za.ac.nmmu.lift_club.util.Match;
import za.ac.nmmu.lift_club.util.Ride;
import javax.ejb.Local;

/**
 *
 * @author Joshua
 */
@Local
public interface ClientInfoHandlerLocal {

    public void addClientInfo(ClientInfo uci);

    public ClientInfo getUserInfoByToken(String authToken);

    public ClientInfo getUserInfoById(String userId);

    public void removeClientInfo(String authToken);

    public void setWebSockId(String authToken, String Id);

    public void addMatch(String authToken, Match match);

    public void updateClientPosition(String authToken, double lat, double lon, boolean onRoute);

    public void clearMatchs(String authToken);
    
    public boolean sendMessage(String userId, String message);
    
    public void removeListedRide(String userId, String rideId);
    
    public void addListedRide(String userId, Ride ride);
    
     public boolean renewExpiry(String userId);
            
    public String hello();
}
