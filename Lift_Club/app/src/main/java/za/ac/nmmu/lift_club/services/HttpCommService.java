package za.ac.nmmu.lift_club.services;

import android.graphics.Bitmap;
import android.os.Message;
import android.util.JsonWriter;
import android.util.Log;
import com.philippheckel.service.AbstractService;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import ch.hsr.geohash.WGS84Point;
import za.ac.nmmu.lift_club.web_clients.AccessClient;
import za.ac.nmmu.lift_club.web_clients.CampusResourceClient;
import za.ac.nmmu.lift_club.web_clients.CarResourceClient;
import za.ac.nmmu.lift_club.web_clients.ImageResourceClient;
import za.ac.nmmu.lift_club.web_clients.PointResourceClient;
import za.ac.nmmu.lift_club.web_clients.RatingResourceClient;
import za.ac.nmmu.lift_club.web_clients.RideResourceClient;
import za.ac.nmmu.lift_club.web_clients.RouteResourceClient;
import za.ac.nmmu.lift_club.web_clients.UserResourceClient;


public class HttpCommService extends AbstractService {
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private ArrayList<Client> clientCache = new ArrayList<>();
    private ArrayList<Client> imgClientCache = new ArrayList<>();

    public HttpCommService() {
    }

    @Override
    public void onStartService() {

    }

    @Override
    public void onStopService() {
        closeAllClients();
        try {
            executorService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveMessage(Message msg) {
        switch(msg.what)
        {
            case 0:{
                doLogin(msg.getData().getString("userName"), msg.getData().getString("password"));
                break;
            }
            case 1:{
                doQueueProcessing();
                doLogout(msg.getData().getString("token"));
                break;
            }
            case 2:{
                getUserInfo(msg.getData().getString("token"));
                break;
            }
            case 3:{
                getCampuses(msg.getData().getString("token"));
                break;
            }
            case 4:{
                getCars(msg.getData().getString("token"));
                break;
            }
            case 5:{
                getPositions(msg.getData().getString("token"));
                break;
            }
            case 6:{
                doQueueProcessing();
                getRoutes(msg.getData().getString("token"));
                break;
            }
            case 7:{
                doQueueProcessing();
                getListedRides(msg.getData().getString("token"));
                break;
            }
            case 8:{
                savePosition(msg.getData().getString("token"), msg.getData().getString("name"), (WGS84Point)msg.getData().get("point"));
                doQueueProcessing();
                break;
            }
            case 9:{
                doQueueProcessing();
                getRoutePoints(msg.getData().getString("token"));
                break;
            }
            case 10:{
                saveRoute(msg.getData().getString("token"), msg.getData().getString("name"), msg.getData().getString("points"),
                       msg.getData().getString("geoHashes"), msg.getData().getString("entryPoint"), msg.getData().getString("campus"));
                doQueueProcessing();
                break;
            }
            case 11:{
                removeRoute(msg.getData().getString("token"));
                break;
            }
            case 12:{
                deletePosition();
                break;
            }
            case 13:{
                getPublicProfile(msg.getData().getString("token"), msg.getData().getString("userId"), msg.getData().getString("matchId", ""), msg.getData().getBoolean("isDriver", false)
                ,msg.getData().getBoolean("forNowRide", false));
                doQueueProcessing();
                break;
            }
            case 15:{
                getCar(msg.getData().getString("token"), msg.getData().getString("vrn"), msg.getData().getString("matchId", ""), msg.getData().getBoolean("forNowRide", false));
                doQueueProcessing();
                break;
            }
            case 16:{
                updateSystemRating(msg.getData().getString("token"), msg.getData().getString("rideId"), msg.getData().getString("rating"));
                doQueueProcessing();
                break;
            }
            case 17:{
                sendRating(msg.getData().getString("token"), msg.getData().getString("rideId"), msg.getData().getInt("rating"),
                        msg.getData().getString("comment"), msg.getData().getString("driverId"), msg.getData().getString("passengerId"));
                break;
            }
            case 18:{
                getImage(msg.getData().getString("imageId"));
                doQueueProcessing();
                break;
            }
            case 19:{
                getMatches(msg.getData().getString("token"));
                doQueueProcessing();
                break;
            }
        }
    }

    private void sendRating(final String token, final String rideId, final int rating, final String comment, final String driverId, final String passengerId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                RatingResourceClient rrc = new RatingResourceClient(token, cacheClient(null, true));

                StringWriter sw = new StringWriter();
                JsonWriter writer = new JsonWriter(sw);

                    writer.beginObject().name("driverId").value(driverId).name("passengerId").
                            value(passengerId).name("rideId").value(rideId).name("rating").value(rating).name("comment").value(comment).endObject().flush();
                    writer.close();
                    rrc.giveRideRating(sw.toString());
                    rrc.close();
                } catch (IOException e) {
                   e.printStackTrace();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getMatches(final String token) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                RideResourceClient rrc = new RideResourceClient(token, cacheClient(null, true));
                String response = rrc.getMatchs();

                Message msg = new Message();
                msg.what = 19;
                msg.getData().putString("response", response);
                send(msg);

                    rrc.close();
//                if(!response.startsWith("error"))
//                    cacheClient(rrc.client, false);
//                else rrc.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getListedRides(final String token) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                RideResourceClient rrc = new RideResourceClient(token, cacheClient(null, true));
                String response = rrc.getRides();

                Message msg = new Message();
                msg.what = 7;
                msg.getData().putString("response", response);
                send(msg);

                    rrc.close();
//                if (!response.startsWith("error"))
//                    cacheClient(rrc.client, false);
//                else rrc.close();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
            }
        });
    }

    private void getImage(final String imageId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                ImageResourceClient irc = new ImageResourceClient("", imgClientCache.size() == 0 ? null : imgClientCache.remove(0));
                Bitmap response = irc.getImage(imageId);

                if(response != null) {
                    Message msg = new Message();
                    msg.what = 18;
                    msg.getData().putString("imageId", imageId);
                    msg.getData().putParcelable("response", response);
                    send(msg);
                }

                    irc.close();
               // imgClientCache.add(irc.client);
            }catch (Exception e)
            {
                e.printStackTrace();
            }
            }
        });

    }

    private void updateSystemRating(final String token, final String rideId, final String rating) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                RatingResourceClient rrc = new RatingResourceClient(token, cacheClient(null, true));
                rrc.updateSystemRating(null, rideId, rating);
                rrc.close();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
            }
        });
    }

    private void getCar(final String token, final String vrn, final String matchId, final boolean forNowRide) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                CarResourceClient crc = new CarResourceClient(token, cacheClient(null, true));
                String response = crc.getCar_JSON(vrn);

                Message msg = new Message();
                msg.what = 15;
                msg.getData().putString("response", response);
                msg.getData().putString("vrn", vrn);
                msg.getData().putString("matchId", matchId);
                msg.getData().putBoolean("forNowRide", forNowRide);
                send(msg);

                    crc.close();
//                if(!response.startsWith("error"))
//                    cacheClient(crc.client, false);
//                else crc.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void saveRoute(final String token, final String name, final String points, final String geoHashes, final String entryPoint, final String campus) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                RouteResourceClient rrc = new RouteResourceClient(token, cacheClient(null, true));

                StringWriter sw = new StringWriter();
                JsonWriter writer = new JsonWriter(sw);

                try {
                    writer.beginObject();
                    writer.name("name").value(name).name("points").value(points)
                            .name("hashes").value(geoHashes).name("campusId").value(campus).name("entryPoint").value(entryPoint).endObject().flush();
                    writer.close();
                } catch (IOException e) {
                    Message msg = new Message();
                    msg.getData().putString("name", name);
                    msg.getData().putString("response", "error:" + e.getMessage());
                    send(msg);
                    e.printStackTrace();
                }

                String response = rrc.addRoute(sw.toString());

                Message msg = new Message();
                msg.what = 10;
                msg.getData().putString("name", name);
                msg.getData().putString("response", response);
                send(msg);

                    rrc.close();
//                if(!response.startsWith("error"))
//                    cacheClient(rrc.client, false);
//                else rrc.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private synchronized Client cacheClient(Client c, boolean get)// Discovered after coding that client closes after its being read, hence no way to cache it. damn!!!
    {
      /*  try{
        if(get && clientCache.size() > 0)
        {
            return clientCache.get(clientCache.size()-1);
        }else if(!get && c != null) {
            clientCache.add(c);
        }

        Log.i("Client count", String.valueOf(clientCache.size()));
        }catch (Exception e)
        {
            e.printStackTrace();
        }*/
        return null;
    }


    private void removeRoute(String token) {

    }

    private void getRoutePoints(String token) {

    }

    private void deletePosition() {

    }

    private void getPublicProfile(final String token, final String userId, final String matchId, final boolean isDriver, final boolean forNowRide) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                UserResourceClient urc = new UserResourceClient(token, cacheClient(null, true));
                String response = urc.getProfile_JSON(userId);

                Message msg = new Message();
                msg.what = 13;
                msg.getData().putString("response", response);
                msg.getData().putString("matchId", matchId);
                msg.getData().putBoolean("isDriver", isDriver);
                msg.getData().putBoolean("forNowRide", forNowRide);
                send(msg);

                   urc.close();
//                if(!response.startsWith("error"))
//                    cacheClient(urc.client, false);
//                else urc.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

    }


    private void savePosition(final String token, final String name, final WGS84Point point) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                PointResourceClient prc = new PointResourceClient(token, cacheClient(null, true));

                StringWriter sw = new StringWriter();
                JsonWriter writer = new JsonWriter(sw);

                try {
                    writer.beginObject().name("name").value(name).name("lat").
                            value(point.getLatitude()).name("lon").value(point.getLongitude()).endObject().flush();
                    writer.close();
                } catch (IOException e) {
                    Message msg = new Message();
                    msg.getData().putString("name", name);
                    msg.getData().putString("response", "error:" + e.getMessage());
                    e.printStackTrace();
                }

                String response = prc.addPoint_JSON(sw.toString());

                Message msg = new Message();
                msg.what = 8;
                msg.getData().putString("name", name);
                msg.getData().putString("response", response);
                send(msg);

                    prc.close();
//                if(!response.startsWith("error"))
//                    cacheClient(prc.client, false);
//                else prc.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getCars(String token) {

    }

    private void getRoutes(final String token) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                RouteResourceClient rrc = new RouteResourceClient(token, cacheClient(null, true));
                String response = rrc.getRoutes_JSON();

                Message msg = new Message();
                msg.what = 6;
                msg.getData().putString("response", response);
                send(msg);

                    rrc.close();
//                if(!response.startsWith("error"))
//                    cacheClient(rrc.client, false);
//                else rrc.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getPositions(final String token) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                PointResourceClient prc = new PointResourceClient(token, cacheClient(null, true));
                String response = prc.getJson();

                Message msg = new Message();
                msg.what = 5;
                msg.getData().putString("response", response);
                send(msg);
                    prc.close();
//                if(!response.startsWith("error"))
//                    cacheClient(prc.client, false);
//                else prc.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getUserInfo(final String token) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                UserResourceClient urc = new UserResourceClient(token, cacheClient(null, true));
                String response = urc.getProfile_JSON();

                Message msg = new Message();
                msg.what = 2;
                msg.getData().putString("response", response);
                send(msg);

                    urc.close();
//                if(!response.startsWith("error"))
//                    cacheClient(urc.client, false);
//                else urc.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getCampuses(final String token)
    {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                CampusResourceClient crc = new CampusResourceClient(token, cacheClient(null, true));
                String response = crc.getJson();

                Message msg = new Message();
                msg.what = 3;
                msg.getData().putString("response", response);
                send(msg);

                    crc.close();
//                if(!response.startsWith("error"))
//                    cacheClient(crc.client, false);
//                else crc.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void doLogin(final String userName, final String password)
    {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                AccessClient ac = new AccessClient();
                String s = ac.login(userName, password);

                Message response = new Message();
                response.what = 0;
                response.getData().putString("response", s);
                send(response);

                    ac.close();
//                if(!s.startsWith("error"))
//                    cacheClient(ac.client, false);
//                else ac.close();
                }catch (Exception e)
                {
                    Message response = new Message();
                    response.what = 0;
                    response.getData().putString("response", "error:" + e.getMessage());
                    send(response);
                    e.printStackTrace();
                }
            }
        });
    }

    private void doLogout(final String authToken) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try{
                AccessClient ac = new AccessClient();
                Future<Response> response = ac.logout(authToken);
                try {
                while(!response.isDone()) {
                        Thread.sleep(3000);
                }

                Log.i("logout", response.get().getStatusInfo().getReasonPhrase());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                ac.close();
                //HttpCommService.this.stopSelf();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void closeAllClients() {
        for(Client c : clientCache)
        {
            c.close();
        }

        for(Client c : imgClientCache)
        {
            c.close();
        }
    }

}
