package io.kuzzle.sdk.util;

import java.util.UUID;

import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.listeners.IKuzzleEventListener;

/**
 * The type Event.
 */
public abstract class Event implements IKuzzleEventListener {
  private UUID id;
  private KuzzleEvent type;

  /**
   * Instantiates a new Event.
   *
   * @param type the type
   */
  public Event(KuzzleEvent type) {
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
  public KuzzleEvent getType() {
    return this.type;
  }
}
