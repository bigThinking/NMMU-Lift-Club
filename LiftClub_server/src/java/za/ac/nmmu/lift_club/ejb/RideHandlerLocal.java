/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.ejb;

import za.ac.nmmu.lift_club.util.Match;
import za.ac.nmmu.lift_club.util.Ride;
import za.ac.nmmu.lift_club.util.RideQuery;
import javax.ejb.Local;

/**
 *
 * @author Joshua
 */
@Local
public interface RideHandlerLocal {

    public Match[] findRides(RideQuery query);

    public Ride getRide(String rideId);

    public boolean removeRide(String rideId);

    public boolean addRide(Ride ride);

    public boolean addRoute(String routeId, String campusId);

    public void removeRoute(String routeId);

    public void activateRide(String rideId);

    public boolean isReady();

    public String selectNowRide(Match match);

    public String selectLaterRide(Match match);

    public void passengerAcknowledgePickup(String rideId, Match match);

    public void driverAcknowledgePickup(String rideId, Match match);

    public void rideDone(String rideId, String qryId);

    public String hello();
}
