package io.kuzzle.sdk.util.memoryStorage;

import java.util.ArrayList;
import java.util.List;

public class ZParams {

  public enum Aggregate {
    SUM, MIN, MAX
  }

  private List<Double> params = new ArrayList<Double>();

  public ZParams weights(final Double... weights) {
    for (final Double weight : weights) {
      params.add(weight);
    }
    return this;
  }

}
