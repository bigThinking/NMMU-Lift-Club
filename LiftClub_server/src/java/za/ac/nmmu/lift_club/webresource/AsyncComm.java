/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

import za.ac.nmmu.lift_club.ejb.ClientInfoHandlerLocal;
import za.ac.nmmu.lift_club.ejb.RideHandlerLocal;
import za.ac.nmmu.lift_club.util.BadMessageException;
import za.ac.nmmu.lift_club.util.ClientInfo;
import za.ac.nmmu.lift_club.util.Match;
import za.ac.nmmu.lift_club.util.MessageTypes;
import za.ac.nmmu.lift_club.util.Resource;
import za.ac.nmmu.lift_club.util.Ride;
import za.ac.nmmu.lift_club.util.RideQuery;
import java.io.StringReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.naming.NamingException;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author Joshua
 */
@ServerEndpoint("/async/AsyncComm")
public class AsyncComm {
    public static final String TOKENHEADER = "Auth_token";
    
    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>(100);

    public static boolean sendMessage(String webSockId, String message) {
        Session session = sessions.get(webSockId);

        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
            return true;
        } else {
            return false;
        }
    }

    @OnOpen
    public void init(Session session) {
        Resource.log("websocket connection open");
        session.setMaxIdleTimeout(60000*30);
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        String[] parts = message.split(MessageTypes.DELIMITER);

        Resource.log("websocket - message recieved" + message);
        if (parts.length != 3) {
            return MessageTypes.BADMESSAGE;
        }
        
        switch (parts[0]) {
            case MessageTypes.AUTHENTICATION: {//works
                Resource.log("websocket - authenticate " + parts[1] + ", " + parts[2]);
                return doAuthentication(parts[1], parts[2], session);
            }
            case MessageTypes.RIDEQUERY: {
                Resource.log("websocket - ridequery " + parts[1] + ", " + parts[2]);
                return doRideQuery(parts[1], parts[2], session);
            }
            case MessageTypes.SELECTRIDE: {
                Resource.log("websocket - selectRide " + parts[1] + ", " + parts[2]);
                return doSelectRide(parts[1], parts[2], session);
            }
            case MessageTypes.ACKPICKUP: {
                Resource.log("websocket - ackPickup " + parts[1] + ", " + parts[2]);
                return ackPickUp(parts[1], parts[2], session);
            }
            case MessageTypes.UPDATEPOSITION: {
                Resource.log("websocket - updatePosition " + parts[1] + ", " + parts[2]);
                return doUpdateDriverPosition(parts[1], parts[2], session);
            }
            case MessageTypes.CANCELRIDE: {//cancel and rideend ;works
                Resource.log("websocket - cancelRide " + parts[1] + ", " + parts[2]);
                return doCancelRide(parts[1], parts[2], session);
            }
            case MessageTypes.ACKDROPOFF: {
                Resource.log("websocket - ackDropOff " + parts[1] + ", " + parts[2]);
                return ackDropOff(parts[1], parts[2], session);
            }
            case MessageTypes.ACTIVATE: {//works
                Resource.log("websocket - activate " + parts[1] + ", " + parts[2]);
                return activateRide(parts[1], parts[2], session);
            }
            case MessageTypes.ADDRIDE: {//works
                Resource.log("websocket - addRide" + parts[1] + ", " + parts[2]);
                return addRide(parts[1], parts[2], session);
            }
            default: {
                Resource.log("websocket - badmessage " + parts[1] + ", " + parts[2]);
                return MessageTypes.NOSUCHELEMENT + MessageTypes.DELIMITER + parts[1];
            }
        }
    }

    @OnError
    public void onError(Throwable t) {
       Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, t);
    }

    @OnClose
    public void close(Session session, CloseReason clr) {
        if (clr.getCloseCode() != CloseCodes.NORMAL_CLOSURE) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, new Exception());
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, clr.toString());
        }

        sessions.remove(session.getId());
    }

    private String doAuthentication(String message, String msgId, Session session) {
        try {
            JsonParser parser = Json.createParser(new StringReader(message));
            JsonParser.Event event = null;

            while (parser.hasNext()) {
                event = parser.next();

                if (event == JsonParser.Event.KEY_NAME) {
                    switch (parser.getString()) {
                        case MessageTypes.AUTHENTICATION: {
                            parser.next();

                            String token = parser.getString();
                            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                            ClientInfo ci = ch.getUserInfoByToken(token);

                            if (ci != null) {
                                session.getUserProperties().put(TOKENHEADER, token);
                                
                                if(ci.getWebSockId() != null)
                                    sessions.remove(ci.getWebSockId());
                                
                                ch.setWebSockId(token, session.getId());
                                session.setMaxIdleTimeout(-1);
                                sessions.put(session.getId(), session);
                                return MessageTypes.ACTIONSUCCESS + MessageTypes.DELIMITER + msgId;
                            } else {
                                Resource.log("unathenticated " + message);
                                return MessageTypes.UNAUTHORISED + MessageTypes.DELIMITER + msgId;
                            }
                        }
                    }
                }
            }   
        } catch (Exception ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }

        return MessageTypes.BADMESSAGE + MessageTypes.DELIMITER + msgId;
    }

    private String doRideQuery(String message, String msgId, Session session) {
        try {
            String authToken = (String) session.getUserProperties().get(TOKENHEADER);
            if (authToken == null) {
                return MessageTypes.UNAUTHORISED;
            }

            RideQuery qry = new RideQuery(message);
            RideHandlerLocal rh = Resource.getRideHandlerLocal();
            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
            Match[] matches = rh.findRides(qry);

            StringBuilder sb = new StringBuilder();
            sb.append(MessageTypes.RIDEMATCH);
            sb.append(MessageTypes.DELIMITER);
            sb.append("{\"matches\" : [");

            for (Match m : matches) {
                sb.append(m.getJson());
                sb.append(",");

                ch.addMatch(authToken, m);
            }

            if(matches.length != 0)
            sb.deleteCharAt(sb.length() - 1);
            
            sb.append("]}");

            Resource.log("ride matches response" + sb.toString());
            return sb.toString();

        } catch (BadMessageException ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.WARNING, message, ex);
            return MessageTypes.BADMESSAGE + MessageTypes.DELIMITER + msgId;
        } catch (NamingException ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }catch (Exception ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }
    }

    private String doSelectRide(String message, String msgId, Session session) {
        try {
            String authToken = (String) session.getUserProperties().get(TOKENHEADER);
            if (authToken == null) {
                return MessageTypes.UNAUTHORISED;
            }

            JsonParser parser = Json.createParser(new StringReader(message));
            JsonParser.Event event = null;

            while (parser.hasNext()) {
                event = parser.next();

                if (event == JsonParser.Event.KEY_NAME) {
                    switch (parser.getString()) {
                        case "matchId": {
                            parser.next();
                            String matchId = parser.getString();
                            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                            ClientInfo ci = ch.getUserInfoByToken(authToken);
                            RideHandlerLocal rh = Resource.getRideHandlerLocal();

                            Match m = (Match) ci.getMatches().get(matchId);

                            if (m != null) {
                                if (m.getRide().isNow()) {
                                    ch.clearMatchs(authToken);
                                    return MessageTypes.SELECTRIDERESPONSE + MessageTypes.DELIMITER + rh.selectNowRide(m);
                                } else {
                                    return MessageTypes.SELECTRIDERESPONSE + MessageTypes.DELIMITER + rh.selectLaterRide(m);
                                }
                            }
                        }
                    }
                }
            }
        } catch (NamingException ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }catch (Exception ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }

        return MessageTypes.BADMESSAGE + MessageTypes.DELIMITER + msgId;
    }

    private String doUpdateDriverPosition(String message, String msgId, Session session) {
        try{
        String authToken = (String) session.getUserProperties().get(TOKENHEADER);
        if (authToken == null) {
            return MessageTypes.UNAUTHORISED;
        }

        JsonParser parser = Json.createParser(new StringReader(message));
        JsonParser.Event event = null;
        double lat = 0, lon = 0;
        boolean onRoute = false;
        byte paramCount = 0;

        while (parser.hasNext()) {
            event = parser.next();

            if (event == JsonParser.Event.KEY_NAME) {
                switch (parser.getString()) {
                    case "lat": {
                        parser.next();
                        lat = Double.valueOf(parser.getString());
                        paramCount++;
                        break;
                    }
                    case "lon": {
                        parser.next();
                        lon = Double.valueOf(parser.getString());
                        paramCount++;
                        break;
                    }
                    case "onRoute": {
                        parser.next();
                        onRoute = event == Event.VALUE_TRUE ? true : false;
                        paramCount++;
                    }
                }
            }
        }

        if (paramCount == 3) {
            try {
                Resource.log("socket - updateing client info");
                ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                ch.updateClientPosition(authToken, lat, lon, onRoute);
                return MessageTypes.ACTIONSUCCESS + MessageTypes.DELIMITER + msgId;
            } catch (NamingException ex) {
                Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, ex);
                return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
            }
        } else {
            Resource.log("socket - bad message " + message);
            return MessageTypes.BADMESSAGE + MessageTypes.DELIMITER + msgId;
        }
        }catch (Exception ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }
    }

    private String doCancelRide(String message, String msgId, Session session) {
        try {
            String authToken = (String) session.getUserProperties().get(TOKENHEADER);
            if (authToken == null) {
                return MessageTypes.UNAUTHORISED;
            }

            JsonParser parser = Json.createParser(new StringReader(message));
            JsonParser.Event event = null;

            while (parser.hasNext()) {
                event = parser.next();

                if (event == JsonParser.Event.KEY_NAME) {
                    switch (parser.getString()) {
                        case "rideId": {
                            parser.next();
                            String rideId = parser.getString();
                            RideHandlerLocal rh = Resource.getRideHandlerLocal();
                            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                            ClientInfo ci = ch.getUserInfoByToken(authToken);
                            ch.removeListedRide(ci.getUserId(), rideId);
                            
                            boolean result;

                            Resource.log("socket - cancelling ride");
                            do {
                                result = rh.removeRide(rideId);
                            } while (!result);

                            Resource.log("socket - ride cancelled");
                            return MessageTypes.ACTIONSUCCESS + MessageTypes.DELIMITER + msgId;
                        }
                    }
                }
            }
        } catch (NamingException ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }catch (Exception ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }

        Resource.log("socket - bad message " + message);
        return MessageTypes.BADMESSAGE + MessageTypes.DELIMITER + msgId;
    }

    private String ackPickUp(String message, String msgId, Session session) {
        try {
            String authToken = (String) session.getUserProperties().get(TOKENHEADER);
            if (authToken == null) {
                return MessageTypes.UNAUTHORISED + MessageTypes.DELIMITER + msgId;
            }

            JsonParser parser = Json.createParser(new StringReader(message));
            JsonParser.Event event = null;

            while (parser.hasNext()) {
                event = parser.next();

                if (event == JsonParser.Event.KEY_NAME) {
                    switch (parser.getString()) {
                        case "sender": {
                            parser.next();
                            String sender = parser.getString();
                            RideHandlerLocal rh = Resource.getRideHandlerLocal();
                            parser.next();
                            parser.next();
                            String rideId = parser.getString();
                            parser.next();
                            parser.next();
                            String qryId = parser.getString();

                            ClientInfoHandlerLocal ch = Resource.getClientInfoHandlerLocal();
                            ClientInfo ci = ch.getUserInfoByToken(authToken);
                            Match m = (Match) ci.getMatches().get(qryId);
                            if (m != null) {
                                if (sender.compareTo(MessageTypes.DRIVER) == 0) {
                                    rh.driverAcknowledgePickup(rideId, m);
                                } else if (sender.compareTo(MessageTypes.PASSENGER) == 0) {
                                    rh.passengerAcknowledgePickup(rideId, m);
                                }
                                return MessageTypes.ACTIONSUCCESS + MessageTypes.DELIMITER + msgId;
                            }
                        }
                    }
                }
            }
        } catch (NamingException ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }catch (Exception ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }

        return MessageTypes.BADMESSAGE + MessageTypes.DELIMITER + msgId;
    }

    private String ackDropOff(String message, String msgId, Session session) {
        try {
            String authToken = (String) session.getUserProperties().get(TOKENHEADER);
            if (authToken == null) {
                return MessageTypes.UNAUTHORISED;
            }

            JsonParser parser = Json.createParser(new StringReader(message));
            RideHandlerLocal rh = Resource.getRideHandlerLocal();
            parser.next();
            parser.next();
            String rideId = parser.getString();
            parser.next();
            parser.next();
            String qryId = parser.getString();
            rh.rideDone(rideId, qryId);
            return MessageTypes.ACTIONSUCCESS + MessageTypes.DELIMITER + msgId;
        } catch (NamingException ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        } catch (IllegalStateException ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, message, ex);
            return MessageTypes.BADMESSAGE + MessageTypes.DELIMITER + msgId;
        }catch (Exception ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }
    }

    private String activateRide(String message, String msgId, Session session) {
        try {
            String authToken = (String) session.getUserProperties().get(TOKENHEADER);
            if (authToken == null) {
                return MessageTypes.UNAUTHORISED;
            }

            JsonParser parser = Json.createParser(new StringReader(message));
            JsonParser.Event event = null;

            while (parser.hasNext()) {
                event = parser.next();

                if (event == JsonParser.Event.KEY_NAME) {
                    switch (parser.getString()) {
                        case "rideId": {
                            parser.next();
                            String rideId = parser.getString();
                            RideHandlerLocal rh = Resource.getRideHandlerLocal();
                            rh.activateRide(rideId);
                            return MessageTypes.ACTIONSUCCESS + MessageTypes.DELIMITER + msgId;
                        }
                    }
                }
            }
        } catch (NamingException ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }catch (Exception ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }

        return MessageTypes.BADMESSAGE + MessageTypes.DELIMITER + msgId;
    }

    public String addRide(String message, String msgId, Session session) {
        try {
            String authToken = (String) session.getUserProperties().get(TOKENHEADER);
            if (authToken == null) {
                return MessageTypes.UNAUTHORISED;
            }

            Ride ride = new Ride(message);
            RideHandlerLocal rh = Resource.getRideHandlerLocal();
            boolean result;
            int adderCount = 0;
            
            do {
                Resource.log("socket - adding ride: " + String.valueOf(++adderCount) + ", " + message);
                result = rh.addRide(ride);
            } while (!result);

            Resource.log("socket - ride added " + message);
            return MessageTypes.ACTIONSUCCESS + MessageTypes.DELIMITER + msgId;
        } catch (NamingException ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        } catch (BadMessageException ex) {
             Resource.log("socket - bad message " + message);
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, null, ex);
            return MessageTypes.BADMESSAGE + MessageTypes.DELIMITER + msgId;
        }catch (Exception ex) {
            Logger.getLogger(AsyncComm.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return MessageTypes.SERVERERROR + MessageTypes.DELIMITER + msgId;
        }
    }
}
