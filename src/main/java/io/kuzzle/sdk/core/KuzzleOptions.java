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
  public KuzzleOptions setAutoReconnect(boolean autoReconnect) {
    this.autoReconnect = autoReconnect;
    return this;
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
  public KuzzleOptions setHeaders(JSONObject headers) {
    this.headers = headers;
    return this;
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
  public KuzzleOptions setUpdateIfExists(boolean updateIfExists) {
    this.updateIfExists = updateIfExists;
    return this;
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
  public KuzzleOptions setMetadata(JSONObject metadata) {
    this.metadata = metadata;
    return this;
  }

  public Mode getConnect() {
    return connect;
  }

  public KuzzleOptions setConnect(Mode connect) {
    this.connect = connect;
    return this;
  }

  public long getReconnectionDelay() {
    return reconnectionDelay;
  }

  public KuzzleOptions setReconnectionDelay(long reconnectionDelay) {
    this.reconnectionDelay = reconnectionDelay;
    return this;
  }

  public Mode getOfflineMode() {
    return offlineMode;
  }

  public KuzzleOptions setOfflineMode(Mode offlineMode) {
    this.offlineMode = offlineMode;
    return this;
  }

  public int getQueueTTL() {
    return queueTTL;
  }

  public KuzzleOptions setQueueTTL(int queueTTL) {
    this.queueTTL = queueTTL;
    return this;
  }

  public boolean isAutoReplay() {
    return autoReplay;
  }

  public KuzzleOptions setAutoReplay(boolean autoReplay) {
    this.autoReplay = autoReplay;
    return this;
  }

  public boolean isQueuable() {
    return queuable;
  }

  public KuzzleOptions setQueuable(boolean queuable) {
    this.queuable = queuable;
    return this;
  }

  public int getQueueMaxSize() {
    return queueMaxSize;
  }

  public KuzzleOptions setQueueMaxSize(int queueMaxSize) {
    this.queueMaxSize = queueMaxSize;
    return this;
  }

  public int getReplayInterval() {
    return replayInterval;
  }

  public KuzzleOptions setReplayInterval(int replayInterval) {
    this.replayInterval = replayInterval;
    return this;
  }

  public boolean isAutoResubscribe() {
    return autoResubscribe;
  }

  public KuzzleOptions setAutoResubscribe(boolean autoResubscribe) {
    this.autoResubscribe = autoResubscribe;
    return this;
  }

  public KuzzleCollectionType getCollectionType() {
    return collectionType;
  }

  public KuzzleOptions setCollectionType(KuzzleCollectionType type) {
    this.collectionType = type;
    return this;
  }

  public KuzzleOptions setHydrate(boolean hydrate) {
    this.hydrate = hydrate;
    return this;
  }

  public boolean isHydrated() {
    return this.hydrate;
  }

  public KuzzleOptions setDefaultIndex(final String index) {
    this.defaultIndex = index;
    return this;
  }

  public String getDefaultIndex() {
    return this.defaultIndex;
  }

  public KuzzleOptions setAutoQueue(boolean autoQueue) {
    this.autoQueue = autoQueue;
    return this;
  }

  public boolean isAutoQueue() {
    return this.autoQueue;
  }
}
