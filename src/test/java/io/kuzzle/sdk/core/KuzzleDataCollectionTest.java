package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.util.Options;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class KuzzleDataCollectionTest {

  @Test
  public void testAdvancedSearch() throws URISyntaxException, IOException, JSONException {
    Kuzzle k = mock(Kuzzle.class);

    KuzzleDataCollection collection = new KuzzleDataCollection(k, "test");
    JSONObject filters = new JSONObject();
    collection.advancedSearch(filters, null);
    verify(k, times(1)).query(eq("test"), eq("read"), eq("search"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testCount() throws IOException, JSONException {
    Kuzzle k = mock(Kuzzle.class);

    KuzzleDataCollection collection = new KuzzleDataCollection(k, "test");
    JSONObject filters = new JSONObject();
    collection.count(filters, null);
    verify(k, times(1)).query(eq("test"), eq("read"), eq("count"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testCreate() throws IOException, JSONException {
    Kuzzle k = mock(Kuzzle.class);

    KuzzleDataCollection collection = new KuzzleDataCollection(k, "test");
    JSONObject filters = new JSONObject();
    Options options = new Options();

    collection.createDocument(filters);
    collection.createDocument(new JSONObject());
    options.setPersist(true);
    options.setUpdateIfExist(true);
    collection.createDocument(filters);
    verify(k, times(3)).query(eq("test"), eq("write"), eq("create"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testDelete() throws IOException, JSONException {
    Kuzzle k = mock(Kuzzle.class);

    KuzzleDataCollection collection = new KuzzleDataCollection(k, "test");
    collection.delete("42");
    collection.delete(new JSONObject());
    verify(k, times(2)).query(eq("test"), eq("write"), eq("delete"), any(JSONObject.class));
  }

  @Test
  public void testFetch() throws IOException, JSONException {
    Kuzzle k = mock(Kuzzle.class);

    KuzzleDataCollection collection = new KuzzleDataCollection(k, "test");
    collection.fetch("42", null);
    verify(k, times(1)).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testReplace() throws IOException, JSONException {
    Kuzzle k = mock(Kuzzle.class);

    KuzzleDataCollection collection = new KuzzleDataCollection(k, "test");
    collection.replace("42", new JSONObject());
    verify(k, times(1)).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class));
  }

  @Test
  public void testSubscribe() throws JSONException, KuzzleException, IOException {
    Kuzzle k = mock(Kuzzle.class);

    KuzzleDataCollection collection = new KuzzleDataCollection(k, "test");
    collection.subscribe(new JSONObject(), null, null);
    collection.subscribe(new JSONObject(), null);
    verify(k, times(2)).query(eq("test"), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testUpdate() throws IOException, JSONException {
    Kuzzle k = mock(Kuzzle.class);

    KuzzleDataCollection collection = new KuzzleDataCollection(k, "test");
    collection.updateDocument("42", new JSONObject());
    verify(k, times(1)).query(eq("test"), eq("write"), eq("update"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

}
