package io.kuzzle.sdk.responses;


import java.util.List;

import io.kuzzle.sdk.security.AbstractKuzzleSecurityDocument;

public class KuzzleSecurityDocumentList implements KuzzleListInterface<AbstractKuzzleSecurityDocument> {
  private List<AbstractKuzzleSecurityDocument> documents;
  private long total;

  public KuzzleSecurityDocumentList(List<AbstractKuzzleSecurityDocument> roles, long total) {
    this.documents = roles;
    this.total = total;
  }

  public List<AbstractKuzzleSecurityDocument> getDocuments() {
    return documents;
  }

  public long getTotal() {
    return total;
  }
}
