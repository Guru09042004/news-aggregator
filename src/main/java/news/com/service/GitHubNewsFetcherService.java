package news.com.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class GitHubNewsFetcherService {

    private static final Logger LOG = Logger.getLogger(GitHubNewsFetcherService.class);

    @ConfigProperty(name = "news.api.github.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<NewsArticle> fetchIndiaNews() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.warnf("GitHub News API returned HTTP %d", response.statusCode());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<NewsArticle> articles = new ArrayList<>();

            // Check if response has articles array
            if (root.has("articles") && root.get("articles").isArray()) {
                JsonNode articlesArray = root.get("articles");
                
                for (JsonNode item : articlesArray) {
                    NewsArticle article = new NewsArticle();
                    
                    // Map fields from GitHub API to NewsArticle
                    article.setTitle(getText(item, "title"));
                    article.setDescription(getText(item, "description"));
                    article.setUrl(getText(item, "url"));
                    article.setImageUrl(getText(item, "urlToImage"));
                    article.setSourceName(getSourceName(item));
                    article.setProvider("github-india");
                    article.setCategory("general");
                    article.setCountry("in");
                    article.setLanguage("en");
                    
                    // Parse publishedAt date
                    String publishedAt = getText(item, "publishedAt");
                    if (publishedAt != null && !publishedAt.isEmpty()) {
                        try {
                            article.setPublishedAt(Instant.parse(publishedAt));
                        } catch (Exception e) {
                            article.setPublishedAt(Instant.now());
                        }
                    } else {
                        article.setPublishedAt(Instant.now());
                    }
                    
                    articles.add(article);
                }
            }

            LOG.infof("GitHub India News API: fetched %d articles", articles.size());
            return articles;

        } catch (Exception e) {
            LOG.errorf(e, "GitHub News API fetch failed");
            return List.of();
        }
    }

    private String getText(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }

    private String getSourceName(JsonNode item) {
        if (item.has("source") && !item.get("source").isNull()) {
            JsonNode source = item.get("source");
            if (source.has("name") && !source.get("name").isNull()) {
                return source.get("name").asText();
            }
        }
        return "Unknown Source";
    }
}
