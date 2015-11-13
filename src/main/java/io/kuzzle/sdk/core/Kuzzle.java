package io.kuzzle.sdk.core;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.listeners.IEventListener;
import io.kuzzle.sdk.state.Context;
import io.kuzzle.sdk.state.States;
import io.kuzzle.sdk.util.Event;
import io.kuzzle.sdk.util.QueryObject;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * The type Kuzzle.
 */
public class Kuzzle {

  private List<Event> eventListeners = new ArrayList<>();
  private Socket socket;
  private Map<String, KuzzleDataCollection> collections = new HashMap<>();
  private Context<QueryObject> ctx = new Context<>();
  private boolean autoReconnect;
  private JSONObject headers;
  private JSONObject metadata;

  /**
   * Kuzzle object constructor.
   *
   * @param url                the url
   * @param options            the options
   * @param connectionCallback the connection callback
   * @throws URISyntaxException       the uri syntax exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public Kuzzle(final String url, KuzzleOptions options, final ResponseListener connectionCallback) throws URISyntaxException, IllegalArgumentException {
    if (url == null || url.isEmpty())
      throw new IllegalArgumentException("Url can't be empty");
    this.autoReconnect = (options != null ? options.isAutoReconnect() : true);
    this.headers = (options != null && options.getHeaders() != null ? options.getHeaders() : new JSONObject());
    this.metadata = (options != null && options.getMetadata() != null ? options.getMetadata() : new JSONObject());
    if (socket == null) {
      socket = createSocket(url);

      socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Log.i("kuzzle", "Kuzzle connected");
          // TODO: initialize kuzzle-provided properties (applicationId, connectionId, connectionTimestamp)
          ctx.setState(States.CONNECTED);
          if (connectionCallback != null) {
            try {
              connectionCallback.onSuccess(args.length != 0 ? (JSONObject) args[0] : null);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }).once(Socket.EVENT_RECONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Log.i("kuzzle", "Kuzzle has reconnected");
        }
      }).once(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          // TODO: Invalidate this object for now. Should handle the autoReconnect flag later + bufferize queries
          Log.e("kuzzle", "Kuzzle connection error");
          Log.e("kuzzle", args[0].toString());
          logout();
        }
      });

      socket.once(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Log.w("Kuzzle", "Kuzzle has disconnected");
          ctx.setState(States.DISCONNECTED);
          for (Event e : eventListeners) {
            if (e.getType() == EventType.DISCONNECTED)
              e.trigger(null, null);
          }
        }
      });
    }
    socket.connect();
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param url the url
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(String url) throws URISyntaxException {
    this(url, null, null);
  }

  /**
   * Instantiates a new Kuzzle.
   *
   * @param url the url
   * @param cb  the cb
   * @throws URISyntaxException the uri syntax exception
   */
  public Kuzzle(String url, ResponseListener cb) throws URISyntaxException {
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

  private Socket createSocket(String url) throws URISyntaxException {
    return IO.socket(url);
  }

  /**
   * Adds a listener to a Kuzzle global event. When an event is fired, listeners are called in the order of their
   * insertion.
   * <p>
   * The ID returned by this function is required to remove this listener at a later time.
   *
   * @param eventType     - name of the global event to subscribe to
   * @param eventListener the event listener
   * @return {string} Unique listener ID
   * @throws NullPointerException the null pointer exception
   */
  public String addListener(final EventType eventType, final IEventListener eventListener) throws NullPointerException {
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
   * Count the number of other connected clients to Kuzzle
   *
   * @param cb - Handles the query response
   * @return {integer} total
   * @throws NullPointerException the null pointer exception
   */
  public Kuzzle count(ResponseListener cb) throws NullPointerException {
    this.isValid();

    // TODO
    int total = 0;
    return this;
  }

  /**
   * Create a new instance of a KuzzleDataCollection object
   *
   * @param collection - The name of the data collection you want to manipulate
   * @return {object} A KuzzleDataCollection instance
   * @throws NullPointerException the null pointer exception
   */
  public KuzzleDataCollection dataCollectionFactory(String collection) throws NullPointerException {
    this.isValid();
    if (!this.collections.containsKey(collection)) {
      this.collections.put(collection, new KuzzleDataCollection(this, collection));
    }
    return this.collections.get(collection);
  }

  /**
   * Disconnects from Kuzzle and invalidate this instance.
   */
  public void logout() {
    this.socket.close();
    this.socket = null;
    this.collections.clear();
    ctx.setState(States.DISCONNECTED);
    getEventListeners().clear();
  }

  /**
   * Returns the current Kuzzle UTC timestamp
   *
   * @param cb - Handles the query response
   * @return {integer}
   */
  public Kuzzle now(ResponseListener cb) {
    this.isValid();

    // TODO
    //cb.onSuccess(null, 0xBADDCAFE);
    return this;
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
   * @throws IOException   the io exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query, KuzzleOptions options, final ResponseListener cb) throws JSONException, IOException {
    this.isValid();
    final JSONObject object = query;
    if (object.isNull("requestId"))
      object.put("requestId", UUID.randomUUID().toString());
    object.put("action", action);

    object.put("metadata", this.metadata);
    if (options != null) {
      if (options.getMetadata() != null) {
        for (Iterator iterator = options.getMetadata().keys(); iterator.hasNext(); ) {
          String key = (String) iterator.next();
          ((JSONObject) object.get("metadata")).put(key, options.getMetadata().get(key));
        }
      }
    }

    if (collection != null) {
      object.put("collection", collection);
    }

    // TODO queue according to machine state
    //if (ctx.state() == States.CONNECTED) {
    socket.once(object.get("requestId").toString(), new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        if (cb != null) {
          try {
            if (!((JSONObject) args[0]).isNull("error")) {
              cb.onError((JSONObject) ((JSONObject) args[0]).get("error"));
            } else {
              cb.onSuccess((JSONObject) ((JSONObject) args[0]).get("result"));
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    });
    this.addHeaders(object, this.headers);
    socket.emit(controller, object);
        /*
        } else if (ctx.state() == States.DISCONNECTED || ctx.state() == States.POLLING) {
            queue(object, controller, cb);
        }*/
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
   * @throws IOException   the io exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query) throws JSONException, IOException {
    return this.query(collection, controller, action, query, null, null);
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
   * @throws IOException   the io exception
   */
  public Kuzzle query(final String collection, final String controller, final String action, final JSONObject query, ResponseListener listener) throws JSONException, IOException {
    return this.query(collection, controller, action, query, null, listener);
  }

  /**
   * Removes a listener from an event.
   *
   * @param eventType  the event type
   * @param listenerId the listener id
   */
  public void removeListener(EventType eventType, String listenerId) {
    this.isValid();

    int i = 0;
    for (Event e : eventListeners) {
      if (e.getId().toString().equals(listenerId)) {
        eventListeners.remove(i);
        break;
      }
      i++;
    }
  }

  // Helper function making a QueryObject and put it in queue
  private void queue(JSONObject object, String controller, ResponseListener cb) {
    QueryObject qo = new QueryObject();
    qo.setObject(object);
    qo.setController(controller);
    qo.setCb(cb);
    ctx.addToQueue(qo);
  }

  /**
   * Helper function ensuring that this Kuzzle object is still valid before performing a query
   *
   * @throws NullPointerException the null pointer exception
   */
  public void isValid() throws NullPointerException {
    if (this.socket == null) {
      throw new NullPointerException("This Kuzzle object has been invalidated. Did you try to access it after a logout call?");
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
   * Gets event listeners.
   *
   * @return the event listeners
   */
  public List<Event> getEventListeners() {
    return eventListeners;
  }
}
