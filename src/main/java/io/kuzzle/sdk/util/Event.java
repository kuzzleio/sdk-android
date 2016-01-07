package io.kuzzle.sdk.util;

import java.util.UUID;

import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.listeners.IEventListener;

/**
 * The type Event.
 */
public abstract class Event implements IEventListener {

  private UUID id;
  private EventType type;

  private Event() {
  }

  /**
   * Instantiates a new Event.
   *
   * @param type the type
   */
  public Event(EventType type) {
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
  public EventType getType() {
    return this.type;
  }
}
