package io.kuzzle.test.core.KuzzleDocument;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class subscribeTest {
  private Kuzzle k;
  private KuzzleDocument doc;
  private KuzzleDataCollection mockCollection;

  @Before
  public void setUp() throws URISyntaxException, JSONException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("http://localhost:7512", opts, null);
    extended.setState(KuzzleStates.CONNECTED);
    k = spy(extended);
    mockCollection = mock(KuzzleDataCollection.class);
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "index", "test"));
  }

  @Test
  public void checkSignaturesVariants() {
    doc.setId("foo");
    doc = spy(doc);
    doc.subscribe(mock(KuzzleResponseListener.class));
    verify(doc).subscribe(eq((KuzzleRoomOptions)null), any(KuzzleResponseListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testSubscribeException() throws JSONException {
    doc = new KuzzleDocument(mockCollection);
    doc.setId("42");
    doThrow(JSONException.class).when(mockCollection).subscribe(any(JSONObject.class), any(KuzzleRoomOptions.class), any(KuzzleResponseListener.class));
    doc.subscribe(mock(KuzzleResponseListener.class));
  }

  @Test(expected = IllegalStateException.class)
  public void testSubscribeNullId() {
    doc.subscribe(mock(KuzzleResponseListener.class));
  }

  @Test
  public void testSubscribe() throws JSONException {
    doc.setId("42");
    doc.subscribe(mock(KuzzleResponseListener.class));
    doc.subscribe(new KuzzleRoomOptions(), mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(k, times(2)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "subscribe");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "on");
  }

}
