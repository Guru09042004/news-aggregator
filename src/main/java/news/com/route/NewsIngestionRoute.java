package news.com.route;

import news.com.service.NewsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NewsIngestionRoute extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(NewsIngestionRoute.class);

    @ConfigProperty(name = "news.fetch.interval-ms", defaultValue = "300000")
    long intervalMs;

    @Inject
    NewsService newsService;

    @Override
    public void configure() {

        errorHandler(defaultErrorHandler()
            .maximumRedeliveries(3)
            .redeliveryDelay(5000));

        // Route 1: Har 5 minute mein sabhi sources se news fetch karo
        from("timer://news-poll?period=" + intervalMs)
            .routeId("news-fetch-route")
            .log("Camel Timer fired - starting news ingest")
            .process(exchange -> newsService.ingestAll())
            .log("Camel news ingest complete");

        // Route 2: App start hone ke 10 second baad ek baar fetch karo
        from("timer://startup-fetch?repeatCount=1&delay=10000")
            .routeId("news-startup-route")
            .log("Startup warm-up fetch triggered")
            .process(exchange -> newsService.ingestAll())
            .log("Startup fetch complete");

        // Route 3: On-demand category fetch
        from("direct:fetchCategory")
            .routeId("news-category-route")
            .log("On-demand category fetch: ${header.category}")
            .process(exchange -> {
                String category = exchange.getMessage().getHeader("category", String.class);
                var articles = newsService.ingestCategory(category);
                exchange.getMessage().setBody(articles);
            })
            .log("On-demand fetch complete");
    }
}
