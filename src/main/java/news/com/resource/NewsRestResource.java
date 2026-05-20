package news.com.resource;

import news.com.model.NewsArticle;
import news.com.model.NewsSearchRequest;
import news.com.model.NewsSearchResult;
import news.com.service.NewsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.util.List;

@Path("/api/v1/news")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "News API", description = "India News - Fetch, Search and Ingest")
public class NewsRestResource {

    @Inject
    NewsService newsService;

    @GET
    @Path("/latest")
    @Operation(summary = "Get latest news")
    public Response getLatestNews(
            @QueryParam("size") @DefaultValue("10") int size) {
        List<NewsArticle> articles = newsService.getLatest(size);
        return Response.ok(articles).build();
    }

    @GET
    @Path("/search")
    @Operation(summary = "Search news articles")
    public Response searchNews(
            @QueryParam("q")        String query,
            @QueryParam("category") String category,
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate")   String toDate,
            @QueryParam("page")     @DefaultValue("0")  int page,
            @QueryParam("size")     @DefaultValue("10") int size) {
        NewsSearchRequest req = new NewsSearchRequest();
        req.setQuery(query);
        req.setCategory(category);
        req.setFromDate(fromDate);
        req.setToDate(toDate);
        req.setPage(page);
        req.setSize(size);
        NewsSearchResult result = newsService.search(req);
        return Response.ok(result).build();
    }

    @GET
    @Path("/categories")
    @Operation(summary = "Get all categories")
    public Response getCategories() {
        List<String> categories = newsService.getCategories();
        return Response.ok(categories).build();
    }

    @POST
    @Path("/ingest")
    @Operation(summary = "Trigger news ingestion")
    public Response ingestNews() {
        newsService.ingestAll();
        return Response.ok("Ingestion started").build();
    }
}
