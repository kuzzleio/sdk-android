package io.kuzzle.sdk.responses;

import org.json.JSONException;
import org.json.JSONObject;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Document;
import io.kuzzle.sdk.enums.Scope;
import io.kuzzle.sdk.enums.State;
import io.kuzzle.sdk.enums.Users;

public class NotificationResponse {
  private int status;
  private String  index;
  private String  collection;
  private String  controller;
  private String  action;
  private State   state;
  private Scope   scope;
  private Users   users;
  private JSONObject  _volatile;
  private String requestId;
  private Document document;
  private JSONObject  result;

  public NotificationResponse(final Kuzzle kuzzle, final JSONObject object) {
    try {
      this.status = object.getInt("status");
      this.index = object.getString("index");
      this.collection = object.getString("collection");
      this.controller = object.getString("controller");
      this.action = object.getString("action");
      this.state = (object.isNull("state") ? null : State.valueOf(object.getString("state").toUpperCase()));
      this._volatile = object.getJSONObject("volatile");
      this.requestId = object.isNull("requestId") ? null : object.getString("requestId");
      this.result = (object.isNull("result") ? null : object.getJSONObject("result"));
      this.scope = (object.isNull("scope") ? null : Scope.valueOf(object.getString("scope").toUpperCase()));
      this.users = (object.isNull("user") ? null : Users.valueOf(object.getString("user").toUpperCase()));
      if (!object.getJSONObject("result").isNull("_source")) {
        this.document = new Document(new Collection(kuzzle, this.collection, this.index), object.getJSONObject("result"));
        this.document.setId(this.result.getString("_id"));
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public int getStatus() {
    return status;
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

  public JSONObject getVolatile() {
    return _volatile;
  }

  public void setVolatile(JSONObject _volatile) {
    this._volatile = _volatile;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Document getDocument() {
    return document;
  }

  public void setDocument(Document document) {
    this.document = document;
  }

  public JSONObject getResult() {
    return result;
  }

  public void setResult(JSONObject result) {
    this.result = result;
  }

  public Users getUsers() {
    return users;
  }

  public void setUsers(Users users) {
    this.users = users;
  }
}
