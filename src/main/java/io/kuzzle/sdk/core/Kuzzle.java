package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
import io.kuzzle.sdk.exceptions.KuzzleException;
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
  public QueueFilter queueFilter = new QueueFilter() {
    @Override
    public boolean filter(JSONObject object) {
      return true;
    }
  };
  private long replayInterval = 10;
  private boolean queuing = false;
  private String index;

  private Map<String, Date> requestHistory = new HashMap<String, Date>();

  private KuzzleQueue<KuzzleQueryObject> offlineQueue = new KuzzleQueue<KuzzleQueryObject>();
  private int queueTTL;
  private int queueMaxSize;

  /**
   * Kuzzle object constructor.
   *
   * @param url                the url
   * @param options            the options
   * @param connectionCallback the connection callback
   * @throws URISyntaxException       the uri syntax exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public Kuzzle(final String url, final KuzzleOptions options, final ResponseListener connectionCallback) throws URISyntaxException {
    if (url == null || url.isEmpty())
      throw new IllegalArgumentException("Url can't be empty");
    this.autoReconnect = (options != null ? options.isAutoReconnect() : true);
    this.headers = (options != null && options.getHeaders() != null ? options.getHeaders() : new JSONObject());
    this.metadata = (options != null && options.getMetadata() != null ? options.getMetadata() : new JSONObject());
    this.reconnectionDelay = (options != null ? options.getReconnectionDelay() : 1000);
    this.queueTTL = (options != null ? options.getQueueTTL() : 0);
    this.autoReplay = (options != null ? options.isAutoReplay() : false);
    this.queueMaxSize = (options != null ? options.getQueueMaxSize() : 0);
    this.url = url;
    this.connectionCallback = connectionCallback;
    this.index = (options != null ? options.getIndex() : "mainindex");
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
   * @param url the url
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(final String url) throws URISyntaxException {
    this(url, null, null);
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param url the url
   * @param cb  the cb
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(final String url, final ResponseListener cb) throws URISyntaxException {
    this(url, null, cb);
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param url     the url
   * @param options the options
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(String url, KuzzleOptions options) throws URISyntaxException {
    this(url, options, null);
  }

  /**
   * Adds a listener to a Kuzzle global event. When an event is fired, listeners are called in the order of their
   * insertion.
   * <p/>
   * The ID returned by this function is required to remove this listener at a later time.
   *
   * @param eventType     - name of the global event to subscribe to
   * @param eventListener the event listener
   * @return {string} Unique listener ID
   * @throws KuzzleException the kuzzle exception
   */
  public String addListener(final EventType eventType, final IEventListener eventListener) throws KuzzleException {
    this.isValid();

    Event e = new Event(eventType) {
      @Override
      public void trigger(String subscriptionId, JSONObject result) {
        eventListener.trigger(subscriptionId, result);
      }
    };
    eventListeners.add(e);
    return e.getId().toString();
  }

  /**
   * Connects to a Kuzzle instance using the provided URL.
   *
   * @return
   * @throws Exception
   */
  public Kuzzle connect() {
    if (!this.isValidSate()) {
      if (connectionCallback != null) {
        connectionCallback.onSuccess(null);
        return this;
      }
    }
    Kuzzle.this.state = States.CONNECTING;
    socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        Kuzzle.this.state = States.CONNECTED;
        try {
          renewSubscriptions(connectionCallback);
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (Kuzzle.this.connectionCallback != null) {
          try {
            Kuzzle.this.connectionCallback.onSuccess(null);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        try {
          Kuzzle.this.dequeue();
        } catch (JSONException e) {
          e.printStackTrace();
        }
        for (Event e : Kuzzle.this.eventListeners) {
          if (e.getType() == EventType.CONNECTED) {
            try {
              e.trigger(null, null);
            } catch (Exception e1) {
              e1.printStackTrace();
            }
          }
        }
      }
    });
    socket.once(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        Kuzzle.this.state = States.ERROR;
        for (Event e : Kuzzle.this.eventListeners) {
          if (e.getType() == EventType.ERROR) {
            try {
              e.trigger(null, null);
            } catch (Exception e1) {
              e1.printStackTrace();
            }
          }
        }
        if (connectionCallback != null) {
          JSONObject error = new JSONObject();
          try {
            error.put("message", ((EngineIOException)args[0]).getMessage());
            error.put("code", ((EngineIOException)args[0]).code);
            connectionCallback.onSuccess(error);
          } catch (ClassCastException e) {
            e.printStackTrace();
          } catch (JSONException e) {
            e.printStackTrace();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    });
    socket.once(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        Kuzzle.this.state = States.OFFLINE;
        if (Kuzzle.this.autoReconnect) {
          logout();
        }
        if (Kuzzle.this.autoQueue) {
          queuing = true;
        }
        for (Event e : eventListeners) {
          if (e.getType() == EventType.DISCONNECTED) {
            try {
              e.trigger(null, null);
            } catch (Exception e1) {
              e1.printStackTrace();
            }
          }
        }
      }
    });
    socket.once(Socket.EVENT_RECONNECT, new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        Kuzzle.this.state = States.CONNECTED;
        if (Kuzzle.this.autoResubscribe) {
          try {
            renewSubscriptions(connectionCallback);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        //replay queued requests
        if (Kuzzle.this.autoReplay) {
          Kuzzle.this.cleanQueue();
          try {
            Kuzzle.this.dequeue();
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }

        // alert listeners
        for (Event e : Kuzzle.this.eventListeners) {
          if (e.getType() == EventType.RECONNECTED) {
            try {
              e.trigger(null, null);
            } catch (Exception e1) {
              e1.printStackTrace();
            }
          }
        }
      }
    });
    socket.connect();
    return this;
  }

  /**
   * Create a new instance of a KuzzleDataCollection object
   *
   * @param collection - The name of the data collection you want to manipulate
   * @return {object} A KuzzleDataCollection instance
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection dataCollectionFactory(String collection) throws KuzzleException {
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
   * @throws KuzzleException the kuzzle exception
   * @throws IOException     the io exception
   * @throws JSONException   the json exception
   */
  public Kuzzle getAllStatistics(final ResponseListener listener) throws KuzzleException, IOException, JSONException {
    this.isValid();

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
    return this;
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests.
   * This method allows getting the last statistics frame
   *
   * @param listener the listener
   * @return statistics statistics
   * @throws KuzzleException the kuzzle exception
   * @throws IOException     the io exception
   * @throws JSONException   the json exception
   */
  public Kuzzle getStatistics(final ResponseListener listener) throws KuzzleException, IOException, JSONException {
    return this.getStatistics(null, listener);
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests.
   * This method allows getting either the last statistics frame, or a set of frames starting from a provided timestamp.
   *
   * @param since    the timestamp
   * @param listener the listener
   * @return statistics statistics
   * @throws KuzzleException the kuzzle exception
   * @throws IOException     the io exception
   * @throws JSONException   the json exception
   */
  public Kuzzle getStatistics(String since, final ResponseListener listener) throws KuzzleException, IOException, JSONException {
    this.isValid();
    JSONObject body = new JSONObject();
    JSONObject data = new JSONObject();
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
    return this;
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @return
   * @throws JSONException
   * @throws KuzzleException
   * @throws IOException
   */
  public Kuzzle listCollections() throws JSONException, KuzzleException, IOException {
    return this.listCollections(null, null);
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @param options
   * @return
   * @throws JSONException
   * @throws KuzzleException
   * @throws IOException
   */
  public Kuzzle listCollections(KuzzleOptions options) throws JSONException, KuzzleException, IOException {
    return this.listCollections(options, null);
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @param listener
   * @return
   * @throws JSONException
   * @throws KuzzleException
   * @throws IOException
   */
  public Kuzzle listCollections(ResponseListener listener) throws JSONException, KuzzleException, IOException {
    return this.listCollections(null, listener);
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @param options
   * @param listener
   * @return
   * @throws KuzzleException
   * @throws IOException
   * @throws JSONException
   */
  public Kuzzle listCollections(KuzzleOptions options, ResponseListener listener) throws KuzzleException, IOException, JSONException {
    return this.query(null, "read", "listCollections", null, options, listener);
  }

  /**
   * Disconnects from Kuzzle and invalidate this instance.
   * Does not fire a disconnected event.
   */
  public void logout() {
    if (this.socket != null)
      this.socket.close();
    this.socket = null;
    this.collections.clear();
    this.state = States.LOGGED_OFF;
  }

  /**
   * Returns the current Kuzzle UTC timestamp
   *
   * @param cb - Handles the query response
   * @return {integer}
   * @throws KuzzleException the kuzzle exception
   */
  public Kuzzle now(ResponseListener cb) throws KuzzleException, IOException, JSONException {
    this.isValid();

    this.query(null, "read", "now", null, null, cb);
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
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   * @throws KuzzleException the kuzzle exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query) throws JSONException, IOException, KuzzleException {
    return this.query(collection, controller, action, query, null, null);
  }

  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query, KuzzleOptions options) throws KuzzleException, IOException, JSONException {
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
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   * @throws KuzzleException the kuzzle exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query, ResponseListener listener) throws JSONException, IOException, KuzzleException {
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
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   * @throws KuzzleException the kuzzle exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query, KuzzleOptions options, final ResponseListener cb) throws JSONException, IOException, KuzzleException {
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
   * @return
   */
  public Kuzzle removeAllListeners() {
    this.eventListeners.clear();
    return this;
  }

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
   * @param eventType  the event type
   * @param listenerId the listener id
   * @throws KuzzleException the kuzzle exception
   */
  public Kuzzle removeListener(EventType eventType, String listenerId) throws KuzzleException {
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

  private void renewSubscriptions(final ResponseListener listener) throws Exception {
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
   * @return
   * @throws JSONException
   */
  public Kuzzle replayQueue() throws JSONException {
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
   * @throws JSONException the json exception
   */
  public Kuzzle setHeaders(JSONObject content, boolean replace) throws JSONException {
    if (this.headers == null) {
      this.headers = new JSONObject();
    }
    if (replace) {
      this.headers = content;
    } else {
      if (content != null) {
        for (Iterator ite = content.keys(); ite.hasNext(); ) {
          String key = (String) ite.next();
          this.headers.put(key, content.get(key));
        }
      }
    }
    return this;
  }

  /**
   * Starts the requests queuing. Works only during offline mode, and if the autoQueue option is set to false.
   *
   * @return
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
   * @return
   */
  public Kuzzle stopQueing() {
    if (this.state == States.OFFLINE && !this.autoQueue) {
      this.queuing = false;
    }
    return this;
  }

  public boolean isValidSate() {
    switch (this.state) {
      case INITIALIZING:
      case READY:
      case LOGGED_OFF:
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

    if (listener != null) {
      socket.once(request.get("requestId").toString(), new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          if (listener != null) {
            try {
              if (!((JSONObject) args[0]).isNull("error")) {
                listener.onError((JSONObject) ((JSONObject) args[0]).get("error"));
              } else {
                listener.onSuccess((JSONObject) ((JSONObject) args[0]).get("result"));
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      });
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
   *
   * @throws KuzzleException the kuzzle exception
   */
  public void isValid() throws KuzzleException {
    if (this.state == States.LOGGED_OFF) {
      throw new KuzzleException("This Kuzzle object has been invalidated. Did you try to access it after a logout call?");
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
   * @param room
   * @return
   */
  public Kuzzle addPendingSubscription(final String id, final KuzzleRoom room) {
    if (!this.pendingSubscriptions.containsKey(id))
      this.pendingSubscriptions.put(id, room);
    return this;
  }

  public Kuzzle deletePendingSubscription(final String id) {
    pendingSubscriptions.remove(id);
    return this;
  }

  /**
   * @param room
   * @return
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

  public void setOfflineQueue(final KuzzleQueryObject object) {
    this.offlineQueue.addToQueue(object);
  }

  /**
   * @return
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
   * @param queueFilter
   * @return
   */
  public Kuzzle setQueueFilter(QueueFilter queueFilter) {
    this.queueFilter = queueFilter;
    return this;
  }

  /**
   * @return
   */
  public QueueFilter  getQueueFilter() {
    return this.queueFilter;
  }

  public boolean isAutoReplay() {
    return autoReplay;
  }

  public void setAutoReplay(boolean autoReplay) {
    this.autoReplay = autoReplay;
  }

  public boolean isAutoQueue() {
    return autoQueue;
  }

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
  private void  dequeue() throws JSONException {
    if (this.offlineQueue.getQueue().size() > 0) {
      this.emitRequest(((KuzzleQueryObject)this.offlineQueue.getQueue().peek()).getQuery(), ((KuzzleQueryObject)this.offlineQueue.getQueue().peek()).getCb());
      Timer timer = new Timer(UUID.randomUUID().toString());
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          try {
            dequeue();
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      }, Math.max(0, this.replayInterval));
    } else {
      this.queuing = false;
    }
  }

  public Kuzzle deleteSubscription(final String id) {
    this.subscriptions.remove(id);
    return this;
  }

  public Map<String, KuzzleRoom> getSubscriptions() {
    return subscriptions;
  }

  public Map<String, Date> getRequestHistory() {
    return requestHistory;
  }
}
