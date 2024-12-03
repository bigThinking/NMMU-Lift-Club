/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.util;

/**
 *
 * @author s210036575
 */
public class MessageTypes {
    public static String NOSUCHELEMENT = "354845";
    
    //server recieve message types
    public static final String AUTHENTICATION = "Auth";
    public static final String RIDEQUERY = "RideQuery";
    public static final String SELECTRIDE = "Pick_ride";
    public static final String ACKPICKUP = "Ack_pickup";
    public static final String UPDATEPOSITION = "Up_pos";
    public static final String CANCELRIDE = "Cancel_ride";
    public static final String ACKDROPOFF = "Ack_dropoff";
    public static final String ACTIVATE = "activate_ride";
    public static final String ADDRIDE = "addRide";
   
    //server sent messages
    public static final String RIDEMATCH = "rideMatch";
    public static final String PASSENGERAHEAD = "passengerAhead";
    public static final String LATERPASSENGER = "laterPassenger";
    public static final String RIDECANCELLED = "rideCancelled";
    public static final String RIDEBEGUN = "rideBegun";
    public static final String SELECTRIDERESPONSE = "srResponse";
    public static final String BADMESSAGE = "6097";
    public static final String SERVERERROR = "8764";
    public static final String UNAUTHORISED = "545666";
    public static final String ACTIONFAIL = "45515";
    public static final String ACTIONSUCCESS = "662644";
    
    //message components
    public static final String PASSENGER = "passenger";
    public static final String DRIVER = "driver";
     public static final String DELIMITER = ";";
     
     
//     Messages sent to server have the following format MessageType(delimiter)msgId(delimiter)message
}
