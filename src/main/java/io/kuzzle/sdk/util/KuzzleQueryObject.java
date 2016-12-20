package io.kuzzle.sdk.util;

import org.json.JSONObject;

import java.util.Date;

import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * The type Query object.
 */
public class KuzzleQueryObject {

  private JSONObject query;
  private String action;
  private Options options;
  private OnQueryDoneListener cb;
  private Date  timestamp;

  /**
   * Gets cb.
   *
   * @return the cb
   */
  public OnQueryDoneListener getCb() {
    return cb;
  }

  /**
   * Sets cb.
   *
   * @param cb the cb
   */
  public void setCb(OnQueryDoneListener cb) {
    this.cb = cb;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public Options getOptions() {
    return options;
  }

  public void setOptions(Options options) {
    this.options = options;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public JSONObject getQuery() {
    return query;
  }

  public void setQuery(JSONObject query) {
    this.query = query;
  }
}
