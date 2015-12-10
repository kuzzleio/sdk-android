package io.kuzzle.sdk.core;

import org.json.JSONObject;

import io.kuzzle.sdk.enums.Mode;

public class KuzzleOptions {

  private boolean autoReconnect = false;
  private JSONObject headers;
  private boolean updateIfExists = false;
  private JSONObject metadata;
  private Mode connect = Mode.AUTO;
  private long reconnectionDelay = 1000;
  private Mode offlineMode;
  private int queueTTL = 120000;
  private boolean autoReplay = false;
  private int replayInterval = 10;
  private boolean queuable = true;
  private int queueMaxSize = 500;
  private boolean autoQueue = false;

  /**
   * Is auto reconnect boolean.
   *
   * @return the boolean
   */
  public boolean isAutoReconnect() {
    return autoReconnect;
  }

  /**
   * Sets auto reconnect.
   *
   * @param autoReconnect the auto reconnect
   */
  public void setAutoReconnect(boolean autoReconnect) {
    this.autoReconnect = autoReconnect;
  }

  /**
   * Gets headers.
   *
   * @return the headers
   */
  public JSONObject getHeaders() {
    return headers;
  }

  /**
   * Sets headers.
   *
   * @param headers the headers
   */
  public void setHeaders(JSONObject headers) {
    this.headers = headers;
  }

  /**
   * Is update if exists boolean.
   *
   * @return the boolean
   */
  public boolean isUpdateIfExists() {
    return updateIfExists;
  }

  /**
   * Sets update if exists.
   *
   * @param updateIfExists the update if exists
   */
  public void setUpdateIfExists(boolean updateIfExists) {
    this.updateIfExists = updateIfExists;
  }

  /**
   * Gets metadata.
   *
   * @return the metadata
   */
  public JSONObject getMetadata() {
    return metadata;
  }

  /**
   * Sets metadata.
   *
   * @param metadata the metadata
   */
  public void setMetadata(JSONObject metadata) {
    this.metadata = metadata;
  }

  public Mode getConnect() {
    return connect;
  }

  public void setConnect(Mode connect) {
    this.connect = connect;
  }

  public long getReconnectionDelay() {
    return reconnectionDelay;
  }

  public void setReconnectionDelay(long reconnectionDelay) {
    this.reconnectionDelay = reconnectionDelay;
  }

  public Mode getOfflineMode() {
    return offlineMode;
  }

  public void setOfflineMode(Mode offlineMode) {
    this.offlineMode = offlineMode;
  }

  public int getQueueTTL() {
    return queueTTL;
  }

  public void setQueueTTL(int queueTTL) {
    this.queueTTL = queueTTL;
  }

  public boolean isAutoReplay() {
    return autoReplay;
  }

  public void setAutoReplay(boolean autoReplay) {
    this.autoReplay = autoReplay;
  }

  public boolean isQueuable() {
    return queuable;
  }

  public void setQueuable(boolean queuable) {
    this.queuable = queuable;
  }

  public int getQueueMaxSize() {
    return queueMaxSize;
  }

  public void setQueueMaxSize(int queueMaxSize) {
    this.queueMaxSize = queueMaxSize;
  }

  public int getReplayInterval() {
    return replayInterval;
  }

  public void setReplayInterval(int replayInterval) {
    this.replayInterval = replayInterval;
  }

  public boolean isAutoQueue() {
    return autoQueue;
  }

  public void setAutoQueue(boolean autoQueue) {
    this.autoQueue = autoQueue;
  }
}
