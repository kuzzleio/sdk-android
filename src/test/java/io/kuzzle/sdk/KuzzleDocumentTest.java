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
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.listeners.ResponseListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleDocumentTest {

  private Kuzzle k;
  private KuzzleDocument doc;

  @Before
  public void setUp() throws JSONException, URISyntaxException {
    k = mock(Kuzzle.class);
    when(k.getHeaders()).thenReturn(new JSONObject());
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "test"));
  }

  @Test
  public void testConstructor() throws JSONException {
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "test"), "42");
    assertEquals(doc.getId(), "42");
  }

  @Test
  public void testDocumentWithContent() throws JSONException {
    JSONObject content = new JSONObject();
    JSONObject body = new JSONObject();
    content.put("foo", "bar");
    body.put("body", content);
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "test"), body);
    assertEquals(doc.getContent().getString("foo"), "bar");
  }

  @Test
  public void testSave() throws KuzzleException, JSONException, IOException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("_id", "id-42");
        response.put("_version", "42");
        response.put("result", response);
        ((ResponseListener) invocation.getArguments()[5]).onSuccess(response);
        ((ResponseListener) invocation.getArguments()[5]).onError(null);
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
    doc.save();
    doc.save(new KuzzleOptions());
    doc.save(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) throws Exception {
        assertEquals(object.getString("_id"), "id-42");
      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    });
    verify(k, times(3)).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testDelete() throws URISyntaxException, IOException, JSONException, KuzzleException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("result", "foo");
        ((ResponseListener) invocation.getArguments()[4]).onSuccess(response);
        ((ResponseListener) invocation.getArguments()[4]).onError(null);
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), eq("delete"), any(JSONObject.class), any(ResponseListener.class));
    doc.setId(null);
    doc.delete();
    assertNull(doc.getId());
    doc.save();
    doc.setId("id-42");
    doc.delete();
    assertNull(doc.getId());
    verify(k, times(1)).query(eq("test"), eq("write"), eq("delete"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test(expected = KuzzleException.class)
  public void testRefreshWithoutId() throws KuzzleException, IOException, JSONException {
    doc.refresh(null);
  }

  @Test
  public void testRefresh() throws IOException, JSONException, KuzzleException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        JSONObject source = new JSONObject();
        source.put("foo", "bar");
        response.put("_source", source);
        response.put("_version", "42");
        ((ResponseListener) invocation.getArguments()[4]).onSuccess(response);
        ((ResponseListener) invocation.getArguments()[4]).onError(null);
        return null;
      }
    }).when(k).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class), any(ResponseListener.class));
    doc.setId("42");
    doc.setContent("foo", "baz");
    doc.refresh(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) throws Exception {
        assertEquals(doc.getVersion(), "42");
        assertEquals(doc.getContent().getString("foo"), "bar");
      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    });
    doc.refresh();
    verify(k, times(2)).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testPublish() throws IOException, JSONException, KuzzleException {
    doc.publish();
    verify(k, times(1)).query(eq("test"), eq("write"), eq("create"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testSetContent() throws JSONException {
    assertEquals(doc.getContent().toString(), new JSONObject().toString());
    JSONObject data = new JSONObject();
    data.put("test", "some content");
    doc.setContent(data, false);
    assertEquals(doc.getContent().get("test"), "some content");
    data = new JSONObject();
    data.put("test 2", "some other content");
    doc.setContent(data, true);
    assertEquals(doc.getContent().get("test 2"), "some other content");
    assertTrue(doc.getContent().isNull("test"));
  }

  @Test(expected = KuzzleException.class)
  public void testSubscribeNullId() throws KuzzleException, JSONException, IOException {
    doc.subscribe();
  }

  @Test
  public void testSubscribe() throws KuzzleException, IOException, JSONException {
    doc.setId("42");
    doc.subscribe();
    doc.subscribe(new KuzzleRoomOptions());
    doc.subscribe(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) throws Exception {

      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    });
    doc.subscribe(new KuzzleRoomOptions(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) throws Exception {

      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    });
    verify(k, times(4)).query(eq("test"), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testGetContent() throws JSONException, IOException, KuzzleException {
    doc.setContent(null);
    assertNotNull(doc.getContent());
    doc.setContent("foo", "bar");
    assertEquals(doc.getContent().getString("foo"), "bar");
  }

  @Test
  public void testSetHeaders() throws JSONException {
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    doc.setHeaders(headers, true);
    assertEquals(doc.getHeaders().getString("foo"), "bar");
    headers.put("oof", "baz");
    doc.setHeaders(headers);
    assertEquals(doc.getHeaders().getString("foo"), "bar");
    assertEquals(doc.getHeaders().getString("oof"), "baz");
  }

  @Test
  public void testGetHeaders() throws JSONException, IOException, KuzzleException {
    doc.setHeaders(null);
    assertNotNull(doc.getHeaders());
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    doc.setHeaders(headers);
    assertEquals(doc.getHeaders().getString("foo"), "bar");
  }

  @Test
  public void testGetVersion() throws JSONException, IOException, KuzzleException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        JSONObject source = new JSONObject();
        source.put("foo", "bar");
        response.put("_source", source);
        response.put("_version", "42");
        response.put("_id", "id");
        ((ResponseListener) invocation.getArguments()[5]).onSuccess(response);
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
    assertNull(doc.getVersion());
    doc.save(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) throws Exception {
        assertEquals(doc.getVersion(), "42");
      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    });
  }

}
