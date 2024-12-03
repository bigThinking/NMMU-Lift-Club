/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.webresource;

import java.io.File;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * REST Web Service
 *
 * @author Joshua
 */
@Path("res")
public class MiscResource {

    @Context
    private UriInfo context;
    
    private static final String IMG_LOCATION = "";

    /**
     * Creates a new instance of Resource
     */
    public MiscResource() {
    }

    /**
     * Retrieves representation of an instance of ac.za.nmmu.matching.web.MiscResource
     * @return an instance of java.lang.String
     */
    @GET
    @Path("/img/{imgId}")
   @Produces("image/*")
    public Response getImage(@PathParam("imgId")String imageId) {
        File file = new File(IMG_LOCATION + "\\" + imageId);

	ResponseBuilder response = Response.ok((Object) file);
	//response.header("Content-Disposition",attachment; filename=image_from_server.png");
	return response.build();

    }

    /**
     * PUT method for updating or creating an instance of MiscResource
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Path("/img/{imgId}")
    public void replaceImage(@PathParam("imgId")String imageId) {
    }
    
//    public Response getFullImage(...) {
//
//    BufferedImage image = ...;
//
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    ImageIO.write(image, "png", baos);
//    byte[] imageData = baos.toByteArray();
//
//    // uncomment line below to send non-streamed
//    // return Response.ok(imageData).build();
//
//    // uncomment line below to send streamed
//    // return Response.ok(new ByteArrayInputStream(imageData)).build();
//    }
    
//        @GET
//    @Path("getImage/{imageId}")
//    @Produces("image/*")
//    public Response getImage(@PathParam(value = "imageId") String imageId) {
//
//        Image image = getImage(imageId);
//
//        if (image != null) {
//
//            // resize the image to fit the GUI's image frame
//            image = resize((BufferedImage) image, 300, 300);
//
//            final ByteArrayOutputStream out = new ByteArrayOutputStream();
//            try {
//                ImageIO.write((BufferedImage) image, "jpg", out);
//
//                final byte[] imgData = out.toByteArray();
//
//                final InputStream bigInputStream = 
//                      new ByteArrayInputStream(imgData);
//
//                return Response.ok(bigInputStream).
//                     cacheControl(getCacheControl(true)).build();
//            }
//            catch (final IOException e) {
//                return Response.noContent().build();
//            }
//        }
//
//        return Response.noContent().build();
//
//    }
}
