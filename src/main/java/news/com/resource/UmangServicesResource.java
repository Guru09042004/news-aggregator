package news.com.resource;

import news.com.repository.NewsOpenSearchRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/api/v1/umang")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UmangServicesResource {

    @Inject
    NewsOpenSearchRepository repository;

    @GET
    @Path("/search")
    @Operation(summary = "Search Umang services")
    public Response search(
            @QueryParam("q") String query,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("page") @DefaultValue("0") int page) {
        int from = page * size;
        String result = repository.searchUmangServices(query, size, from);
        return Response.ok(result).build();
    }

    @GET
    @Path("/count")
    @Operation(summary = "Get Umang services count")
    public Response getCount() {
        String result = repository.getUmangCount();
        return Response.ok(result).build();
    }

    @GET
    @Path("/exists")
    @Operation(summary = "Check if Umang index exists")
    public Response indexExists() {
        boolean exists = repository.umangIndexExists();
        return Response.ok("{\"exists\": " + exists + "}").build();
    }
}
