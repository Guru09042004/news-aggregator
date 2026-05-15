package news.com.model;

public class NewsSearchRequest {
    private String query;
    private String category;
    private String provider;
    private String fromDate;
    private String toDate;
    private int size = 10;
    private int page = 0;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
}
