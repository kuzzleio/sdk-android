package io.kuzzle.test.core.Kuzzle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
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

public class setAutoRefreshTest {
  private KuzzleExtend kuzzle;
  private KuzzleResponseListener listener;


  @Before
  public void setup() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    options.setDefaultIndex("testIndex");

    kuzzle = new KuzzleExtend("localhost", options, null);
    kuzzle.setSocket(mock(Socket.class));

    listener = mock(KuzzleResponseListener.class);
  }

  @Test
  public void testSignatureVariants() {
    KuzzleOptions options = mock(KuzzleOptions.class);

    kuzzle = spy(kuzzle);

    kuzzle.setAutoRefresh(true);
    kuzzle.setAutoRefresh(true, options);
    kuzzle.setAutoRefresh(true, options, listener);
    kuzzle.setAutoRefresh("foo", true);
    kuzzle.setAutoRefresh("foo", true, options);
    kuzzle.setAutoRefresh("foo", true, listener);

    verify(kuzzle, times(6)).setAutoRefresh(any(String.class), any(Boolean.class), any(KuzzleOptions.class), any(KuzzleResponseListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testSetAutoRefreshException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONArray.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", true));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.setAutoRefresh(true, listener);
  }

  @Test(expected = RuntimeException.class)
  public void testSetAutoRefreshQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.setAutoRefresh(true, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetAutoRefreshIllegalDefaultIndex() {
    kuzzle = spy(kuzzle);
    kuzzle.setSuperDefaultIndex(null);
    kuzzle.setAutoRefresh(null, true, mock(KuzzleOptions.class), listener);
  }

  @Test
  public void testGetAutoRefresh() throws URISyntaxException, JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", true));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzle.setAutoRefresh(true);
    kuzzle.setAutoRefresh(true, listener);
    kuzzle.setAutoRefresh(true, mock(KuzzleOptions.class), listener);

    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    ArgumentCaptor request = ArgumentCaptor.forClass(JSONObject.class);
    verify(kuzzle, times(3)).query((Kuzzle.QueryArgs) argument.capture(), (JSONObject) request.capture(), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "index");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "setAutoRefresh");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).index, kuzzle.getDefaultIndex());
    assertEquals(((JSONObject) request.getValue()).getJSONObject("body").getBoolean("autoRefresh"), true);
  }

}
