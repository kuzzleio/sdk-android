package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.kuzzle.sdk.enums.CollectionType;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.responses.SearchResult;

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
  private JSONObject _volatile = new JSONObject();
  private int queueMaxSize = 500;
  private int queueTTL = 120000;
  private long reconnectionDelay = 1000;
  private String ifExist = "error";
  private Mode connect = Mode.AUTO;
  private Mode offlineMode = Mode.MANUAL;
  private int replayInterval = 10;
  private boolean queuable = true;
  private String defaultIndex = null;
  private boolean replaceIfExist = false;
  private String refresh = null;
  private Long from = null;
  private Long size = null;
  private Integer port = 7512;
  private String scroll = null;
  private SearchResult previous = null;
  private String scrollId = null;
  private int retryOnConflict = 0;

  // MemoryStorage specific options
  private Long start = null;
  private Long end = null;
  private String unit = null;
  private boolean withcoord = false;
  private boolean withdist = false;
  private Long count = null;
  private String sort = null;
  private String match = null;
  private Long ex = null;
  private boolean nx = false;
  private Long px = null;
  private boolean xx = false;
  private boolean alpha = false;
  private String by = null;
  private String direction = null;
  private JSONArray get = null;
  private JSONArray limit = null;
  private boolean ch = false;
  private boolean incr = false;
  private String aggregate = null;
  private JSONArray weights = null;

  // Used for getting collections
  private CollectionType collectionType = CollectionType.ALL;

  public Options() {
    // Default constructor
  }

  public Options(@NonNull Options originalOptions) throws JSONException {
    this.autoQueue = originalOptions.autoQueue;
    this.autoReconnect = originalOptions.autoReconnect;
    this.autoResubscribe = originalOptions.autoResubscribe;
    this.headers = new JSONObject(originalOptions.headers.toString());
    this._volatile = new JSONObject(originalOptions._volatile.toString());
    this.queueMaxSize = originalOptions.queueMaxSize;
    this.queueTTL = originalOptions.queueTTL;
    this.reconnectionDelay = originalOptions.reconnectionDelay;
    this.ifExist = originalOptions.ifExist;
    this.connect = originalOptions.connect;
    this.offlineMode = originalOptions.offlineMode;
    this.replayInterval = originalOptions.replayInterval;
    this.queuable = originalOptions.queuable;
    this.defaultIndex = originalOptions.defaultIndex;
    this.replaceIfExist = originalOptions.replaceIfExist;
    this.refresh = originalOptions.refresh;
    this.from = originalOptions.from;
    this.size = originalOptions.size;
    this.port = originalOptions.port;
    this.scroll = originalOptions.scroll;
    this.previous = originalOptions.previous;
    this.scrollId = originalOptions.scrollId;
  }

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
  public String getIfExist() {
    return ifExist;
  }

  /**
   * Sets update if exists.
   *
   * @param value the update if exists
   * @return the update if exists
   * @throws
   */
  public Options setIfExist(String value) {
    if (value != "error" && value != "replace") {
      throw new IllegalArgumentException("Invalid value for option 'ifExists': " + value);
    }

    this.ifExist = value;
    return this;
  }

  /**
   * Gets volatile data.
   *
   * @return the volatile property
   */
  public JSONObject getVolatile() {
    return _volatile;
  }

  /**
   * Sets volatile data.
   *
   * @param _volatile the volatile property
   * @return kuzzle instance
   */
  public Options setVolatile(JSONObject _volatile) {
    this._volatile = _volatile;
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

  public String getScroll() {
    return scroll;
  }

  public Options setScroll(String scroll) {
    this.scroll = scroll;
    return this;
  }

  public SearchResult getPrevious() {
    return previous;
  }

  public Options setPrevious(SearchResult previous) {
    this.previous = previous;
    return this;
  }

  public String getScrollId() {
    return scrollId;
  }

  public Options setScrollId(String scrollId) {
    this.scrollId = scrollId;
    return this;
  }

  public int getRetryOnConflict() {
    return retryOnConflict;
  }

  public Options setRetryOnConflict(int retryOnConflict) {
    if (retryOnConflict < 0) {
      throw new IllegalArgumentException("Invalid value for the retryOnConflict option (positive or null integer allowed)");
    }

    this.retryOnConflict = retryOnConflict;
    return this;
  }

  public Long getStart() {
    return start;
  }

  public Options setStart(Long start) {
    this.start = start;
    return this;
  }

  public Long getEnd() {
    return end;
  }

  public Options setEnd(Long end) {
    this.end = end;
    return this;
  }

  public String getUnit() {
    return unit;
  }

  public Options setUnit(String unit) {
    this.unit = unit;
    return this;
  }

  public boolean getWithcoord() {
    return withcoord;
  }

  public Options setWithcoord(boolean withcoord) {
    this.withcoord = withcoord;
    return this;
  }

  public boolean getWithdist() {
    return withdist;
  }

  public Options setWithdist(boolean withdist) {
    this.withdist = withdist;
    return this;
  }

  public Long getCount() {
    return count;
  }

  public Options setCount(Long count) {
    this.count = count;
    return this;
  }

  public String getSort() {
    return sort;
  }

  public Options setSort(String sort) {
    this.sort = sort;
    return this;
  }

  public String getMatch() {
    return match;
  }

  public Options setMatch(String match) {
    this.match = match;
    return this;
  }

  public Long getEx() {
    return ex;
  }

  public Options setEx(Long ex) {
    this.ex = ex;
    return this;
  }

  public boolean getNx() {
    return nx;
  }

  public Options setNx(boolean nx) {
    this.nx = nx;
    return this;
  }

  public Long getPx() {
    return px;
  }

  public Options setPx(Long px) {
    this.px = px;
    return this;
  }

  public boolean getXx() {
    return xx;
  }

  public Options setXx(boolean xx) {
    this.xx = xx;
    return this;
  }

  public boolean getAlpha() {
    return alpha;
  }

  public Options setAlpha(boolean alpha) {
    this.alpha = alpha;
    return this;
  }

  public String getBy() {
    return by;
  }

  public Options setBy(String by) {
    this.by = by;
    return this;
  }

  public String getDirection() {
    return direction;
  }

  public Options setDirection(String direction) {
    this.direction = direction;
    return this;
  }

  public JSONArray getGet() {
    return get;
  }

  public Options setGet(JSONArray get) {
    this.get = get;
    return this;
  }

  public JSONArray getLimit() {
    return limit;
  }

  public Options setLimit(JSONArray limit) {
    this.limit = limit;
    return this;
  }

  public boolean getCh() {
    return ch;
  }

  public Options setCh(boolean ch) {
    this.ch = ch;
    return this;
  }

  public boolean getIncr() {
    return incr;
  }

  public Options setIncr(boolean incr) {
    this.incr = incr;
    return this;
  }

  public String getAggregate() {
    return aggregate;
  }

  public Options setAggregate(String aggregate) {
    this.aggregate = aggregate;
    return this;
  }

  public JSONArray getWeights() {
    return weights;
  }

  public Options setWeights(JSONArray weights) {
    this.weights = weights;
    return this;
  }
}
