package io.kuzzle.test.core.KuzzleDataCollection;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.States;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class fetchAllDocumentsTest {
  private Kuzzle kuzzle;
  private Collection collection;
  private ResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    Options opts = new Options();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    extended.setSocket(mock(Socket.class));
    extended.setState(States.CONNECTED);

    kuzzle = spy(extended);
    when(kuzzle.getHeaders()).thenReturn(new JSONObject());

    collection = new Collection(kuzzle, "test", "index");
    listener = mock(ResponseListener.class);
  }

  @Test
  public void checkSignaturesVariants() {
    collection = spy(collection);
    collection.fetchAllDocuments(listener);
    verify(collection).fetchAllDocuments(eq((Options)null), eq(listener));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFetchAllDocumentsIllegalListener() {
    collection.fetchAllDocuments(null);
  }

  @Test
  public void testFetchAllDocuments() throws JSONException {
    collection.fetchAllDocuments(new Options(), mock(ResponseListener.class));
    collection.fetchAllDocuments(mock(ResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "document");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "search");
  }

  @Test
  public void testPagination() throws JSONException {
    Options options = new Options();
    options.setFrom(1L);
    options.setSize(42L);
    collection.fetchAllDocuments(options, mock(ResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(kuzzle).query(any(Kuzzle.QueryArgs.class), (JSONObject)argument.capture(), any(Options.class), any(OnQueryDoneListener.class));
    assertEquals(((JSONObject) argument.getValue()).getLong("from"), 1);
    assertEquals(((JSONObject) argument.getValue()).getLong("size"), 42);
  }

}
