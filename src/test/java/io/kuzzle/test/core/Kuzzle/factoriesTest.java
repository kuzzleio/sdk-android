package io.kuzzle.test.core.Kuzzle;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class factoriesTest {
  private KuzzleExtend kuzzle;
  private Socket s;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    options.setDefaultIndex("testIndex");

    s = mock(Socket.class);
    kuzzle = new KuzzleExtend("localhost", options, null);
    kuzzle.setSocket(s);

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
  public void testDataCollectionFactory() {
    assertEquals(kuzzle.dataCollectionFactory("test").getCollection(), "test");
    assertEquals(kuzzle.dataCollectionFactory("test2").getCollection(), "test2");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIllegalDefaultIndex() {
    kuzzle.setSuperDefaultIndex(null);
    kuzzle.dataCollectionFactory("foo");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIllegalIndex() {
    kuzzle.setSuperDefaultIndex(null);
    kuzzle.dataCollectionFactory("collection", null);
  }
}
