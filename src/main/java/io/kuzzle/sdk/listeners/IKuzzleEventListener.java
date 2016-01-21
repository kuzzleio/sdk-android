package io.kuzzle.sdk.listeners;

/**
 * The interface Event listener.
 */
public interface IKuzzleEventListener {

  /**
   * Trigger.
   *
   * @param args the args
   */
  void trigger(Object ... args);

}
