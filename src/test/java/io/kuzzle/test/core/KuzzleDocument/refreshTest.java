package io.kuzzle.test.core.KuzzleDocument;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Document;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class refreshTest {
  private Kuzzle k;
  private Document doc;
  private KuzzleResponseListener mockListener;

  @Before
  public void setUp() throws URISyntaxException, JSONException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    extended.setState(KuzzleStates.CONNECTED);
    extended.setSocket(mock(Socket.class));
    k = spy(extended);
    mockListener = mock(KuzzleResponseListener.class);
    doc = new Document(new Collection(k, "test", "index"));
  }

  @Test
  public void checkSignaturesVariants() {
    doc.setId("foo");
    doc = spy(doc);
    doc.refresh(mockListener);
    verify(doc).refresh(eq((KuzzleOptions)null), eq(mockListener));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRefreshNullListenerException() throws IllegalArgumentException {
    doc.setId("42");
    doc.refresh(null);
  }

  @Test(expected = RuntimeException.class)
  public void testRefreshQueryException() throws JSONException {
    doc.setId("42");
    doThrow(JSONException.class).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.refresh(mockListener);
  }

  @Test(expected = RuntimeException.class)
  public void testRefreshException() throws JSONException {
    doc.setId("42");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject result = new JSONObject()
            .put("result", new JSONObject()
              .put("_id", "foo")
              .put("_source", mock(JSONObject.class))
            );
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(result);
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(mockListener).onSuccess(any(Document.class));
    doc.refresh(mockListener);
  }

  @Test(expected = IllegalStateException.class)
  public void testRefreshWithoutId() {
    doc.refresh(null, null);
  }

  @Test
  public void testRefresh() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject()
          .put("result", new JSONObject()
            .put("_id", "42")
            .put("_version", 1337)
            .put("_source", new JSONObject().put("foo", "bar")));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(null);
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.setId("42");
    doc.setContent("foo", "baz");
    doc.refresh(new KuzzleResponseListener<Document>() {
      @Override
      public void onSuccess(Document object) {
        try {
          assertEquals(1337, object.getVersion());
          assertEquals("bar", object.getContent().getString("foo"));
          assertNotEquals(doc, object);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    doc.refresh(mockListener);
    doc.refresh(mock(KuzzleOptions.class), mockListener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(k, times(3)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "document");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "get");

  }
}
