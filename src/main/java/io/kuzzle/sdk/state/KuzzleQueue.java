package io.kuzzle.sdk.state;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * The type KuzzleQueue.
 *
 * @param <T> the type parameter
 */
public class KuzzleQueue<T> {

  private Queue<T> _queue = new ArrayDeque<>();

  /**
   * Add to queue.
   *
   * @param object the object
   */
  public synchronized void addToQueue(T object) {
    _queue.add(object);
  }

  /**
   * Dequeue t.
   *
   * @return the t
   */
  public synchronized T dequeue() {
    return _queue.poll();
  }

  private States _currentState = States.DISCONNECTED;

  /**
   * Sets state.
   *
   * @param states the states
   */
  public void setState(States states) {
    _currentState = states;
  }

  /**
   * State states.
   *
   * @return the states
   */
  public States state() {
    return _currentState;
  }

  public Queue  getQueue() {
    return _queue;
  }

}
