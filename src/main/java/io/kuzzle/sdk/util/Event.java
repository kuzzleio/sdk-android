package io.kuzzle.sdk.util;

import io.kuzzle.sdk.listeners.EventListener;

/**
 * The type Event.
 */
public abstract class Event implements EventListener {
  private io.kuzzle.sdk.enums.Event type;

  /**
   * Instantiates a new Event.
   *
   * @param type the type
   */
  public Event(io.kuzzle.sdk.enums.Event type) {
    this.type = type;
  }

  public abstract void trigger(Object... args);

  /**
   * Gets type.
   *
   * @return the type
   */
  public io.kuzzle.sdk.enums.Event getType() {
    return this.type;
  }
}
