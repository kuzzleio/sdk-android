package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
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
import io.kuzzle.sdk.listeners.IKuzzleEventListener;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnKuzzleLoginDoneListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleQueue;
import io.kuzzle.sdk.state.KuzzleStates;
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
  private Map<String, Map<String, KuzzleDataCollection>> collections = new HashMap<String, Map<String, KuzzleDataCollection>>();
  private Map<String, KuzzleRoom> subscriptions = new ConcurrentHashMap<String, KuzzleRoom>();
  private Map<String, KuzzleRoom> pendingSubscriptions = new ConcurrentHashMap<String, KuzzleRoom>();
  private boolean autoReconnect = true;
  private JSONObject headers = new JSONObject();
  private JSONObject metadata;
  private String url;
  private KuzzleResponseListener<Void> connectionCallback;
  private KuzzleStates state = KuzzleStates.INITIALIZING;
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
  private String defaultIndex;

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
   * Kuzzle object constructor.
   *
   * @param url                the url
   * @param index              the index
   * @param options            the options
   * @param connectionCallback the connection callback
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(@NonNull final String url, @NonNull final String index, final KuzzleOptions options, final KuzzleResponseListener<Void> connectionCallback) throws URISyntaxException {
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
    this.defaultIndex = index;
    if (socket == null) {
      socket = createSocket(this.url);
    }
    if (options != null && options.getOfflineMode() == Mode.AUTO && this.autoReconnect) {
      this.autoQueue = this.autoReplay = this.autoResubscribe = true;
    }
    if (options == null || options.getConnect() == null || options.getConnect() == Mode.AUTO) {
      connect();
    } else {
      this.state = KuzzleStates.READY;
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
  public Kuzzle(final String url, final String index, final KuzzleResponseListener cb) throws URISyntaxException {
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
  public String addListener(final EventType eventType, final IKuzzleEventListener eventListener) {
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
    Kuzzle.this.state = KuzzleStates.CONNECTING;
    if (socket != null)
      socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = KuzzleStates.CONNECTED;
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
          Kuzzle.this.state = KuzzleStates.ERROR;
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
              throw new RuntimeException(e);
            }
            connectionCallback.onError(error);
          }
        }
      });
    if (socket != null)
      socket.once(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = KuzzleStates.OFFLINE;
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
          Kuzzle.this.state = KuzzleStates.CONNECTED;
          if (Kuzzle.this.autoResubscribe) {
            // Resubscribe
            try {
              renewSubscriptions();
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
   * Data collection factory kuzzle data collection.
   *
   * @param collection the collection
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection dataCollectionFactory(final String collection) {
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
    if (index == null && this.defaultIndex == null) {
      throw new IllegalArgumentException("KuzzleDataCollection: unable to create a new data collection object: no index specified");
    }
    this.isValid();
    if (!this.collections.containsKey(collection)) {
      Map<String, KuzzleDataCollection> col = new HashMap<String, KuzzleDataCollection>();
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
          if (listener != null) {
            try {
              listener.onSuccess(object.getJSONObject("result").getJSONArray("hits"));
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
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
   * Gets statistics.
   *
   * @param listener the listener
   * @return the statistics
   */
  public Kuzzle getStatistics(@NonNull final KuzzleResponseListener<JSONObject> listener) {
    return this.getStatistics((KuzzleOptions)null, listener);
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
          if (listener != null) {
            try {
              listener.onSuccess(response.getJSONObject("result"));
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
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
          if (listener != null) {
            try {
              listener.onSuccess(response.getJSONObject("result").getJSONArray("hits"));
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
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
   * @param username the username
   * @param password the password
   * @return kuzzle kuzzle
   */
  public Kuzzle login(final String strategy, final String username, final String password) {
    return this.login(strategy, username, password, -1, null, null, null);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy  the strategy
   * @param username  the username
   * @param password  the password
   * @param expiresIn the expires in
   * @return kuzzle kuzzle
   */
  public Kuzzle login(final String strategy, final String username, final String password, final int expiresIn) {
    return this.login(strategy, username, password, expiresIn, null, null, null);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy  the strategy
   * @param username  the username
   * @param password  the password
   * @param expiresIn the expires in
   * @param listener  the listener
   * @return kuzzle kuzzle
   */
  public Kuzzle login(final String strategy, final String username, final String password, final int expiresIn, KuzzleResponseListener<Void> listener) {
    return this.login(strategy, username, password, expiresIn, null, listener, null);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy       the strategy
   * @param username       the username
   * @param password       the password
   * @param expiresIn      the expires in
   * @param listener       callback called when strategy's redirectUri is received
   * @param loggedCallback Last collback called when user is logged
   * @return kuzzle kuzzle
   */
  public Kuzzle login(final String strategy, final String username, final String password, final int expiresIn, KuzzleResponseListener<Void> listener, final OnKuzzleLoginDoneListener loggedCallback) {
    return this.login(strategy, username, password, expiresIn, null, listener, loggedCallback);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy the strategy
   * @param username the username
   * @param password the password
   * @param options  the options
   * @return kuzzle kuzzle
   */
  public Kuzzle login(final String strategy, final String username, final String password, final KuzzleOptions options) {
    return this.login(strategy, username, password, -1, options, null, null);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy  the strategy
   * @param username  the username
   * @param password  the password
   * @param expiresIn the expires in
   * @param options   the options
   * @return kuzzle kuzzle
   */
  public Kuzzle login(final String strategy, final String username, final String password, final int expiresIn, final KuzzleOptions options) {
    return this.login(strategy, username, password, expiresIn, options, null, null);
  }

  /**
   * Login kuzzle.
   *
   * @param strategy the strategy
   * @param username the username
   * @param password the password
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle login(final String strategy, final String username, final String password, final KuzzleOptions options, final KuzzleResponseListener<Void> listener) {
    return this.login(strategy, username, password, -1, options, listener, null);
  }

  /**
   * Login kuzzle.
   *
   * @param strategy  the strategy
   * @param username  the username
   * @param password  the password
   * @param expiresIn the expires in
   * @param options   the options
   * @param listener  the listener
   * @return the kuzzle
   */
  public Kuzzle login(final String strategy, final String username, final String password, int expiresIn, final KuzzleOptions options, final KuzzleResponseListener<Void> listener) {
    return this.login(strategy, username, password, expiresIn, options, listener, null);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy       the strategy
   * @param username       the username
   * @param password       the password
   * @param expiresIn      the expires in
   * @param options        the options
   * @param listener       callback called when strategy's redirectUri is received
   * @param loggedCallback Last collback called when user is logged
   * @return kuzzle kuzzle
   */
  public Kuzzle login(final String strategy, final String username, final String password, int expiresIn, final KuzzleOptions options, final KuzzleResponseListener<Void> listener, final OnKuzzleLoginDoneListener loggedCallback) {
    JSONObject query = new JSONObject();
    JSONObject body = new JSONObject();
    try {
      body.put("strategy", strategy);
      body.put("username", username);
      body.put("password", password);
      if (expiresIn >= 0) {
        body.put("expiresIn", expiresIn);
      }
      query.put("body", body);
      loginCallback = loggedCallback;
      QueryArgs args = new QueryArgs();
      args.controller = "auth";
      args.action = "login";
      return this.query(args, query, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          try {
            if (!object.isNull("jwt")) {
              Kuzzle.this.jwtToken = object.getString("jwt");
            }
            if (listener != null) {
              listener.onSuccess(null);
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
      if (url.contains("code")) {
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
            } catch (JSONException e) {
              e.printStackTrace();
            } catch (ProtocolException e) {
              e.printStackTrace();
            } catch (MalformedURLException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }).start();
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
    if (this.socket != null)
      this.socket.close();
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
    return this.logout(null, null);
  }

  /**
   * Logout kuzzle.
   *
   * @param options the options
   * @return the kuzzle
   */
  public Kuzzle logout(final KuzzleOptions options) {
    return this.logout(options, null);
  }

  /**
   * Logout kuzzle.
   *
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle logout(final KuzzleResponseListener<Void> listener) {
    return this.logout(null, listener);
  }

  /**
   * Logout kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle
   */
  public Kuzzle logout(final KuzzleOptions options, final KuzzleResponseListener<Void> listener) {
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
          if (listener != null) {
            try {
              listener.onSuccess(new Date(response.getJSONObject("result").getLong("now")));
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
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
    if (object.isNull("requestId"))
      object.put("requestId", UUID.randomUUID().toString());
    object.put("action", queryArgs.action);
    object.put("controller", queryArgs.controller);

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
    object.put("index", this.defaultIndex);

    if (queryArgs.collection != null) {
      object.put("collection", queryArgs.collection);
    }
    if (queryArgs.index != null) {
      object.put("index", queryArgs.index);
    }
    this.addHeaders(object, this.headers);

    if (this.state == KuzzleStates.CONNECTED || (options != null && !options.isQueuable())) {
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
    } else if (this.queuing || (this.state == KuzzleStates.INITIALIZING || this.state == KuzzleStates.CONNECTING)) {
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

  private void renewSubscriptions() {
    Iterator ite = subscriptions.entrySet().iterator();
    while (ite.hasNext()) {
      Map.Entry e = (Map.Entry) ite.next();
      ((KuzzleRoom)e.getValue()).renew(null, ((KuzzleRoom)e.getValue()).getListener());
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
  public Kuzzle stopQueing() {
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

  private void emitRequest(final JSONObject request, final OnQueryDoneListener listener) throws JSONException {
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
              listener.onError(((JSONObject)args[0]).getJSONObject("error"));
            } else {
              listener.onSuccess((JSONObject)args[0]);
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
}
