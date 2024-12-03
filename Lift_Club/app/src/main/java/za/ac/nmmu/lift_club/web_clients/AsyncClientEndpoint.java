package za.ac.nmmu.lift_club.web_clients;

import android.content.pm.PackageInstaller;
import android.util.Log;

import org.glassfish.tyrus.client.ClientManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.json.*;

import za.ac.nmmu.lift_club.util.Listener;
import za.ac.nmmu.lift_club.util.MessageTypes;

/**
 * Created by s210036575 on 2015-08-30.
 */
@ClientEndpoint
public class AsyncClientEndpoint implements URL {
    private Session session;
    private Listener listener;
    private ArrayList<String> queue = new ArrayList<>();
    private String token;
    private Random rand = new Random();

    public boolean sendMessage(String message) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int stop = queue.size();
                for (int i = 0; i < stop; i++) {
                    Log.i("sock sending", queue.get(0));
                    session.getAsyncRemote().sendText(queue.remove(0));
                }
            }
        });

        message = message + MessageTypes.DELIMITER + String.valueOf(rand.nextInt());
        queue.add(message);
        if (session != null && session.isOpen()) {
            thread.start();
            return true;
        } else return false;
    }

    public AsyncClientEndpoint(String token) {
        this.token = token;
    }

    public Future<Session> connect() throws URISyntaxException, DeploymentException {
        ClientManager client = ClientManager.createClient();

        return client.asyncConnectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            doOnMessage(message);
                            Log.i("asyncClient", "### Tyrus Client onMessage: " + message);
                        }
                    });

                    Log.i("TYRUS-TEST", "### 2 Tyrus Client onOpen");
                    init(session);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Session session, Throwable thr) {
                doOnError(thr);
            }

            @Override
            public void onClose(Session session, CloseReason clr) {
                doOnClose(session, clr);
            }
        }, ClientEndpointConfig.Builder.create().build(), new URI(ASYNC_URI));
    }

    public boolean isConnected() {
        if (session != null)
            return session.isOpen();

        return false;
    }

    public void setMessageListener(Listener listener) {
        this.listener = listener;
    }

    public void init(Session session) {
        this.session = session;

            String authMessage = MessageTypes.AUTHENTICATION + MessageTypes.DELIMITER + "{\"" + MessageTypes.AUTHENTICATION + "\":\"" + token + "\"}";
            sendMessage(authMessage);

    }

    public void doOnMessage(String message) {

        String[] parts = message.split(MessageTypes.DELIMITER);

        if (parts.length != 2) {
            return;
        }

        switch (parts[0]) {
            case MessageTypes.UNAUTHORISED:
            case MessageTypes.SERVERERROR:
            case MessageTypes.ACTIONFAIL:
            case MessageTypes.ACTIONSUCCESS: {
                for (int i = 0; i < queue.size(); i++) {
                    String[] part1 = queue.get(i).split(MessageTypes.DELIMITER);
                    if (part1[2].compareTo(parts[1]) == 0) {//msg ids the same
                        listener.messageRecieved(parts[0] + MessageTypes.DELIMITER + part1[0] + MessageTypes.DELIMITER + part1[1]);//reply, initial message type, initial message
                        Log.i("sMessage1", message);
                        return;
                    }
                }
                break;
            }
            case MessageTypes.RIDEMATCH:
            case MessageTypes.PASSENGERAHEAD:
            case MessageTypes.LATERPASSENGER:
            case MessageTypes.RIDECANCELLED:
            case MessageTypes.RIDEBEGUN:
            case MessageTypes.SELECTRIDERESPONSE:
            case MessageTypes.ACKDROPOFF: {
                listener.messageRecieved(message);
                Log.i("sMessage2", message);
                return;
            }
            case MessageTypes.BADMESSAGE: {
                for (int i = 0; i < queue.size(); i++) {
                    String[] part1 = queue.get(i).split(MessageTypes.DELIMITER);
                    if (part1[2].compareTo(parts[1]) == 0) {//msg ids the same
                        Log.i("Bad message: ", parts[0] + MessageTypes.DELIMITER + part1[0] + MessageTypes.DELIMITER + part1[1]);//reply, initial message type, initial message
                        Log.i("sMessage3", message);
                        return;
                    }
                }
            }
        }
    }


    public void doOnError(Throwable t) {
        t.printStackTrace();
    }

    public void doOnClose(Session session, CloseReason clr) {
        if (clr.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE) {
            Log.i("websocket closed : ", clr.toString());
        }
    }
}
