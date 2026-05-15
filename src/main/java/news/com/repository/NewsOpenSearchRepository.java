package news.com.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import news.com.model.NewsArticle;
import news.com.model.NewsSearchRequest;
import news.com.model.NewsSearchResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class NewsOpenSearchRepository {

    private static final Logger LOG = Logger.getLogger(NewsOpenSearchRepository.class);

    @Inject
    RestClient restClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "news.opensearch.index", defaultValue = "news-articles")
    String indexName;

    public void ensureIndexExists() {
        try {
            Request headRequest = new Request("HEAD", "/" + indexName);
            Response response = restClient.performRequest(headRequest);
            if (response.getStatusLine().getStatusCode() == 200) {
                LOG.infof("Index '%s' already exists.", indexName);
                return;
            }
        } catch (Exception e) {
            LOG.infof("Index '%s' not found - creating...", indexName);
        }
        try {
            Request createRequest = new Request("PUT", "/" + indexName);
            createRequest.setJsonEntity(buildIndexMapping());
            restClient.performRequest(createRequest);
            LOG.infof("Index '%s' created.", indexName);
        } catch (IOException e) {
            LOG.errorf(e, "Failed to create index '%s'", indexName);
        }
    }

    public void indexArticle(NewsArticle article) {
        try {
            String json = objectMapper.writeValueAsString(article);
            Request request = new Request("PUT", "/" + indexName + "/_doc/" + article.getId());
            request.setJsonEntity(json);
            restClient.performRequest(request);
            LOG.debugf("Indexed article id=%s", article.getId());
        } catch (IOException e) {
            LOG.errorf(e, "Failed to index article id=%s", article.getId());
        }
    }

    public void indexArticles(List<NewsArticle> articles) {
        if (articles == null || articles.isEmpty()) return;
        StringBuilder bulk = new StringBuilder();
        for (NewsArticle article : articles) {
            try {
                bulk.append("{\"index\":{\"_index\":\"").append(indexName)
                    .append("\",\"_id\":\"").append(article.getId()).append("\"}}\n");
                bulk.append(objectMapper.writeValueAsString(article)).append("\n");
            } catch (IOException e) {
                LOG.warnf(e, "Skipping article id=%s", article.getId());
            }
        }
        try {
            Request request = new Request("POST", "/_bulk");
            request.setJsonEntity(bulk.toString());
            restClient.performRequest(request);
            LOG.infof("Bulk-indexed %d articles.", articles.size());
        } catch (IOException e) {
            LOG.errorf(e, "Bulk indexing failed");
        }
    }

    public NewsSearchResult search(NewsSearchRequest req) {
        try {
            String queryJson = buildSearchQuery(req);
            int from = req.getPage() * req.getSize();
            Request request = new Request("POST",
                "/" + indexName + "/_search?from=" + from + "&size=" + req.getSize());
            request.setJsonEntity(queryJson);
            Response response = restClient.performRequest(request);
            return parseSearchResponse(response, req.getPage(), req.getSize());
        } catch (IOException e) {
            LOG.errorf(e, "Search failed");
            return new NewsSearchResult(0, req.getPage(), req.getSize(), List.of());
        }
    }

    public List<NewsArticle> findLatest(int size) {
        try {
            String queryJson = "{\"sort\":[{\"publishedAt\":{\"order\":\"desc\"}}],\"size\":" + size + "}";
            Request request = new Request("POST", "/" + indexName + "/_search");
            request.setJsonEntity(queryJson);
            Response response = restClient.performRequest(request);
            return parseSearchResponse(response, 0, size).getArticles();
        } catch (IOException e) {
            LOG.errorf(e, "findLatest failed");
            return List.of();
        }
    }

    private String buildSearchQuery(NewsSearchRequest req) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode bool = objectMapper.createObjectNode();
        ArrayNode must = objectMapper.createArrayNode();
        ArrayNode filter = objectMapper.createArrayNode();

        if (req.getQuery() != null && !req.getQuery().isBlank()) {
            ObjectNode multiMatch = objectMapper.createObjectNode();
            ObjectNode mm = objectMapper.createObjectNode();
            mm.put("query", req.getQuery());
            ArrayNode fields = objectMapper.createArrayNode();
            fields.add("title^3"); fields.add("description^2"); fields.add("content");
            mm.set("fields", fields);
            mm.put("type", "best_fields");
            multiMatch.set("multi_match", mm);
            must.add(multiMatch);
        } else {
            ObjectNode matchAll = objectMapper.createObjectNode();
            matchAll.set("match_all", objectMapper.createObjectNode());
            must.add(matchAll);
        }

        addTermFilter(filter, "category", req.getCategory());
        addTermFilter(filter, "provider", req.getProvider());

        if (req.getFromDate() != null || req.getToDate() != null) {
            ObjectNode range = objectMapper.createObjectNode();
            ObjectNode rangeField = objectMapper.createObjectNode();
            if (req.getFromDate() != null) rangeField.put("gte", req.getFromDate());
            if (req.getToDate() != null) rangeField.put("lte", req.getToDate());
            range.set("publishedAt", rangeField);
            ObjectNode rangeQuery = objectMapper.createObjectNode();
            rangeQuery.set("range", range);
            filter.add(rangeQuery);
        }

        bool.set("must", must);
        if (!filter.isEmpty()) bool.set("filter", filter);
        query.set("bool", bool);
        root.set("query", query);

        ArrayNode sort = objectMapper.createArrayNode();
        ObjectNode sortField = objectMapper.createObjectNode();
        sortField.put("publishedAt", "desc");
        sort.add(sortField);
        root.set("sort", sort);

        return objectMapper.writeValueAsString(root);
    }

    private void addTermFilter(ArrayNode filter, String field, String value) {
        if (value == null || value.isBlank()) return;
        ObjectNode term = objectMapper.createObjectNode();
        ObjectNode termValue = objectMapper.createObjectNode();
        termValue.put(field, value);
        term.set("term", termValue);
        filter.add(term);
    }

    private NewsSearchResult parseSearchResponse(Response response, int page, int size) throws IOException {
        try (InputStream is = response.getEntity().getContent()) {
            JsonNode root = objectMapper.readTree(is);
            long totalHits = root.path("hits").path("total").path("value").asLong(0);
            ArrayNode hitsArr = (ArrayNode) root.path("hits").path("hits");
            List<NewsArticle> articles = new ArrayList<>();
            for (JsonNode hit : hitsArr) {
                JsonNode source = hit.path("_source");
                NewsArticle article = objectMapper.treeToValue(source, NewsArticle.class);
                articles.add(article);
            }
            return new NewsSearchResult(totalHits, page, size, articles);
        }
    }

    private String buildIndexMapping() {
        return "{\"settings\":{\"number_of_shards\":1,\"number_of_replicas\":0}," +
               "\"mappings\":{\"properties\":{" +
               "\"id\":{\"type\":\"keyword\"}," +
               "\"title\":{\"type\":\"text\"}," +
               "\"description\":{\"type\":\"text\"}," +
               "\"content\":{\"type\":\"text\"}," +
               "\"url\":{\"type\":\"keyword\"}," +
               "\"publishedAt\":{\"type\":\"date\"}," +
               "\"indexedAt\":{\"type\":\"date\"}," +
               "\"sourceName\":{\"type\":\"keyword\"}," +
               "\"provider\":{\"type\":\"keyword\"}," +
               "\"category\":{\"type\":\"keyword\"}," +
               "\"country\":{\"type\":\"keyword\"}," +
               "\"language\":{\"type\":\"keyword\"}" +
               "}}}";
    }
}
