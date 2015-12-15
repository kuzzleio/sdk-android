package io.kuzzle.sdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDataMapping;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.listeners.ResponseListener;

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
  public void testAdvancedSearch() throws URISyntaxException, IOException, JSONException, KuzzleException {
    JSONObject filters = new JSONObject();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject("{\"took\":1,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":4,\"max_score\":1,\"hits\":[{\"_index\":\"mainindex\",\"_type\":\"collection_test\",\"_id\":\"AVE_WabrHN-wg-mnha2s\",\"_score\":1,\"_source\":{\"foo\":\"bar\"}},{\"_index\":\"mainindex\",\"_type\":\"collection_test\",\"_id\":\"AVE_U7iFHN-wg-mnha2o\",\"_score\":1,\"_source\":{\"foo\":\"bar\"}},{\"_index\":\"mainindex\",\"_type\":\"collection_test\",\"_id\":\"AVE_Vqd9HN-wg-mnha2r\",\"_score\":1,\"_source\":{\"foo\":\"bar\"}}]},\"requestId\":\"b3cea072-e3a2-495e-9e79-bc97267d9b9f\",\"controller\":\"read\",\"action\":\"search\",\"collection\":\"collection_test\",\"metadata\":{},\"_source\":{}}");
        ((ResponseListener) invocation.getArguments()[5]).onSuccess(response);
        ((ResponseListener) invocation.getArguments()[5]).onError(new JSONObject());
        return null;
      }
    }).when(k).query(eq("test"), eq("read"), eq("search"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));

    collection.advancedSearch(filters, null, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          assertEquals(object.getInt("total"), 3);
          assertEquals(object.getJSONArray("documents").getJSONObject(1).getJSONObject("body").getString("foo"), "bar");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {
      }
    });
    collection.advancedSearch(filters);
    collection.advancedSearch(filters, new KuzzleOptions());
    collection.advancedSearch(filters, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(4)).query(eq("test"), eq("read"), eq("search"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testCount() throws IOException, JSONException, KuzzleException {
    JSONObject filters = new JSONObject();
    collection.count(filters);
    collection.count(filters, new KuzzleOptions());
    collection.count(filters, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.count(filters, new KuzzleOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(4)).query(eq("test"), eq("read"), eq("count"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testCreate() throws KuzzleException, IOException, JSONException {
    collection.create(null, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(1)).query(eq("test"), eq("write"), eq("createCollection"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));;
  }

  @Test
  public void testCreateDocument() throws JSONException, IOException, KuzzleException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    collection.createDocument(doc);
    collection.createDocument(doc, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(2)).query(eq("test"), eq("write"), eq("create"), eq(doc), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testCreateDocumentWithOptions() throws JSONException, IOException, KuzzleException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    KuzzleOptions options = new KuzzleOptions();
    options.setUpdateIfExists(true);
    collection.createDocument(doc, options);

    verify(k, times(1)).query(eq("test"), eq("write"), eq("createOrUpdate"), eq(doc), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testDelete() throws KuzzleException, IOException, JSONException {
    collection.delete();
    collection.delete(new KuzzleOptions());
    collection.delete(new KuzzleOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {


      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.delete(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(4)).query(eq("test"), eq("admin"), eq("deleteCollection"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testDeleteDocument() throws IOException, JSONException, KuzzleException {
    collection.deleteDocument("42");
    collection.deleteDocument(new JSONObject());
    verify(k, times(2)).query(eq("test"), eq("write"), eq("delete"), any(JSONObject.class));
  }

  @Test
  public void testFetchDocument() throws IOException, JSONException, KuzzleException {
    collection.fetchDocument("42", new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.fetchDocument("42", new KuzzleOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(2)).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testFetchAllDocuments() throws KuzzleException, IOException, JSONException {
    collection.fetchAllDocuments(new KuzzleOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.fetchAllDocuments(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(2)).query(eq("test"), eq("read"), eq("search"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testGetMapping() throws KuzzleException, IOException, JSONException {
    collection.getMapping();
    collection.getMapping(new KuzzleOptions());
    collection.getMapping(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.getMapping(new KuzzleOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(4)).query(eq("test"), eq("admin"), eq("getMapping"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testPublishMessage() throws JSONException, IOException, KuzzleException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    collection.publishMessage(doc);
    collection.publishMessage(doc, new KuzzleOptions());
    verify(k, times(2)).query(eq("test"), eq("write"), eq("publish"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void putMapping() throws KuzzleException, IOException, JSONException {
    KuzzleDataMapping mapping = new KuzzleDataMapping(collection);
    collection.putMapping(mapping);
    collection.putMapping(mapping, new KuzzleOptions());
    collection.putMapping(mapping, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.putMapping(mapping, new KuzzleOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(4)).query(eq("test"), eq("admin"), eq("putMapping"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testReplaceDocument() throws IOException, JSONException, KuzzleException {
    KuzzleDocument doc = new KuzzleDocument(collection);
    doc.setContent("foo", "bar");
    collection.replaceDocument("42", doc);
    verify(k, times(1)).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class));
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
  public void testSubscribe() throws JSONException, KuzzleException, IOException {
    collection.subscribe(new JSONObject(), new KuzzleRoomOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.subscribe(new JSONObject(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.subscribe(new JSONObject());
    collection.subscribe(new JSONObject(), new KuzzleRoomOptions());
    collection.subscribe(new KuzzleRoomOptions());
    collection.subscribe();
    verify(k, times(6)).query(eq("test"), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testTruncate() throws KuzzleException, IOException, JSONException {
    collection.truncate();
    collection.truncate(new KuzzleOptions());
    collection.truncate(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.truncate(new KuzzleOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(4)).query(eq("test"), eq("admin"), eq("truncateCollection"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testUpdateDocument() throws IOException, JSONException, KuzzleException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("_id", "42");
        if (invocation.getArguments()[5] != null) {
          ((ResponseListener) invocation.getArguments()[5]).onSuccess(response);
          ((ResponseListener) invocation.getArguments()[5]).onError(new JSONObject());
        }
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), eq("update"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));

    KuzzleDocument doc = new KuzzleDocument(collection);
    collection.updateDocument("42", doc);
    collection.updateDocument("42", doc, new KuzzleOptions());
    collection.updateDocument("42", doc, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          assertEquals(object.getString("_id"), "42");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    collection.updateDocument("42", doc, new KuzzleOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          assertEquals(object.getString("_id"), "42");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(4)).query(eq("test"), eq("write"), eq("update"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

}
