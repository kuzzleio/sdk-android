package io.kuzzle.sdk.listeners;

public interface KuzzleSubscribeListener<T> {
  void onDone(T response);
}
