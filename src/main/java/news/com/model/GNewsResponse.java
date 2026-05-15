package news.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public class GNewsResponse {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Root {
        private int totalArticles;
        private List<Article> articles;
        public int getTotalArticles() { return totalArticles; }
        public void setTotalArticles(int totalArticles) { this.totalArticles = totalArticles; }
        public List<Article> getArticles() { return articles; }
        public void setArticles(List<Article> articles) { this.articles = articles; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Article {
        private String title;
        private String description;
        private String content;
        private String url;
        private String image;
        private String publishedAt;
        private Source source;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
        public Source getSource() { return source; }
        public void setSource(Source source) { this.source = source; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        private String name;
        private String url;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
