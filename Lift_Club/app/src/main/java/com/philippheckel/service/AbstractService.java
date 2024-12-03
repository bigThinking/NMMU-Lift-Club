/* 
 * This example demonstrates a good way to communicate between Activity and Service.
 * 
 * 1. Implement a service by inheriting from AbstractService
 * 2. Add a ServiceManager to your activity
 *   - Control the service with ServiceManager.start() and .stop()
 *   - Send messages to the service via ServiceManager.send() 
 *   - Receive messages with by passing a Handler in the constructor
 * 3. Send and receive messages on the service-side using send() and onReceiveMessage()
 * 
 * Author: Philipp C. Heckel; based on code by Lance Lefebure from
 *         http://stackoverflow.com/questions/4300291/example-communication-between-activity-and-service-using-messaging
 * Source: https://code.launchpad.net/~binwiederhier/+junk/android-service-example
 * Date:   6 Jun 2012
 */
package com.philippheckel.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public abstract class AbstractService extends Service {
    static final int MSG_REGISTER_CLIENT = 9991;
    static final int MSG_UNREGISTER_CLIENT = 9992;
    public static final int ADD_PROCESSING_QUEUE = 85484;
    protected List<Message> outMessageQueue = Collections.synchronizedList(new ArrayList<Message>());
    protected List<Message> processingQueue = Collections.synchronizedList(new ArrayList<Message>());

    List<Messenger> mClients = Collections.synchronizedList(new ArrayList<Messenger>());// Keeps track of all current registered clients.
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.
    Thread thread = null;

    private class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    Log.i("MyService", "Client registered: " + msg.replyTo);
                    mClients.add(msg.replyTo);

                    //if (outMessageQueue.size() > 0)
                     //   send(null);

                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.i("MyService", "Client un-registered: " + msg.replyTo);
                    mClients.remove(msg.replyTo);
                    break;
                case ADD_PROCESSING_QUEUE:
                    processingQueue.add((Message) msg.getData().getParcelable("msg"));
                    break;
                default:
                    //super.handleMessage(msg);
                    onReceiveMessage(msg);
            }
        }
    }

    protected void doQueueProcessing()
    {
        if((thread == null || thread.getState() == Thread.State.TERMINATED) && processingQueue.size() > 0) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int stop = processingQueue.size();
                    for (int i = 0; i < stop; i++) {
//                    Message msg1 = Message.obtain();
//                    msg1.copyFrom(processingQueue.remove(i));
                        onReceiveMessage(processingQueue.remove(0));
//                    processingQueue.remove(i);
                    }
                }
            });
            thread.start();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        onStartService();

        Log.i("MyService", "Service Started.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MyService", "Received start id " + startId + ": " + intent);
        return START_STICKY; // run until explicitly stopped.
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        onStopService();

        Log.i("MyService", "Service Stopped.");
    }

    public synchronized void send(Message msg) {
        if (msg != null)
            outMessageQueue.add(msg);
        int stop = outMessageQueue.size();

        for (int i = mClients.size()-1; i >= 0; i--) {
            try {
                for (int j = 0; j < stop; j++) {
                    Log.i("MyService", "Sending message to clients: " + msg);
                    Message msg1 = Message.obtain();
                    msg1.copyFrom(outMessageQueue.get(j));
                    mClients.get(i).send(msg1);
                }
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                Log.i("Message queue crash", String.valueOf(stop));
                Log.e("MyService", "Client is dead. Removing from list: " + i);
                mClients.remove(i);
            }
        }

        outMessageQueue.clear();
    }

    protected boolean checkClients() {
        send(new Message());
        if (mClients.size() > 0)
            return true;
        return false;
    }

    public abstract void onStartService();

    public abstract void onStopService();

    public abstract void onReceiveMessage(Message msg);

}
