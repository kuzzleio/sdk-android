package io.kuzzle.sdk.responses;

import java.util.List;

import io.kuzzle.sdk.core.KuzzleDocument;

public class KuzzleDocumentList {
  private List<KuzzleDocument> documents;
  private long total;

  public KuzzleDocumentList(List<KuzzleDocument> hints, long total) {
    this.documents = hints;
    this.total = total;
  }

  public List<KuzzleDocument> getDocuments() {
    return documents;
  }

  public long getTotal() {
    return total;
  }
}
