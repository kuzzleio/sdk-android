package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.IKuzzleEventListener;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnKuzzleLoginDoneListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleTokenValidity;
import io.kuzzle.sdk.security.KuzzleSecurity;
import io.kuzzle.sdk.state.KuzzleQueue;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.sdk.util.Event;
import io.kuzzle.sdk.util.EventList;
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
  private final int EVENT_TIMEOUT = 200;

  private ConcurrentHashMap<KuzzleEvent, EventList> eventListeners = new ConcurrentHashMap<>();
  private Socket socket;
  private Map<String, Map<String, KuzzleDataCollection>> collections = new ConcurrentHashMap<>();
  private boolean autoReconnect = true;
  private JSONObject headers = new JSONObject();
  private JSONObject metadata;
  private String url;
  private KuzzleResponseListener<Void> connectionCallback;
  protected KuzzleStates state = KuzzleStates.INITIALIZING;
  private long  reconnectionDelay;
  private boolean autoResubscribe;
  private boolean autoQueue;
  private boolean autoReplay;
  /**
   * The Queue filter.
   */
  public QueueFilter queueFilter = new QueueFilter() {
    @Override
    public boolean filter(JSONObject object) {
      return true;
    }
  };
  private long replayInterval;
  private boolean queuing = false;
  private String defaultIndex;

  private ConcurrentHashMap<String, Date> requestHistory = new ConcurrentHashMap<>();

  private KuzzleQueue<KuzzleQueryObject> offlineQueue = new KuzzleQueue<>();
  private int queueTTL;
  private int queueMaxSize;

  private String jwtToken = null;

  /*
   These two properties contain the centralized subscription list in the following format:
    roomId:
      kuzzleRoomID_1: kuzzleRoomInstance_1,
      kuzzleRoomID_2: kuzzleRoomInstance_2,
      kuzzleRoomID_...: kuzzleRoomInstance_...


   This was made to allow multiple subscriptions on the same set of filters,
   something that Kuzzle does not permit.
   This structure also allows renewing subscriptions after a connection loss
   */
  private ConcurrentHashMap<String, ConcurrentHashMap<String, KuzzleRoom>> subscriptions = new ConcurrentHashMap<>();

  // Security static class
  public KuzzleSecurity security;

  /**
   * The type Query args.
   */
  public static class QueryArgs {
    /**
     * The Controller.
     */
    public String controller;
    /**
     * The Action.
     */
    public String action;
    /**
     * The Index.
     */
    public String index;
    /**
     * The Collection.
     */
    public String collection;
  }

  // Listener which is called when an OAuth login is done
  private OnKuzzleLoginDoneListener loginCallback;

  /**
   * Emit an event to all registered listeners
   * An event cannot be emitted multiple times before a timeout has been reached.
   *
   * @param event
   */
  protected void emitEvent(KuzzleEvent event, Object ...args) {
    long now = System.currentTimeMillis();

    if (this.eventListeners.containsKey(event)) {
      EventList l = this.eventListeners.get(event);

      if (l.lastEmitted < now - this.EVENT_TIMEOUT) {
        for(Event e : l.values()) {
          e.trigger(args);
        }

        l.lastEmitted = now;
      }
    }
  }

  /**
   * Kuzzle object constructor.
   *
   * @param url                the url
   * @param options            the options
   * @param connectionCallback the connection callback
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(@NonNull final String url, final KuzzleOptions options, final KuzzleResponseListener<Void> connectionCallback) throws URISyntaxException {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("Url can't be empty");
    }

    KuzzleOptions opt = (options != null ? options : new KuzzleOptions());

    this.autoQueue = opt.isAutoQueue();
    this.autoReconnect = opt.isAutoReconnect();
    this.autoReplay = opt.isAutoReplay();
    this.autoResubscribe = opt.isAutoResubscribe();
    this.defaultIndex = opt.getDefaultIndex();
    this.headers = opt.getHeaders();
    this.metadata = opt.getMetadata();
    this.queueMaxSize = opt.getQueueMaxSize();
    this.queueTTL = opt.getQueueTTL();
    this.reconnectionDelay = opt.getReconnectionDelay();
    this.replayInterval = opt.getReplayInterval();

    this.url = url;
    this.connectionCallback = connectionCallback;

    if (socket == null) {
      socket = createSocket();
    }

    if (opt.getOfflineMode() == Mode.AUTO) {
      this.autoReconnect = this.autoQueue = this.autoReplay = this.autoResubscribe = true;
    }
    if (opt.getConnect() == Mode.AUTO) {
      connect();
    } else {
      this.state = KuzzleStates.READY;
    }

    this.security = new KuzzleSecurity(this);
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param url   the url
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(@NonNull final String url) throws URISyntaxException {
    this(url, null, null);
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param url   the url
   * @param cb    the cb
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(final String url, final KuzzleResponseListener<Void> cb) throws URISyntaxException {
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
   * The ID returned by this function is required to remove this listener at a later time.
   *
   * @param kuzzleEvent     - name of the global event to subscribe to
   * @param listener the event listener
   * @return {string} Unique listener ID
   */
  public String addListener(final KuzzleEvent kuzzleEvent, final IKuzzleEventListener listener) {
    this.isValid();

    Event e = new Event(kuzzleEvent) {
      @Override
      public void trigger(Object... args) {
        listener.trigger(args);
      }
    };

    if (!eventListeners.containsKey(kuzzleEvent)) {
      eventListeners.put(kuzzleEvent, new EventList());
    }

    String id = e.getId().toString();
    eventListeners.get(kuzzleEvent).put(id, e);
    return id;
  }

  /**
   * Check token kuzzle.
   *
   * @param token    the token
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle checkToken(@NonNull final String token, @NonNull final KuzzleResponseListener<KuzzleTokenValidity> listener) {
    return this.checkToken(token, null, listener);
  }

  /**
   * Check token kuzzle.
   *
   * @param token    the token
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle checkToken(@NonNull final String token, final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleTokenValidity> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.checkToken: listener required");
    }
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("Kuzzle.checkToken: token required");
    }
    try {
      QueryArgs args = new QueryArgs();
      args.controller = "auth";
      args.action = "checkToken";
      JSONObject request = new JSONObject();
      request.put("body", new JSONObject().put("token", token));
      this.query(args, request, options, new OnQueryDoneListener() {

        @Override
        public void onSuccess(JSONObject response) {
          try {
            KuzzleTokenValidity validity = new KuzzleTokenValidity();
            JSONObject result = response.getJSONObject("result");
            validity.setValid(result.getBoolean("valid"));
            if (validity.isValid()) {
              validity.setExpiresAt(new Date(result.getLong("expiresAt")));
            } else {
              validity.setState(result.getString("state"));
            }
            listener.onSuccess(validity);
          } catch (JSONException e) {
            throw new RuntimeException();
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Connects to a Kuzzle instance using the provided URL.
   *
   * @return kuzzle kuzzle
   */
  public Kuzzle connect() throws URISyntaxException {
    if (!this.isValidState()) {
      if (connectionCallback != null) {
        connectionCallback.onSuccess(null);
        return this;
      }
    }

    if (this.socket == null) {
      this.socket = createSocket();
    }

    Kuzzle.this.state = KuzzleStates.CONNECTING;

    if (socket != null) {
      socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = KuzzleStates.CONNECTED;

          Kuzzle.this.renewSubscriptions();
          Kuzzle.this.dequeue();
          Kuzzle.this.emitEvent(KuzzleEvent.connected);

          if (Kuzzle.this.connectionCallback != null) {
            Kuzzle.this.connectionCallback.onSuccess(null);
          }
        }
      });
    }

    if (socket != null) {
      socket.once(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = KuzzleStates.ERROR;
          Kuzzle.this.emitEvent(KuzzleEvent.error, args);

          if (connectionCallback != null) {
            JSONObject error = new JSONObject();
            try {
              error.put("message", ((EngineIOException) args[0]).getMessage());
              error.put("code", ((EngineIOException) args[0]).code);
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
            connectionCallback.onError(error);
          }
        }
      });
    }

    if (socket != null) {
      socket.once(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = KuzzleStates.OFFLINE;
          if (!Kuzzle.this.autoReconnect) {
            Kuzzle.this.disconnect();
          }
          if (Kuzzle.this.autoQueue) {
            Kuzzle.this.queuing = true;
          }

          Kuzzle.this.emitEvent(KuzzleEvent.disconnected);
        }
      });
    }

    if (socket != null) {
      socket.once(Socket.EVENT_RECONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = KuzzleStates.CONNECTED;

          if (Kuzzle.this.jwtToken != null) {
            Kuzzle.this.checkToken(jwtToken, new KuzzleResponseListener<KuzzleTokenValidity>() {
              @Override
              public void onSuccess(KuzzleTokenValidity response) {
                if (!response.isValid()) {
                  Kuzzle.this.jwtToken = null;
                  Kuzzle.this.emitEvent(KuzzleEvent.jwtTokenExpired);
                }

                Kuzzle.this.reconnect();
              }

              @Override
              public void onError(JSONObject error) {
                Kuzzle.this.jwtToken = null;
                Kuzzle.this.emitEvent(KuzzleEvent.jwtTokenExpired);
                Kuzzle.this.reconnect();
              }
            });
          } else {
            Kuzzle.this.reconnect();
          }
        }
      });
    }

    if (socket != null) {
      socket.connect();
    }

    return this;
  }

  /**
   * Data collection factory kuzzle data collection.
   *
   * @param collection the collection
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection dataCollectionFactory(@NonNull final String collection) {
    this.isValid();
    if (this.defaultIndex == null) {
      throw new IllegalArgumentException("KuzzleDataCollection: unable to create a new data collection object: no index specified");
    }

    return this.dataCollectionFactory(this.defaultIndex, collection);
  }

  /**
   * Create a new instance of a KuzzleDataCollection object
   *
   * @param index      the index
   * @param collection - The name of the data collection you want to manipulate
   * @return {object} A KuzzleDataCollection instance
   */
  public KuzzleDataCollection dataCollectionFactory(@NonNull final String index, final String collection) {
    this.isValid();
    if (index == null && this.defaultIndex == null) {
      throw new IllegalArgumentException("KuzzleDataCollection: unable to create a new data collection object: no index specified");
    }

    if (!this.collections.containsKey(collection)) {
      Map<String, KuzzleDataCollection> col = new ConcurrentHashMap<>();
      col.put(collection, new KuzzleDataCollection(this, index, collection));
      this.collections.put(index, col);
    }
    return this.collections.get(index).get(collection);
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
   * Gets all statistics.
   *
   * @param listener the listener
   * @return the all statistics
   */
  public Kuzzle getAllStatistics(@NonNull final KuzzleResponseListener<JSONArray> listener) {
    return this.getAllStatistics(null, listener);
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests.
   * This method returns all available statistics from Kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   * @return the all statistics
   */
  public Kuzzle getAllStatistics(final KuzzleOptions options, @NonNull final KuzzleResponseListener<JSONArray> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.getAllStatistics: listener required");
    }

    this.isValid();
    try {
      QueryArgs args = new QueryArgs();
      args.controller = "admin";
      args.action = "getAllStats";
      this.query(args, null, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          try {
            listener.onSuccess(object.getJSONObject("result").getJSONArray("hits"));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Gets statistics.
   *
   * @param listener the listener
   * @return the statistics
   */
  public Kuzzle getStatistics(@NonNull final KuzzleResponseListener<JSONObject> listener) {
    return this.getStatistics((KuzzleOptions) null, listener);
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests.
   * This method allows getting the last statistics frame
   *
   * @param options  the options
   * @param listener the listener
   * @return statistics statistics
   */
  public Kuzzle getStatistics(final KuzzleOptions options, @NonNull final KuzzleResponseListener<JSONObject> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.getStatistics: listener required");
    }
    this.isValid();
    JSONObject body = new JSONObject();
    JSONObject data = new JSONObject();
    try {
      body.put("body", data);
      QueryArgs args = new QueryArgs();
      args.controller = "admin";
      args.action = "getLastStats";
      this.query(args, body, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result"));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Gets statistics.
   *
   * @param timestamp the timestamp
   * @param listener  the listener
   * @return the statistics
   */
  public Kuzzle getStatistics(@NonNull final String timestamp, @NonNull final KuzzleResponseListener<JSONArray> listener) {
    return this.getStatistics(timestamp, null, listener);
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests starting from a provided timestamp
   *
   * @param timestamp the timestamp
   * @param options   the options
   * @param listener  the listener
   * @return statistics statistics
   */
  public Kuzzle getStatistics(@NonNull final String timestamp, final KuzzleOptions options, @NonNull final KuzzleResponseListener<JSONArray> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.getStatistics: listener required");
    }
    if (timestamp == null) {
      throw new IllegalArgumentException("Kuzzle.getStatistics: timestamp required");
    }
    this.isValid();
    JSONObject body = new JSONObject();
    JSONObject data = new JSONObject();
    try {
      data.put("since", timestamp);
      body.put("body", data);
      QueryArgs args = new QueryArgs();
      args.controller = "admin";
      args.action = "getStats";
      this.query(args, body, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result").getJSONArray("hits"));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Gets server info.
   *
   * @param listener the listener
   * @return the server info
   */
  public Kuzzle getServerInfo(@NonNull final KuzzleResponseListener<JSONObject> listener) {
    return this.getServerInfo(null, listener);
  }

  /**
   * Gets server info.
   *
   * @param options  the options
   * @param listener the listener
   * @return the server info
   */
  public Kuzzle getServerInfo(final KuzzleOptions options, @NonNull final KuzzleResponseListener<JSONObject> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.getServerInfo: listener required");
    }
    QueryArgs args = new QueryArgs();
    args.controller = "read";
    args.action = "serverInfo";
    try {
      this.query(args, null, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result").getJSONObject("serverInfo"));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
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
   * @param listener the listener
   * @return kuzzle kuzzle
   */
  public Kuzzle listCollections(@NonNull final KuzzleResponseListener<JSONObject> listener) {
    return this.listCollections(null, null, listener);
  }

  /**
   * List collections kuzzle.
   *
   * @param index    the index
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle listCollections(String index, @NonNull final KuzzleResponseListener<JSONObject> listener) {
    return this.listCollections(index, null, listener);
  }

  /**
   * List collections kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle listCollections(KuzzleOptions options, @NonNull final KuzzleResponseListener<JSONObject> listener) {
    return this.listCollections(null, options, listener);
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @param index    the index
   * @param options  the options
   * @param listener the listener
   * @return kuzzle kuzzle
   */
  public Kuzzle listCollections(String index, KuzzleOptions options, @NonNull final KuzzleResponseListener<JSONObject> listener) {
    if (index == null) {
      if (this.defaultIndex == null) {
        throw new IllegalArgumentException("Kuzzle.listCollections: index required");
      } else {
        index = this.defaultIndex;
      }
    }
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.listCollections: listener required");
    }
    try {
      QueryArgs args = new QueryArgs();
      args.controller = "read";
      args.action = "listCollections";
      args.index = index;
      JSONObject query = new JSONObject();
      if (options == null) {
        options = new KuzzleOptions();
      }
      query.put("body", new JSONObject().put("type", options.getCollectionType()));
      return this.query(args, query, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject collections) {
          try {
            listener.onSuccess(collections.getJSONObject("result").getJSONObject("collections"));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * List indexes kuzzle.
   *
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle listIndexes(@NonNull final KuzzleResponseListener<String[]> listener) {
    return this.listIndexes(null, listener);
  }

  /**
   * List indexes kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle listIndexes(final KuzzleOptions options, @NonNull final KuzzleResponseListener<String[]> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.listIndexes: listener required");
    }
    QueryArgs args = new QueryArgs();
    args.controller = "read";
    args.action = "listIndexes";
    try {
      this.query(args, null, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONArray array = response.getJSONObject("result").getJSONArray("hits");
            int length = array.length();
            String[] indexes = new String[length];
            for (int i = 0; i < length; i++) {
              indexes[i] = array.getString(i);
            }
            listener.onSuccess(indexes);
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy the strategy
   * @param credentials    login credentials
   * @return kuzzle kuzzle
   */
  public Kuzzle login(@NonNull final String strategy, @NonNull final JSONObject credentials) {
    return this.login(strategy, credentials, -1, null, null);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy  the strategy
   * @param credentials    login credentials
   * @param expiresIn the expires in
   * @return kuzzle kuzzle
   */
  public Kuzzle login(@NonNull final String strategy, @NonNull final JSONObject credentials, final int expiresIn) {
    return this.login(strategy, credentials, expiresIn, null, null);
  }

  /**
   * Login kuzzle.
   *
   * @param strategy the strategy
   * @param credentials    login credentials
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle login(@NonNull final String strategy, @NonNull final JSONObject credentials, final KuzzleResponseListener<JSONObject> listener) {
    return this.login(strategy, credentials, -1, listener, null);
  }

  /**
   * Login kuzzle.
   *
   * @param strategy      the strategy
   * @param credentials   login credentials
   * @param expiresIn     the expires in
   * @param listener      the listener
   * @return the kuzzle
   */
  public Kuzzle login(@NonNull final String strategy, @NonNull final JSONObject credentials, int expiresIn, final KuzzleResponseListener<JSONObject> listener) {
    return this.login(strategy, credentials, expiresIn, listener, null);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy       the strategy
   * @param credentials    login credentials
   * @param expiresIn      the expires in
   * @param listener       callback called when strategy's redirectUri is received
   * @param loggedCallback Last callback called when user is logged
   * @return kuzzle kuzzle
   */
  public Kuzzle login(@NonNull final String strategy, @NonNull final JSONObject credentials, int expiresIn, final KuzzleResponseListener<JSONObject> listener, final OnKuzzleLoginDoneListener loggedCallback) {
    if (strategy == null) {
      throw new IllegalArgumentException("Kuzzle.login: cannot authenticate to Kuzzle without an authentication strategy");
    }

    if (credentials == null) {
      throw new IllegalArgumentException("Kuzzle.login: cannot authenticate with null credentials");
    }

    try {
      KuzzleOptions options = new KuzzleOptions();
      JSONObject query = new JSONObject();
      JSONObject body = new JSONObject(credentials.toString()).put("strategy", strategy);

      if (expiresIn >= 0) {
        body.put("expiresIn", expiresIn);
      }

      query.put("body", body);
      loginCallback = loggedCallback;
      QueryArgs args = new QueryArgs();
      args.controller = "auth";
      args.action = "login";
      options.setQueuable(false);

      return this.query(args, query, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          try {
            JSONObject result = object.getJSONObject("result");

            if (!result.isNull("jwt")) {
              Kuzzle.this.jwtToken = result.getString("jwt");
              Kuzzle.this.renewSubscriptions();
            }
            if (listener != null) {
              listener.onSuccess(object);
            }
          } catch (JSONException e) {
            throw new RuntimeException(e);
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
   * WebViewClient to forward kuzzle's jwt token after an OAuth authentication
   */
  private class KuzzleWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, final String url) {
      Log.e("url", url);
      if (url.contains("code=")) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              HttpURLConnection conn = (HttpURLConnection)  URI.create(url).toURL().openConnection();
              conn.setRequestMethod("GET");
              conn.setUseCaches(false);

              BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
              StringBuilder sb = new StringBuilder();
              String line;
              while ((line = br.readLine()) != null) {
                sb.append(line);
              }
              br.close();

              Log.e("login", sb.toString());
              JSONObject response = new JSONObject(sb.toString());
              if (response.isNull("error")) {
                JSONObject result = response.getJSONObject("result");
                Kuzzle.this.jwtToken = result.getString("jwt");
                if (loginCallback != null) {
                  loginCallback.onSuccess(result);
                }
              } else {
                if (loginCallback != null) {
                  loginCallback.onError(response.getJSONObject("error"));
                }
              }
            } catch (JSONException|IOException e) {
              e.printStackTrace();
            }
          }
        }).start();
      } else {
        view.loadUrl(url);
      }
      return true;
    }
  }

  /**
   * Gets kuzzle web view client.
   *
   * @return the kuzzle web view client
   */
  public KuzzleWebViewClient getKuzzleWebViewClient() {
    return new KuzzleWebViewClient();
  }

  /**
   * Disconnects from Kuzzle and invalidate this instance.
   * Does not fire a disconnected event.
   */
  public void disconnect() {
    if (this.state == KuzzleStates.CONNECTED) {
      this.logout();
    }

    if (this.socket != null) {
      this.socket.close();
    }

    this.socket = null;
    this.collections.clear();
    this.state = KuzzleStates.DISCONNECTED;
  }

  /**
   * Logout kuzzle.
   *
   * @return the kuzzle
   */
  public Kuzzle logout() {
    return this.logout(null);
  }

  /**
   * Logout kuzzle.
   *
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle logout(final KuzzleResponseListener<Void> listener) {
    KuzzleOptions options = new KuzzleOptions();

    options.setQueuable(false);

    try {
      QueryArgs args = new QueryArgs();
      args.controller = "auth";
      args.action = "logout";
      return this.query(args, new JSONObject(), options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          Kuzzle.this.jwtToken = null;
          if (listener != null) {
            listener.onSuccess(null);
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
   * Now kuzzle.
   *
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle now(@NonNull final KuzzleResponseListener<Date> listener) {
    return this.now(null, listener);
  }

  /**
   * Returns the current Kuzzle UTC timestamp
   *
   * @param options  the options
   * @param listener the listener
   * @return kuzzle timestamp
   */
  public Kuzzle now(final KuzzleOptions options, @NonNull final KuzzleResponseListener<Date> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.now: listener required");
    }
    this.isValid();
    try {
      QueryArgs args = new QueryArgs();
      args.controller = "read";
      args.action = "now";
      this.query(args, null, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(new Date(response.getJSONObject("result").getLong("now")));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Query kuzzle.
   *
   * @param queryArgs the query args
   * @param query     the query
   * @return the kuzzle
   * @throws JSONException the json exception
   */
  public Kuzzle query(final QueryArgs queryArgs, final JSONObject query) throws JSONException {
    return this.query(queryArgs, query, null, null);
  }

  /**
   * Query kuzzle.
   *
   * @param queryArgs the query args
   * @param query     the query
   * @param options   the options
   * @return the kuzzle
   * @throws JSONException the json exception
   */
  public Kuzzle query(final QueryArgs queryArgs, final JSONObject query, final KuzzleOptions options) throws JSONException {
    return this.query(queryArgs, query, options, null);
  }

  /**
   * Query kuzzle.
   *
   * @param queryArgs the query args
   * @param query     the query
   * @param listener  the listener
   * @return the kuzzle
   * @throws JSONException the json exception
   */
  public Kuzzle query(final QueryArgs queryArgs, final JSONObject query, final OnQueryDoneListener listener) throws JSONException {
    return this.query(queryArgs, query, null, listener);
  }

  /**
   * This is a low-level method, exposed to allow advanced SDK users to bypass high-level methods.
   * Base method used to send read queries to Kuzzle
   *
   * @param queryArgs the query args
   * @param query     - The query data
   * @param options   the options
   * @param listener  the listener
   * @return the kuzzle
   * @throws JSONException the json exception
   */
  public Kuzzle query(final QueryArgs queryArgs, final JSONObject query, final KuzzleOptions options, final OnQueryDoneListener listener) throws JSONException {
    this.isValid();
    JSONObject object = query != null ? query : new JSONObject();

    if (object.isNull("requestId")) {
      object.put("requestId", UUID.randomUUID().toString());
    }

    object
      .put("action", queryArgs.action)
      .put("controller", queryArgs.controller);

    // Global metadata
    JSONObject meta = new JSONObject();
    for (Iterator ite = this.metadata.keys(); ite.hasNext();) {
      String key = (String) ite.next();
      meta.put(key, this.metadata.get(key));
    }

    // Metadata for this query
    if (options != null) {
      if (!options.isQueuable() && this.state == KuzzleStates.OFFLINE) {
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

    if (queryArgs.collection != null) {
      object.put("collection", queryArgs.collection);
    }

    if (queryArgs.index != null) {
      object.put("index", queryArgs.index);
    }

    this.addHeaders(object, this.headers);

    /*
     * Do not add the token for the checkToken route, to avoid getting a token error when
     * a developer simply wish to verify his token
     */
    if (this.jwtToken != null && !(queryArgs.controller.equals("auth") && queryArgs.action.equals("checkToken"))) {
      if (!object.has("headers")) {
        object.put("headers", new JSONObject());
      }

      object.getJSONObject("headers").put("authorization", "Bearer " + this.jwtToken);
    }

    if (this.state == KuzzleStates.CONNECTED || (options != null && !options.isQueuable())) {
      if (this.state == KuzzleStates.CONNECTED) {
        emitRequest(object, new OnQueryDoneListener() {
          @Override
          public void onSuccess(JSONObject response) {
            if (listener != null) {
              listener.onSuccess(response);
            }
          }

          @Override
          public void onError(JSONObject error) {
            if (error != null) {
              listener.onError(error);
            }
          }
        });
      }
      else if (listener != null) {
        listener.onError(new JSONObject().put("message", "Unable to execute request: not connected to a Kuzzle server.\\nDiscarded request: " + object.toString()));
      }
    } else if (this.queuing || this.state == KuzzleStates.INITIALIZING || this.state == KuzzleStates.CONNECTING) {
      cleanQueue();

      if (queueFilter.filter(object)) {
        KuzzleQueryObject o = new KuzzleQueryObject();
        o.setTimestamp(new Date());
        o.setCb(listener);
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
  public Kuzzle removeAllListeners(KuzzleEvent type) {
    if (eventListeners.containsKey(type)) {
      eventListeners.get(type).clear();
    }

    return this;
  }

  /**
   * Removes a listener from an event.
   *
   * @param event the type
   * @param listenerId the listener id
   * @return the kuzzle
   */
  public Kuzzle removeListener(KuzzleEvent event, String listenerId) {
    if (eventListeners.containsKey(event)) {
      eventListeners.get(event).remove(listenerId);
    }

    return this;
  }

  private void renewSubscriptions() {
    for(Map<String, KuzzleRoom> roomSubscriptions: subscriptions.values()) {
      for (KuzzleRoom room : roomSubscriptions.values()) {
        room.renew(room.getListener());
      }
    }
  }

  /**
   * Replays the requests queued during offline mode.
   * Works only if the SDK is not in a disconnected state, and if the autoReplay option is set to false.
   *
   * @return kuzzle kuzzle
   */
  public Kuzzle replayQueue() {
    if (this.state != KuzzleStates.OFFLINE && !this.autoReplay) {
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
  public Kuzzle setHeaders(final JSONObject content) throws JSONException {
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
  public Kuzzle setHeaders(final JSONObject content, boolean replace) {
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
    if (this.state == KuzzleStates.OFFLINE && !this.autoQueue) {
      this.queuing = true;
    }
    return this;
  }

  /**
   * Stops the requests queuing. Works only during offline mode, and if the autoQueue option is set to false.
   *
   * @return kuzzle kuzzle
   */
  public Kuzzle stopQueuing() {
    if (this.state == KuzzleStates.OFFLINE && !this.autoQueue) {
      this.queuing = false;
    }
    return this;
  }

  /**
   * Is valid sate boolean.
   *
   * @return the boolean
   */
  protected boolean isValidState() {
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

  /**
   * Handles network reconnection
   */
  private void reconnect() {
    if (this.autoResubscribe) {
      this.renewSubscriptions();
    }

    if (this.autoReplay) {
      this.cleanQueue();
      this.dequeue();
    }

    this.emitEvent(KuzzleEvent.reconnected);
  }

  private Socket createSocket() throws URISyntaxException {
    IO.Options opt = new IO.Options();
    opt.forceNew = true;
    opt.reconnection = this.autoReconnect;
    opt.reconnectionDelay = this.reconnectionDelay;
    return IO.socket(this.url, opt);
  }

  private void emitRequest(final JSONObject request, final OnQueryDoneListener listener) throws JSONException {
    Date now = new Date();
    Calendar c = Calendar.getInstance();
    c.setTime(now);
    c.add(Calendar.SECOND, -MAX_EMIT_TIMEOUT);

    if (this.jwtToken != null || listener != null) {
      socket.once(request.get("requestId").toString(), new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          try {
            // checking token expiration
            if (!((JSONObject) args[0]).isNull("error") && ((JSONObject) args[0]).getJSONObject("error").getString("message").equals("Token expired")) {
              emitEvent(KuzzleEvent.jwtTokenExpired, listener);
            }

            if (listener != null) {
              if (!((JSONObject) args[0]).isNull("error")) {
                listener.onError(((JSONObject) args[0]).getJSONObject("error"));
              } else {
                listener.onSuccess((JSONObject) args[0]);
              }
            }
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      });
    }

    socket.emit("kuzzle", request);

    // Track requests made to allow KuzzleRoom.subscribeToSelf to work
    this.requestHistory.put(request.getString("requestId"), new Date());

    // Clean history from requests made more than 10s ago
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
    if (this.state == KuzzleStates.DISCONNECTED) {
      throw new RuntimeException("This Kuzzle object has been invalidated. Did you try to access it after a disconnect call?");
    }
  }

  /**
   * Helper function copying headers to the query data
   *
   * @param query   the query
   * @param headers the headers
   */
  public void addHeaders(final JSONObject query, final JSONObject headers) {
    for (Iterator iterator = headers.keys(); iterator.hasNext(); ) {
      String key = (String) iterator.next();
      if (query.isNull(key)) {
        try {
          query.put(key, headers.get(key));
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
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
  protected Kuzzle addPendingSubscription(final String id, final KuzzleRoom room) {
    ConcurrentHashMap<String, KuzzleRoom> pending = this.subscriptions.get("pending");

    if (pending == null) {
      pending = new ConcurrentHashMap<>();
      this.subscriptions.put("pending", pending);
    }

    pending.put(id, room);

    return this;
  }

  /**
   * Delete pending subscription kuzzle.
   *
   * @param id the id
   * @return the kuzzle
   */
  protected Kuzzle deletePendingSubscription(final String id) {
    ConcurrentHashMap<String, KuzzleRoom> pending = this.subscriptions.get("pending");

    if (pending != null) {
      pending.remove(id);
    }

    return this;
  }

  /**
   * Add subscription kuzzle.
   *
   * @param roomId Room's unique ID
   * @param id   KuzzleRoom object unique ID
   * @param kuzzleRoom KuzzleRoom instance
   * @return kuzzle kuzzle
   */
  protected Kuzzle addSubscription(final String roomId, final String id, final KuzzleRoom kuzzleRoom) {
    ConcurrentHashMap<String, KuzzleRoom> room = this.subscriptions.get(roomId);

    if (room == null) {
      room = new ConcurrentHashMap<>();
      this.subscriptions.put(id, room);
    }

    room.put(id, kuzzleRoom);

    return this;
  }

  /**
   * Gets socket.
   *
   * @return the socket
   */
  protected Socket getSocket() {
    return socket;
  }

  /**
   * Sets socket.
   *
   * @param socket the socket
   */
  protected void setSocket(Socket socket) {
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
  public void setAutoQueue(final boolean autoQueue) {
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
  protected Kuzzle deleteSubscription(final String roomId, final String id) {
    if (this.subscriptions.containsKey(roomId)) {
      this.subscriptions.get(roomId).remove(id);

      if (this.subscriptions.get(roomId).isEmpty()) {
        this.subscriptions.remove(roomId);
      }
    }

    return this;
  }

  /**
   * Gets subscriptions.
   *
   * @return the subscriptions
   */
  protected Map<String, KuzzleRoom> getSubscriptions(String roomId) {
    return this.subscriptions.get(roomId);
  }

  /**
   * Getter for the pendingSubscriptions private property
   *
   * @return
   */
  protected Map<String, KuzzleRoom> getPendingSubscriptions() {
    return this.subscriptions.get("pending");
  }

  /**
   * Gets request history.
   *
   * @return the request history
   */
  protected Map<String, Date> getRequestHistory() {
    return requestHistory;
  }

  /**
   * Gets index.
   *
   * @return the index
   */
  public String getDefaultIndex() {
    return this.defaultIndex;
  }

  /**
   * Sets default index.
   *
   * @param index the index
   * @return the default index
   */
  public Kuzzle setDefaultIndex(@NonNull final String index) {
    if (index == null || index.isEmpty()) {
      throw new IllegalArgumentException("Kuzzle.setDefaultIndex: index required");
    }
    this.defaultIndex = index;
    return this;
  }

  public Kuzzle setJwtToken(final String jwtToken) {
    this.jwtToken = jwtToken;
    return this;
  }

  public String getJwtToken() {
    return this.jwtToken;
  }

  /**
   * Retrieves current user information
   *
   * @param listener the listener
   * @return kuzzle
   */
  public Kuzzle whoAmI(@NonNull final KuzzleResponseListener<JSONObject> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.whoAmI: listener required");
    }

    try {
      QueryArgs args = new QueryArgs();
      args.controller = "auth";
      args.action = "getCurrentUser";
      JSONObject request = new JSONObject();

      this.query(args, request, null, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject result = response.getJSONObject("result");
            listener.onSuccess(result);
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

}
