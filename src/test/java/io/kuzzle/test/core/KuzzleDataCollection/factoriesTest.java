package io.kuzzle.test.core.KuzzleDataCollection;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDataMapping;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class factoriesTest {
  private Kuzzle kuzzle;
  private KuzzleDataCollection collection;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("http://localhost:7512", opts, null);
    extended.setSocket(mock(Socket.class));
    extended.setState(KuzzleStates.CONNECTED);

    kuzzle = spy(extended);
    when(kuzzle.getHeaders()).thenReturn(new JSONObject());

    collection = new KuzzleDataCollection(kuzzle, "index", "test");
    listener = mock(KuzzleResponseListener.class);
  }

  @Test
  public void testRoomFactory() {
    assertThat(collection.roomFactory(mock(KuzzleRoomOptions.class)), instanceOf(KuzzleRoom.class));
    assertThat(collection.roomFactory(), instanceOf(KuzzleRoom.class));
  }

  @Test
  public void testDocumentFactory() throws JSONException {
    assertThat(collection.documentFactory(), instanceOf(KuzzleDocument.class));
    assertThat(collection.documentFactory("id"), instanceOf(KuzzleDocument.class));
    assertThat(collection.documentFactory("id", new JSONObject()), instanceOf(KuzzleDocument.class));
    assertThat(collection.documentFactory(new JSONObject()), instanceOf(KuzzleDocument.class));
  }

  @Test
  public void testDataMappingFactory() {
    assertThat(collection.dataMappingFactory(new JSONObject()), instanceOf(KuzzleDataMapping.class));
  }
}
