package io.kuzzle.test.core.Kuzzle;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;

import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.test.testUtils.KuzzleDataCollectionExtend;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.kuzzle.test.testUtils.KuzzleRoomExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class unsetJwtTokenTest {
  private KuzzleExtend kuzzle;
  private Socket s;
  private ConcurrentHashMap<String, KuzzleRoom> chp = new ConcurrentHashMap<>();
  private KuzzleRoom  room;

  @Before
  public void setUp() throws URISyntaxException {
    kuzzle = new KuzzleExtend("host", mock(KuzzleOptions.class), mock(KuzzleResponseListener.class));
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    options.setDefaultIndex("testIndex");

    s = mock(Socket.class);
    kuzzle = new KuzzleExtend("localhost", options, null);
    kuzzle.getSubscriptions().put("1", chp);
    kuzzle.setSocket(s);

    kuzzle = spy(kuzzle);
    doNothing().when(kuzzle).emitEvent(any(KuzzleEvent.class), any(JSONObject.class));
    room = spy(new KuzzleRoomExtend(new KuzzleDataCollectionExtend(kuzzle, "index", "collection")));
    chp.put("2", room);
  }

  @Test
  public void shouldUnsetTokenAndUnsubscribeAllRoom() {
    kuzzle.setJwtTokenWithoutSubscribe("42");
    kuzzle.unsetJwtToken();
    assertEquals(null, kuzzle.getJwtToken());
    verify(room).unsubscribe();
  }
}
