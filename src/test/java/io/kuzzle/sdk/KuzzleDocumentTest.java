package io.kuzzle.sdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.listeners.KuzzResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleDocumentTest {

  private Kuzzle k;
  private KuzzleDocument doc;
  private KuzzleDataCollection  mockCollection;

  @Before
  public void setUp() throws URISyntaxException {
    k = mock(Kuzzle.class);
    when(k.getHeaders()).thenReturn(new JSONObject());
    mockCollection = mock(KuzzleDataCollection.class);
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "test"));
  }

  @Test
  public void testConstructor() {
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "test"), "42");
    assertEquals(doc.getId(), "42");
  }

  @Test
  public void testCollection() {
    Kuzzle k = mock(Kuzzle.class);
    KuzzleDataCollection collection = new KuzzleDataCollection(k, "test");
    KuzzleDocument doc = new KuzzleDocument(collection);
    assertEquals(doc.getCollection(), collection.getCollection());
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

  @Test(expected = RuntimeException.class)
  public void testSaveHeadersException() throws JSONException {
    doc.put("headers", "A string");
    doc.save();
  }

  @Test(expected = RuntimeException.class)
  public void testSaveQueryException() throws JSONException {
    doc.setId("42");
    doThrow(JSONException.class).when(k).query(any(String.class), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.save();
  }

  @Test(expected = RuntimeException.class)
  public void testSaveException() throws JSONException {
    doc.setId("42");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", new JSONObject().put("_id", "42").put("_version", "42")));
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(k).query(any(String.class), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    KuzzResponseListener mockListener = mock(KuzzResponseListener.class);
    doThrow(JSONException.class).when(mockListener).onSuccess(any(KuzzleDocument.class));
    doc.save(mockListener);
  }

  @Test
  public void testSave() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("_id", "id-42");
        response.put("_version", "42");
        response.put("result", response);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(null);
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.save();
    doc.save(new KuzzleOptions());
    doc.save(new KuzzResponseListener<KuzzleDocument>() {
      @Override
      public void onSuccess(KuzzleDocument object) {
        try {
          assertEquals(object.getString("_id"), "id-42");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(k, times(3)).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteException() throws JSONException {
    doc.setId("42");
    doThrow(JSONException.class).when(k).query(any(String.class), eq("write"), eq("delete"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.delete();
  }

  @Test
  public void testDelete() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("result", "foo");
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(null);
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), eq("delete"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.setId(null);
    doc.delete(mock(KuzzResponseListener.class));
    assertNull(doc.getId());
    doc.setId("id-42");
    doc.delete(mock(KuzzResponseListener.class));
    doc.setId("id-42");
    doc.delete();
    doc.setId("id-42");
    doc.delete(mock(KuzzleOptions.class));
    assertNull(doc.getId());
    verify(k, times(3)).query(eq("test"), eq("write"), eq("delete"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testRefreshQueryException() throws JSONException {
    doc.setId("42");
    doThrow(JSONException.class).when(k).query(any(String.class), eq("read"), eq("get"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.refresh();
  }

  @Test(expected = RuntimeException.class)
  public void testRefreshException() throws JSONException {
    doc.setId("42");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(mock(JSONObject.class));
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(k).query(any(String.class), eq("read"), eq("get"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    KuzzResponseListener mockListener = mock(KuzzResponseListener.class);
    doThrow(JSONException.class).when(mockListener).onSuccess(any(KuzzleDocument.class));
    doc.refresh(mockListener);
  }

  @Test(expected = RuntimeException.class)
  public void testRefreshWithoutId() {
    doc.refresh(null, null);
  }

  @Test
  public void testRefresh() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        JSONObject source = new JSONObject();
        source.put("foo", "bar");
        response.put("_source", source);
        response.put("_version", "42");
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(null);
        return null;
      }
    }).when(k).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.setId("42");
    doc.setContent("foo", "baz");
    doc.refresh(new KuzzResponseListener<KuzzleDocument>() {
      @Override
      public void onSuccess(KuzzleDocument object) {
        try {
          assertEquals(doc.getVersion(), "42");
          assertEquals(doc.getContent().getString("foo"), "bar");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    doc.refresh();
    doc.refresh(mock(KuzzleOptions.class));
    verify(k, times(3)).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testPublishException() throws JSONException {
    doThrow(JSONException.class).when(k).query(any(String.class), eq("write"), eq("publish"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doc.publish();
  }

  @Test
  public void testPublish() throws JSONException {
    doc.publish();
    verify(k, times(1)).query(eq("test"), eq("write"), eq("publish"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testSetContentPutException() throws JSONException {
    doc = spy(doc);
    doThrow(JSONException.class).when(doc).put(any(String.class), any(JSONObject.class));
    doc.setContent(mock(JSONObject.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetContentException() {
    doc.setContent(null, mock(Object.class));
  }

  @Test(expected = RuntimeException.class)
  public void testSetContentJSONException() {
    doc.remove("body");
    doc.setContent("test", "test");
  }

  @Test(expected = RuntimeException.class)
  public void testSetContentWithReplaceException() throws JSONException {
    doc = spy(doc);
    doThrow(JSONException.class).when(doc).put(any(String.class), any(JSONObject.class));
    doc.setContent(mock(JSONObject.class), true);
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

  @Test(expected = RuntimeException.class)
  public void testSubscribeException() throws JSONException {
    doc = new KuzzleDocument(mockCollection);
    doc.setId("42");
    doThrow(JSONException.class).when(mockCollection).subscribe(any(JSONObject.class), any(KuzzleRoomOptions.class), any(KuzzResponseListener.class));
    doc.subscribe(mock(KuzzResponseListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testSubscribeNullId() {
    doc.subscribe(mock(KuzzResponseListener.class));
  }

  @Test
  public void testSubscribe() throws JSONException {
    doc.setId("42");
    doc.subscribe(mock(KuzzResponseListener.class));
    doc.subscribe(new KuzzleRoomOptions(), mock(KuzzResponseListener.class));
    verify(k, times(2)).query(eq("test"), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testGetContentException() {
    doc.remove("body");
    doc.getContent("illegal body");
  }

  @Test(expected = RuntimeException.class)
  public void testGetContentIsNullException() throws JSONException {
    doc = spy(doc);
    doThrow(JSONException.class).when(doc).isNull(any(String.class));
    doc.getContent();
  }

  @Test
  public void testGetContent() throws JSONException {
    doc.setContent(null);
    assertNotNull(doc.getContent());
    doc.setContent("foo", "bar");
    assertEquals(doc.getContent().getString("foo"), "bar");
    assertNull(doc.getContent("!exist"));
  }

  @Test(expected = RuntimeException.class)
  public void testSetHeadersPutException() throws JSONException {
    doc = spy(doc);
    doThrow(JSONException.class).when(doc).put(any(String.class), any(JSONObject.class));
    doc.setHeaders(mock(JSONObject.class), true);
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

  @Test(expected = RuntimeException.class)
  public void testGetHeadersException() {
    doc.remove("headers");
    doc.getHeaders();
  }

  @Test
  public void testGetHeaders() throws JSONException {
    doc.setHeaders(null);
    assertNotNull(doc.getHeaders());
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    doc.setHeaders(headers);
    assertEquals(doc.getHeaders().getString("foo"), "bar");
  }

  @Test(expected = RuntimeException.class)
  public void testGetVersionException() {
    doc = spy(doc);
    doThrow(JSONException.class).when(doc).isNull(any(String.class));
    doc.getVersion();
  }

  @Test
  public void testGetVersion() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        JSONObject source = new JSONObject();
        source.put("foo", "bar");
        response.put("_source", source);
        response.put("_version", "42");
        response.put("_id", "id");
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", response));
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertNull(doc.getVersion());
    doc.save(mock(KuzzResponseListener.class));
    verify(k, times(1)).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testGetIdException() {
    doc = spy(doc);
    doThrow(JSONException.class).when(doc).isNull(any(String.class));
    doc.getId();
  }

  @Test(expected = RuntimeException.class)
  public void testSetIdException() throws JSONException {
    doc = spy(doc);
    doThrow(JSONException.class).when(doc).put(any(String.class), any(String.class));
    doc.setId("42");
  }

}
