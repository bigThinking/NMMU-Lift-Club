package za.ac.nmmu.lift_club.services;

import android.os.Message;
import android.util.JsonWriter;
import android.util.Log;

import com.philippheckel.service.AbstractService;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.websocket.DeploymentException;
import javax.websocket.Session;

import ch.hsr.geohash.WGS84Point;
import za.ac.nmmu.lift_club.util.Listener;
import za.ac.nmmu.lift_club.util.MessageTypes;
import za.ac.nmmu.lift_club.web_clients.AsyncClientEndpoint;

public class SockCommService extends AbstractService implements Listener {
    AsyncClientEndpoint endPoint;
    String token = null;
    boolean connecting = false;

    public SockCommService() {
    }

    @Override
    public void onStartService() {
        Log.i("sock service started", Log.getStackTraceString(new Exception()));
        Message msg = new Message();
        msg.what = 0;
        msg.arg1 = 10;
        send(msg);
    }

    @Override
    public void onStopService() {

    }

    @Override
    public void onReceiveMessage(final Message msg) {

        if(msg.what != 0 && (endPoint == null || !endPoint.isConnected()) && !connecting)
        {
            Message msg2 = Message.obtain();
            msg2.copyFrom(msg);
            processingQueue.add(msg2);

            if(token != null && !connecting)
                doConnect();
            else if (token == null){
                Message msg1 = new Message();
                msg1.what = 0;
                msg1.arg1 = 10;
                send(msg1);
                return;
            }

        }

        switch (msg.what) {
            case 0: {
                this.token = msg.getData().getString("token");
                doConnect();
                break;
            }
            case 1: {
                endPoint.sendMessage(MessageTypes.ADDRIDE + MessageTypes.DELIMITER + msg.getData().getString("ride"));
                doQueueProcessing();
                break;
            }
            case 2: {
                endPoint.sendMessage(MessageTypes.RIDEQUERY + MessageTypes.DELIMITER + msg.getData().getString("rideQuery"));
                doQueueProcessing();
                break;
            }
            case 3: {
                pushAckPickup(msg.getData().getBoolean("isDriver"), msg.getData().getString("qryId"), msg.getData().getString("rideId"));
                doQueueProcessing();
                break;
            }
            case 4: {
                doQueueProcessing();
                pushUpdatePosition(msg.getData().getBoolean("onRoute"), (WGS84Point) msg.getData().getSerializable("point"));
                break;
            }
            case 5:{
                pushSelectRide(msg.getData().getString("matchId"));
                doQueueProcessing();
                break;
            }
            case 6: {
                pushAckDropOff(msg.getData().getString("qryId"), msg.getData().getString("rideId"));
                doQueueProcessing();
                break;
            }
            case 7:{
                doQueueProcessing();
                pushActivateRide(msg.getData().getString("rideId"));
                break;
            }
            case 8:{
                pushCancelRide(msg.getData().getString("rideId"));
                doQueueProcessing();
                break;
            }
            case 9:{
                break;
            }
        }
    }

    private void doConnect()
    {
        if(token == null)
            return;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connecting = true;
                    endPoint = new AsyncClientEndpoint(token);
                    endPoint.setMessageListener(SockCommService.this);
                    Future<Session> s = endPoint.connect();

                    while (!s.isDone())
                        Thread.sleep(1000);

                    Session session = s.get();
                    Message msg = new Message();
                    msg.what = 0;

                    if(session != null && session.isOpen())
                    {
                        msg.getData().putBoolean("successful", true);
                        send(msg);

                        doQueueProcessing();
                        return;
                    }

                    processingQueue.clear();
                    msg.getData().putBoolean("successful", false);
                    send(msg);
                } catch (URISyntaxException | DeploymentException | ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    connecting = false;
                }
            }
        });
        thread.start();
    }

    private void pushAckPickup(boolean isDriver, String qryId, String rideId)
    {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        try {
            writer.beginObject().name("sender").value(isDriver ? MessageTypes.DRIVER : MessageTypes.PASSENGER)
                    .name("rideId").value(rideId).name("qryId").value(qryId).endObject().flush();
            writer.close();

            endPoint.sendMessage(MessageTypes.ACKPICKUP + MessageTypes.DELIMITER + sw.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void pushSelectRide(String matchId)
    {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        try {
            writer.beginObject().name("matchId").value(matchId).endObject().flush();
            writer.close();

            endPoint.sendMessage(MessageTypes.SELECTRIDE + MessageTypes.DELIMITER + sw.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pushAckDropOff(String qryId, String rideId)
    {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        try {
            writer.beginObject().name("rideId").value(rideId).name("qryId").value(qryId).endObject().flush();
            writer.close();

            endPoint.sendMessage(MessageTypes.ACKDROPOFF + MessageTypes.DELIMITER + sw.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pushActivateRide(String rideId)
    {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        try {
            writer.beginObject().name("rideId").value(rideId).endObject().flush();
            writer.close();
            endPoint.sendMessage(MessageTypes.ACTIVATE + MessageTypes.DELIMITER + sw.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pushUpdatePosition(boolean onRoute, WGS84Point point)
    {
        if(point != null) {
            StringWriter sw = new StringWriter();
            JsonWriter writer = new JsonWriter(sw);
            try {
                writer.beginObject().name("onRoute").value(onRoute)
                        .name("lat").value(point.getLatitude()).name("lon").value(point.getLongitude()).endObject();
                writer.flush();
                writer.close();
                endPoint.sendMessage(MessageTypes.UPDATEPOSITION + MessageTypes.DELIMITER + sw.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void pushCancelRide(String rideId)
    {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);

        try {
            writer.beginObject().name("rideId").value(rideId).endObject().flush();
            writer.close();
            endPoint.sendMessage(MessageTypes.CANCELRIDE + MessageTypes.DELIMITER + sw.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void messageRecieved(String message) {

        String[] parts = message.split(MessageTypes.DELIMITER);

        switch (parts[0])
        {
            case MessageTypes.UNAUTHORISED:
            case MessageTypes.BADMESSAGE:
            case MessageTypes.SERVERERROR:
            case MessageTypes.ACTIONFAIL:{
                Message msg = new Message();
                msg.what = 20;
                msg.arg1 = 0;
                msg.getData().putString("message", message);
                send(msg);
                break;
            }
          //  case MessageTypes.ACTIONSUCCESS:
            case MessageTypes.RIDEMATCH:
            case MessageTypes.PASSENGERAHEAD:
            case MessageTypes.LATERPASSENGER:
            case MessageTypes.RIDECANCELLED:
            case MessageTypes.RIDEBEGUN:
            case MessageTypes.SELECTRIDERESPONSE:
            case MessageTypes.ACKDROPOFF:{
                Message msg = new Message();
                msg.what = 20;
                msg.arg1 = 1;
                msg.getData().putString("message", message);
                send(msg);
                break;
            }
        }

    }
}
