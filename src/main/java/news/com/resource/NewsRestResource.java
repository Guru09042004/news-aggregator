package news.com.resource;

import news.com.model.NewsArticle;
import news.com.model.NewsSearchRequest;
import news.com.model.NewsSearchResult;
import news.com.service.NewsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/api/v1/news")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "News API", description = "India News - Fetch, Search and Ingest")
public class NewsRestResource {

    @Inject
    NewsService newsService;

    @Inject
    ProducerTemplate producerTemplate;

    @GET
    @Path("/latest")
    @Operation(summary = "Get latest news",
               description = "Returns most recently published articles from OpenSearch")
    public Response getLatestNews(
            @Parameter(description = "Number of articles (max 50)")
            @QueryParam("size") @DefaultValue("10") int size) {
        List<NewsArticle> articles = newsService.getLatest(size);
        return Response.ok(articles).build();
    }

    @GET
    @Path("/search")
    @Operation(summary = "Search news articles",
               description = "Full-text search with optional filters")
    public Response searchNews(
            @QueryParam("q")        String query,
            @QueryParam("category") String category,
            @QueryParam("provider") String provider,
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate")   String toDate,
            @QueryParam("page")     @DefaultValue("0")  int page,
            @QueryParam("size")     @DefaultValue("10") int size) {
        NewsSearchRequest req = new NewsSearchRequest();
        req.setQuery(query);
        req.setCategory(category);
        req.setProvider(provider);
        req.setFromDate(fromDate);
        req.setToDate(toDate);
        req.setPage(page);
        req.setSize(Math.min(size, 50));
        return Response.ok(newsService.search(req)).build();
    }

    @POST
    @Path("/search")
    @Operation(summary = "Advanced search (POST body)")
    public Response searchNewsPost(NewsSearchRequest request) {
        if (request == null) request = new NewsSearchRequest();
        request.setSize(Math.min(request.getSize(), 50));
        return Response.ok(newsService.search(request)).build();
    }

    @GET
    @Path("/categories")
    @Operation(summary = "List all supported categories")
    public Response getCategories() {
        return Response.ok(newsService.getCategories()).build();
    }

    @GET
    @Path("/category/{category}")
    @Operation(summary = "Get news by category")
    public Response getByCategory(
            @PathParam("category") String category,
            @QueryParam("size") @DefaultValue("10") int size) {
        NewsSearchRequest req = new NewsSearchRequest();
        req.setCategory(category);
        req.setSize(Math.min(size, 50));
        return Response.ok(newsService.search(req).getArticles()).build();
    }

    @POST
    @Path("/ingest")
    @Operation(summary = "Trigger manual ingest from all sources")
    public Response triggerIngest() {
        newsService.ingestAll();
        return Response.accepted(Map.of(
            "status", "triggered",
            "message", "News ingest started via Kafka + OpenSearch"
        )).build();
    }

    @POST
    @Path("/ingest/{category}")
    @Operation(summary = "Trigger category ingest via Apache Camel route")
    public Response triggerCategoryIngest(@PathParam("category") String category) {
        producerTemplate.sendBodyAndHeader("direct:fetchCategory", null, "category", category);
        return Response.accepted(Map.of(
            "status", "triggered",
            "category", category,
            "message", "Routed through Apache Camel direct route"
        )).build();
    }

    @GET
    @Path("/status")
    @Operation(summary = "System status check")
    public Response getStatus() {
        return Response.ok(Map.of(
            "service", "Real-Time News Aggregation System",
            "framework", "Quarkus 3.8.4",
            "version", "1.0.0",
            "sources", List.of("NewsAPI.org", "GNews.io"),
            "kafka", "localhost:9092",
            "kafdrop", "http://localhost:9000",
            "opensearch", "localhost:9200",
            "swagger", "http://localhost:8080/swagger-ui",
            "graphql", "http://localhost:8080/graphql-ui"
        )).build();
    }
}
