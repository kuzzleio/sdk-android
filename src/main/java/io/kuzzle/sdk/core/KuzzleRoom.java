package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.util.Event;
import io.socket.emitter.Emitter;

/**
 * Created by kblondel on 13/10/15.
 */
public class KuzzleRoom {

    private String          collection;
    private JSONObject      filters;
    private JSONObject      headers;
    private boolean         listeningToConnections;
    private boolean         listeningToDisconnections;
    private JSONObject      metadata;
    private boolean         subscribeToSelf;
    private String          subscriptionId;
    private String          subscriptionTimestamp;
    private String         roomId;
    private Kuzzle          kuzzle;

    public KuzzleRoom(KuzzleDataCollection kuzzleDataCollection) {
        this(kuzzleDataCollection, null);
    }

    /**
     * This object is the result of a subscription request, allowing to manipulate the subscription itself.
     *
     * In Kuzzle, you don’t exactly subscribe to a room or a topic but, instead, you subscribe to documents.
     *
     * What it means is that, to subscribe, you provide to Kuzzle a set of matching filters.
     * Once you have subscribed, if a pub/sub message is published matching your filters, or if a matching stored
     * document change (because it is created, updated or deleted), then you’ll receive a notification about it.
     * @param kuzzleDataCollection
     * @param options
     */
    public KuzzleRoom(final KuzzleDataCollection kuzzleDataCollection, final KuzzleRoomOption options) throws NullPointerException {
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
    }

    /**
     * Returns the number of other subscriptions on that room.
     *
     * @param cb
     * @return
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleRoom   count(final ResponseListener cb) throws JSONException, IOException {
        JSONObject body = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("roomId", this.roomId);
        body.put("body", data);
        this.kuzzle.addHeaders(body, this.headers);
        this.kuzzle.query(this.collection, "subscribe", "count", body, cb);
        return this;
    }

    protected void  triggerEvents(boolean listening, EventType globalEvent, JSONObject args2, ResponseListener cb, Object... args) throws Exception {
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
                e.trigger(KuzzleRoom.this.subscriptionId, result);
            else if (e.getType() == EventType.UNSUBSCRIBED && globalEvent == EventType.UNSUBSCRIBED)
                e.trigger(KuzzleRoom.this.subscriptionId, result);
        }
    }

    protected void  callAfterRenew(final ResponseListener cb, final Object... args) throws Exception {
        if (args == null)
            throw new NullPointerException("Response is null");
        JSONObject response = ((JSONObject) args[0]);
        JSONObject result = (JSONObject)response.get("result");
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
            if (listening || KuzzleRoom.this.eventExist(globalEvent)) {
                KuzzleRoom.this.count(new ResponseListener() {
                    @Override
                    public void onSuccess(JSONObject args2) throws Exception {
                        triggerEvents(listening, globalEvent, (JSONObject)args2, cb, args);
                    }

                    @Override
                    public void onError(JSONObject args2) throws Exception {
                        cb.onError(args2);
                    }
                });
            }
        } else {
            cb.onSuccess((JSONObject) ((JSONObject) args[0]).get("result"));
        }
    }

    /**
     * Renew the subscription using new filters
     *
     * @param filters
     * @param cb
     * @return
     */
    public KuzzleRoom   renew(final JSONObject filters, final ResponseListener cb, final ResponseListener ready) throws JSONException, IOException, KuzzleException {
        this.filters = (filters == null ? new JSONObject() : filters);
        this.unsubscribe();
        final JSONObject data = new JSONObject();
        data.put("body", this.filters);
        this.kuzzle.addHeaders(data, this.headers);

        this.kuzzle.query(this.collection, "subscribe", "on", data, new ResponseListener() {
            @Override
            public void onSuccess(JSONObject args) throws Exception {

                KuzzleRoom.this.roomId = args.get("roomId").toString();
                KuzzleRoom.this.subscriptionId = args.get("roomName").toString();
                KuzzleRoom.this.subscriptionTimestamp = new Date().toString();
                KuzzleRoom.this.kuzzle.getSocket().on(KuzzleRoom.this.roomId, new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        try {
                           callAfterRenew(cb, args);
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
        for (Event e : kuzzle.getEventListeners())
            if (e.getType() == event)
                return true;
        return false;
    }

    public KuzzleRoom   unsubscribe() throws JSONException {
        if (this.roomId != null) {
            JSONObject  data = new JSONObject();
            this.kuzzle.addHeaders(data, this.headers);
            this.kuzzle.getSocket().off(String.valueOf(this.roomId));
            this.roomId = null;
            this.subscriptionId = null;
            this.subscriptionTimestamp = null;
        }
        return this;
    }

    public String getCollection() {
        return collection;
    }

    public JSONObject getFilters() {
        return filters;
    }

    public void setFilters(JSONObject filters) {
        this.filters = filters;
    }

    public JSONObject getHeaders() {
        return headers;
    }

    public void setHeaders(JSONObject headers) {
        this.headers = headers;
    }

    public boolean isListeningToConnections() {
        return listeningToConnections;
    }

    public void setListeningToConnections(boolean listeningToConnections) {
        this.listeningToConnections = listeningToConnections;
    }

    public boolean isListeningToDisconnections() {
        return listeningToDisconnections;
    }

    public void setListeningToDisconnections(boolean listeningToDisconnections) {
        this.listeningToDisconnections = listeningToDisconnections;
    }

    public JSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    public boolean isSubscribeToSelf() {
        return subscribeToSelf;
    }

    public void setSubscribeToSelf(boolean subscribeToSelf) {
        this.subscribeToSelf = subscribeToSelf;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getSubscriptionTimestamp() {
        return subscriptionTimestamp;
    }
}
