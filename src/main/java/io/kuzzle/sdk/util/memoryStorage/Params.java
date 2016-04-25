package io.kuzzle.sdk.util.memoryStorage;

import java.util.HashMap;
import java.util.Map;

public abstract class Params {

  protected Map<String, Object> params;

  @SuppressWarnings("unchecked")
  public <T> T getParam(String name) {
    if (params == null) return null;

    return (T) params.get(name);
  }

  public boolean contains(String name) {
    if (params == null) return false;

    return params.containsKey(name);
  }

  protected void addParam(String name, Object value) {
    if (params == null) {
      params = new HashMap<String, Object>();
    }
    params.put(name, value);
  }

  protected void addParam(String name) {
    if (params == null) {
      params = new HashMap<String, Object>();
    }
    params.put(name, null);
  }

}