package news.com.resource;

import news.com.model.NewsArticle;
import news.com.model.NewsSearchRequest;
import news.com.model.NewsSearchResult;
import news.com.service.NewsService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.*;

import java.util.List;

@GraphQLApi
public class NewsGraphQLResource {

    @Inject
    NewsService newsService;

    @Query("latestNews")
    @Description("Get most recently published news articles")
    public List<NewsArticle> getLatestNews(
            @Name("size") @DefaultValue("10") int size) {
        return newsService.getLatest(size);
    }

    @Query("searchNews")
    @Description("Full-text search with filters")
    public NewsSearchResult searchNews(
            @Name("query")    @DefaultValue("") String query,
            @Name("category") @DefaultValue("") String category,
            @Name("provider") @DefaultValue("") String provider,
            @Name("fromDate") @DefaultValue("") String fromDate,
            @Name("toDate")   @DefaultValue("") String toDate,
            @Name("page")     @DefaultValue("0") int page,
            @Name("size")     @DefaultValue("10") int size) {
        NewsSearchRequest req = new NewsSearchRequest();
        req.setQuery(nullIfBlank(query));
        req.setCategory(nullIfBlank(category));
        req.setProvider(nullIfBlank(provider));
        req.setFromDate(nullIfBlank(fromDate));
        req.setToDate(nullIfBlank(toDate));
        req.setPage(page);
        req.setSize(Math.min(size, 50));
        return newsService.search(req);
    }

    @Query("newsByCategory")
    @Description("Get news articles for a specific category")
    public List<NewsArticle> getNewsByCategory(
            @Name("category") String category,
            @Name("size") @DefaultValue("10") int size) {
        NewsSearchRequest req = new NewsSearchRequest();
        req.setCategory(category);
        req.setSize(Math.min(size, 50));
        return newsService.search(req).getArticles();
    }

    @Query("newsCategories")
    @Description("List all supported news categories")
    public List<String> getNewsCategories() {
        return newsService.getCategories();
    }

    @Query("newsByProvider")
    @Description("Get news from a specific provider: newsapi or gnews")
    public List<NewsArticle> getNewsByProvider(
            @Name("provider") String provider,
            @Name("size") @DefaultValue("10") int size) {
        NewsSearchRequest req = new NewsSearchRequest();
        req.setProvider(provider);
        req.setSize(Math.min(size, 50));
        return newsService.search(req).getArticles();
    }

    @Mutation("triggerIngest")
    @Description("Manually trigger news ingest from all sources")
    public String triggerIngest() {
        newsService.ingestAll();
        return "Ingest triggered! Articles being fetched via Kafka and OpenSearch.";
    }

    @Mutation("triggerCategoryIngest")
    @Description("Trigger news ingest for a specific category")
    public String triggerCategoryIngest(@Name("category") String category) {
        List<NewsArticle> articles = newsService.ingestCategory(category);
        return String.format("Ingested %d articles for category: %s", articles.size(), category);
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
