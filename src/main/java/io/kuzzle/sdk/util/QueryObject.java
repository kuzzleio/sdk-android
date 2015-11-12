package io.kuzzle.sdk.util;

import org.json.JSONObject;

import io.kuzzle.sdk.listeners.ResponseListener;

/**
 * The type Query object.
 */
public class QueryObject {

  private JSONObject object;
  private String controller;
  private ResponseListener cb;

  /**
   * Gets object.
   *
   * @return the object
   */
  public JSONObject getObject() {
    return object;
  }

  /**
   * Sets object.
   *
   * @param object the object
   */
  public void setObject(JSONObject object) {
    this.object = object;
  }

  /**
   * Gets controller.
   *
   * @return the controller
   */
  public String getController() {
    return controller;
  }

  /**
   * Sets controller.
   *
   * @param controller the controller
   */
  public void setController(String controller) {
    this.controller = controller;
  }

  /**
   * Gets cb.
   *
   * @return the cb
   */
  public ResponseListener getCb() {
    return cb;
  }

  /**
   * Sets cb.
   *
   * @param cb the cb
   */
  public void setCb(ResponseListener cb) {
    this.cb = cb;
  }
}
