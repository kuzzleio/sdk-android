package io.kuzzle.sdk.responses;

import java.util.List;

public interface KuzzleListInterface<T> {
  public List<T> getDocuments();
  public long getTotal();
}
