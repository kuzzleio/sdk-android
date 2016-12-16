package io.kuzzle.test.core.KuzzleDocument;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class saveTest {
  private Kuzzle k;
  private KuzzleDocument doc;
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
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "test", "index"));
  }

  @Test
  public void checkSignaturesVariants() {
    doc = spy(doc);
    doc.save();
    doc.save(mock(KuzzleOptions.class));
    doc.save(mockListener);
    verify(doc, times(3)).save(any(KuzzleOptions.class), any(KuzzleResponseListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testSaveQueryException() throws JSONException {
    doc.setId("42");
    doThrow(JSONException.class).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.save();
  }

  @Test(expected = RuntimeException.class)
  public void testSaveException() throws JSONException {
    doc.setId("42");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("_id", "42").put("_version", "42")));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(mockListener).onSuccess(any(KuzzleDocument.class));
    doc.save(mockListener);
  }

  @Test
  public void testSave() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("_id", "id-42");
        response.put("_version", "42");
        response.put("result", response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(null);
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.save();
    doc.save(new KuzzleOptions());
    doc.save(new KuzzleResponseListener<KuzzleDocument>() {
      @Override
      public void onSuccess(KuzzleDocument object) {
        assertEquals(object.getId(), "id-42");
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(k, times(3)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "document");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplace");
  }
}
