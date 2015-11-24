package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.util.Event;
import io.socket.emitter.Emitter;

/**
 * The type Kuzzle room.
 */
public class KuzzleRoom {

  private String collection;
  private JSONObject filters;
  private JSONObject headers;
  private boolean listeningToConnections;
  private boolean listeningToDisconnections;
  private JSONObject metadata;
  private boolean subscribeToSelf;
  private String roomId;
  private Kuzzle kuzzle;

  /**
   * Instantiates a new Kuzzle room.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   */
  public KuzzleRoom(KuzzleDataCollection kuzzleDataCollection) throws KuzzleException {
    this(kuzzleDataCollection, null);
  }

  /**
   * This object is the result of a subscription request, allowing to manipulate the subscription itself.
   * <p/>
   * In Kuzzle, you don’t exactly subscribe to a room or a topic but, instead, you subscribe to documents.
   * <p/>
   * What it means is that, to subscribe, you provide to Kuzzle a set of matching filters.
   * Once you have subscribed, if a pub/sub message is published matching your filters, or if a matching stored
   * document change (because it is created, updated or deleted), then you’ll receive a notification about it.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @param options              the options
   * @throws NullPointerException the null pointer exception
   */
  public KuzzleRoom(final KuzzleDataCollection kuzzleDataCollection, final KuzzleRoomOptions options) throws NullPointerException, KuzzleException {
    if (kuzzleDataCollection == null) {
      throw new IllegalArgumentException("KuzzleRoom: missing parameters");
    }

    kuzzleDataCollection.getKuzzle().isValid();

    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.collection = kuzzleDataCollection.getCollection();
    this.headers = kuzzleDataCollection.getHeaders();
    this.listeningToConnections = (options != null ? options.isListeningToConnections() : false);
    this.listeningToDisconnections = (options != null ? options.isListeningToDisconnections() : false);
    this.subscribeToSelf = (options != null ? options.isSubscribeToSelf() : false);
    this.metadata = (options != null ? options.getMetadata() : new JSONObject());
  }

  /**
   * Returns the number of other subscriptions on that room.
   *
   * @param cb the cb
   * @return kuzzle room
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleRoom count(final ResponseListener cb) throws JSONException, IOException, KuzzleException {
    JSONObject body = new JSONObject();
    JSONObject data = new JSONObject();
    data.put("roomId", this.roomId);
    body.put("body", data);
    this.kuzzle.addHeaders(body, this.headers);
    this.kuzzle.query(this.collection, "subscribe", "count", body, cb);
    return this;
  }

  /**
   * Trigger events.
   *
   * @param listening   the listening
   * @param globalEvent the global event
   * @param args2       the args 2
   * @param cb          the cb
   * @param args        the args
   * @throws Exception the exception
   */
  protected void triggerEvents(boolean listening, EventType globalEvent, JSONObject args2, ResponseListener cb, Object... args) throws Exception {
    JSONObject response = args2;
    if (response == null || !response.isNull("error")) {
      if (listening && cb != null) {
        cb.onSuccess(response);
      }
      if (response == null)
        throw new NullPointerException("Response is null");
      throw new KuzzleException(response.get("error").toString());
    }
    JSONObject result = (JSONObject) ((JSONObject) args[0]).get("result");
    result.put("count", args2);
    if (listening && cb != null) {
      cb.onSuccess(result);
    }

    for (Event e : KuzzleRoom.this.kuzzle.getEventListeners()) {
      if (e.getType() == EventType.SUBSCRIBED && globalEvent == EventType.SUBSCRIBED)
        e.trigger(KuzzleRoom.this.roomId, result);
      else if (e.getType() == EventType.UNSUBSCRIBED && globalEvent == EventType.UNSUBSCRIBED)
        e.trigger(KuzzleRoom.this.roomId, result);
    }
  }

  /**
   * Call after renew.
   *
   * @param cb   the cb
   * @param args the args
   * @throws Exception the exception
   */
  protected void callAfterRenew(final ResponseListener cb, final Object args) throws Exception {
    if (args == null)
      throw new NullPointerException("Response is null");
    //JSONObject response = (JSONObject) args;
    JSONObject response = new JSONObject(args.toString());
    JSONObject result = (JSONObject) response.get("result");
    final EventType globalEvent;
    final boolean listening;
    if (response != null && !response.isNull("error")) {
      throw new KuzzleException(response.get("error").toString());
    } else if (result.get("action").toString().equals("on") || result.get("action").toString().equals("off")) {
      if (result.get("action").toString().equals("on")) {
        globalEvent = EventType.SUBSCRIBED;
        listening = KuzzleRoom.this.listeningToConnections;
      } else {
        globalEvent = EventType.UNSUBSCRIBED;
        listening = KuzzleRoom.this.listeningToDisconnections;
      }
      if (listening && cb != null)
        cb.onSuccess(((JSONObject) args).getJSONObject("result"));
      if (KuzzleRoom.this.eventExist(globalEvent)) {
        triggerEvents(listening, globalEvent, (KuzzleDocument) args, cb, args);
      }
    } else {
      if (cb != null)
        cb.onSuccess(((JSONObject) args).getJSONObject("result"));
    }
  }

  /**
   * Renew the subscription using new filters
   *
   * @param filters the filters
   * @param cb      the cb
   * @return kuzzle room
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleRoom renew(final JSONObject filters, final ResponseListener cb) throws JSONException, IOException, KuzzleException {
    this.filters = (filters == null ? new JSONObject() : filters);
    this.unsubscribe();
    final JSONObject data = new JSONObject();
    final KuzzleOptions options = new KuzzleOptions();
    options.setMetadata(this.metadata);
    data.put("body", this.filters);
    this.kuzzle.addHeaders(data, this.headers);

    this.kuzzle.query(this.collection, "subscribe", "on", data, options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {
        KuzzleRoom.this.roomId = args.get("roomId").toString();
        KuzzleRoom.this.kuzzle.getSocket().on(KuzzleRoom.this.roomId, new Emitter.Listener() {
          @Override
          public void call(final Object... args) {
            try {
              callAfterRenew(cb, args[0]);
            } catch (JSONException e) {
              e.printStackTrace();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
      }

      @Override
      public void onError(JSONObject arg) throws Exception {
        cb.onError(arg);
      }
    });
    return this;
  }

  private boolean eventExist(EventType event) {
    for (Event e : kuzzle.getEventListeners()) {
      if (e.getType() == event)
        return true;
    }
    return false;
  }

  /**
   * Unsubscribe kuzzle room.
   *
   * @param listener the listener
   * @return the kuzzle room
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleRoom unsubscribe(final ResponseListener listener) throws JSONException, IOException, KuzzleException {
    if (this.roomId != null) {
      JSONObject roomId = new JSONObject();
      roomId.put("roomId", this.roomId);
      JSONObject data = new JSONObject();
      this.kuzzle.addHeaders(data, this.headers);
      data.put("body", roomId);
      this.kuzzle.query(this.collection, "subscribe", "off", data, new ResponseListener() {
        @Override
        public void onSuccess(JSONObject object) throws Exception {
          KuzzleRoom.this.kuzzle.getSocket().off(String.valueOf(KuzzleRoom.this.roomId));
          KuzzleRoom.this.roomId = null;
          if (listener != null)
            listener.onSuccess(object);
        }

        @Override
        public void onError(JSONObject error) throws Exception {
          if (listener != null)
            listener.onError(error);
        }
      });
    }
    return this;
  }

  /**
   * Unsubscribe kuzzle room.
   *
   * @return the kuzzle room
   * @throws IOException   the io exception
   * @throws JSONException the json exception
   */
  public KuzzleRoom unsubscribe() throws IOException, JSONException, KuzzleException {
    return this.unsubscribe(null);
  }

  /**
   * Gets collection.
   *
   * @return the collection
   */
  public String getCollection() {
    return collection;
  }

  /**
   * Gets filters.
   *
   * @return the filters
   */
  public JSONObject getFilters() {
    return filters;
  }

  /**
   * Sets filters.
   *
   * @param filters the filters
   */
  public void setFilters(JSONObject filters) {
    this.filters = filters;
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
   * Is listening to connections boolean.
   *
   * @return the boolean
   */
  public boolean isListeningToConnections() {
    return listeningToConnections;
  }

  /**
   * Sets listening to connections.
   *
   * @param listeningToConnections the listening to connections
   */
  public void setListeningToConnections(boolean listeningToConnections) {
    this.listeningToConnections = listeningToConnections;
  }

  /**
   * Is listening to disconnections boolean.
   *
   * @return the boolean
   */
  public boolean isListeningToDisconnections() {
    return listeningToDisconnections;
  }

  /**
   * Sets listening to disconnections.
   *
   * @param listeningToDisconnections the listening to disconnections
   */
  public void setListeningToDisconnections(boolean listeningToDisconnections) {
    this.listeningToDisconnections = listeningToDisconnections;
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
  public void setMetadata(JSONObject metadata) {
    this.metadata = metadata;
  }

  /**
   * Is subscribe to self boolean.
   *
   * @return the boolean
   */
  public boolean isSubscribeToSelf() {
    return subscribeToSelf;
  }

  /**
   * Sets subscribe to self.
   *
   * @param subscribeToSelf the subscribe to self
   */
  public void setSubscribeToSelf(boolean subscribeToSelf) {
    this.subscribeToSelf = subscribeToSelf;
  }

}
