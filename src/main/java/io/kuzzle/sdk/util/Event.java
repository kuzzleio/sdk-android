package io.kuzzle.sdk.util;

import org.json.JSONObject;

import java.util.UUID;

import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.listeners.IEventListener;

/**
 * Created by kblondel on 13/10/15.
 */
public abstract class Event implements IEventListener {

    private UUID    id;
    private EventType   type;

    private Event() {}

    public Event(EventType type) {
        this.id = UUID.randomUUID();
        this.type = type;
    }

    public abstract void trigger(String subscriptionId, JSONObject result);

    public UUID getId() {
        return id;
    }

    public EventType    getType() {
        return this.type;
    }
}
