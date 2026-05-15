package news.com.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import news.com.model.NewsApiResponse;
import news.com.model.NewsArticle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class NewsApiFetcherService {

    private static final Logger LOG = Logger.getLogger(NewsApiFetcherService.class);

    @ConfigProperty(name = "news.api.newsapi.base-url", defaultValue = "https://newsapi.org/v2")
    String baseUrl;

    @ConfigProperty(name = "news.api.newsapi.api-key")
    String apiKey;

    @ConfigProperty(name = "news.api.newsapi.country", defaultValue = "in")
    String country;

    @ConfigProperty(name = "news.api.newsapi.page-size", defaultValue = "20")
    int pageSize;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<NewsArticle> fetchTopHeadlines(String category) {
        try {
            StringBuilder url = new StringBuilder(baseUrl)
                .append("/top-headlines?country=").append(country)
                .append("&pageSize=").append(pageSize)
                .append("&apiKey=").append(apiKey);
            if (category != null && !category.isBlank()) {
                url.append("&category=").append(category);
            }
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("Accept", "application/json")
                .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warnf("NewsAPI returned HTTP %d", response.statusCode());
                return Collections.emptyList();
            }
            NewsApiResponse.Root root = objectMapper.readValue(response.body(), NewsApiResponse.Root.class);
            if (!"ok".equals(root.getStatus())) return Collections.emptyList();
            List<NewsArticle> articles = root.getArticles().stream()
                .map(a -> toArticle(a, category))
                .collect(Collectors.toList());
            LOG.infof("NewsAPI: fetched %d articles (category=%s)", articles.size(), category);
            return articles;
        } catch (Exception e) {
            LOG.errorf(e, "NewsAPI fetch failed (category=%s)", category);
            return Collections.emptyList();
        }
    }

    private NewsArticle toArticle(NewsApiResponse.Article a, String category) {
        NewsArticle article = new NewsArticle();
        article.setTitle(a.getTitle());
        article.setDescription(a.getDescription());
        article.setContent(a.getContent());
        article.setUrl(a.getUrl());
        article.setImageUrl(a.getUrlToImage());
        article.setAuthor(a.getAuthor());
        article.setSourceName(a.getSource() != null ? a.getSource().getName() : "Unknown");
        article.setProvider("newsapi");
        article.setCategory(category != null ? category : "general");
        article.setCountry(country);
        article.setLanguage("en");
        try {
            if (a.getPublishedAt() != null) article.setPublishedAt(Instant.parse(a.getPublishedAt()));
        } catch (Exception e) { article.setPublishedAt(Instant.now()); }
        return article;
    }
}
