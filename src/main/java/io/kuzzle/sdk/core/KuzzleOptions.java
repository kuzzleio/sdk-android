package io.kuzzle.sdk.core;

import org.json.JSONObject;

import io.kuzzle.sdk.enums.KuzzleCollectionType;
import io.kuzzle.sdk.enums.Mode;

public class KuzzleOptions {
  // Default values
  private boolean autoQueue = false;
  private boolean autoReconnect = true;
  private boolean autoReplay = false;
  private boolean autoResubscribe = true;
  private JSONObject headers = new JSONObject();
  private JSONObject metadata = new JSONObject();
  private int queueMaxSize = 500;
  private int queueTTL = 120000;
  private long reconnectionDelay = 1000;
  private boolean updateIfExists = false;
  private Mode connect = Mode.AUTO;
  private Mode offlineMode = Mode.MANUAL;
  private int replayInterval = 10;
  private boolean queuable = true;
  private String defaultIndex = null;

  // Used for getting collections
  private KuzzleCollectionType  collectionType = KuzzleCollectionType.ALL;

  // Used by Kuzzle security objects
  private boolean hydrate = true;

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

  public boolean isAutoResubscribe() {
    return autoResubscribe;
  }

  public void setAutoResubscribe(boolean autoResubscribe) {
    this.autoResubscribe = autoResubscribe;
  }

  public KuzzleCollectionType getCollectionType() {
    return collectionType;
  }

  public void setCollectionType(KuzzleCollectionType type) {
    this.collectionType = type;
  }

  public void setHydrate(boolean hydrate) {
    this.hydrate = hydrate;
  }

  public boolean isHydrated() {
    return this.hydrate;
  }

  public void setDefaultIndex(final String index) {
    this.defaultIndex = index;
  }

  public String getDefaultIndex() {
    return this.defaultIndex;
  }

  public void setAutoQueue(boolean autoQueue) {
    this.autoQueue = autoQueue;
  }

  public boolean isAutoQueue() {
    return this.autoQueue;
  }
}
