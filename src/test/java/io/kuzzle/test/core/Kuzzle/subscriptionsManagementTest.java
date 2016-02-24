package io.kuzzle.test.core.Kuzzle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class subscriptionsManagementTest {
  private KuzzleExtend kuzzle;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    options.setDefaultIndex("testIndex");

    kuzzle = new KuzzleExtend("http://localhost:7512", options, null);
    kuzzle.setSocket(mock(Socket.class));

    listener = new KuzzleResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
  }

  @Test
  public void testDeleteSubscription() throws JSONException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Map<String, ConcurrentHashMap<String, KuzzleRoom>> subscriptions = kuzzle.getSubscriptions();

    subscriptions.put("foo", new ConcurrentHashMap<String, KuzzleRoom>());
    subscriptions.get("foo").put("bar", mock(KuzzleRoom.class));
    subscriptions.get("foo").put("baz", mock(KuzzleRoom.class));
    subscriptions.get("foo").put("qux", mock(KuzzleRoom.class));

    kuzzle.deleteSubscription("foobar", "whatever");

    // there is always a "pending" room ID used to store pending subscriptions
    assertEquals(subscriptions.keySet().size(), 2);
    assertEquals(subscriptions.get("pending").size(), 0);
    assertEquals(subscriptions.get("foo").size(), 3);

    kuzzle.deleteSubscription("foo", "baz");

    assertEquals(subscriptions.keySet().size(), 2);
    assertEquals(subscriptions.get("pending").size(), 0);
    assertEquals(subscriptions.get("foo").size(), 2);

    kuzzle.deleteSubscription("foo", "qux");

    assertEquals(subscriptions.keySet().size(), 2);
    assertEquals(subscriptions.get("pending").size(), 0);
    assertEquals(subscriptions.get("foo").size(), 1);

    kuzzle.deleteSubscription("foo", "bar");

    assertEquals(subscriptions.keySet().size(), 1);
    assertEquals(subscriptions.get("pending").size(), 0);
    assertEquals(subscriptions.containsKey("foo"), false);
  }
}
