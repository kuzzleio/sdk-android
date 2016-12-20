package io.kuzzle.sdk.responses;

import org.json.JSONObject;

import java.util.List;

import io.kuzzle.sdk.core.Document;

public class DocumentList implements KuzzleList<Document> {
  private List<Document> documents;
  private long total;
  private JSONObject aggregations;

  public DocumentList(List<Document> documents, long total) {
    this(documents, total, null);
  }

  public DocumentList(List<Document> documents, long total, JSONObject aggregations) {
    this.documents = documents;
    this.total = total;
    this.aggregations = aggregations;
  }

  public List<Document> getDocuments() {
    return documents;
  }

  public long getTotal() {
    return total;
  }

  public JSONObject getAggregations() {
    return aggregations;
  }
}
