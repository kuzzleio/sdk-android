package io.kuzzle.test.core.KuzzleDataCollection;

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
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleDocumentList;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;
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
import static org.mockito.Mockito.when;

public class searchTest {
  private Kuzzle kuzzle;
  private KuzzleDataCollection collection;
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

    collection = new KuzzleDataCollection(kuzzle, "test", "index");
    listener = mock(KuzzleResponseListener.class);
  }

  @Test
  public void checkSignaturesVariants() {
    collection = spy(collection);
    collection.search(new JSONObject(), listener);
    verify(collection).search(any(JSONObject.class), eq((KuzzleOptions)null), eq(listener));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchIllegalListener() {
    collection.search(null, null, null);
  }

  @Test(expected = RuntimeException.class)
  public void testSearchQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.search(null, listener);
  }

  @Test(expected = RuntimeException.class)
  public void testSearchException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("count", 42)));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(Integer.class));
    collection.search(null, listener);
  }

  @Test
  public void testSearch() throws JSONException {
    JSONObject filters = new JSONObject();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject("{\"result\": {\n" +
          "    \"_shards\": {\n" +
          "      \"failed\": 0,\n" +
          "      \"successful\": 5,\n" +
          "      \"total\": 5\n" +
          "    },\n" +
          "    \"hits\": [\n" +
          "      {\n" +
          "        \"_id\": \"AVJAwyDMZAGQHg9Dhfw2\",\n" +
          "        \"_index\": \"cabble\",\n" +
          "        \"_score\": 1,\n" +
          "        \"_source\": {\n" +
          "          \"pos\": {\n" +
          "            \"lat\": 43.6073821,\n" +
          "            \"lon\": 3.9130721\n" +
          "          },\n" +
          "          \"sibling\": \"none\",\n" +
          "          \"status\": \"idle\",\n" +
          "          \"type\": \"customer\"\n" +
          "        },\n" +
          "        \"_type\": \"users\"\n" +
          "      },\n" +
          "      {\n" +
          "        \"_id\": \"AVJAwyOvZAGQHg9Dhfw3\",\n" +
          "        \"_index\": \"cabble\",\n" +
          "        \"_score\": 1,\n" +
          "        \"_source\": {\n" +
          "          \"pos\": {\n" +
          "            \"lat\": 43.6073683,\n" +
          "            \"lon\": 3.8999983\n" +
          "          },\n" +
          "          \"sibling\": \"none\",\n" +
          "          \"status\": \"idle\",\n" +
          "          \"type\": \"cab\"\n" +
          "        },\n" +
          "        \"_type\": \"users\"\n" +
          "      }\n" +
          "    ],\n" +
          "    \"max_score\": 1,\n" +
          "    \"timed_out\": false,\n" +
          "    \"took\": 307,\n" +
          "    \"total\": 2\n" +
          "  }" +
          "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject());
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    collection.search(filters, null, new KuzzleResponseListener<KuzzleDocumentList>() {
      @Override
      public void onSuccess(KuzzleDocumentList result) {
        assertEquals(result.getTotal(), 2);
        try {
          assertEquals(result.getDocuments().get(1).getContent("sibling"), "none");
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(JSONObject error) {
      }
    });
    collection.search(filters, mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "read");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "search");
  }
}
