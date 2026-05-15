package news.com.service;

import news.com.model.NewsArticle;
import news.com.model.NewsSearchRequest;
import news.com.model.NewsSearchResult;
import news.com.repository.NewsOpenSearchRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class NewsService {

    private static final Logger LOG = Logger.getLogger(NewsService.class);

    private static final List<String> CATEGORIES = Arrays.asList(
        "general", "business", "technology", "sports", "entertainment", "science", "health"
    );

    @Inject
    NewsApiFetcherService newsApiFetcher;

    @Inject
    GNewsFetcherService gNewsFetcher;

    @Inject
    GitHubNewsFetcherService gitHubNewsFetcher;

    @Inject
    NewsKafkaProducer kafkaProducer;

    @Inject
    NewsOpenSearchRepository searchRepository;

    public void ingestAll() {
        LOG.info("Starting full news ingest cycle...");
        List<NewsArticle> all = new ArrayList<>();
        
        // Fetch from GNews (NewsAPI is not working)
        for (String category : CATEGORIES) {
            all.addAll(gNewsFetcher.fetchTopHeadlines(category));
        }
        
        // Fetch from GitHub India News API
        all.addAll(gitHubNewsFetcher.fetchIndiaNews());
        
        LOG.infof("Fetched %d total articles. Publishing to Kafka...", all.size());
        all.forEach(kafkaProducer::publish);
        searchRepository.indexArticles(all);
        LOG.infof("Ingest cycle complete. %d articles published.", all.size());
    }

    public List<NewsArticle> ingestCategory(String category) {
        List<NewsArticle> articles = new ArrayList<>();
        articles.addAll(newsApiFetcher.fetchTopHeadlines(category));
        articles.addAll(gNewsFetcher.fetchTopHeadlines(category));
        if ("general".equals(category)) {
            articles.addAll(gitHubNewsFetcher.fetchIndiaNews());
        }
        articles.forEach(kafkaProducer::publish);
        searchRepository.indexArticles(articles);
        LOG.infof("Ingested %d articles for category: %s", articles.size(), category);
        return articles;
    }

    public NewsSearchResult search(NewsSearchRequest request) {
        return searchRepository.search(request);
    }

    public List<NewsArticle> getLatest(int size) {
        return searchRepository.findLatest(Math.min(size, 50));
    }

    public List<String> getCategories() {
        return CATEGORIES;
    }
}
