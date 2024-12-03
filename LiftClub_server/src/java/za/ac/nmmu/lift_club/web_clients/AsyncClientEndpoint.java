/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.web_clients;

import javax.websocket.ClientEndpoint;

/**
 *
 * @author s210036575
 */
@ClientEndpoint
public class AsyncClientEndpoint {
    
    private AsyncClientEndpoint() {
    }
    
    public static AsyncClientEndpoint getInstance() {
        return AsyncClientEndpointHolder.INSTANCE;
    }
    
    private static class AsyncClientEndpointHolder {

        private static final AsyncClientEndpoint INSTANCE = new AsyncClientEndpoint();
    }
}
