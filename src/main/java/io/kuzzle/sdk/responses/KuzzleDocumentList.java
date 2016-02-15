package io.kuzzle.sdk.responses;

import java.util.List;

import io.kuzzle.sdk.core.KuzzleDocument;

public class KuzzleDocumentList implements KuzzleListInterface<KuzzleDocument> {
  private List<KuzzleDocument> documents;
  private long total;

  public KuzzleDocumentList(List<KuzzleDocument> documents, long total) {
    this.documents = documents;
    this.total = total;
  }

  public List<KuzzleDocument> getDocuments() {
    return documents;
  }

  public long getTotal() {
    return total;
  }
}
