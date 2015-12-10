package io.kuzzle.sdk.core;

import org.json.JSONObject;

public class KuzzleRoomOptions {

  private boolean subscribeToSelf = true;
  private boolean listeningToConnections = false;
  private boolean listeningToDisconnections = false;
  private JSONObject  metadata = new JSONObject();

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
  public void setSubscribeToSelf(boolean subscribeToSelf) {
    this.subscribeToSelf = subscribeToSelf;
  }

  /**
   * Is listening to connections boolean.
   *
   * @return the boolean
   */
  public boolean isListeningToConnections() {
    return listeningToConnections;
  }

  /**
   * Sets listening to connections.
   *
   * @param listeningToConnections the listening to connections
   */
  public void setListeningToConnections(boolean listeningToConnections) {
    this.listeningToConnections = listeningToConnections;
  }

  /**
   * Is listening to disconnections boolean.
   *
   * @return the boolean
   */
  public boolean isListeningToDisconnections() {
    return listeningToDisconnections;
  }

  /**
   * Sets listening to disconnections.
   *
   * @param listeningToDisconnections the listening to disconnections
   */
  public void setListeningToDisconnections(boolean listeningToDisconnections) {
    this.listeningToDisconnections = listeningToDisconnections;
  }

  public JSONObject getMetadata() {
    return metadata;
  }

  public void setMetadata(JSONObject metadata) {
    this.metadata = metadata;
  }
}
