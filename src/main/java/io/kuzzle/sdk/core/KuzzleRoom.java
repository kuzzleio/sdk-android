package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.UUID;

import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.enums.Scope;
import io.kuzzle.sdk.enums.State;
import io.kuzzle.sdk.enums.Users;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleNotificationResponse;
import io.kuzzle.sdk.util.Event;
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
  private KuzzleResponseListener<KuzzleNotificationResponse> listener;

  /**
   * Instantiates a new Kuzzle room.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   */
  public KuzzleRoom(@NonNull final KuzzleDataCollection kuzzleDataCollection) {
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
   */
  public KuzzleRoom(@NonNull final KuzzleDataCollection kuzzleDataCollection, final KuzzleRoomOptions options) {
    if (kuzzleDataCollection == null) {
      throw new IllegalArgumentException("KuzzleRoom: missing dataCollection");
    }
    kuzzleDataCollection.getKuzzle().isValid();
    dataCollection = kuzzleDataCollection;

    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.collection = kuzzleDataCollection.getCollection();
    this.headers = kuzzleDataCollection.getHeaders();
    this.subscribeToSelf = (options == null || options.isSubscribeToSelf());
    this.metadata = (options != null ? options.getMetadata() : new JSONObject());
    this.scope = (options != null ? options.getScope() : Scope.ALL);
    this.state = (options != null ? options.getState() : State.DONE);
    this.users = (options != null ? options.getUsers() : Users.NONE);
  }

  /**
   * Returns the number of other subscriptions on that room.
   *
   * @param listener the listener
   * @return kuzzle room
   */
  public KuzzleRoom count(@NonNull final KuzzleResponseListener<Integer> listener) {
    JSONObject body = new JSONObject();
    JSONObject data = new JSONObject();
    try {
      data.put("roomId", this.roomId);
      body.put("body", data);
      this.kuzzle.addHeaders(body, this.headers);
      this.kuzzle.query(this.dataCollection.makeQueryArgs("subscribe", "count"), body, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              listener.onSuccess(response.getJSONObject("result").getInt("count"));
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
   * Call after renew.
   *
   * @param listener the listener
   * @param args     the args
   */
  protected void callAfterRenew(final KuzzleResponseListener<KuzzleNotificationResponse> listener, final Object args) {
    if (args == null) {
      throw new IllegalArgumentException("KuzzleRoom.renew: response required");
    }
    this.listener = listener;
    try {
      if (listener != null) {
        if (!((JSONObject) args).isNull("error"))
          listener.onError((JSONObject) args);
        else {
          String key = ((JSONObject) args).getString("requestId");
          if (((JSONObject) args).getString("action").equals("jwtTokenExpired")) {
            KuzzleRoom.this.kuzzle.setJwtToken(null);
            KuzzleRoom.this.kuzzle.emitEvent(KuzzleEvent.jwtTokenExpired);
          }
          if (KuzzleRoom.this.kuzzle.getRequestHistory().containsKey(key)) {
            if (KuzzleRoom.this.subscribeToSelf) {
              listener.onSuccess(new KuzzleNotificationResponse(kuzzle, (JSONObject) args));
            }
            KuzzleRoom.this.kuzzle.getRequestHistory().remove(key);
          } else {
            listener.onSuccess(new KuzzleNotificationResponse(kuzzle, (JSONObject) args));
          }
        }
      }
    } catch (JSONException e) {
      if (listener != null) {
        try {
          listener.onError(((JSONObject) args).getJSONObject("error"));
        } catch (JSONException err) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * Renew the subscription. Force a resubscription using the same filters if no new ones are provided.
   * Unsubscribes first if this KuzzleRoom was already listening to events.
   *
   * @param filters  the filters
   * @param listener the listener
   * @return kuzzle room
   */
  public KuzzleRoom renew(final JSONObject filters, final KuzzleResponseListener<KuzzleNotificationResponse> listener) {
    this.filters = (filters == null ? new JSONObject() : filters);
    final KuzzleOptions options = new KuzzleOptions();
    final JSONObject subscribeQuery = new JSONObject();

    try {
      this.unsubscribe();
      subscribeQuery.put("body", this.filters);
      subscribeQuery.put("scope", this.scope.toString().toLowerCase());
      subscribeQuery.put("state", this.state.toString().toLowerCase());
      subscribeQuery.put("users", this.users.toString().toLowerCase());

      this.kuzzle.addPendingSubscription(this.id, this);
      options.setMetadata(this.metadata);
      this.kuzzle.addHeaders(subscribeQuery, this.headers);

      this.kuzzle.query(this.dataCollection.makeQueryArgs("subscribe", "on"), subscribeQuery, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject args) {
          KuzzleRoom.this.kuzzle.addSubscription(KuzzleRoom.this.id, KuzzleRoom.this);
          KuzzleRoom.this.kuzzle.deletePendingSubscription(KuzzleRoom.this.id);
          try {
            JSONObject result = args.getJSONObject("result");
            KuzzleRoom.this.channel = result.getString("channel");
            KuzzleRoom.this.roomId = result.getString("roomId");
          } catch (JSONException e) {
            e.printStackTrace();
          }

          KuzzleRoom.this.kuzzle.getSocket().on(KuzzleRoom.this.channel, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
              callAfterRenew(listener, args[0]);
            }
          });
        }

        @Override
        public void onError(JSONObject arg) {
          if (listener != null) {
            listener.onError(arg);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Cancels the current subscription.
   *
   * @return the kuzzle room
   */
  public KuzzleRoom unsubscribe() {
    return this.unsubscribe(null);
  }

  /**
   * Cancels the current subscription.
   *
   * @param listener the listener
   * @return the kuzzle room
   */
  public KuzzleRoom unsubscribe(final KuzzleResponseListener<String> listener) {
    if (this.roomId != null) {
      JSONObject roomId = new JSONObject();
      JSONObject data = new JSONObject();
      try {
        roomId.put("roomId", this.roomId);
        this.kuzzle.addHeaders(data, this.headers);
        data.put("body", roomId);
        this.kuzzle.query(this.dataCollection.makeQueryArgs("subscribe", "off"), data, new OnQueryDoneListener() {
          @Override
          public void onSuccess(JSONObject object) {
            KuzzleRoom.this.kuzzle.deleteSubscription(KuzzleRoom.this.id);
            KuzzleRoom.this.kuzzle.getSocket().off(KuzzleRoom.this.channel);
            KuzzleRoom.this.roomId = null;
            if (listener != null) {
              try {
                listener.onSuccess(object.getJSONObject("result").getString("roomId"));
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
    }
    return this;
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
   * @return the filters
   */
  public KuzzleRoom setFilters(final JSONObject filters) {
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
   */
  public KuzzleRoom setHeaders(final JSONObject content) {
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
  public KuzzleRoom setHeaders(final JSONObject content, final boolean replace) {
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
  public KuzzleRoom setMetadata(final JSONObject metadata) {
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
   * @return the subscribe to self
   */
  public KuzzleRoom setSubscribeToSelf(final boolean subscribeToSelf) {
    this.subscribeToSelf = subscribeToSelf;
    return this;
  }

  /**
   * Get roomId
   *
   * @return roomId room id
   */
  public String getRoomId() {
    return this.roomId;
  }

  public KuzzleResponseListener<KuzzleNotificationResponse> getListener() {
    return this.listener;
  }

}
