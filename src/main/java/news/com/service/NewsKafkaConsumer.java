package news.com.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import news.com.model.NewsArticle;
import news.com.repository.NewsOpenSearchRepository;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NewsKafkaConsumer {

    private static final Logger LOG = Logger.getLogger(NewsKafkaConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    NewsOpenSearchRepository repository;

    @Incoming("news-in")
    @Blocking
    public void consume(String json) {
        try {
            NewsArticle article = objectMapper.readValue(json, NewsArticle.class);
            repository.indexArticle(article);
            LOG.infof("Consumed & indexed: id=%s source=%s", article.getId(), article.getSourceName());
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to deserialise Kafka message");
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error processing Kafka message");
        }
    }
}
