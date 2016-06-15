package io.kuzzle.sdk.core;

import org.json.JSONObject;

import io.kuzzle.sdk.enums.KuzzleCollectionType;
import io.kuzzle.sdk.enums.Mode;

/**
 * The type Kuzzle options.
 */
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
  private boolean replaceIfExist = false;
  private Long from;
  private Long size;

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
   * @return the auto reconnect
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
   * @return the headers
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
   * @return the update if exists
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
   * @return the metadata
   */
  public KuzzleOptions setMetadata(JSONObject metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Gets connect.
   *
   * @return the connect
   */
  public Mode getConnect() {
    return connect;
  }

  /**
   * Sets connect.
   *
   * @param connect the connect
   * @return the connect
   */
  public KuzzleOptions setConnect(Mode connect) {
    this.connect = connect;
    return this;
  }

  /**
   * Gets reconnection delay.
   *
   * @return the reconnection delay
   */
  public long getReconnectionDelay() {
    return reconnectionDelay;
  }

  /**
   * Sets reconnection delay.
   *
   * @param reconnectionDelay the reconnection delay
   * @return the reconnection delay
   */
  public KuzzleOptions setReconnectionDelay(long reconnectionDelay) {
    this.reconnectionDelay = reconnectionDelay;
    return this;
  }

  /**
   * Gets offline mode.
   *
   * @return the offline mode
   */
  public Mode getOfflineMode() {
    return offlineMode;
  }

  /**
   * Sets offline mode.
   *
   * @param offlineMode the offline mode
   * @return the offline mode
   */
  public KuzzleOptions setOfflineMode(Mode offlineMode) {
    this.offlineMode = offlineMode;
    return this;
  }

  /**
   * Gets queue ttl.
   *
   * @return the queue ttl
   */
  public int getQueueTTL() {
    return queueTTL;
  }

  /**
   * Sets queue ttl.
   *
   * @param queueTTL the queue ttl
   * @return the queue ttl
   */
  public KuzzleOptions setQueueTTL(int queueTTL) {
    this.queueTTL = queueTTL;
    return this;
  }

  /**
   * Is auto replay boolean.
   *
   * @return the boolean
   */
  public boolean isAutoReplay() {
    return autoReplay;
  }

  /**
   * Sets auto replay.
   *
   * @param autoReplay the auto replay
   * @return the auto replay
   */
  public KuzzleOptions setAutoReplay(boolean autoReplay) {
    this.autoReplay = autoReplay;
    return this;
  }

  /**
   * Is queuable boolean.
   *
   * @return the boolean
   */
  public boolean isQueuable() {
    return queuable;
  }

  /**
   * Sets queuable.
   *
   * @param queuable the queuable
   * @return the queuable
   */
  public KuzzleOptions setQueuable(boolean queuable) {
    this.queuable = queuable;
    return this;
  }

  /**
   * Gets queue max size.
   *
   * @return the queue max size
   */
  public int getQueueMaxSize() {
    return queueMaxSize;
  }

  /**
   * Sets queue max size.
   *
   * @param queueMaxSize the queue max size
   * @return the queue max size
   */
  public KuzzleOptions setQueueMaxSize(int queueMaxSize) {
    this.queueMaxSize = queueMaxSize;
    return this;
  }

  /**
   * Gets replay interval.
   *
   * @return the replay interval
   */
  public int getReplayInterval() {
    return replayInterval;
  }

  /**
   * Sets replay interval.
   *
   * @param replayInterval the replay interval
   * @return the replay interval
   */
  public KuzzleOptions setReplayInterval(int replayInterval) {
    this.replayInterval = replayInterval;
    return this;
  }

  /**
   * Is auto resubscribe boolean.
   *
   * @return the boolean
   */
  public boolean isAutoResubscribe() {
    return autoResubscribe;
  }

  /**
   * Sets auto resubscribe.
   *
   * @param autoResubscribe the auto resubscribe
   * @return the auto resubscribe
   */
  public KuzzleOptions setAutoResubscribe(boolean autoResubscribe) {
    this.autoResubscribe = autoResubscribe;
    return this;
  }

  /**
   * Gets collection type.
   *
   * @return the collection type
   */
  public KuzzleCollectionType getCollectionType() {
    return collectionType;
  }

  /**
   * Sets collection type.
   *
   * @param type the type
   * @return the collection type
   */
  public KuzzleOptions setCollectionType(KuzzleCollectionType type) {
    this.collectionType = type;
    return this;
  }

  /**
   * Sets hydrate.
   *
   * @param hydrate the hydrate
   * @return the hydrate
   */
  public KuzzleOptions setHydrate(boolean hydrate) {
    this.hydrate = hydrate;
    return this;
  }

  /**
   * Is hydrated boolean.
   *
   * @return the boolean
   */
  public boolean isHydrated() {
    return this.hydrate;
  }

  /**
   * Sets default index.
   *
   * @param index the index
   * @return the default index
   */
  public KuzzleOptions setDefaultIndex(final String index) {
    this.defaultIndex = index;
    return this;
  }

  /**
   * Gets default index.
   *
   * @return the default index
   */
  public String getDefaultIndex() {
    return this.defaultIndex;
  }

  /**
   * Sets auto queue.
   *
   * @param autoQueue the auto queue
   * @return the auto queue
   */
  public KuzzleOptions setAutoQueue(boolean autoQueue) {
    this.autoQueue = autoQueue;
    return this;
  }

  /**
   * Is auto queue boolean.
   *
   * @return the boolean
   */
  public boolean isAutoQueue() {
    return this.autoQueue;
  }

  /**
   * Sets replace if exist.
   *
   * @param replace the replace
   * @return the replace if exist
   */
  public KuzzleOptions setReplaceIfExist(boolean replace) {
    this.replaceIfExist = replace;
    return this;
  }

  /**
   * Is replace if exist boolean.
   *
   * @return the boolean
   */
  public boolean isReplaceIfExist() {
    return this.replaceIfExist;
  }

  public Long getFrom() {
    return from;
  }

  public void setFrom(Long from) {
    this.from = from;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }
}
