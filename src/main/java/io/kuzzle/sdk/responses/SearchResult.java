package io.kuzzle.sdk.responses;

import org.json.JSONObject;

import java.util.List;

import io.kuzzle.sdk.core.Document;
import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Options;

public class SearchResult implements KuzzleList<Document> {
  private Collection collection;
  private long total;
  private List<Document> documents;
  private JSONObject aggregations;
  private Options searchArgs;
  private long fetchedDocument;

  public SearchResult(Collection collection, long total, List<Document> documents) {
    this(collection, total, documents, null, new Options(), null);
  }

  public SearchResult(Collection collection, long total, List<Document> documents, JSONObject aggregations) {
    this(collection, total, documents, aggregations, new Options(), null);
  }

  public SearchResult(Collection collection, long total, List<Document> documents, JSONObject aggregations, Options searchArgs, SearchResult previous) {
    this.collection = collection;
    this.total = total;
    this.documents = documents;
    this.aggregations = aggregations;
    this.searchArgs = searchArgs;
    this.fetchedDocument = previous != null ? documents.size() + previous.getFetchedDocument() : documents.size();
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

  public Options getSearchArgs() {
    return searchArgs;
  }

  public long getFetchedDocument() {
    return fetchedDocument;
  }

  public Collection getCollection() {
    return collection;
  }

  /**
   * @return TODO replace this mock with real implementation once scroll is implemented
   */
  public SearchResult fetchNext() {
    return this;
  }
}
