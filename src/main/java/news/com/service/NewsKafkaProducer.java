package news.com.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import news.com.model.NewsArticle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NewsKafkaProducer {

    private static final Logger LOG = Logger.getLogger(NewsKafkaProducer.class);

    @Inject
    @Channel("news-out")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    public void publish(NewsArticle article) {
        try {
            String json = objectMapper.writeValueAsString(article);
            emitter.send(json);
            LOG.debugf("Published to Kafka: id=%s title='%s'", article.getId(), article.getTitle());
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to serialise article for Kafka: id=%s", article.getId());
        }
    }
}
