package io.kuzzle.test.core.KuzzleDataCollection;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.CollectionMapping;
import io.kuzzle.sdk.core.Kuzzle;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class factoriesTest {
  private Kuzzle kuzzle;
  private Collection collection;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    extended.setSocket(mock(Socket.class));
    extended.setState(KuzzleStates.CONNECTED);

    kuzzle = spy(extended);
    when(kuzzle.getHeaders()).thenReturn(new JSONObject());

    collection = new Collection(kuzzle, "test", "index");
    listener = mock(KuzzleResponseListener.class);
  }

  @Test
  public void testRoomFactory() {
    assertThat(collection.room(mock(KuzzleRoomOptions.class)), instanceOf(KuzzleRoom.class));
    assertThat(collection.room(), instanceOf(KuzzleRoom.class));
  }

  @Test
  public void testDocumentFactory() throws JSONException {
    assertThat(collection.document(), instanceOf(KuzzleDocument.class));
    assertThat(collection.document("id"), instanceOf(KuzzleDocument.class));
    assertThat(collection.document("id", new JSONObject()), instanceOf(KuzzleDocument.class));
    assertThat(collection.document(new JSONObject()), instanceOf(KuzzleDocument.class));
  }

  @Test
  public void testDataMappingFactory() {
    assertThat(collection.collectionMapping(), instanceOf(CollectionMapping.class));
    assertThat(collection.collectionMapping(new JSONObject()), instanceOf(CollectionMapping.class));
  }
}
