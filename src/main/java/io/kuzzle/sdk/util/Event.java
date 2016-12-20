package io.kuzzle.sdk.util;

import java.util.UUID;

import io.kuzzle.sdk.listeners.EventListener;

/**
 * The type Event.
 */
public abstract class Event implements EventListener {
  private UUID id;
  private io.kuzzle.sdk.enums.Event type;

  /**
   * Instantiates a new Event.
   *
   * @param type the type
   */
  public Event(io.kuzzle.sdk.enums.Event type) {
    this.id = UUID.randomUUID();
    this.type = type;
  }

  public abstract void trigger(Object... args);

  /**
   * Gets id.
   *
   * @return the id
   */
  public UUID getId() {
    return id;
  }

  /**
   * Gets type.
   *
   * @return the type
   */
  public io.kuzzle.sdk.enums.Event getType() {
    return this.type;
  }
}
