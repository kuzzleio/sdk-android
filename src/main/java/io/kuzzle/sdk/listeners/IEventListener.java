package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

/**
 * The interface Event listener.
 */
public interface IEventListener {

  /**
   * Trigger.
   *
   * @param subscriptionId the subscription id
   * @param result         the result
   */
  void trigger(String subscriptionId, JSONObject result) throws Exception;

}
