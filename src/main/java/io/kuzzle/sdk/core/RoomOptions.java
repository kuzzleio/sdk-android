package io.kuzzle.sdk.core;

import org.json.JSONObject;

import io.kuzzle.sdk.enums.Scope;
import io.kuzzle.sdk.enums.State;
import io.kuzzle.sdk.enums.Users;

public class RoomOptions {

  private boolean subscribeToSelf = true;
  private JSONObject  _volatile = new JSONObject();
  private Scope scope = Scope.ALL;
  private State state = State.DONE;
  private Users users = Users.NONE;

  /**
   * Is subscribe to self boolean.
   *
   * @return the boolean
   */
  public boolean isSubscribeToSelf() {
    return subscribeToSelf;
  }

  /**
   * Sets subscribe to self.
   *
   * @param subscribeToSelf the subscribe to self
   */
  public RoomOptions setSubscribeToSelf(boolean subscribeToSelf) {
    this.subscribeToSelf = subscribeToSelf;

    return this;
  }

  public JSONObject getVolatile() {
    return _volatile;
  }

  public RoomOptions setVolatile(JSONObject _volatile) {
    this._volatile = _volatile;

    return this;
  }

  public Scope getScope() {
    return scope;
  }

  public RoomOptions setScope(Scope scope) {
    this.scope = scope;

    return this;
  }

  public State getState() {
    return state;
  }

  public RoomOptions setState(State state) {
    this.state = state;

    return this;
  }

  public Users getUsers() {
    return users;
  }

  public RoomOptions setUsers(Users users) {
    this.users = users;

    return this;
  }

}
