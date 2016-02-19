package io.kuzzle.sdk;

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
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDataMapping;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleDocumentList;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.sdk.toolbox.KuzzleTestToolbox;
import io.socket.client.Socket;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleDataCollectionTest {

  private Kuzzle kuzzle;
  private KuzzleDataCollection collection;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", opts);
    KuzzleTestToolbox.forceConnectedState(kuzzle, KuzzleStates.CONNECTED);
    KuzzleTestToolbox.setSocket(kuzzle, mock(Socket.class));
    kuzzle = spy(kuzzle);
    when(kuzzle.getHeaders()).thenReturn(new JSONObject());

    collection = new KuzzleDataCollection(kuzzle, "index", "test");
    listener = mock(KuzzleResponseListener.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAdvancedSearchIllegalListener() {
    collection.advancedSearch(null, null, null);
  }

  @Test(expected = RuntimeException.class)
  public void testAdvancedSearchQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.advancedSearch(null, listener);
  }

  @Test(expected = RuntimeException.class)
  public void testAdvancedSearchException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("count", 42)));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(Integer.class));
    collection.advancedSearch(null, listener);
  }

  @Test
  public void testAdvancedSearch() throws JSONException {
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
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    collection.advancedSearch(filters, null, new KuzzleResponseListener<KuzzleDocumentList>() {
      @Override
      public void onSuccess(KuzzleDocumentList result) {
        assertEquals(result.getTotal(), 2);
        try {
          assertEquals(result.getDocuments().get(1).getContent("sibling"), "none");
        }
        catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(JSONObject error) {
      }
    });
    collection.advancedSearch(filters, mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "read");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "search");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCountIllegalListener() {
    collection.count(null);
  }

  @Test(expected = RuntimeException.class)
  public void testCountQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.count(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testCountException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("count", 42)));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(Integer.class));
    collection.count(listener);
  }

  @Test
  public void testCount() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("count", 42)));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    JSONObject filters = new JSONObject();
    collection.count(filters, listener);
    collection.count(filters, new KuzzleOptions(), listener);
    collection.count(listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(3)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "read");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "count");
  }

  @Test(expected = RuntimeException.class)
  public void testCreateQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.create(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", mock(JSONObject.class)));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    collection.create(listener);
  }

  @Test
  public void testCreate() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", mock(JSONObject.class)));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.create(new KuzzleOptions(), listener);
    collection.create(listener);
    collection.create(new KuzzleOptions());
    collection.create();
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(4)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "write");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createCollection");
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDocumentQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.createDocument(mock(KuzzleDocument.class), listener);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDocumentException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject()));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    collection.createDocument(mock(KuzzleDocument.class), listener);
  }

  @Test
  public void testCreateDocument() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject result = new JSONObject()
          .put("result", new JSONObject()
            .put("_id", "foo")
            .put("_version", 1337)
            .put("_source", new JSONObject()));

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(result);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    collection.createDocument(doc);
    collection.createDocument(doc, mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "write");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "create");
  }

  @Test
  public void testCreateDocumentWithOptions() throws JSONException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    KuzzleOptions options = new KuzzleOptions();
    options.setUpdateIfExists(true);
    collection.createDocument(doc, options);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "write");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplace");
  }

  @Test
  public void testDataMappingFactory() {
    assertThat(collection.dataMappingFactory(new JSONObject()), instanceOf(KuzzleDataMapping.class));
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.delete(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("_id", "id-42")));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(String.class));
    collection.delete(listener);
  }

  @Test
  public void testDelete() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("_id", "id-42")));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.delete();
    collection.delete(new KuzzleOptions());
    collection.delete(new KuzzleOptions(), listener);
    collection.delete(listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(4)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "deleteCollection");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeleteDocumentIllegalId() {
    collection.deleteDocument((String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeleteDocumentIllegalFilter() {
    collection.deleteDocument((JSONObject) null);
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteDocumentQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.deleteDocument("id", listener);
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteDocumentException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("_id", "id-42")));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(String.class));
    collection.deleteDocument("id", listener);
  }

  @Test
  public void testDeleteDocumentById() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("_id", "id-42")));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.deleteDocument("42");
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "write");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "delete");
  }

  @Test
  public void testDeleteDocumentByFilter() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("hits", new JSONArray().put("val"))));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.deleteDocument(new JSONObject(), listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "write");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "deleteByQuery");
  }

  @Test
  public void testDocumentFactory() throws JSONException {
    assertThat(collection.documentFactory(), instanceOf(KuzzleDocument.class));
    assertThat(collection.documentFactory("id"), instanceOf(KuzzleDocument.class));
    assertThat(collection.documentFactory("id", new JSONObject()), instanceOf(KuzzleDocument.class));
    assertThat(collection.documentFactory(new JSONObject()), instanceOf(KuzzleDocument.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFetchDocumentIllegalDocumentId() {
    collection.fetchDocument(null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFetchDocumentIllegalListener() {
    collection.fetchDocument("id", null);
  }

  @Test(expected = RuntimeException.class)
  public void testFetchDocumentQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.fetchDocument("id", listener);
  }

  @Test(expected = RuntimeException.class)
  public void testFetchDocumentException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject()));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(String.class));
    collection.fetchDocument("id", listener);
  }

  @Test
  public void testFetchDocument() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(
          new JSONObject()
            .put("result", new JSONObject()
                .put("_id", "foo")
                .put("_version", 42)
                .put("_source", new JSONObject())
            )
        );
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.fetchDocument("42", mock(KuzzleResponseListener.class));
    collection.fetchDocument("42", new KuzzleOptions(), mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "read");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "get");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFetchAllDocumentsIllegalListener() {
    collection.fetchAllDocuments(null);
  }

  @Test
  public void testFetchAllDocuments() throws JSONException {
    collection.fetchAllDocuments(new KuzzleOptions(), mock(KuzzleResponseListener.class));
    collection.fetchAllDocuments(mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "read");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "search");
  }

  @Test
  public void testGetMapping() throws JSONException {
    collection.getMapping(mock(KuzzleOptions.class), mock(KuzzleResponseListener.class));
    collection.getMapping(mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getMapping");
  }

  @Test(expected = RuntimeException.class)
  public void testPublishMessageException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.publishMessage(mock(KuzzleDocument.class), mock(KuzzleOptions.class));
  }

  @Test
  public void testPublishMessage() throws JSONException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    collection.publishMessage(doc);
    collection.publishMessage(doc, new KuzzleOptions());
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "write");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "publish");
  }

  @Test(expected = IllegalArgumentException.class)
  public void getMappingIllegalListener() {
    collection.getMapping(null);
  }

    @Test(expected = IllegalArgumentException.class)
  public void testReplaceDocumentIllegalDocumentId() {
    collection.replaceDocument(null, null);
  }

  @Test(expected = RuntimeException.class)
  public void testReplaceDocumentQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
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
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
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
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    collection.replaceDocument("42", doc.serialize(), listener);
    collection.replaceDocument("42", mock(JSONObject.class), mock(KuzzleOptions.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "write");
   }

  @Test
  public void testRoomFactory() {
    assertThat(collection.roomFactory(mock(KuzzleRoomOptions.class)), instanceOf(KuzzleRoom.class));
    assertThat(collection.roomFactory(), instanceOf(KuzzleRoom.class));
  }

  @Test
  public void testSetHeaders() throws JSONException {
    when(kuzzle.getHeaders()).thenCallRealMethod();
    when(kuzzle.setHeaders(any(JSONObject.class), anyBoolean())).thenCallRealMethod();
    JSONObject content = new JSONObject();
    content.put("foo", "bar");
    collection.setHeaders(content);
    assertEquals(collection.getHeaders().getString("foo"), "bar");
    content.put("foo", "baz");
    collection.setHeaders(content, true);
    assertEquals(collection.getHeaders().getString("foo"), "baz");
    content.put("bar", "foo");
    collection.setHeaders(content, false);
    assertEquals(content.getString("foo"), "baz");
    assertEquals(content.getString("bar"), "foo");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSubscribeIllegalListener() {
    collection.subscribe(mock(JSONObject.class), null);
  }

  @Test
  public void testSubscribe() throws JSONException {
    collection.subscribe(mock(JSONObject.class), new KuzzleRoomOptions(), listener);
    collection.subscribe(mock(JSONObject.class), listener);
    collection.subscribe(new KuzzleRoomOptions(), listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(3)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "subscribe");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "on");
  }

  @Test(expected = RuntimeException.class)
  public void testTruncateQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.truncate(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testTruncateException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject()));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(String.class));
    collection.truncate(listener);
  }

  @Test
  public void testTruncate() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject()));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.truncate();
    collection.truncate(new KuzzleOptions());
    collection.truncate(mock(KuzzleResponseListener.class));
    collection.truncate(new KuzzleOptions(), mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(4)).query((Kuzzle.QueryArgs)argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "truncateCollection");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUpdateDocumentIllegalDocumentId() {
    collection.updateDocument(null, mock(JSONObject.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUpdateDocumentIllegalContent() {
    collection.updateDocument("id", null);
  }

  @Test(expected = RuntimeException.class)
  public void testupdateDocumentQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    collection.updateDocument("id", mock(JSONObject.class), listener);
  }

  @Test(expected = RuntimeException.class)
  public void testupdateDocumentException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject()));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(String.class));
    collection.updateDocument("id", mock(JSONObject.class), listener);
  }

  @Test
  public void testUpdateDocument() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject()
          .put("result", new JSONObject()
              .put("_id", "42")
              .put("_version", 1337)
              .put("_source", new JSONObject())
          );
        if (invocation.getArguments()[3] != null) {
          ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
          ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject());
        }
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    KuzzleDocument doc = new KuzzleDocument(collection);
    collection.updateDocument("42", doc.serialize());
    collection.updateDocument("42", doc.serialize(), new KuzzleOptions());
    collection.updateDocument("42", doc.serialize(), new KuzzleResponseListener<KuzzleDocument>() {
      @Override
      public void onSuccess(KuzzleDocument document) {
        assertEquals(document.getId(), "42");
        assertEquals(document.getVersion(), 1337);
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.updateDocument("42", doc.serialize(), new KuzzleOptions(), new KuzzleResponseListener<KuzzleDocument>() {
      @Override
      public void onSuccess(KuzzleDocument document) {
        assertEquals(document.getId(), "42");
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(4)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "write");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "update");
  }

}
