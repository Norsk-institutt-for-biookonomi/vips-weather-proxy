package no.nibio.vips.weather.proxy;

import java.net.URI;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/")
public class DefaultResource {

    @GET
    public Response defaultResponse() {
        return Response.status(Response.Status.FOUND) // 302 status code
                       .location(URI.create("/q/swagger-ui"))
                       .build();
    }
}
