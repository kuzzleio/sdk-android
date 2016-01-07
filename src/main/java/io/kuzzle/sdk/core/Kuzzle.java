package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.IEventListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.state.KuzzleQueue;
import io.kuzzle.sdk.state.States;
import io.kuzzle.sdk.util.Event;
import io.kuzzle.sdk.util.KuzzleQueryObject;
import io.kuzzle.sdk.util.QueueFilter;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.EngineIOException;

/**
 * The type Kuzzle.
 */
public class Kuzzle {

  private final int MAX_EMIT_TIMEOUT = 10;

  private List<Event> eventListeners = new ArrayList<Event>();
  private Socket socket;
  private Map<String, KuzzleDataCollection> collections = new HashMap<String, KuzzleDataCollection>();
  private Map<String, KuzzleRoom> subscriptions = new ConcurrentHashMap<String, KuzzleRoom>();
  private Map<String, KuzzleRoom> pendingSubscriptions = new ConcurrentHashMap<String, KuzzleRoom>();
  private boolean autoReconnect = true;
  private JSONObject headers = new JSONObject();
  private JSONObject metadata;
  private String url;
  private ResponseListener connectionCallback;
  private States state = States.INITIALIZING;
  private long  reconnectionDelay;
  private boolean autoResubscribe = true;
  private boolean autoQueue = false;
  private boolean autoReplay = false;
  /**
   * The Queue filter.
   */
  public QueueFilter queueFilter = new QueueFilter() {
    @Override
    public boolean filter(JSONObject object) {
      return true;
    }
  };
  private long replayInterval = 10;
  private boolean queuing = false;
  private final String index;

  private Map<String, Date> requestHistory = new HashMap<String, Date>();

  private KuzzleQueue<KuzzleQueryObject> offlineQueue = new KuzzleQueue<KuzzleQueryObject>();
  private int queueTTL;
  private int queueMaxSize;

  // Auth related
  private String  loginStrategy;
  private String  loginUsername;
  private String  loginPassword;
  // in second
  private int loginExpiresIn = 0;
  private String jwtToken;

  /**
   * Kuzzle object constructor.
   *
   * @param url                the url
   * @param index              the index
   * @param options            the options
   * @param connectionCallback the connection callback
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(final String url, final String index, final KuzzleOptions options, final ResponseListener connectionCallback) throws URISyntaxException {
    if (url == null || url.isEmpty())
      throw new IllegalArgumentException("Url can't be empty");
    if (index == null || index.isEmpty())
      throw new IllegalArgumentException("Index is missing");

    this.autoReconnect = (options != null ? options.isAutoReconnect() : false);
    this.headers = (options != null && options.getHeaders() != null ? options.getHeaders() : new JSONObject());
    this.metadata = (options != null && options.getMetadata() != null ? options.getMetadata() : new JSONObject());
    this.reconnectionDelay = (options != null ? options.getReconnectionDelay() : 1000);
    this.queueTTL = (options != null ? options.getQueueTTL() : 0);
    this.autoReplay = (options != null ? options.isAutoReplay() : false);
    this.queueMaxSize = (options != null ? options.getQueueMaxSize() : 0);
    this.autoResubscribe = (options != null ? options.isAutoResubscribe() : true);
    // login related
    this.loginStrategy = (options != null ? options.getLoginStrategy() : null);
    this.loginUsername = (options != null ? options.getLoginUsername() : null);
    this.loginPassword = (options != null ? options.getLoginPassword() : null);
    this.loginExpiresIn = (options != null ? options.getLoginExpiresIn() : -1);
    this.url = url;
    this.connectionCallback = connectionCallback;
    this.index = index;
    if (socket == null) {
      socket = createSocket(this.url);
    }
    if (options != null && options.getOfflineMode() == Mode.AUTO && this.autoReconnect) {
      this.autoQueue = this.autoReplay = this.autoResubscribe = true;
    }
    if (options == null || options.getConnect() == null || options.getConnect() == Mode.AUTO) {
      connect();
    } else {
      this.state = States.READY;
    }
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param url   the url
   * @param index the index
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(final String url, final String index) throws URISyntaxException {
    this(url, index, null, null);
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param url   the url
   * @param index the index
   * @param cb    the cb
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(final String url, final String index, final ResponseListener cb) throws URISyntaxException {
    this(url, index, null, cb);
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param url     the url
   * @param index   the index
   * @param options the options
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(String url, final String index, KuzzleOptions options) throws URISyntaxException {
    this(url, index, options, null);
  }

  /**
   * Adds a listener to a Kuzzle global event. When an event is fired, listeners are called in the order of their
   * insertion.
   * The ID returned by this function is required to remove this listener at a later time.
   *
   * @param eventType     - name of the global event to subscribe to
   * @param eventListener the event listener
   * @return {string} Unique listener ID
   */
  public String addListener(final EventType eventType, final IEventListener eventListener) {
    this.isValid();

    Event e = new Event(eventType) {
      @Override
      public void trigger(Object... args) {
        eventListener.trigger(args);
      }
    };
    eventListeners.add(e);
    return e.getId().toString();
  }

  /**
   * Connects to a Kuzzle instance using the provided URL.
   *
   * @return kuzzle kuzzle
   */
  public Kuzzle connect() {
    if (!this.isValidState()) {
      if (connectionCallback != null) {
        connectionCallback.onSuccess(null);
        return this;
      }
    }
    Kuzzle.this.state = States.CONNECTING;
    if (socket != null)
      socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = States.CONNECTED;
          if (loginStrategy != null && loginUsername != null && loginPassword != null) {
            Kuzzle.this.login(loginStrategy, loginUsername, loginPassword, loginExpiresIn);
          }
          if (Kuzzle.this.connectionCallback != null) {
            Kuzzle.this.connectionCallback.onSuccess(null);
          }
          Kuzzle.this.dequeue();
          for (Event e : Kuzzle.this.eventListeners) {
            if (e.getType() == EventType.CONNECTED) {
              e.trigger();
            }
          }
        }
      });
    if (socket != null)
      socket.once(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = States.ERROR;
          for (Event e : Kuzzle.this.eventListeners) {
            if (e.getType() == EventType.ERROR) {
              e.trigger(args);
            }
          }
          if (connectionCallback != null) {
            JSONObject error = new JSONObject();
            try {
              error.put("message", ((EngineIOException)args[0]).getMessage());
              error.put("code", ((EngineIOException)args[0]).code);
            } catch (JSONException e) {
              e.printStackTrace();
            }
            connectionCallback.onSuccess(error);
          }
        }
      });
    if (socket != null)
      socket.once(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = States.OFFLINE;
          if (!Kuzzle.this.autoReconnect) {
            disconnect();
          }
          if (Kuzzle.this.autoQueue) {
            queuing = true;
          }
          for (Event e : eventListeners) {
            if (e.getType() == EventType.DISCONNECTED) {
              e.trigger();
            }
          }
        }
      });
    if (socket != null)
      socket.once(Socket.EVENT_RECONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = States.CONNECTED;
          if (Kuzzle.this.autoResubscribe) {
            // Resubscribe
            try {
              renewSubscriptions(connectionCallback);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          //replay queued requests
          if (Kuzzle.this.autoReplay) {
            Kuzzle.this.cleanQueue();
            Kuzzle.this.dequeue();
          }

          // alert listeners
          for (Event e : Kuzzle.this.eventListeners) {
            if (e.getType() == EventType.RECONNECTED) {
              e.trigger();
            }
          }
        }
      });
    if (socket != null)
      socket.connect();
    return this;
  }

  /**
   * Create a new instance of a KuzzleDataCollection object
   *
   * @param collection - The name of the data collection you want to manipulate
   * @return {object} A KuzzleDataCollection instance
   */
  public KuzzleDataCollection dataCollectionFactory(String collection) {
    this.isValid();
    if (!this.collections.containsKey(collection)) {
      this.collections.put(collection, new KuzzleDataCollection(this, collection));
    }
    return this.collections.get(collection);
  }

  /**
   * Empties the offline queue without replaying it.
   *
   * @return Kuzzle instance
   */
  public Kuzzle flushQueue() {
    this.getOfflineQueue().clear();
    return this;
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests.
   * This method returns all available statistics from Kuzzle.
   *
   * @param listener the listener
   * @return the all statistics
   */
  public Kuzzle getAllStatistics(final ResponseListener listener) {
    this.isValid();
    try {
      this.query(null, "admin", "getAllStats", null, new ResponseListener() {
        @Override
        public void onSuccess(JSONObject object) {
          if (listener != null) {
            listener.onSuccess(object);
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests.
   * This method allows getting the last statistics frame
   *
   * @param listener the listener
   * @return statistics statistics
   */
  public Kuzzle getStatistics(final ResponseListener listener) {
    return this.getStatistics(null, listener);
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests.
   * This method allows getting either the last statistics frame, or a set of frames starting from a provided timestamp.
   *
   * @param since    the timestamp
   * @param listener the listener
   * @return statistics statistics
   */
  public Kuzzle getStatistics(String since, final ResponseListener listener) {
    this.isValid();
    JSONObject body = new JSONObject();
    JSONObject data = new JSONObject();
    try {
      data.put("since", since);
      body.put("body", data);
      this.query(null, "admin", "getStats", body, new ResponseListener() {
        @Override
        public void onSuccess(JSONObject object) {
          if (listener != null) {
            listener.onSuccess(object);
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null)
            listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @return kuzzle kuzzle
   */
  public Kuzzle listCollections() {
    return this.listCollections(null, null);
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @param options the options
   * @return kuzzle kuzzle
   */
  public Kuzzle listCollections(KuzzleOptions options) {
    return this.listCollections(options, null);
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @param listener the listener
   * @return kuzzle kuzzle
   */
  public Kuzzle listCollections(ResponseListener listener) {
    return this.listCollections(null, listener);
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @param options  the options
   * @param listener the listener
   * @return kuzzle kuzzle
   */
  public Kuzzle listCollections(KuzzleOptions options, ResponseListener listener) {
    try {
      return this.query(null, "read", "listCollections", null, options, listener);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public Kuzzle login(final String strategy, final String username, final String password) {
    return this.login(strategy, username, password, -1, null, null);
  }

  public Kuzzle login(final String strategy, final String username, final String password, final int expiresIn) {
    return this.login(strategy, username, password, expiresIn, null, null);
  }

  public Kuzzle login(final String strategy, final String username, final String password, final int expiresIn, ResponseListener listener) {
    return this.login(strategy, username, password, expiresIn, null, listener);
  }

  public Kuzzle login(final String strategy, final String username, final String password, final KuzzleOptions options) {
    return this.login(strategy, username, password, -1, options, null);
  }

  public Kuzzle login(final String strategy, final String username, final String password, final int expiresIn, final KuzzleOptions options) {
    return this.login(strategy, username, password, expiresIn, options, null);
  }

  public Kuzzle login(final String strategy, final String username, final String password, int expiresIn, final KuzzleOptions options, final ResponseListener listener) {
    JSONObject query = new JSONObject();
    try {
      query.put("strategy", strategy);
      query.put("username", username);
      query.put("password", password);
      if (expiresIn >= 0) {
        query.put("expiresIn", expiresIn);
      }
      return this.query(null, "auth", "login", query, options, new ResponseListener() {
        @Override
        public void onSuccess(JSONObject object) {
          try {
            Kuzzle.this.jwtToken = object.getString("jwt");
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
          if (listener != null) {
            listener.onSuccess(object);
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Disconnects from Kuzzle and invalidate this instance.
   * Does not fire a disconnected event.
   */
  public void disconnect() {
    if (this.socket != null)
      this.socket.close();
    this.socket = null;
    this.collections.clear();
    this.state = States.DISCONNECTED;
  }

  public Kuzzle logout() {
    return this.logout(null, null);
  }

  public Kuzzle logout(final KuzzleOptions options) {
    return this.logout(options, null);
  }

  public Kuzzle logout(final ResponseListener listener) {
    return this.logout(null, listener);
  }

  public Kuzzle logout(final KuzzleOptions options, final ResponseListener listener) {
    try {
      return this.query(null, "auth", "logout", new JSONObject(), options, new ResponseListener() {
        @Override
        public void onSuccess(JSONObject object) {
          Kuzzle.this.jwtToken = null;
          if (listener != null) {
            listener.onSuccess(object);
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the current Kuzzle UTC timestamp
   *
   * @param cb - Handles the query response
   * @return kuzzle timestamp
   */
  public Kuzzle now(ResponseListener cb) {
    this.isValid();
    try {
      this.query(null, "read", "now", null, null, cb);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Query kuzzle.
   *
   * @param collection the collection
   * @param controller the controller
   * @param action     the action
   * @param query      the query
   * @return the kuzzle
   * @throws JSONException the json exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query) throws JSONException {
    return this.query(collection, controller, action, query, null, null);
  }

  /**
   * Query kuzzle.
   *
   * @param collection the collection
   * @param controller the controller
   * @param action     the action
   * @param query      the query
   * @param options    the options
   * @return the kuzzle
   * @throws JSONException the json exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query, KuzzleOptions options) throws JSONException {
    return this.query(collection, controller, action, query, options, null);
  }

  /**
   * Query kuzzle.
   *
   * @param collection the collection
   * @param controller the controller
   * @param action     the action
   * @param query      the query
   * @param listener   the listener
   * @return the kuzzle
   * @throws JSONException the json exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query, ResponseListener listener) throws JSONException {
    return this.query(collection, controller, action, query, null, listener);
  }

  /**
   * This is a low-level method, exposed to allow advanced SDK users to bypass high-level methods.
   * Base method used to send read queries to Kuzzle
   *
   * @param collection - Name of the data collection you want to manipulate
   * @param controller - The Kuzzle controller that will handle this query
   * @param action     - The controller action to perform
   * @param query      - The query data
   * @param options    the options
   * @param cb         the cb
   * @return the kuzzle
   * @throws JSONException the json exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query, KuzzleOptions options, final ResponseListener cb) throws JSONException {
    this.isValid();
    JSONObject object = query != null ? query : new JSONObject();
    if (object.isNull("requestId"))
      object.put("requestId", UUID.randomUUID().toString());
    object.put("action", action);
    object.put("controller", controller);

    // Global metadata
    JSONObject meta = new JSONObject();
    for (Iterator ite = this.metadata.keys(); ite.hasNext();) {
      String key = (String) ite.next();
      meta.put(key, this.metadata.get(key));
    }

    // Metadata for this query
    if (options != null) {
      if (!options.isQueuable() && this.state == States.OFFLINE) {
        return this;
      }
      if (options.getMetadata() != null) {
        for (Iterator iterator = options.getMetadata().keys(); iterator.hasNext(); ) {
          String key = (String) iterator.next();
          meta.put(key, options.getMetadata().get(key));
        }
      }
    }
    object.put("metadata", meta);
    object.put("index", this.index);

    if (collection != null) {
      object.put("collection", collection);
    }
    this.addHeaders(object, this.headers);

    if (this.state == States.CONNECTED || (options != null && !options.isQueuable())) {
      emitRequest(object, cb);
    } else if (this.queuing || (this.state == States.INITIALIZING || this.state == States.CONNECTING)) {
      cleanQueue();
      if (queueFilter.filter(object)) {
        KuzzleQueryObject o = new KuzzleQueryObject();
        o.setTimestamp(new Date());
        o.setCb(cb);
        o.setQuery(object);
        this.offlineQueue.addToQueue(o);
      }
    }
    return this;
  }

  /**
   * Removes all listeners, either from a specific event or from all events
   *
   * @return kuzzle kuzzle
   */
  public Kuzzle removeAllListeners() {
    this.eventListeners.clear();
    return this;
  }

  /**
   * Remove all listeners kuzzle.
   *
   * @param type the type
   * @return the kuzzle
   */
  public Kuzzle removeAllListeners(EventType type) {
    for (Iterator ite = this.eventListeners.iterator(); ite.hasNext();) {
      if (((Event)ite.next()).getType() == type) {
        ite.remove();
      }
    }
    return this;
  }

  /**
   * Removes a listener from an event.
   *
   * @param listenerId the listener id
   * @return the kuzzle
   */
  public Kuzzle removeListener(String listenerId) {
    this.isValid();

    int i = 0;
    for (Event e : eventListeners) {
      if (e.getId().toString().equals(listenerId)) {
        eventListeners.remove(i);
        break;
      }
      i++;
    }
    return this;
  }

  private void renewSubscriptions(final ResponseListener listener) {
    Iterator ite = subscriptions.entrySet().iterator();
    while (ite.hasNext()) {
      Map.Entry e = (Map.Entry) ite.next();
      ((KuzzleRoom)e.getValue()).renew(null, listener);
    }
  }

  /**
   * Replays the requests queued during offline mode.
   * Works only if the SDK is not in a disconnected state, and if the autoReplay option is set to false.
   *
   * @return kuzzle kuzzle
   */
  public Kuzzle replayQueue() {
    if (this.state != States.OFFLINE && !this.autoReplay) {
      this.cleanQueue();
      this.dequeue();
    }
    return this;
  }

  /**
   * Helper function allowing to set headers while chaining calls.
   * If the replace argument is set to true, replace the current headers with the provided content.
   * Otherwise, it appends the content to the current headers, only replacing already existing values
   *
   * @param content the headers
   * @return the headers
   * @throws JSONException the json exception
   */
  public Kuzzle setHeaders(JSONObject content) throws JSONException {
    return this.setHeaders(content, false);
  }

  /**
   * Helper function allowing to set headers while chaining calls.
   * If the replace argument is set to true, replace the current headers with the provided content.
   * Otherwise, it appends the content to the current headers, only replacing already existing values
   *
   * @param content - new headers content
   * @param replace - default: false = append the content. If true: replace the current headers with tj
   * @return the headers
   */
  public Kuzzle setHeaders(JSONObject content, boolean replace) {
    if (this.headers == null) {
      this.headers = new JSONObject();
    }
    if (replace) {
      this.headers = content;
    } else {
      if (content != null) {
        try {
          for (Iterator ite = content.keys(); ite.hasNext(); ) {
            String key = (String) ite.next();
            this.headers.put(key, content.get(key));
          }
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return this;
  }

  /**
   * Starts the requests queuing. Works only during offline mode, and if the autoQueue option is set to false.
   *
   * @return kuzzle kuzzle
   */
  public Kuzzle startQueuing() {
    if (this.state == States.OFFLINE && !this.autoQueue) {
      this.queuing = true;
    }
    return this;
  }

  /**
   * Stops the requests queuing. Works only during offline mode, and if the autoQueue option is set to false.
   *
   * @return kuzzle kuzzle
   */
  public Kuzzle stopQueing() {
    if (this.state == States.OFFLINE && !this.autoQueue) {
      this.queuing = false;
    }
    return this;
  }

  /**
   * Is valid sate boolean.
   *
   * @return the boolean
   */
  public boolean isValidState() {
    switch (this.state) {
      case INITIALIZING:
      case READY:
      case DISCONNECTED:
      case ERROR:
      case OFFLINE:
        return true;
    }
    return false;
  }

  private Socket createSocket(String url) throws URISyntaxException {
    IO.Options opt = new IO.Options();
    opt.forceNew = true;
    opt.reconnection = this.autoReconnect;
    opt.reconnectionDelay = this.reconnectionDelay;
    return IO.socket(url);
  }

  private void emitRequest(final JSONObject request, final ResponseListener listener) throws JSONException {
    Date now = new Date();
    Calendar c = Calendar.getInstance();
    c.setTime(now);
    c.add(Calendar.SECOND, -MAX_EMIT_TIMEOUT);
    socket.once(request.get("requestId").toString(), new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        try {
          // checking token expiration
          if (!((JSONObject) args[0]).isNull("error") && ((JSONObject) args[0]).getJSONObject("error").getString("message").equals("Token expired")) {
            for (Event e : Kuzzle.this.eventListeners) {
              if (e.getType() == EventType.JWT_TOKEN_EXPIRED) {
                e.trigger(request, listener);
              }
            }
          }
          if (listener != null) {
            if (!((JSONObject) args[0]).isNull("error")) {
              listener.onError((JSONObject) ((JSONObject) args[0]).get("error"));
            } else {
              listener.onSuccess((JSONObject) ((JSONObject) args[0]).get("result"));
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
    // Set JWT token if defined
    if (this.jwtToken != null) {
      request.put("headers", new JSONObject().put("authorization", "Bearer " + this.jwtToken));
    }
    socket.emit("kuzzle", request);
    // Track requests made to allow KuzzleRoom.subscribeToSelf to work
    this.requestHistory.put(request.getString("requestId"), new Date());
    Iterator ite = requestHistory.keySet().iterator();

    while (ite.hasNext()) {
      if (this.requestHistory.get(ite.next()).before(c.getTime())) {
        ite.remove();
      }
    }
  }

  /**
   * Helper function ensuring that this Kuzzle object is still valid before performing a query
   */
  public void isValid() {
    if (this.state == States.DISCONNECTED) {
      throw new RuntimeException("This Kuzzle object has been invalidated. Did you try to access it after a disconnect call?");
    }
  }

  /**
   * Helper function copying headers to the query data
   *
   * @param query   the query
   * @param headers the headers
   * @throws JSONException the json exception
   */
  public void addHeaders(JSONObject query, JSONObject headers) throws JSONException {
    for (Iterator iterator = headers.keys(); iterator.hasNext(); ) {
      String key = (String) iterator.next();
      if (query.isNull(key)) {
        query.put(key, headers.get(key));
      }
    }
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
    return this.headers;
  }

  /**
   * Add pending subscription kuzzle.
   *
   * @param id   the id
   * @param room the room
   * @return kuzzle kuzzle
   */
  public Kuzzle addPendingSubscription(final String id, final KuzzleRoom room) {
    if (!this.pendingSubscriptions.containsKey(id))
      this.pendingSubscriptions.put(id, room);
    return this;
  }

  /**
   * Delete pending subscription kuzzle.
   *
   * @param id the id
   * @return the kuzzle
   */
  public Kuzzle deletePendingSubscription(final String id) {
    pendingSubscriptions.remove(id);
    return this;
  }

  /**
   * Add subscription kuzzle.
   *
   * @param id   the id
   * @param room the room
   * @return kuzzle kuzzle
   */
  public Kuzzle addSubscription(final String id, final KuzzleRoom room) {
    if (!this.subscriptions.containsKey(id))
      this.subscriptions.put(id, room);
    return this;
  }

  /**
   * Gets socket.
   *
   * @return the socket
   */
  public Socket getSocket() {
    return socket;
  }

  /**
   * Sets socket.
   *
   * @param socket the socket
   */
  public void setSocket(Socket socket) {
    this.socket = socket;
  }

  /**
   * Sets offline queue.
   *
   * @param object the object
   */
  public void setOfflineQueue(final KuzzleQueryObject object) {
    this.offlineQueue.addToQueue(object);
  }

  /**
   * Gets offline queue.
   *
   * @return offline queue
   */
  public Queue<KuzzleQueryObject> getOfflineQueue() {
    return this.offlineQueue.getQueue();
  }

  /**
   * Gets event listeners.
   *
   * @return the event listeners
   */
  public List<Event> getEventListeners() {
    return eventListeners;
  }

  /**
   * Sets queue filter.
   *
   * @param queueFilter the queue filter
   * @return queue filter
   */
  public Kuzzle setQueueFilter(QueueFilter queueFilter) {
    this.queueFilter = queueFilter;
    return this;
  }

  /**
   * Gets queue filter.
   *
   * @return queue filter
   */
  public QueueFilter  getQueueFilter() {
    return this.queueFilter;
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
   */
  public void setAutoReplay(boolean autoReplay) {
    this.autoReplay = autoReplay;
  }

  /**
   * Is auto queue boolean.
   *
   * @return the boolean
   */
  public boolean isAutoQueue() {
    return autoQueue;
  }

  /**
   * Sets auto queue.
   *
   * @param autoQueue the auto queue
   */
  public void setAutoQueue(boolean autoQueue) {
    this.autoQueue = autoQueue;
  }

  /**
   * Clean up the queue, ensuring the queryTTL and queryMaxSize properties are respected
   */
  private void  cleanQueue() {
    Date now = new Date();
    Calendar cal = Calendar.getInstance();
    cal.setTime(now);
    cal.add(Calendar.MILLISECOND, -queueTTL);

    if (this.queueTTL > 0) {
      KuzzleQueryObject o;
      while ((o = (KuzzleQueryObject) offlineQueue.getQueue().peek()) != null) {
        if (o.getTimestamp().before(cal.getTime())) {
          offlineQueue.getQueue().poll();
        } else {
          break;
        }
      }
    }

    int size = this.offlineQueue.getQueue().size();
    if (this.queueMaxSize > 0 && size > this.queueMaxSize) {
      int i = 0;
      while (offlineQueue.getQueue().peek() != null && (size - this.queueMaxSize) >= i) {
        this.offlineQueue.getQueue().poll();
        i++;
      }
    }
  }

  /**
   * Play all queued requests, in order.
   */
  private void  dequeue() {
    if (this.offlineQueue.getQueue().size() > 0) {
      try {
        this.emitRequest(((KuzzleQueryObject) this.offlineQueue.getQueue().peek()).getQuery(), ((KuzzleQueryObject) this.offlineQueue.getQueue().poll()).getCb());
      } catch (JSONException e) {
        throw new RuntimeException(e);
      }
      Timer timer = new Timer(UUID.randomUUID().toString());
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          dequeue();
        }
      }, Math.max(0, this.replayInterval));
    } else {
      this.queuing = false;
    }
  }

  /**
   * Delete subscription kuzzle.
   *
   * @param id the id
   * @return the kuzzle
   */
  public Kuzzle deleteSubscription(final String id) {
    this.subscriptions.remove(id);
    return this;
  }

  /**
   * Gets subscriptions.
   *
   * @return the subscriptions
   */
  public Map<String, KuzzleRoom> getSubscriptions() {
    return subscriptions;
  }

  /**
   * Gets request history.
   *
   * @return the request history
   */
  public Map<String, Date> getRequestHistory() {
    return requestHistory;
  }

  /**
   * Gets index.
   *
   * @return the index
   */
  public String getIndex() {
    return this.index;
  }
}
