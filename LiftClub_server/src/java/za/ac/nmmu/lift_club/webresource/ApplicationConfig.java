/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

import java.util.Set;
import javax.ws.rs.core.Application;

/**
 *
 * @author Joshua
 */
@javax.ws.rs.ApplicationPath("")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method.
     * It is automatically populated with
     * all resources defined in the project.
     * If required, comment out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(za.ac.nmmu.lift_club.webresource.AuthenticationFilter.class);
        resources.add(za.ac.nmmu.lift_club.webresource.CampusResource.class);
        resources.add(za.ac.nmmu.lift_club.webresource.CarResource.class);
        resources.add(za.ac.nmmu.lift_club.webresource.CorsFilter.class);
        resources.add(za.ac.nmmu.lift_club.webresource.MiscResource.class);
        resources.add(za.ac.nmmu.lift_club.webresource.PointResource.class);
        resources.add(za.ac.nmmu.lift_club.webresource.RatingResource.class);
        resources.add(za.ac.nmmu.lift_club.webresource.RideResource.class);
        resources.add(za.ac.nmmu.lift_club.webresource.RouteResource.class);
        resources.add(za.ac.nmmu.lift_club.webresource.TestResource.class);
        resources.add(za.ac.nmmu.lift_club.webresource.UserResource.class);
    }
    
}
