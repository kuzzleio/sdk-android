package io.kuzzle.sdk.state;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by kblondel on 14/10/15.
 */
public class Context<T> {

    private Queue<T>    _queue = new ArrayDeque<>();

    public synchronized void addToQueue(T object) {
        _queue.add(object);
    }

    public synchronized T   dequeue() {
        return _queue.poll();
    }

    private States _currentState = States.DISCONNECTED;

    public void setState(States states) {
        _currentState = states;
    }

    public States state() {
        return _currentState;
    }

}
