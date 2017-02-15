package io.kuzzle.sdk.core;

import org.json.JSONObject;

import io.kuzzle.sdk.enums.CollectionType;
import io.kuzzle.sdk.enums.Mode;

/**
 * The type Kuzzle options.
 */
public class Options {
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
  private String ifExists = "error";
  private Mode connect = Mode.AUTO;
  private Mode offlineMode = Mode.MANUAL;
  private int replayInterval = 10;
  private boolean queuable = true;
  private String defaultIndex = null;
  private boolean replaceIfExist = false;
  private String refresh = null;
  private Long from;
  private Long size;
  private Integer port = 7512;

  // Used for getting collections
  private CollectionType collectionType = CollectionType.ALL;

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
  public Options setAutoReconnect(boolean autoReconnect) {
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
  public Options setHeaders(JSONObject headers) {
    this.headers = headers;
    return this;
  }

  /**
   * Is update if exists boolean.
   *
   * @return value of the ifExists parameter
   */
  public String getIfExists() {
    return ifExists;
  }

  /**
   * Sets update if exists.
   *
   * @param value the update if exists
   * @return the update if exists
   * @throws
   */
  public Options setIfExists(String value) {
    if (value != "error" && value != "replace") {
      throw new IllegalArgumentException("Invalid value for option 'ifExists': " + value);
    }

    this.ifExists = value;
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
  public Options setMetadata(JSONObject metadata) {
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
  public Options setConnect(Mode connect) {
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
  public Options setReconnectionDelay(long reconnectionDelay) {
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
  public Options setOfflineMode(Mode offlineMode) {
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
  public Options setQueueTTL(int queueTTL) {
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
  public Options setAutoReplay(boolean autoReplay) {
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
  public Options setQueuable(boolean queuable) {
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
  public Options setQueueMaxSize(int queueMaxSize) {
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
  public Options setReplayInterval(int replayInterval) {
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
  public Options setAutoResubscribe(boolean autoResubscribe) {
    this.autoResubscribe = autoResubscribe;
    return this;
  }

  /**
   * Gets collection type.
   *
   * @return the collection type
   */
  public CollectionType getCollectionType() {
    return collectionType;
  }

  /**
   * Sets collection type.
   *
   * @param type the type
   * @return the collection type
   */
  public Options setCollectionType(CollectionType type) {
    this.collectionType = type;
    return this;
  }

  /**
   * Sets default index.
   *
   * @param index the index
   * @return the default index
   */
  public Options setDefaultIndex(final String index) {
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
  public Options setAutoQueue(boolean autoQueue) {
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
  public Options setReplaceIfExist(boolean replace) {
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

  public Options setFrom(Long from) {
    this.from = from;
    return this;
  }

  public Long getSize() {
    return size;
  }

  public Options setSize(Long size) {
    this.size = size;
    return this;
  }

  public Options setPort(Integer port) {
    this.port = port;
    return this;
  }

  public Integer getPort() {
    return this.port;
  }

  public String getRefresh() {
    return refresh;
  }

  public Options setRefresh(String refresh) {
    this.refresh = refresh;
    return this;
  }
}
