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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class deleteTest {
  private Kuzzle k;
  private KuzzleDocument doc;

  @Before
  public void setUp() throws URISyntaxException, JSONException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    extended.setState(KuzzleStates.CONNECTED);
    k = spy(extended);
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "test", "index"));
  }

  @Test
  public void checkSignaturesVariants() {
    doc.setId("foo");
    doc = spy(doc);
    doc.delete();
    doc.delete(mock(KuzzleOptions.class));
    doc.delete(mock(KuzzleResponseListener.class));
    verify(doc, times(3)).delete(any(KuzzleOptions.class), any(KuzzleResponseListener.class));
  }

  @Test(expected = IllegalStateException.class)
  public void cannotDeleteWithoutID() {
    doc.delete();
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteException() throws JSONException {
    doc.setId("42");
    doThrow(JSONException.class).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.delete();
  }

  @Test
  public void testDelete() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("result", "foo");
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(null);
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.setId("id-42");
    doc.delete(mock(KuzzleResponseListener.class));
    doc.setId("id-42");
    doc.delete();
    doc.setId("id-42");
    doc.delete(mock(KuzzleOptions.class));
    assertNull(doc.getId());
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(k, times(3)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "write");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "delete");
  }
}
