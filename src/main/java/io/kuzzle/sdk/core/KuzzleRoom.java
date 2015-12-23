package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.UUID;

import io.kuzzle.sdk.enums.Scope;
import io.kuzzle.sdk.enums.State;
import io.kuzzle.sdk.enums.Users;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.socket.emitter.Emitter;

/**
 * The type Kuzzle room.
 */
public class KuzzleRoom {

  private String id = UUID.randomUUID().toString();
  private String collection;
  private KuzzleDataCollection  dataCollection;
  private JSONObject filters;
  private JSONObject headers;
  private JSONObject metadata;
  private boolean subscribeToSelf;
  private String roomId;
  private Kuzzle kuzzle;
  private String  channel;
  private Scope scope;
  private State state;
  private Users users;

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
   * In Kuzzle, you don't exactly subscribe to a room or a topic but, instead, you subscribe to documents.
   * What it means is that, to subscribe, you provide to Kuzzle a set of matching filters.
   * Once you have subscribed, if a pub/sub message is published matching your filters, or if a matching stored
   * document change (because it is created, updated or deleted), then you'll receive a notification about it.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @param options              the options
   * @throws NullPointerException the null pointer exception
   */
  public KuzzleRoom(final KuzzleDataCollection kuzzleDataCollection, final KuzzleRoomOptions options) throws NullPointerException, KuzzleException {
    if (kuzzleDataCollection == null) {
      throw new IllegalArgumentException("KuzzleRoom: missing dataCollection");
    }
    kuzzleDataCollection.getKuzzle().isValid();
    dataCollection = kuzzleDataCollection;

    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.collection = kuzzleDataCollection.getCollection();
    this.headers = kuzzleDataCollection.getHeaders();
    this.subscribeToSelf = (options != null ? options.isSubscribeToSelf() : true);
    this.metadata = (options != null ? options.getMetadata() : new JSONObject());
    this.scope = (options != null ? options.getScope() : Scope.ALL);
    this.state = (options != null ? options.getState() : State.DONE);
    this.users = (options != null ? options.getUsers() : Users.NONE);
  }

  /**
   * Returns the number of other subscriptions on that room.
   *
   * @param cb the cb
   * @return kuzzle room
   * @throws JSONException the json exception
   */
  public KuzzleRoom count(final ResponseListener cb) throws JSONException, KuzzleException {
    JSONObject body = new JSONObject();
    JSONObject data = new JSONObject();
    data.put("roomId", this.roomId);
    body.put("body", data);
    this.kuzzle.addHeaders(body, this.headers);
    this.kuzzle.query(this.collection, "subscribe", "count", body, cb);
    return this;
  }

  /**
   * Call after renew.
   *
   * @param cb   the cb
   * @param args the args
   */
  protected void callAfterRenew(final ResponseListener cb, final Object args) {
    if (args == null)
      throw new NullPointerException("Response is null");
    try {
      if (cb != null) {
        if (!((JSONObject) args).isNull("error"))
          cb.onError((JSONObject)args);
        else {
          String key = ((JSONObject) args).getJSONObject("result").getString("requestId");
          if (KuzzleRoom.this.kuzzle.getRequestHistory().containsKey(key)) {
            if (KuzzleRoom.this.subscribeToSelf) {
              cb.onSuccess(((JSONObject) args).getJSONObject("result"));
            }
            KuzzleRoom.this.kuzzle.getRequestHistory().remove(key);
          } else {
            cb.onSuccess(((JSONObject) args).getJSONObject("result"));
          }
        }
      }
    } catch (JSONException e) {
      if (cb != null) {
        cb.onError((JSONObject) args);
      }
    }
  }

  /**
   * Renew the subscription. Force a resubscription using the same filters if no new ones are provided.
   * Unsubscribes first if this KuzzleRoom was already listening to events.
   *
   * @param filters the filters
   * @param cb      the cb
   * @return kuzzle room
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleRoom renew(final JSONObject filters, final ResponseListener cb) throws JSONException, KuzzleException {
    this.filters = (filters == null ? new JSONObject() : filters);
    this.unsubscribe();
    final KuzzleOptions options = new KuzzleOptions();
    final JSONObject subscribeQuery = new JSONObject();

    subscribeQuery.put("body", this.filters);
    subscribeQuery.put("scope", this.scope.toString().toLowerCase());
    subscribeQuery.put("state", this.state.toString().toLowerCase());
    subscribeQuery.put("users", this.users.toString().toLowerCase());

    this.kuzzle.addPendingSubscription(this.id, this);
    options.setMetadata(this.metadata);
    this.kuzzle.addHeaders(subscribeQuery, this.headers);

    this.kuzzle.query(this.collection, "subscribe", "on", subscribeQuery, options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) {
        KuzzleRoom.this.kuzzle.addSubscription(KuzzleRoom.this.id, KuzzleRoom.this);
        KuzzleRoom.this.kuzzle.deletePendingSubscription(KuzzleRoom.this.id);
        try {
          KuzzleRoom.this.channel = args.getString("channel");
          KuzzleRoom.this.roomId = args.getString("roomId");
        } catch (JSONException e) {
          e.printStackTrace();
        }
        KuzzleRoom.this.kuzzle.getSocket().on(KuzzleRoom.this.channel, new Emitter.Listener() {
          @Override
          public void call(final Object... args) {
            callAfterRenew(cb, args[0]);
          }
        });
      }

      @Override
      public void onError(JSONObject arg) {
        if (cb != null) {
          cb.onError(arg);
        }
      }
    });
    return this;
  }

  /**
   * Cancels the current subscription.
   *
   * @param listener the listener
   * @return the kuzzle room
   * @throws JSONException the json exception
   */
  public KuzzleRoom unsubscribe(final ResponseListener listener) throws JSONException, KuzzleException {
    if (this.roomId != null) {
      JSONObject roomId = new JSONObject();
      roomId.put("roomId", this.roomId);
      JSONObject data = new JSONObject();
      this.kuzzle.addHeaders(data, this.headers);
      data.put("body", roomId);
      this.kuzzle.deleteSubscription(this.id);
      this.kuzzle.query(this.collection, "subscribe", "off", data, new ResponseListener() {
        @Override
        public void onSuccess(JSONObject object) {
          KuzzleRoom.this.kuzzle.getSocket().off(KuzzleRoom.this.channel);
          KuzzleRoom.this.roomId = null;
          if (listener != null)
            listener.onSuccess(object);
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null)
            listener.onError(error);
        }
      });
    }
    return this;
  }

  /**
   * Cancels the current subscription.
   *
   * @return the kuzzle room
   * @throws JSONException the json exception
   */
  public KuzzleRoom unsubscribe() throws JSONException, KuzzleException {
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
  public KuzzleRoom setFilters(JSONObject filters) {
    this.filters = filters;
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
   * Helper function allowing to set headers while chaining calls.
   * If the replace argument is set to true, replace the current headers with the provided content.
   * Otherwise, it appends the content to the current headers, only replacing already existing values
   *
   * @param content the headers
   * @return the headers
   * @throws JSONException the json exception
   */
  public KuzzleRoom setHeaders(JSONObject content) throws JSONException {
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
  public KuzzleRoom setHeaders(JSONObject content, boolean replace) throws JSONException {
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
  public KuzzleRoom setMetadata(JSONObject metadata) {
    this.metadata = metadata;
    return this;
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
  public KuzzleRoom setSubscribeToSelf(boolean subscribeToSelf) {
    this.subscribeToSelf = subscribeToSelf;
    return this;
  }

  /**
   * Get roomId
   * @return roomId
   */
  public String getRoomId() {
    return this.roomId;
  }

}
