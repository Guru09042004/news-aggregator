package news.com.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import news.com.model.GNewsResponse;
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
public class GNewsFetcherService {

    private static final Logger LOG = Logger.getLogger(GNewsFetcherService.class);

    @ConfigProperty(name = "news.api.gnews.base-url", defaultValue = "https://gnews.io/api/v4")
    String baseUrl;

    @ConfigProperty(name = "news.api.gnews.api-key")
    String apiKey;

    @ConfigProperty(name = "news.api.gnews.country", defaultValue = "in")
    String country;

    @ConfigProperty(name = "news.api.gnews.lang", defaultValue = "en")
    String lang;

    @ConfigProperty(name = "news.api.gnews.max", defaultValue = "10")
    int max;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<NewsArticle> fetchTopHeadlines(String topic) {
        try {
            StringBuilder url = new StringBuilder(baseUrl)
                .append("/top-headlines?country=").append(country)
                .append("&lang=").append(lang)
                .append("&max=").append(max)
                .append("&token=").append(apiKey);
            if (topic != null && !topic.isBlank()) {
                url.append("&topic=").append(topic);
            }
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("Accept", "application/json")
                .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warnf("GNews returned HTTP %d", response.statusCode());
                return Collections.emptyList();
            }
            GNewsResponse.Root root = objectMapper.readValue(response.body(), GNewsResponse.Root.class);
            if (root.getArticles() == null) return Collections.emptyList();
            List<NewsArticle> articles = root.getArticles().stream()
                .map(a -> toArticle(a, topic))
                .collect(Collectors.toList());
            LOG.infof("GNews: fetched %d articles (topic=%s)", articles.size(), topic);
            return articles;
        } catch (Exception e) {
            LOG.errorf(e, "GNews fetch failed (topic=%s)", topic);
            return Collections.emptyList();
        }
    }

    private NewsArticle toArticle(GNewsResponse.Article a, String topic) {
        NewsArticle article = new NewsArticle();
        article.setTitle(a.getTitle());
        article.setDescription(a.getDescription());
        article.setContent(a.getContent());
        article.setUrl(a.getUrl());
        article.setImageUrl(a.getImage());
        article.setSourceName(a.getSource() != null ? a.getSource().getName() : "Unknown");
        article.setProvider("gnews");
        article.setCategory(topic != null ? topic : "general");
        article.setCountry(country);
        article.setLanguage(lang);
        try {
            if (a.getPublishedAt() != null) article.setPublishedAt(Instant.parse(a.getPublishedAt()));
        } catch (Exception e) { article.setPublishedAt(Instant.now()); }
        return article;
    }
}
