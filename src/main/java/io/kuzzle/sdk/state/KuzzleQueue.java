package io.kuzzle.sdk.state;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/**
 * The type KuzzleQueue.
 *
 * @param <T> the type parameter
 */
public class KuzzleQueue<T> implements Iterable<T> {

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

  private KuzzleStates _currentState = KuzzleStates.DISCONNECTED;

  /**
   * Sets state.
   *
   * @param states the states
   */
  public void setState(KuzzleStates states) {
    _currentState = states;
  }

  /**
   * State states.
   *
   * @return the states
   */
  public KuzzleStates state() {
    return _currentState;
  }

  public Queue  getQueue() {
    return _queue;
  }

  @Override
  public Iterator<T> iterator() {
    return _queue.iterator();
  }
}
