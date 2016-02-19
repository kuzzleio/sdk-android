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
import java.util.Iterator;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.kuzzle.test.testUtils.QueryArgsHelper;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class getAllStatisticsTest {
  private KuzzleExtend kuzzle;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    options.setDefaultIndex("testIndex");

    kuzzle = new KuzzleExtend("http://localhost:7512", options, null);
    kuzzle.setSocket(mock(Socket.class));

    listener = new KuzzleResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetAllStatisticsNoListener() {
    kuzzle.getAllStatistics(null);
  }

  @Test(expected = RuntimeException.class)
  public void testGetAllStatisticsException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("hits", mock(JSONObject.class))));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetAllStatisticsQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(listener);
  }

  @Test
  public void testGetAllStatsSuccess() throws JSONException {
    kuzzle = spy(kuzzle);
    final JSONObject response = new JSONObject("{\n" +
      "    \"total\": 25,\n" +
      "    \"hits\": [\n" +
      "      {\n" +
      "        \"completedRequests\": {\n" +
      "          \"websocket\": 148,\n" +
      "          \"rest\": 24,\n" +
      "          \"mq\": 78\n" +
      "        },\n" +
      "        \"failedRequests\": {\n" +
      "          \"websocket\": 3\n" +
      "        },\n" +
      "        \"ongoingRequests\": {\n" +
      "          \"mq\": 8,\n" +
      "          \"rest\": 2\n" +
      "        },\n" +
      "        \"connections\": {\n" +
      "          \"websocket\": 13\n" +
      "        },\n" +
      "        \"timestamp\": \"2016-01-13T13:46:19.917Z\"\n" +
      "      }\n" +
      "    ]\n" +
      "  }\n");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", response));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(new KuzzleResponseListener<JSONArray>() {
      @Override
      public void onSuccess(JSONArray result) {
        try {
          for (int i = 0; i < result.length(); i++) {
            for (Iterator ite = result.getJSONObject(i).keys(); ite.hasNext(); ) {
              String key = (String) ite.next();
              assertEquals(result.getJSONObject(i).get(key), response.getJSONArray("hits").getJSONObject(i).get(key));
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "getAllStats");
  }

  @Test
  public void testGetAllStatsError() throws JSONException {
    kuzzle = spy(kuzzle);

    final JSONObject responseError = new JSONObject();
    responseError.put("error", "rorre");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(responseError);
        return null;
      }
    }).when(kuzzle).query(eq(QueryArgsHelper.makeQueryArgs("admin", "getAllStats")), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(new KuzzleResponseListener<JSONArray>() {
      @Override
      public void onSuccess(JSONArray object) {
      }

      @Override
      public void onError(JSONObject error) {
        try {
          assertEquals(error.get("error"), "rorre");
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }
    });
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "getAllStats");
  }
}
