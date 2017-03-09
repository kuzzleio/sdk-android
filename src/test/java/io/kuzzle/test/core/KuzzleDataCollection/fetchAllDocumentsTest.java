package io.kuzzle.test.core.KuzzleDataCollection;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Document;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.SearchResult;
import io.kuzzle.sdk.state.States;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
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
    verify(collection).fetchAllDocuments(any(Options.class), eq(listener));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFetchAllDocumentsIllegalListener() {
    collection.fetchAllDocuments(null);
  }

  @Test
  public void testFetchAllDocuments() throws JSONException {
    ResponseListener<ArrayList<Document>> spyListener = new ResponseListener<ArrayList<Document>>() {
      @Override
      public void onSuccess(ArrayList<Document> response) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };

    spyListener = spy(spyListener);

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
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));

    collection.fetchAllDocuments(new Options(), spyListener);

    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "document");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "search");
    ArgumentCaptor listenerArgument = ArgumentCaptor.forClass(ArrayList.class);
    verify(spyListener).onSuccess((ArrayList<Document>) listenerArgument.capture());
    assertEquals(((ArrayList<Document>) listenerArgument.getValue()).size(), 2);
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
