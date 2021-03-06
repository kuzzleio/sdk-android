package io.kuzzle.test.core.KuzzleDataCollection;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Document;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.States;
import io.kuzzle.test.testUtils.KuzzleExtend;
import tech.gusavila92.websocketclient.WebSocketClient;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class replaceDocumentTest {
  private Kuzzle kuzzle;
  private Collection collection;
  private ResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    Options opts = new Options();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    extended.setSocket(mock(WebSocketClient.class));
    extended.setState(States.CONNECTED);

    kuzzle = spy(extended);
    when(kuzzle.getHeaders()).thenReturn(new JSONObject());

    collection = new Collection(kuzzle, "test", "index");
    listener = mock(ResponseListener.class);
  }

  @Test
  public void checkSignaturesVariants() {
    JSONObject content = mock(JSONObject.class);
    String id = "foo";
    collection = spy(collection);

    collection.replaceDocument(id, content);
    collection.replaceDocument(id, content, mock(Options.class));
    collection.replaceDocument(id, content, listener);

    verify(collection, times(3)).replaceDocument(any(String.class), any(JSONObject.class), any(Options.class), any(ResponseListener.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReplaceDocumentIllegalDocumentId() {
    collection.replaceDocument(null, null);
  }

  @Test(expected = RuntimeException.class)
  public void testReplaceDocumentQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    collection.replaceDocument("id", mock(JSONObject.class), listener);
  }

  @Test(expected = RuntimeException.class)
  public void testReplaceDocumentException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("_id", "id-42")));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(String.class));
    collection.replaceDocument("id", mock(JSONObject.class), listener);
  }

  @Test
  public void testReplaceDocument() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject result = new JSONObject()
          .put("result", new JSONObject()
            .put("_id", "42")
            .put("_version", 1337)
            .put("_source", new JSONObject()));

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(result);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    Document doc = new Document(collection);
    doc.setContent("foo", "bar");
    collection.replaceDocument("42", doc.serialize(), listener);
    collection.replaceDocument("42", mock(JSONObject.class), mock(Options.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "document");
  }

}
