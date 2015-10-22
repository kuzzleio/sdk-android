package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

/**
 * Created by kblondel on 13/10/15.
 */
public interface IEventListener {

    void trigger(String subscriptionId, JSONObject result);

}
