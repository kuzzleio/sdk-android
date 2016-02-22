package io.kuzzle.sdk.responses;

import org.json.JSONException;
import org.json.JSONObject;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.enums.Scope;
import io.kuzzle.sdk.enums.State;

public class KuzzleNotificationResponse {
  private int status;
  private JSONObject error;
  private String  index;
  private String  collection;
  private String  controller;
  private String  action;
  private State   state;
  private Scope   scope;
  private JSONObject  metadata;
  private String requestId;
  private KuzzleDocument document;
  private JSONObject  result;

  public KuzzleNotificationResponse(final Kuzzle kuzzle, final JSONObject object) {
    try {
      this.status = object.getInt("status");
      this.error = (object.isNull("error") ? null : object.getJSONObject("error"));
      this.index = object.getString("index");
      this.collection = object.getString("collection");
      this.controller = object.getString("controller");
      this.action = object.getString("action");
      this.state = State.valueOf(object.getString("state").toUpperCase());
      this.metadata = object.getJSONObject("metadata");
      this.requestId = object.getString("requestId");
      this.result = (object.isNull("result") ? null : object.getJSONObject("result"));
      this.scope = (this.result.isNull("scope") ? null : Scope.valueOf(this.result.getString("scope").toUpperCase()));
      if (!object.getJSONObject("result").isNull("_source")) {
        this.document = new KuzzleDocument(new KuzzleDataCollection(kuzzle, this.index, this.collection), object.getJSONObject("result"));
        this.document.setId(this.result.getString("_id"));
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public int getStatus() {
    return status;
  }

  public JSONObject getError() {
    return error;
  }

  public String getIndex() {
    return index;
  }

  public String getCollection() {
    return collection;
  }

  public String getController() {
    return controller;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public Scope getScope() {
    return scope;
  }

  public void setScope(Scope scope) {
    this.scope = scope;
  }

  public JSONObject getMetadata() {
    return metadata;
  }

  public void setMetadata(JSONObject metadata) {
    this.metadata = metadata;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public KuzzleDocument getDocument() {
    return document;
  }

  public void setDocument(KuzzleDocument document) {
    this.document = document;
  }

  public JSONObject getResult() {
    return result;
  }

  public void setResult(JSONObject result) {
    this.result = result;
  }
}
