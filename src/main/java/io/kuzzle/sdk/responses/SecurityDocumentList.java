package io.kuzzle.sdk.responses;


import java.util.List;

import io.kuzzle.sdk.security.AbstractSecurityDocument;

public class SecurityDocumentList implements KuzzleList<AbstractSecurityDocument> {
  private List<AbstractSecurityDocument> documents;
  private long total;

  public SecurityDocumentList(List<AbstractSecurityDocument> roles, long total) {
    this.documents = roles;
    this.total = total;
  }

  public List<AbstractSecurityDocument> getDocuments() {
    return documents;
  }

  public long getTotal() {
    return total;
  }
}
