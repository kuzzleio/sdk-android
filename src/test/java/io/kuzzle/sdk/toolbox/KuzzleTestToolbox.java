package io.kuzzle.sdk.toolbox;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.sdk.util.EventList;
import io.socket.client.Socket;

public class KuzzleTestToolbox {
  /**
   * Force the internal status to 'CONNECTED' to make query act as-if connected to Kuzzle
   *
   * @param kuzzle
   * @param state
   */
  public static void forceConnectedState(Kuzzle kuzzle, KuzzleStates state) {
    try {
      Field internalState = Kuzzle.class.getDeclaredField("state");
      internalState.setAccessible(true);
      internalState.set(kuzzle, state);
    }
    catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Force the internal socket with the provided socket argument
   *
   * @param kuzzle
   * @param s
   */
  public static void setSocket(Kuzzle kuzzle, Socket s) {
    try {
      Field internalSocket = Kuzzle.class.getDeclaredField("socket");
      internalSocket.setAccessible(true);
      internalSocket.set(kuzzle, s);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * * Returns all registered listeners on a given event
   *
   * @param kuzzle
   * @param event
   */
  public static EventList getEventListeners(Kuzzle kuzzle, KuzzleEvent event) {
    try {
      Field events = kuzzle.getClass().getDeclaredField("eventListeners");
      events.setAccessible(true);

      return ((ConcurrentHashMap<KuzzleEvent, EventList>)events.get(kuzzle)).get(event);
    }
    catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the subscription object from a Kuzzle instance
   *
   * @param kuzzle
   * @return
   */
  public static Map<String, Map<String, KuzzleRoom>> getSubscriptions(Kuzzle kuzzle) {
    try {
      Field subscriptions = kuzzle.getClass().getDeclaredField("subscriptions");
      subscriptions.setAccessible(true);

      return ((Map<String, Map<String, KuzzleRoom>>)subscriptions.get(kuzzle));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the request history from a Kuzzle instance
   *
   * @param kuzzle
   * @return
   */
  public static Map<String, Date> getRequestHistory(Kuzzle kuzzle) {
    try {
      Field history = kuzzle.getClass().getDeclaredField("requestHistory");
      history.setAccessible(true);
      return ((Map<String, Date>)history.get(kuzzle));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the internal socket instance from the kuzzle object
   * @return
   */
  public static Socket getSocket(Kuzzle kuzzle) {
    try {
      Field socket = kuzzle.getClass().getDeclaredField("socket");
      socket.setAccessible(true);
      return ((Socket)socket.get(kuzzle));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
