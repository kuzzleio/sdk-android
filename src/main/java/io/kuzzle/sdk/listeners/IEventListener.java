package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

/**
 * The interface Event listener.
 */
public interface IEventListener {

  /**
   * Trigger.
   *
   */
  void trigger(final String id, final JSONObject object);

}
