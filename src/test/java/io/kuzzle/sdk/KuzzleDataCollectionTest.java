package io.kuzzle.sdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDataMapping;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.listeners.KuzzResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzSearchResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleDataCollectionTest {

  private Kuzzle  k;
  private KuzzleDataCollection collection;

  @Before
  public void setUp() {
    k = mock(Kuzzle.class);
    collection = new KuzzleDataCollection(k, "test");
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

        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(new JSONObject());
        return null;
      }
    }).when(k).query(eq("test"), eq("read"), eq("search"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    collection.advancedSearch(filters, null, new KuzzResponseListener<KuzzSearchResponse>() {
      @Override
      public void onSuccess(KuzzSearchResponse result) {
        assertEquals(result.getTotal(), 2);
        assertEquals(result.getDocuments().get(1).getContent("sibling"), "none");
      }

      @Override
      public void onError(JSONObject error) {
      }
    });
    collection.advancedSearch(filters, mock(KuzzResponseListener.class));
    verify(k, times(2)).query(eq("test"), eq("read"), eq("search"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testCount() throws JSONException {
    JSONObject filters = new JSONObject();
    collection.count(filters, mock(KuzzResponseListener.class));
    collection.count(filters, new KuzzleOptions(), mock(KuzzResponseListener.class));
    verify(k, times(2)).query(eq("test"), eq("read"), eq("count"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testCreate() throws JSONException {
    KuzzResponseListener listener = mock(KuzzResponseListener.class);
    collection.create(new KuzzleOptions(), listener);
    collection.create(listener);
    collection.create(new KuzzleOptions());
    collection.create();
    verify(k, times(4)).query(eq("test"), eq("write"), eq("createCollection"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testCreateDocument() throws JSONException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    collection.createDocument(doc);
    collection.createDocument(doc, mock(KuzzResponseListener.class));
    verify(k, times(2)).query(eq("test"), eq("write"), eq("create"), eq(doc), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testCreateDocumentWithOptions() throws JSONException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    KuzzleOptions options = new KuzzleOptions();
    options.setUpdateIfExists(true);
    collection.createDocument(doc, options);
    verify(k, times(1)).query(eq("test"), eq("write"), eq("createOrUpdate"), eq(doc), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testDelete() throws JSONException {
    collection.delete();
    collection.delete(new KuzzleOptions());
    collection.delete(new KuzzleOptions(), mock(KuzzResponseListener.class));
    collection.delete(mock(KuzzResponseListener.class));
    verify(k, times(4)).query(eq("test"), eq("admin"), eq("deleteCollection"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testDeleteDocument() throws JSONException {
    collection.deleteDocument("42");
    collection.deleteDocument(new JSONObject());
    verify(k, times(1)).query(eq("test"), eq("write"), eq("delete"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    verify(k, times(1)).query(eq("test"), eq("write"), eq("deleteByQuery"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testFetchDocument() throws JSONException {
    collection.fetchDocument("42", mock(KuzzResponseListener.class));
    collection.fetchDocument("42", new KuzzleOptions(), mock(KuzzResponseListener.class));
    verify(k, times(2)).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testFetchAllDocuments() throws JSONException {
    collection.fetchAllDocuments(new KuzzleOptions(), mock(KuzzResponseListener.class));
    collection.fetchAllDocuments(mock(KuzzResponseListener.class));
    verify(k, times(2)).query(eq("test"), eq("read"), eq("search"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testGetMapping() throws JSONException {
    collection.getMapping(mock(KuzzleOptions.class), mock(KuzzResponseListener.class));
    collection.getMapping(mock(KuzzResponseListener.class));
    verify(k, times(2)).query(eq("test"), eq("admin"), eq("getMapping"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testPublishMessage() throws JSONException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    collection.publishMessage(doc);
    collection.publishMessage(doc, new KuzzleOptions());
    verify(k, times(2)).query(eq("test"), eq("write"), eq("publish"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void putMapping() throws JSONException {
    KuzzleDataMapping mapping = new KuzzleDataMapping(collection);
    collection.putMapping(mapping);
    collection.putMapping(mapping, new KuzzleOptions());
    collection.putMapping(mapping, mock(KuzzResponseListener.class));
    collection.putMapping(mapping, new KuzzleOptions(), mock(KuzzResponseListener.class));
    verify(k, times(4)).query(eq("test"), eq("admin"), eq("putMapping"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testReplaceDocument() throws JSONException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    collection.replaceDocument("42", doc);
    verify(k, times(1)).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testSetHeaders() throws JSONException {
    when(k.getHeaders()).thenCallRealMethod();
    when(k.setHeaders(any(JSONObject.class), anyBoolean())).thenCallRealMethod();
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

  @Test
  public void testSubscribe() throws JSONException {
    collection.subscribe(new JSONObject(), new KuzzleRoomOptions(), mock(KuzzResponseListener.class));
    collection.subscribe(new JSONObject(), mock(KuzzResponseListener.class));
    verify(k, times(2)).query(eq("test"), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testTruncate() throws JSONException {
    collection.truncate();
    collection.truncate(new KuzzleOptions());
    collection.truncate(mock(KuzzResponseListener.class));
    collection.truncate(new KuzzleOptions(), mock(KuzzResponseListener.class));
    verify(k, times(4)).query(eq("test"), eq("admin"), eq("truncateCollection"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testUpdateDocument() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("_id", "42");
        response.put("result", new JSONObject());
        if (invocation.getArguments()[5] != null) {
          ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(response);
          ((OnQueryDoneListener) invocation.getArguments()[5]).onError(new JSONObject());
        }
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), eq("update"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    KuzzleDocument doc = new KuzzleDocument(collection);
    collection.updateDocument("42", doc);
    collection.updateDocument("42", doc, new KuzzleOptions());
    collection.updateDocument("42", doc, new KuzzResponseListener<KuzzleDocument>() {
      @Override
      public void onSuccess(KuzzleDocument document) {
        assertEquals(document.getId(), "42");
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.updateDocument("42", doc, new KuzzleOptions(), new KuzzResponseListener<KuzzleDocument>() {
      @Override
      public void onSuccess(KuzzleDocument document) {
        assertEquals(document.getId(), "42");
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(4)).query(eq("test"), eq("write"), eq("update"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

}
