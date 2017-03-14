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

import io.kuzzle.sdk.enums.Event;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.EventListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.TokenValidity;
import io.kuzzle.sdk.security.Security;
import io.kuzzle.sdk.security.User;
import io.kuzzle.sdk.state.KuzzleQueue;
import io.kuzzle.sdk.state.States;
import io.kuzzle.sdk.util.EventList;
import io.kuzzle.sdk.util.OfflineQueueLoader;
import io.kuzzle.sdk.util.QueryObject;
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

  /**
   * The Event listeners.
   */
  protected ConcurrentHashMap<Event, EventList> eventListeners = new ConcurrentHashMap<>();
  /**
   * The Socket.
   */
  protected Socket socket;
  /**
   * The Collections.
   */
  protected Map<String, Map<String, Collection>> collections = new ConcurrentHashMap<>();
  /**
   * The Auto reconnect.
   */
  protected boolean autoReconnect = true;
  /**
   * The Headers.
   */
  protected JSONObject headers = new JSONObject();
  /**
   * The Metadata.
   */
  protected JSONObject metadata;
  /**
   * The target Kuzzle host
   */
  protected String host;
  /**
   * Target Kuzzle network port
   */
  protected Integer port;
  /**
   * The Connection callback.
   */
  protected ResponseListener<Void> connectionCallback;
  /**
   * The State.
   */
  protected States state = States.INITIALIZING;
  /**
   * The Reconnection delay.
   */
  protected long  reconnectionDelay;
  /**
   * The Auto resubscribe.
   */
  protected boolean autoResubscribe;
  /**
   * The Auto queue.
   */
  protected boolean autoQueue;
  /**
   * The Auto replay.
   */
  protected boolean autoReplay;
  /**
   * The Queue filter.
   */
  protected QueueFilter queueFilter = new QueueFilter() {
    @Override
    public boolean filter(JSONObject object) {
      return true;
    }
  };
  /**
   * The Replay interval.
   */
  protected long replayInterval;
  /**
   * The Queuing.
   */
  protected boolean queuing = false;
  /**
   * The Default index.
   */
  protected String defaultIndex;

  /**
   * The Request history.
   */
  protected ConcurrentHashMap<String, Date> requestHistory = new ConcurrentHashMap<>();

  /**
   * The Offline queue.
   */
  protected KuzzleQueue<QueryObject> offlineQueue = new KuzzleQueue<>();
  /**
   * The Queue ttl.
   */
  protected int queueTTL;
  /**
   * The Queue max size.
   */
  protected int queueMaxSize;

  /**
   * The Jwt token.
   */
  protected String jwtToken = null;

  /**
   * The Subscriptions.
   */
/*
   This property contains the centralized subscription list in the following format:
    roomId:
      kuzzleRoomID_1: kuzzleRoomInstance_1,
      kuzzleRoomID_2: kuzzleRoomInstance_2,
      ...
    pending: // pending subscriptions
      kuzzleRoomID_x: kuzzleRoomInstance_x,
      ...

   This was made to allow multiple subscriptions on the same set of filters,
   something that Kuzzle does not permit.
   This structure also allows renewing subscriptions after a connection loss
   */
  protected ConcurrentHashMap<String, ConcurrentHashMap<String, Room>> subscriptions = new ConcurrentHashMap<>();

  private OfflineQueueLoader offlineQueueLoader;

  /**
   * Security static class
   */
  public Security security;

  private ResponseListener<JSONObject> loginCallback;

  public MemoryStorage memoryStorage;

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

  /**
   * Emit an event to all registered listeners
   * An event cannot be emitted multiple times before a timeout has been reached.
   *
   * @param event the event
   * @param args  the args
   */
  protected void emitEvent(Event event, Object ...args) {
    long now = System.currentTimeMillis();

    if (this.eventListeners.containsKey(event)) {
      EventList l = this.eventListeners.get(event);

      if (l.lastEmitted < now - this.EVENT_TIMEOUT) {
        for(io.kuzzle.sdk.util.Event e : l.values()) {
          e.trigger(args);
        }

        l.lastEmitted = now;
      }
    }
  }

  /**
   * Kuzzle object constructor.
   *
   * @param host               target host name or IP address
   * @param options            the options
   * @param connectionCallback the connection callback
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(@NonNull final String host, final Options options, final ResponseListener<Void> connectionCallback) throws URISyntaxException {
    if (host == null || host.isEmpty()) {
      throw new IllegalArgumentException("Host name/address can't be empty");
    }

    this.host = host;

    Options opt = (options != null ? options : new Options());

    this.autoQueue = opt.isAutoQueue();
    this.autoReconnect = opt.isAutoReconnect();
    this.autoReplay = opt.isAutoReplay();
    this.autoResubscribe = opt.isAutoResubscribe();
    this.defaultIndex = opt.getDefaultIndex();
    this.headers = opt.getHeaders();
    this.metadata = opt.getMetadata();
    this.port = opt.getPort();
    this.queueMaxSize = opt.getQueueMaxSize();
    this.queueTTL = opt.getQueueTTL();
    this.reconnectionDelay = opt.getReconnectionDelay();
    this.replayInterval = opt.getReplayInterval();

    this.connectionCallback = connectionCallback;

    if (opt.getOfflineMode() == Mode.AUTO) {
      this.autoReconnect = this.autoQueue = this.autoReplay = this.autoResubscribe = true;
    }
    if (opt.getConnect() == Mode.AUTO) {
      connect();
    } else {
      this.state = States.READY;
    }

    this.security = new Security(this);
    this.memoryStorage = new MemoryStorage(this);
    this.subscriptions.put("pending", new ConcurrentHashMap<String, Room>());
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param host target Kuzzle host name or IP address
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(@NonNull final String host) throws URISyntaxException {
    this(host, null, null);
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param host target Kuzzle host name or IP address
   * @param cb  the cb
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(@NonNull final String host, final ResponseListener<Void> cb) throws URISyntaxException {
    this(host, null, cb);
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param host target Kuzzle host name or IP address
   * @param options the options
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(@NonNull final String host, Options options) throws URISyntaxException {
    this(host, options, null);
  }

  /**
   * Adds a listener to a Kuzzle global event. When an event is fired, listeners are called in the order of their
   * insertion.
   * The ID returned by this function is required to remove this listener at a later time.
   *
   * @param kuzzleEvent - name of the global event to subscribe to
   * @param listener    the event listener
   * @return {string} Unique listener ID
   */
  public String addListener(final Event kuzzleEvent, final EventListener listener) {
    this.isValid();

    io.kuzzle.sdk.util.Event e = new io.kuzzle.sdk.util.Event(kuzzleEvent) {
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
   */
  public void checkToken(@NonNull final String token, @NonNull final ResponseListener<TokenValidity> listener) {
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
      this.query(args, request, new Options().setQueuable(false), new OnQueryDoneListener() {

        @Override
        public void onSuccess(JSONObject response) {
          try {
            TokenValidity validity = new TokenValidity();
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
  }

  /**
   * Connects to a Kuzzle instance using the provided host and port.
   *
   * @return kuzzle kuzzle
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle connect() throws URISyntaxException {
    if (!this.isValidState()) {
      if (connectionCallback != null) {
        connectionCallback.onSuccess(null);
        return this;
      }
    }

    if (this.socket != null) {
      this.disconnect();
    }

    this.socket = createSocket();

    Kuzzle.this.state = States.CONNECTING;

    if (socket != null) {
      socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = States.CONNECTED;

          Kuzzle.this.renewSubscriptions();
          Kuzzle.this.dequeue();
          Kuzzle.this.emitEvent(Event.connected);

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
          Kuzzle.this.state = States.ERROR;
          Kuzzle.this.emitEvent(Event.error, args);

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
          Kuzzle.this.state = States.OFFLINE;
          if (!Kuzzle.this.autoReconnect) {
            Kuzzle.this.disconnect();
          }
          if (Kuzzle.this.autoQueue) {
            Kuzzle.this.queuing = true;
          }

          Kuzzle.this.emitEvent(Event.disconnected);
        }
      });
    }

    if (socket != null) {
      socket.once(Socket.EVENT_RECONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Kuzzle.this.state = States.CONNECTED;

          if (Kuzzle.this.jwtToken != null) {
            Kuzzle.this.checkToken(jwtToken, new ResponseListener<TokenValidity>() {
              @Override
              public void onSuccess(TokenValidity response) {
                if (!response.isValid()) {
                  Kuzzle.this.jwtToken = null;
                  Kuzzle.this.emitEvent(Event.jwtTokenExpired);
                }

                Kuzzle.this.reconnect();
              }

              @Override
              public void onError(JSONObject error) {
                Kuzzle.this.jwtToken = null;
                Kuzzle.this.emitEvent(Event.jwtTokenExpired);
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
  public Collection collection(@NonNull final String collection) {
    this.isValid();
    if (this.defaultIndex == null) {
      throw new IllegalArgumentException("Collection: unable to create a new data collection object: no index specified");
    }

    return this.collection(collection, this.defaultIndex);
  }

  /**
   * Create a new instance of a Collection object
   *
   * @param collection - The name of the data collection you want to manipulate
   * @param index      the index
   * @return {object} A Collection instance
   */
  public Collection collection(@NonNull final String collection, @NonNull final String index) {
    this.isValid();
    if (index == null && this.defaultIndex == null) {
      throw new IllegalArgumentException("Collection: unable to create a new data collection object: no index specified");
    }

    if (!this.collections.containsKey(collection)) {
      Map<String, Collection> col = new ConcurrentHashMap<>();
      col.put(collection, new Collection(this, collection, index));
      this.collections.put(index, col);
    }
    return this.collections.get(index).get(collection);
  }

  /**
   * Create an index in Kuzzle
   * @param index
   * @param cb
   * @return
   */
  public Kuzzle createIndex(@NonNull final String index, final ResponseListener<JSONObject> cb) {
    return createIndex(index, null, cb);
  }

  /**
   * Create an index in Kuzzle
   * @param index
   * @return
   */
  public Kuzzle createIndex(@NonNull final String index) {
    return createIndex(index, null, null);
  }

  /**
   * Create an index in Kuzzle
   * @param index
   * @param options
   * @param cb
   * @return
   */
  public Kuzzle createIndex(@NonNull final String index, final Options options, final ResponseListener<JSONObject> cb) {
    if (index == null && defaultIndex == null) {
      throw new IllegalArgumentException("Collection.createIndex: index required");
    }

    QueryArgs args = new QueryArgs();
    args.controller = "index";
    args.action = "create";

    JSONObject request = new JSONObject();
    try {
      request.put("index", index == null ? defaultIndex : index);
      this.query(args, request, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            cb.onSuccess(response.getJSONObject("result"));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          cb.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }

    return this;
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
   */
  public void getAllStatistics(@NonNull final ResponseListener<JSONArray> listener) {
    this.getAllStatistics(null, listener);
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests.
   * This method returns all available statistics from Kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   */
  public void getAllStatistics(final Options options, @NonNull final ResponseListener<JSONArray> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.getAllStatistics: listener required");
    }

    this.isValid();
    try {
      QueryArgs args = new QueryArgs();
      args.controller = "server";
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
  }

  /**
   * Gets statistics.
   *
   * @param listener the listener
   */
  public void getStatistics(@NonNull final ResponseListener<JSONObject> listener) {
    this.getStatistics(null, listener);
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests.
   * This method allows getting the last statistics frame
   *
   * @param options  the options
   * @param listener the listener
   */
  public void getStatistics(final Options options, @NonNull final ResponseListener<JSONObject> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.getStatistics: listener required");
    }
    this.isValid();
    JSONObject body = new JSONObject();
    JSONObject data = new JSONObject();
    try {
      body.put("body", data);
      QueryArgs args = new QueryArgs();
      args.controller = "server";
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
  }

  /**
   * Gets statistics.
   *
   * @param timestamp the timestamp
   * @param listener  the listener
   */
  public void getStatistics(long timestamp, @NonNull final ResponseListener<JSONArray> listener) {
    this.getStatistics(timestamp, null, listener);
  }

  /**
   * Kuzzle monitors active connections, and ongoing/completed/failed requests starting from a provided timestamp
   *
   * @param timestamp the timestamp
   * @param options   the options
   * @param listener  the listener
   */
  public void getStatistics(long timestamp, final Options options, @NonNull final ResponseListener<JSONArray> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.getStatistics: listener required");
    }

    this.isValid();
    JSONObject body = new JSONObject();
    JSONObject data = new JSONObject();
    try {
      data.put("since", timestamp);
      body.put("body", data);
      QueryArgs args = new QueryArgs();
      args.controller = "server";
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
  }

  /**
   * Gets server info.
   *
   * @param listener the listener
   */
  public void getServerInfo(@NonNull final ResponseListener<JSONObject> listener) {
    this.getServerInfo(null, listener);
  }

  /**
   * Gets server info.
   *
   * @param options  the options
   * @param listener the listener
   */
  public void getServerInfo(final Options options, @NonNull final ResponseListener<JSONObject> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.getServerInfo: listener required");
    }
    QueryArgs args = new QueryArgs();
    args.controller = "server";
    args.action = "info";
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
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @param listener the listener
   */
  public void listCollections(@NonNull final ResponseListener<JSONObject> listener) {
    this.listCollections(null, null, listener);
  }

  /**
   * List collections kuzzle.
   *
   * @param index    the index
   * @param listener the listener
   */
  public void listCollections(String index, @NonNull final ResponseListener<JSONObject> listener) {
    this.listCollections(index, null, listener);
  }

  /**
   * List collections kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   */
  public void listCollections(Options options, @NonNull final ResponseListener<JSONObject> listener) {
    this.listCollections(null, options, listener);
  }

  /**
   * Returns the list of known persisted data collections.
   *
   * @param index    the index
   * @param options  the options
   * @param listener the listener
   */
  public void listCollections(String index, Options options, @NonNull final ResponseListener<JSONObject> listener) {
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
      args.controller = "collection";
      args.action = "list";
      args.index = index;
      JSONObject query = new JSONObject();
      if (options == null) {
        options = new Options();
      }
      JSONObject body = new JSONObject().put("type", options.getCollectionType());
      query.put("body", body);
      this.query(args, query, options, new OnQueryDoneListener() {
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
   */
  public void listIndexes(@NonNull final ResponseListener<String[]> listener) {
    this.listIndexes(null, listener);
  }

  /**
   * List indexes kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   */
  public void listIndexes(final Options options, @NonNull final ResponseListener<String[]> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.listIndexes: listener required");
    }
    QueryArgs args = new QueryArgs();
    args.controller = "index";
    args.action = "list";
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
  }

  /**
   * Login kuzzle.
   *
   * @param strategy the strategy
   */
  public void login(@NonNull final String strategy) {
    this.login(strategy, null, -1, null);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy    the strategy
   * @param credentials login credentials
   */
  public void login(@NonNull final String strategy, final JSONObject credentials) {
    this.login(strategy, credentials, -1, null);
  }

  /**
   * Login kuzzle.
   *
   * @param strategy  the strategy
   * @param expiresIn the expires in
   */
  public void login(@NonNull final String strategy, final int expiresIn) {
    this.login(strategy, null, expiresIn, null);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy    the strategy
   * @param credentials login credentials
   * @param expiresIn   the expires in
   */
  public void login(@NonNull final String strategy, final JSONObject credentials, final int expiresIn) {
    this.login(strategy, credentials, expiresIn, null);
  }

  /**
   * Login kuzzle.
   *
   * @param strategy    the strategy
   * @param credentials login credentials
   * @param listener    the listener
   */
  public void login(@NonNull final String strategy, final JSONObject credentials, final ResponseListener<JSONObject> listener) {
    this.login(strategy, credentials, -1, listener);
  }

  /**
   * Login kuzzle.
   *
   * @param strategy the strategy
   * @param listener the listener
   */
  public void login(@NonNull final String strategy, final ResponseListener<JSONObject> listener) {
    this.login(strategy, null, -1, listener);
  }

  /**
   * Login kuzzle.
   *
   * @param strategy  the strategy
   * @param expiresIn the expires in
   * @param listener  the listener
   */
  public void login(@NonNull final String strategy, final int expiresIn, final ResponseListener<JSONObject> listener) {
    this.login(strategy, null, expiresIn, listener);
  }

  /**
   * Log a user according to the strategy and credentials.
   *
   * @param strategy    the strategy
   * @param credentials login credentials
   * @param expiresIn   the expires in
   * @param listener    callback called when strategy's redirectUri is received
   */
  public void login(@NonNull final String strategy, final JSONObject credentials, int expiresIn, final ResponseListener<JSONObject> listener) {
    if (strategy == null) {
      throw new IllegalArgumentException("Kuzzle.login: cannot authenticate to Kuzzle without an authentication strategy");
    }

    this.loginCallback = listener;

    try {
      Options options = new Options();
      JSONObject query = new JSONObject();
      JSONObject body;
      if (credentials != null) {
        body = new JSONObject(credentials.toString());
      } else {
        body = new JSONObject().put("strategy", strategy);
      }

      if (expiresIn >= 0) {
        body.put("expiresIn", expiresIn);
      }

      query.put("body", body);
      QueryArgs args = new QueryArgs();
      args.controller = "auth";
      args.action = "login";
      options.setQueuable(false);

      this.query(args, query, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          try {
            JSONObject result = object.getJSONObject("result");

            if (result.has("jwt")) {
              Kuzzle.this.setJwtToken(result.getString("jwt"));
            }

            if (listener != null) {
              listener.onSuccess(result);
            }
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          try {
            emitEvent(Event.loginAttempt, new JSONObject()
                .put("success", false)
                .put("error", error));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
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
  protected class KuzzleWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, final String url) {
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

              JSONObject response = new JSONObject(sb.toString());
              if (response.isNull("error")) {
                Kuzzle.this.setJwtToken(response);

                if (loginCallback != null) {
                  loginCallback.onSuccess(response.getJSONObject("result"));
                }
              } else {
                  emitEvent(Event.loginAttempt, new JSONObject()
                      .put("success", false)
                      .put("error", response.getJSONObject("error")));
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
    if (this.socket != null) {
      this.socket.close();
    }

    this.socket = null;
    this.collections.clear();
    this.state = States.DISCONNECTED;
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
  public Kuzzle logout(final ResponseListener<Void> listener) {
    Options options = new Options();

    options.setQueuable(false);

    try {
      QueryArgs args = new QueryArgs();
      args.controller = "auth";
      args.action = "logout";

      this.query(args, new JSONObject(), options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
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

    Kuzzle.this.jwtToken = null;
    return this;
  }

  /**
   * Now kuzzle.
   *
   * @param listener the listener
   */
  public void now(@NonNull final ResponseListener<Date> listener) {
    this.now(null, listener);
  }

  /**
   * Returns the current Kuzzle UTC timestamp
   *
   * @param options  the options
   * @param listener the listener
   */
  public void now(final Options options, @NonNull final ResponseListener<Date> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.now: listener required");
    }
    this.isValid();
    try {
      QueryArgs args = new QueryArgs();
      args.controller = "server";
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
  public Kuzzle query(final QueryArgs queryArgs, final JSONObject query, final Options options) throws JSONException {
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
   * Base method used to send queries to Kuzzle
   *
   * @param queryArgs the query args
   * @param query     - The query data
   * @param options   the options
   * @param listener  the listener
   * @return the kuzzle
   * @throws JSONException the json exception
   */
  public Kuzzle query(final QueryArgs queryArgs, final JSONObject query, final Options options, final OnQueryDoneListener listener) throws JSONException {
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
      if (!options.isQueuable() && this.state != States.CONNECTED) {
        discardRequest(listener, object);
        return this;
      }

      if (options.getRefresh() != null) {
        object.put("refresh", options.getRefresh());
      }

      if (options.getMetadata() != null) {
        for (Iterator iterator = options.getMetadata().keys(); iterator.hasNext(); ) {
          String key = (String) iterator.next();
          meta.put(key, options.getMetadata().get(key));
        }
      }

      if (options.getFrom() != null) {
        object.put("from", options.getFrom());
      }

      if (options.getSize() != null) {
        object.put("size", options.getSize());
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
      object.put("jwt", this.jwtToken);
    }

    if (this.state == States.CONNECTED || (options != null && !options.isQueuable())) {
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
    } else if (this.queuing || (options != null && options.isQueuable()) || this.state == States.INITIALIZING || this.state == States.CONNECTING) {
      cleanQueue();

      if (queueFilter.filter(object)) {
        QueryObject o = new QueryObject();
        o.setTimestamp(new Date());
        o.setCb(listener);
        o.setQuery(object);
        this.offlineQueue.addToQueue(o);
        Kuzzle.this.emitEvent(Event.offlineQueuePush, o);
      }
    } else {
      discardRequest(listener, object);
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
  public Kuzzle removeAllListeners(Event type) {
    if (eventListeners.containsKey(type)) {
      eventListeners.get(type).clear();
    }

    return this;
  }

  /**
   * Removes a listener from an event.
   *
   * @param event      the type
   * @param listenerId the listener id
   * @return the kuzzle
   */
  public Kuzzle removeListener(Event event, String listenerId) {
    if (eventListeners.containsKey(event)) {
      eventListeners.get(event).remove(listenerId);
    }

    return this;
  }

  /**
   * Renew all registered subscriptions. Usually called after:
   * - a connection, if subscriptions occured before
   * - a reconnection
   * - after a successful login attempt, to subscribe with the new credentials
   */
  protected void renewSubscriptions() {
    for(Map<String, Room> roomSubscriptions: subscriptions.values()) {
      for (Room room : roomSubscriptions.values()) {
        room.renew(room.getListener(), room.getSubscribeListener());
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
  public Kuzzle stopQueuing() {
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

    this.emitEvent(Event.reconnected);
  }

  protected Socket createSocket() throws URISyntaxException {
    IO.Options opt = new IO.Options();
    opt.forceNew = true;
    opt.reconnection = this.autoReconnect;
    opt.reconnectionDelay = this.reconnectionDelay;
    return IO.socket("http://" + host + ":" + this.port, opt);
  }

  /**
   * Emit request.
   *
   * @param request  the request
   * @param listener the listener
   * @throws JSONException the json exception
   */
  protected void emitRequest(final JSONObject request, final OnQueryDoneListener listener) throws JSONException {
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
            if (!((JSONObject) args[0]).isNull("error") && ((JSONObject) args[0]).getJSONObject("error").getString("message").equals("Token expired") && !((JSONObject)args[0]).getString("action").equals("logout")) {
              emitEvent(Event.jwtTokenExpired, listener);
            }

            if (listener != null) {
              if (!((JSONObject) args[0]).isNull("error")) {
                listener.onError(((JSONObject) args[0]).getJSONObject("error"));
              } else {
                listener.onSuccess((JSONObject) args[0]);
              }
            }
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }
      });
    }

    socket.emit("kuzzle", request);

    // Track requests made to allow Room.subscribeToSelf to work
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
  protected void isValid() {
    if (this.state == States.DISCONNECTED) {
      throw new IllegalStateException("This Kuzzle object has been invalidated. Did you try to access it after a disconnect call?");
    }
  }

  /**
   * Helper function copying headers to the query data
   *
   * @param query   the query
   * @param headers the headers
   */
  public void addHeaders(JSONObject query, final JSONObject headers) {
    try {
      for (Iterator iterator = headers.keys(); iterator.hasNext(); ) {
        String key = (String) iterator.next();
        if (query.isNull(key)) {
          query.put(key, headers.get(key));
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
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
   * Getter for the AutoResubscribe property
   *
   * @return boolean boolean
   */
  public boolean isAutoResubscribe() {
    return this.autoResubscribe;
  }

  /**
   * Setter for the AutoResubscribe property
   *
   * @param resubscribe the resubscribe
   * @return auto resubscribe
   */
  public Kuzzle setAutoResubscribe(boolean resubscribe) {
    this.autoResubscribe = resubscribe;
    return this;
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
  protected Kuzzle addPendingSubscription(final String id, final Room room) {
    this.subscriptions.get("pending").put(id, room);
    return this;
  }

  /**
   * Delete pending subscription kuzzle.
   *
   * @param id the id
   * @return the kuzzle
   */
  protected Kuzzle deletePendingSubscription(final String id) {
    ConcurrentHashMap<String, Room> pending = this.subscriptions.get("pending");

    if (pending != null) {
      pending.remove(id);
    }

    return this;
  }

  /**
   * Add subscription kuzzle.
   *
   * @param roomId     Room's unique ID
   * @param id         Room object unique ID
   * @param kuzzleRoom Room instance
   * @return kuzzle kuzzle
   */
  protected Kuzzle addSubscription(final String roomId, final String id, final Room kuzzleRoom) {
    ConcurrentHashMap<String, Room> room = this.subscriptions.get(roomId);

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
   * @return the offline queue
   */
  public Kuzzle setOfflineQueue(final QueryObject object) {
    this.offlineQueue.addToQueue(object);
    return this;
  }

  /**
   * Gets offline queue.
   *
   * @return offline queue
   */
  public Queue<QueryObject> getOfflineQueue() {
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
  public QueueFilter getQueueFilter() {
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
   * Gets network port
   *
   * @return integer
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the network port
   *
   * @param port the new port
   * @return this
   */
  public Kuzzle setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * Gets the kuzzle host
   *
   * @return string
   */
  public String getHost() {
    return this.host;
  }

  /**
   * Sets the kuzzle host instance
   *
   * @param host the new host to set
   * @return
   */
  public Kuzzle setHost(String host) {
    this.host = host;
    return this;
  }

  /**
   * Sets auto replay.
   *
   * @param autoReplay the auto replay
   * @return this
   */
  public Kuzzle setAutoReplay(boolean autoReplay) {
    this.autoReplay = autoReplay;
    return this;
  }

  /**
   * Setter for the QueueMaxSize property
   *
   * @param newMaxSize the new max size
   * @return queue max size
   */
  public Kuzzle setQueueMaxSize(int newMaxSize) {
    this.queueMaxSize = Math.max(0, newMaxSize);
    return this;
  }

  /**
   * Gets queue max size.
   *
   * @return the queue max size
   */
  public int getQueueMaxSize() {
    return this.queueMaxSize;
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
   * @return the auto queue
   */
  public Kuzzle setAutoQueue(final boolean autoQueue) {
    this.autoQueue = autoQueue;
    return this;
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
      QueryObject o;
      while ((o = (QueryObject) offlineQueue.getQueue().peek()) != null) {
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

  private void  mergeOfflineQueueWithLoader() {
    KuzzleQueue<QueryObject> additionalOfflineQueue = this.offlineQueueLoader.load();
    try {
      for (QueryObject additionalQuery : additionalOfflineQueue) {
        for (QueryObject offlineQuery : this.offlineQueue) {
          if (additionalQuery.getQuery() != null && additionalQuery.getQuery().has("requestId") && additionalQuery.getQuery().has("action") && additionalQuery.getQuery().has("controller")) {
            if (!offlineQuery.getQuery().getString("requestId").equals(additionalQuery.getQuery().getString("requestId"))) {
              this.offlineQueue.addToQueue(additionalOfflineQueue.dequeue());
            } else {
              additionalOfflineQueue.dequeue();
            }
          } else {
            throw new IllegalArgumentException("Invalid offline queue request. One or more missing properties: requestId, action, controller.");
          }
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Play all queued requests, in order.
   */
  private void dequeue() {
    if (offlineQueueLoader != null) {
      this.mergeOfflineQueueWithLoader();
    }
    if (this.offlineQueue.getQueue().size() > 0) {
      try {
        QueryObject query = (QueryObject) this.offlineQueue.getQueue().poll();
        this.emitRequest(query.getQuery(), query.getCb());
        this.emitEvent(Event.offlineQueuePop, query);
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
   * @param roomId the room id
   * @param id     the id
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
   * @param roomId the room id
   * @return the subscriptions
   */
  protected Map<String, Room> getSubscriptions(String roomId) {
    if (roomId != null && this.subscriptions.containsKey(roomId)) {
      return this.subscriptions.get(roomId);
    }

    return null;
  }

  /**
   * Getter for the pendingSubscriptions private property
   *
   * @return pending subscriptions
   */
  protected Map<String, Room> getPendingSubscriptions() {
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

  /**
   * Sets jwt token and trigger the 'loginAttempt' event.
   *
   * @param jwtToken the jwt token
   * @return this object
   */
  public Kuzzle setJwtToken(final String jwtToken) {
    this.jwtToken = jwtToken;
    try {
      this.renewSubscriptions();
      this.emitEvent(Event.loginAttempt, new JSONObject().put("success", true));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Unset the jwtToken and unsubscribe all rooms if any
   *
   * @return Kuzzle instance
   */
  public Kuzzle unsetJwtToken() {
    this.jwtToken = null;

    for(Map<String, Room> roomSubscriptions: subscriptions.values()) {
      for (Room room : roomSubscriptions.values()) {
        room.unsubscribe();
      }
    }
    return this;
  }

  /**
   * Sets the JWT Token from a Kuzzle response object
   *
   * @param response a Kuzzle response containing a jwt token
   * @return this object
   * @throws JSONException the json exception
   */
  public Kuzzle setJwtToken(@NonNull final JSONObject response) throws JSONException {
    JSONObject result;

    if (response == null) {
      throw new IllegalArgumentException("Cannot set token from a null Kuzzle response");
    }

    if (response.has("result") && (result = response.getJSONObject("result")).has("jwt")) {
      this.jwtToken = result.getString("jwt");
      this.renewSubscriptions();
      this.emitEvent(Event.loginAttempt, new JSONObject().put("success", true));
    }
    else {
      this.emitEvent(Event.loginAttempt, new JSONObject()
        .put("success", false)
        .put("error", "Cannot find a valid JWT token in the following object: " + response.toString())
      );
    }

    return this;
  }

  /**
   * Gets jwt token.
   *
   * @return the jwt token
   */
  public String getJwtToken() {
    return this.jwtToken;
  }

  /**
   * Setter for the QueueTTL property
   *
   * @param newTTL the new ttl
   * @return queue ttl
   */
  public Kuzzle setQueueTTL(int newTTL) {
    this.queueTTL = Math.max(0, newTTL);
    return this;
  }

  /**
   * Getter for the QueueTTL property
   *
   * @return queue ttl
   */
  public int getQueueTTL() {
    return this.queueTTL;
  }

  /**
   * Setter for the metadata property
   *
   * @param newMetadata the new metadata
   * @return metadata metadata
   */
  public Kuzzle setMetadata(JSONObject newMetadata) {
    this.metadata = newMetadata;
    return this;
  }

  /**
   * Getter for the metadata property
   *
   * @return metadata metadata
   */
  public JSONObject getMetadata() {
    return this.metadata;
  }


  /**
   * Sets replay interval.
   *
   * @param interval the interval
   * @return the replay interval
   */
  public Kuzzle setReplayInterval(long interval) {
    this.replayInterval = Math.max(0, interval);
    return this;
  }

  /**
   * Getter for the ReplayInterval property
   *
   * @return replay interval
   */
  public long getReplayInterval() {
    return this.replayInterval;
  }

  /**
   * Getter for the ReconnectionDelay property
   *
   * @return reconnection delay
   */
  public long getReconnectionDelay() {
    return this.reconnectionDelay;
  }

  /**
   * Sets offline queue loader.
   *
   * @param offlineQueueLoader the offline queue loader
   */
  public void setOfflineQueueLoader(OfflineQueueLoader offlineQueueLoader) {
    this.offlineQueueLoader = offlineQueueLoader;
  }

  /**
   * Update current user in Kuzzle.
   *
   * @param content
   * @return itself
   */
  public Kuzzle updateSelf(final JSONObject content) {
    return updateSelf(content, null, null);
  }

  /**
   * Update current user in Kuzzle.
   *
   * @param content
   * @param options
   * @return itself
   */
  public Kuzzle updateSelf(final JSONObject content, final Options options) {
    return updateSelf(content, options, null);
  }

  /**
   * Update current user in Kuzzle.
   *
   * @param content
   * @param listener
   * @return itself
   */
  public Kuzzle updateSelf(final JSONObject content, final ResponseListener listener) {
    return updateSelf(content, null, listener);
  }

  /**
   * Update current user in Kuzzle.
   *
   * @param content
   * @param options
   * @param listener
   * @return itself
   */
  public Kuzzle updateSelf(final JSONObject content, final Options options, final ResponseListener<JSONObject> listener) {
    QueryArgs args = new QueryArgs();
    args.controller = "auth";
    args.action = "updateSelf";

    try {
      JSONObject query = new JSONObject().put("body", content);
      this.query(args, query, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            listener.onSuccess(response);
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
   * Retrieves current user information
   *
   * @param listener the listener
   */
  public void whoAmI(@NonNull final ResponseListener<User> listener) {
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
            listener.onSuccess(new User(Kuzzle.this, result.getString("_id"), result.getJSONObject("_source")));
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
   * Forces the current/provided index to refresh
   *
   * @return itself
   */
  public Kuzzle refreshIndex() {
    return this.refreshIndex(null, null, null);
  }

  /**
   * Forces the current/provided index to refresh
   *
   * @param index
   * @return itself
   */
  public Kuzzle refreshIndex(String index) {
    return this.refreshIndex(index, null, null);
  }

  /**
   * Forces the current/provided index to refresh
   *
   * @param index
   * @param listener
   * @return itself
   */
  public Kuzzle refreshIndex(String index, final ResponseListener<JSONObject> listener) {
    return this.refreshIndex(index, null, listener);
  }

  /**
   * Forces the current/provided index to refresh
   *
   * @param index
   * @param options
   * @return itself
   */
  public Kuzzle refreshIndex(String index, final Options options) {
    return this.refreshIndex(index, options, null);
  }

  /**
   * Forces the current/provided index to refresh
   *
   * @param options
   * @return itself
   */
  public Kuzzle refreshIndex(final Options options) {
    return this.refreshIndex(null, options, null);
  }

  /**
   * Forces the current/provided index to refresh
   *
   * @param options
   * @param listener
   * @return itself
   */
  public Kuzzle refreshIndex(final Options options, final ResponseListener<JSONObject> listener) {
    return this.refreshIndex(null, options, listener);
  }

  /**
   * Forces the current/provided index to refresh
   *
   * @param listener
   * @return itself
   */
  public Kuzzle refreshIndex(final ResponseListener<JSONObject> listener) {
    return this.refreshIndex(null, null, listener);
  }

  /**
   * Forces the current/provided index to refresh
   *
   * @param index
   * @param options
   * @param listener
   * @return itself
   */
  public Kuzzle refreshIndex(String index, final Options options, final ResponseListener<JSONObject> listener) {
    if (index == null) {
      if (this.defaultIndex == null) {
        throw new IllegalArgumentException("Kuzzle.refreshIndex: index required");
      } else {
        index = this.defaultIndex;
      }
    }

    QueryArgs args = new QueryArgs();
    args.index = index;
    args.controller = "index";
    args.action = "refresh";
    JSONObject request = new JSONObject();

    try {
      this.query(args, request, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener == null) {
            return;
          }

          try {
            JSONObject result = response.getJSONObject("result");

            listener.onSuccess(result);
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener == null) {
            return;
          }

          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }

    return this;
  }


  /**
   * Gets the current/provided index autorefresh status
   *
   * @param listener
   */
  public void getAutoRefresh(@NonNull final ResponseListener<Boolean> listener) {
    this.getAutoRefresh(null, null, listener);
  }

  /**
   * Gets the current/provided index autorefresh status
   *
   * @param index
   * @param listener
   */
  public void getAutoRefresh(String index, @NonNull final ResponseListener<Boolean> listener) {
    this.getAutoRefresh(index, null, listener);
  }

  /**
   * Gets the current/provided index autorefresh status
   *
   * @param options
   * @param listener
   */
  public void getAutoRefresh(Options options, @NonNull final ResponseListener<Boolean> listener) {
    this.getAutoRefresh(null, options, listener);
  }

  /**
   * Gets the current/provided index autorefresh status
   *
   * @param index
   * @param options
   * @param listener
   */
  public void getAutoRefresh(String index, final Options options, @NonNull final ResponseListener<Boolean> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Kuzzle.getAutoRefresh: listener required");
    }

    if (index == null) {
      if (this.defaultIndex == null) {
        throw new IllegalArgumentException("Kuzzle.getAutoRefresh: index required");
      } else {
        index = this.defaultIndex;
      }
    }

    QueryArgs args = new QueryArgs();
    args.index = index;
    args.controller = "index";
    args.action = "getAutoRefresh";
    JSONObject request = new JSONObject();

    try {
      this.query(args, request, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            boolean result = response.getBoolean("result");
            listener.onSuccess(result);
          } catch(JSONException e) { throw new RuntimeException(e); }
        }

        @Override
        public void onError(JSONObject error) { listener.onError(error); }
      });
    } catch(JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * (Un)Sets the autorefresh status for the current/given index
   *
   * @param autoRefresh
   * @return itself
   */
  public Kuzzle setAutoRefresh(final boolean autoRefresh) {
    return this.setAutoRefresh(null, autoRefresh, null, null);
  }

  /**
   * (Un)Sets the autorefresh status for the current/given index
   *
   * @param autoRefresh
   * @param listener
   * @return itself
   */
  public Kuzzle setAutoRefresh(final boolean autoRefresh, final ResponseListener<Boolean> listener) {
    return this.setAutoRefresh(null, autoRefresh, null, listener);
  }

  /**
   * (Un)Sets the autorefresh status for the current/given index
   *
   * @param index
   * @param autoRefresh
   * @return itself
   */
  public Kuzzle setAutoRefresh(String index, final boolean autoRefresh) {
    return this.setAutoRefresh(index, autoRefresh, null, null);
  }

  /**
   * (Un)Sets the autorefresh status for the current/given index
   *
   * @param autoRefresh
   * @param options
   * @return itself
   */
  public Kuzzle setAutoRefresh(final boolean autoRefresh, final Options options) {
    return this.setAutoRefresh(null, autoRefresh, options, null);
  }

  /**
   * (Un)Sets the autorefresh status for the current/given index
   *
   * @param index
   * @param autoRefresh
   * @param options
   * @return itself
   */
  public Kuzzle setAutoRefresh(String index, final boolean autoRefresh, final Options options) {
    return this.setAutoRefresh(index, autoRefresh, options, null);
  }

  /**
   * (Un)Sets the autorefresh status for the current/given index
   *
   * @param index
   * @param autoRefresh
   * @param listener
   * @return itself
   */
  public Kuzzle setAutoRefresh(String index, final boolean autoRefresh, final ResponseListener<Boolean> listener) {
    return this.setAutoRefresh(index, autoRefresh, null, listener);
  }

  /**
   * (Un)Sets the autorefresh status for the current/given index
   *
   * @param autoRefresh
   * @param options
   * @param listener
   * @return itself
   */
  public Kuzzle setAutoRefresh(final boolean autoRefresh, final Options options, final ResponseListener<Boolean> listener) {
    return this.setAutoRefresh(null, autoRefresh, options, listener);
  }

  /**
   * (Un)Sets the autorefresh status for the current/given index
   *
   * @param index
   * @param autoRefresh
   * @param options
   * @param listener
   * @return itself
   */
  public Kuzzle setAutoRefresh(String index, final boolean autoRefresh, final Options options, final ResponseListener<Boolean> listener) {
    if (index == null) {
      if (this.defaultIndex == null) {
        throw new IllegalArgumentException("Kuzzle.setAutoRefresh: index required");
      } else {
        index = this.defaultIndex;
      }
    }

    QueryArgs args = new QueryArgs();
    args.index = index;
    args.controller = "index";
    args.action = "setAutoRefresh";
    JSONObject request;

    try {
      request = new JSONObject().put("body", new JSONObject().put("autoRefresh", autoRefresh));
      this.query(args, request, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener == null) {
            return;
          }

          try {
            boolean result = response.getBoolean("result");
            listener.onSuccess(result);
          } catch (JSONException e) { throw new RuntimeException(e); }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch(JSONException e) {
      throw  new RuntimeException(e);
    }

    return this;
  }

  /**
   * Gets the rights array of the currently logged user.
   *
   * @param listener
   * @return the Security instance
   */
  public Kuzzle getMyRights(@NonNull final ResponseListener<JSONArray> listener) {
    return getMyRights(null, listener);
  }

  /**
   * Gets the rights array of the currently logged user.
   *
   * @param options
   * @param listener
   * @return the Security instance
   */
  public Kuzzle getMyRights(final Options options, @NonNull final ResponseListener<JSONArray> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Security.getMyRights: listener is mandatory.");
    }
    try {
      Kuzzle.this.query(buildQueryArgs("getMyRights"), new JSONObject(), options, new OnQueryDoneListener() {
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
   * Helper function meant to easily build the first Kuzzle.query() argument
   *
   * @param action - Security controller action name
   * @return JSONObject - Kuzzle.query() 1st argument object
   * @throws JSONException the json exception
   */
  protected io.kuzzle.sdk.core.Kuzzle.QueryArgs buildQueryArgs(@NonNull final String action) throws JSONException {
    io.kuzzle.sdk.core.Kuzzle.QueryArgs args = new io.kuzzle.sdk.core.Kuzzle.QueryArgs();
    args.action = action;
    args.controller = "security";
    return args;
  }

  /**
   * Invokes a query listener with a custom error message and status
   * @param listener - query listener to discard
   * @param query - discarded query
   */
  protected void discardRequest(final OnQueryDoneListener listener, JSONObject query) throws JSONException {
    if (listener != null) {
      JSONObject err = new JSONObject()
        .put("status", 400)
        .put("message", "Unable to execute request: not connected to a Kuzzle server.\nDiscarded request: " + query.toString());

      listener.onError(err);
    }
  }
}
