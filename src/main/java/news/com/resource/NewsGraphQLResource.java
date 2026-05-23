package news.com.resource;

import news.com.model.NewsArticle;
import news.com.model.NewsSearchRequest;
import news.com.model.NewsSearchResult;
import news.com.repository.NewsOpenSearchRepository;
import news.com.service.NewsService;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import jakarta.inject.Inject;
import java.util.List;

@GraphQLApi
public class NewsGraphQLResource {

    @Inject
    NewsService newsService;

    @Inject
    NewsOpenSearchRepository repository;

    @Query("latestNews")
    @Description("Get latest news articles")
    public List<NewsArticle> getLatestNews(
            @Name("size") @DefaultValue("10") int size) {
        return newsService.getLatest(size);
    }

    @Query("searchNews")
    @Description("Search news with filters")
    public NewsSearchResult searchNews(
            @Name("query")    @DefaultValue("") String query,
            @Name("category") @DefaultValue("") String category,
            @Name("fromDate") @DefaultValue("") String fromDate,
            @Name("toDate")   @DefaultValue("") String toDate,
            @Name("page")     @DefaultValue("0") int page,
            @Name("size")     @DefaultValue("10") int size) {
        NewsSearchRequest req = new NewsSearchRequest();
        req.setQuery(query);
        req.setCategory(category);
        req.setFromDate(fromDate);
        req.setToDate(toDate);
        req.setPage(page);
        req.setSize(size);
        return newsService.search(req);
    }

    @Query("categories")
    @Description("Get all available news categories")
    public List<String> getCategories() {
        return newsService.getCategories();
    }

    // ========== UMANG SERVICES QUERIES ==========

    @Query("umangServices")
    @Description("Search Umang services data")
    public String umangServices(
            @Name("query") @DefaultValue("") String query,
            @Name("size") @DefaultValue("10") int size,
            @Name("page") @DefaultValue("0") int page) {
        int from = page * size;
        return repository.searchUmangServices(query, size, from);
    }

    @Query("umangCount")
    @Description("Get total count of Umang services")
    public String umangCount() {
        return repository.getUmangCount();
    }

    @Query("umangExists")
    @Description("Check if Umang index exists")
    public boolean umangExists() {
        return repository.umangIndexExists();
    }
}
