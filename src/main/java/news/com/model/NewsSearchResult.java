package news.com.model;

import java.util.List;

public class NewsSearchResult {
    private long totalHits;
    private int page;
    private int size;
    private List<NewsArticle> articles;

    public NewsSearchResult() {}
    public NewsSearchResult(long totalHits, int page, int size, List<NewsArticle> articles) {
        this.totalHits = totalHits;
        this.page = page;
        this.size = size;
        this.articles = articles;
    }

    public long getTotalHits() { return totalHits; }
    public void setTotalHits(long totalHits) { this.totalHits = totalHits; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public List<NewsArticle> getArticles() { return articles; }
    public void setArticles(List<NewsArticle> articles) { this.articles = articles; }
}
