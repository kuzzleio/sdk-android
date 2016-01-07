package io.kuzzle.sdk.listeners;

/**
 * The interface Event listener.
 */
public interface IEventListener {

  /**
   * Trigger.
   *
   * @param args the args
   */
  void trigger(Object ... args);

}
