package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.kuzzle.sdk.enums.Event;
import io.kuzzle.sdk.enums.Scope;
import io.kuzzle.sdk.enums.State;
import io.kuzzle.sdk.enums.Users;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.SubscribeListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.NotificationResponse;
import io.kuzzle.sdk.state.KuzzleStates;
import io.socket.emitter.Emitter;

/**
 * The type Kuzzle room.
 */
public class Room {

  private String id = UUID.randomUUID().toString();
  /**
   * The Collection.
   */
  protected String collection;
  /**
   * The Data collection.
   */
  protected Collection dataCollection;
  /**
   * The Filters.
   */
  protected JSONObject filters = new JSONObject();
  /**
   * The Headers.
   */
  protected JSONObject headers;
  /**
   * The Metadata.
   */
  protected JSONObject metadata;
  /**
   * The Subscribe to self.
   */
  protected boolean subscribeToSelf;
  /**
   * The Room id.
   */
  protected String roomId;
  /**
   * The Kuzzle.
   */
  protected Kuzzle kuzzle;
  /**
   * The Channel.
   */
  protected String channel;
  /**
   * The Scope.
   */
  protected Scope scope;
  /**
   * The State.
   */
  protected State state;
  /**
   * The Users.
   */
  protected Users users;
  /**
   * The Listener.
   */
  protected ResponseListener<NotificationResponse> listener;

  // Used to avoid subscription renewals to trigger multiple times because of
  // multiple but similar events
  private long lastRenewal = 0;
  private long renewalDelay = 500;

  /**
   * The Subscribing.
   */
// Used to delay method calls when subscription is in progress
  protected boolean subscribing = false;
  private ArrayList<Runnable> queue = new ArrayList<>();
  private SubscribeListener doneListener;

  /**
   * Instantiates a new Kuzzle room.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   */
  public Room(@NonNull final Collection kuzzleDataCollection) {
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
  public Room(@NonNull final Collection kuzzleDataCollection, final RoomOptions options) {
    RoomOptions opts = options != null ? options : new RoomOptions();

    if (kuzzleDataCollection == null) {
      throw new IllegalArgumentException("Room: missing dataCollection");
    }
    kuzzleDataCollection.getKuzzle().isValid();

    this.dataCollection = kuzzleDataCollection;
    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.collection = kuzzleDataCollection.getCollection();

    try {
      this.headers = new JSONObject(kuzzleDataCollection.getHeaders().toString());
    }
    catch (JSONException e) {
      throw new RuntimeException(e);
    }

    this.subscribeToSelf = opts.isSubscribeToSelf();
    this.metadata = opts.getMetadata();
    this.scope = opts.getScope();
    this.state = opts.getState();
    this.users = opts.getUsers();
  }

  /**
   * Returns the number of other subscriptions on that room.
   *
   * @param listener the listener
   */
  public void count(@NonNull final ResponseListener<Integer> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Room.count: a callback listener is required");
    }

    // Delays this call until after the subscription is finished
    if (!this.isReady()) {
      this.queue.add(new Runnable() {
        @Override
        public void run() {
          Room.this.count(listener);
        }
      });
      return;
    }

    if (this.roomId == null) {
      throw new IllegalStateException("Room.count: cannot count subscriptions on an inactive room");
    }

    try {
      JSONObject data = new JSONObject().put("body", new JSONObject().put("roomId", this.roomId));
      this.kuzzle.addHeaders(data, this.headers);

      this.kuzzle.query(this.dataCollection.makeQueryArgs("realtime", "count"), data, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result").getInt("count"));
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
   * Call after renew.
   *
   * @param args the args
   */
  protected void callAfterRenew(final Object args) {
    if (args == null) {
      throw new IllegalArgumentException("Room.renew: response required");
    }

    try {
      if (!((JSONObject) args).isNull("error")) {
        listener.onError((JSONObject) args);
      }
      else {
        String key = ((JSONObject) args).getString("requestId");

        if (((JSONObject) args).getString("action").equals("jwtTokenExpired")) {
          Room.this.kuzzle.jwtToken = null;
          Room.this.kuzzle.emitEvent(Event.jwtTokenExpired);
        }

        if (Room.this.kuzzle.getRequestHistory().containsKey(key)) {
          if (Room.this.subscribeToSelf) {
            listener.onSuccess(new NotificationResponse(kuzzle, (JSONObject) args));
          }
          Room.this.kuzzle.getRequestHistory().remove(key);
        } else {
          listener.onSuccess(new NotificationResponse(kuzzle, (JSONObject) args));
        }
      }
    } catch (JSONException e) {
      try {
        listener.onError(((JSONObject) args).getJSONObject("error"));
      } catch (JSONException err) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Renew the subscription. Force a resubscription using the same filters if no new ones are provided.
   * Unsubscribes first if this Room was already listening to events.
   *
   * @param listener the listener
   * @return kuzzle room
   */
  public Room renew(@NonNull final ResponseListener<NotificationResponse> listener) {
    return this.renew(null, listener, null);
  }

  /**
   * Renew the subscription. Force a resubscription using the same filters if no new ones are provided.
   * Unsubscribes first if this Room was already listening to events.
   *
   * @param listener the listener
   * @param subscribeResponseListener
   * @return kuzzle room
   */
  public Room renew(@NonNull final ResponseListener<NotificationResponse> listener, final SubscribeListener subscribeResponseListener) {
    return this.renew(null, listener, subscribeResponseListener);
  }

  /**
   * Renew the subscription. Force a resubscription using the same filters if no new ones are provided.
   * Unsubscribes first if this Room was already listening to events.
   *
   * @param filters  the filters
   * @param listener the listener
   * @return kuzzle room
   */
  public Room renew(final JSONObject filters, @NonNull final ResponseListener<NotificationResponse> listener, final SubscribeListener subscribeResponseListener) {
    long now = System.currentTimeMillis();

    if (listener == null) {
      throw new IllegalArgumentException("Room.renew: a callback listener is required");
    }

    // Skip subscription renewal if another one was performed just a moment before
    if (this.lastRenewal > 0 && (now - this.lastRenewal) <= this.renewalDelay) {
      return this;
    }

    if (filters != null) {
      this.filters = filters;
    }

    /*
      If not yet connected, registers itself into the subscriptions list and wait for the
      main Kuzzle object to renew subscriptions once online
     */
    if (this.kuzzle.state != KuzzleStates.CONNECTED) {
      this.listener = listener;
      this.doneListener = subscribeResponseListener;
      this.kuzzle.addPendingSubscription(this.id, this);
      return this;
    }

    if (this.subscribing) {
      this.queue.add(new Runnable() {
        @Override
        public void run() {
          Room.this.renew(filters, listener, subscribeResponseListener);
        }
      });

      return this;
    }

    this.unsubscribe();
    this.roomId = null;
    this.subscribing = true;
    this.listener = listener;
    this.doneListener = subscribeResponseListener;
    this.kuzzle.addPendingSubscription(this.id, this);

    try {
      final Options options = new Options();
      final JSONObject
          subscribeQuery = new JSONObject()
          .put("body", this.filters)
          .put("scope", this.scope.toString().toLowerCase())
          .put("state", this.state.toString().toLowerCase())
          .put("users", this.users.toString().toLowerCase());

      options.setMetadata(this.metadata);
      this.kuzzle.addHeaders(subscribeQuery, this.headers);

      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            Room.this.kuzzle.query(Room.this.dataCollection.makeQueryArgs("realtime", "subscribe"), subscribeQuery, options, new OnQueryDoneListener() {
              @Override
              public void onSuccess(JSONObject args) {
                try {
                  Room.this.kuzzle.deletePendingSubscription(Room.this.id);
                  Room.this.subscribing = false;
                  Room.this.lastRenewal = System.currentTimeMillis();

                  JSONObject result = args.getJSONObject("result");
                  Room.this.channel = result.getString("channel");
                  Room.this.roomId = result.getString("roomId");
                  if (subscribeResponseListener != null) {
                    subscribeResponseListener.done(null, Room.this);
                  }
                } catch (JSONException e) {
                  throw new RuntimeException(e);
                }

                Room.this.kuzzle.addSubscription(Room.this.roomId, Room.this.id, Room.this);

                Room.this.kuzzle.getSocket().on(Room.this.channel, new Emitter.Listener() {
                  @Override
                  public void call(final Object... args) {
                    callAfterRenew(args[0]);
                  }
                });

                Room.this.dequeue();
              }

              @Override
              public void onError(JSONObject arg) {
                Room.this.subscribing = false;
                Room.this.queue.clear();
                if (subscribeResponseListener != null) {
                  subscribeResponseListener.done(arg, null);
                }
              }
            });
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }
      }).start();

    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Unsubscribes from Kuzzle.
   * Stop listening immediately. If there is no listener left on that room, sends an unsubscribe request to Kuzzle, once
   * pending subscriptions reaches 0, and only if there is still no listener on that room.
   * We wait for pending subscriptions to finish to avoid unsubscribing while another subscription on that room is
   *
   * @return the kuzzle room
   */
  public Room unsubscribe() {
    if (!this.isReady()) {
      this.queue.add(new Runnable() {
        @Override
        public void run() {
          Room.this.unsubscribe();
        }
      });
      return this;
    }

    if (this.roomId == null) {
      return this;
    }

    try {
      final JSONObject data = new JSONObject().put("body", new JSONObject().put("roomId", this.roomId));
      this.kuzzle.addHeaders(data, this.headers);

      this.kuzzle.getSocket().off(Room.this.channel);
      this.kuzzle.deleteSubscription(this.roomId, this.id);

      if (this.kuzzle.getSubscriptions(this.roomId) == null) {
        final String roomId = this.roomId;
        if (this.kuzzle.getPendingSubscriptions().isEmpty()) {
          this.kuzzle.query(this.dataCollection.makeQueryArgs("realtime", "unsubscribe"), data);
        } else {
          final Timer timer = new Timer(UUID.randomUUID().toString());
          unsubscribeTask(timer, roomId, data).run();
        }
      }

      this.roomId = null;
    }
    catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Unsubscribe task timer task.
   *
   * @param timer  the timer
   * @param roomId the room id
   * @param data   the data
   * @return the timer task
   */
  protected TimerTask unsubscribeTask(final Timer timer, final String roomId, final JSONObject data) {
    return new TimerTask() {
      @Override
      public void run() {
        try {
          if (Room.this.kuzzle.getPendingSubscriptions().isEmpty()) {
            if (Room.this.kuzzle.getSubscriptions(roomId) == null) {
              Room.this.kuzzle.query(Room.this.dataCollection.makeQueryArgs("realtime", "unsubscribe"), data);
            }
          } else {
            timer.schedule(unsubscribeTask(timer, roomId, data), 100);
          }
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }
    };
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
  public Room setFilters(final JSONObject filters) {
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
  public Room setHeaders(final JSONObject content) {
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
  public Room setHeaders(final JSONObject content, final boolean replace) {
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
  public Room setMetadata(final JSONObject metadata) {
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
  public Room setSubscribeToSelf(final boolean subscribeToSelf) {
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

  /**
   * Getter for the listener property
   *
   * @return listener
   */
  public ResponseListener<NotificationResponse> getListener() {
    return this.listener;
  }

  public SubscribeListener getSubscribeListener() {
    return doneListener;
  }

  /**
   * Runs all queued methods called while subscription was in progress
   */
  protected void dequeue() {
    if (this.queue.size() > 0) {
      ExecutorService threadPool = Executors.newSingleThreadExecutor();

      for(Runnable r: this.queue) {
        threadPool.execute(r);
      }

      threadPool.shutdown();

      try {
        threadPool.awaitTermination(1, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        // do nothing
      }
      finally {
        this.queue.clear();
      }
    }
  }

  private boolean isReady() {
    return this.kuzzle.state == KuzzleStates.CONNECTED && !this.subscribing;
  }
}
