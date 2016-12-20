package io.kuzzle.sdk.helpers;

import org.json.JSONObject;

/**
 * The type Kuzzle filter helper.
 */
public class FilterHelper {

  private JSONObject filter = new JSONObject();

  /**
   * And kuzzle filter helper.
   *
   * @return the kuzzle filter helper
   */
  public FilterHelper and() {
    return this;
  }

}
