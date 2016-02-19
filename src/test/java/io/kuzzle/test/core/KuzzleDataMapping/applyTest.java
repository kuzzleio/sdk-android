package io.kuzzle.test.core.KuzzleDataMapping;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDataMapping;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class applyTest {
  private Kuzzle k;
  private KuzzleDataCollection dataCollection;
  private KuzzleDataMapping dataMapping;

  @Before
  public void setUp() {
    k = mock(Kuzzle.class);
    when(k.getDefaultIndex()).thenReturn("index");
    when(k.getHeaders()).thenReturn(new JSONObject());
    dataCollection = new KuzzleDataCollection(k, "index", "test");
    dataMapping = new KuzzleDataMapping(dataCollection);
  }

  @Test(expected = RuntimeException.class)
  public void testApplyQueryException() throws JSONException {
    doThrow(JSONException.class).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    dataMapping.apply();
  }

  @Test(expected = RuntimeException.class)
  public void testApplyException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(mock(JSONObject.class));
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    KuzzleResponseListener mockListener = mock(KuzzleResponseListener.class);
    doThrow(JSONException.class).when(mockListener).onSuccess(any(KuzzleDataMapping.class));
    dataMapping.apply(mockListener);
  }

  @Test
  public void testApply() throws JSONException {
    final JSONObject mockResponse = new JSONObject("{\n" +
      "    properties: {\n" +
      "      field1: {type: \"field type\"},\n" +
      "      field2: {type: \"field type\"},\n" +
      "      fieldn: {type: \"field type\"}\n" +
      "    }\n" +
      "  }");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(mockResponse);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(null);
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    dataMapping.apply();
    dataMapping.apply(new KuzzleOptions());
    dataMapping.apply(mock(KuzzleResponseListener.class));
    dataMapping.apply(new KuzzleOptions(), mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(k, times(4)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "updateMapping");
  }

}
