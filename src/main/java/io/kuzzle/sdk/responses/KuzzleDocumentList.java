package io.kuzzle.sdk.responses;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.List;

import io.kuzzle.sdk.core.KuzzleDocument;

public class KuzzleDocumentList implements KuzzleListInterface<KuzzleDocument> {
  private List<KuzzleDocument> documents;
  private long total;
  private JSONObject aggregations;

  public KuzzleDocumentList(List<KuzzleDocument> documents, long total) {
    this(documents, total, null);
  }

  public KuzzleDocumentList(List<KuzzleDocument> documents, long total, JSONObject aggregations) {
    this.documents = documents;
    this.total = total;
    this.aggregations = aggregations;
  }

  public List<KuzzleDocument> getDocuments() {
    return documents;
  }

  public long getTotal() {
    return total;
  }

  public JSONObject getAggregations() {
    return aggregations;
  }
}
