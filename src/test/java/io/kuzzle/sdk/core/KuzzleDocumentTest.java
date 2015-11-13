package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URISyntaxException;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.exceptions.KuzzleException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
  public void testSaveError() throws KuzzleException, JSONException, IOException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("error", "foo");
        //Call callback with error response
        ((ResponseListener) invocation.getArguments()[4]).onError(response);
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), any(String.class), any(JSONObject.class), any(ResponseListener.class));

    doc.save(false);
    verify(k, times(1)).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testSaveSuccess() throws KuzzleException, JSONException, IOException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("_id", "id-42");
        response.put("result", response);
        ((ResponseListener) invocation.getArguments()[4]).onSuccess(response);
        return null;
      }
    }).when(k).query(eq("test"), eq("write"), any(String.class), any(JSONObject.class), any(ResponseListener.class));

    doc.save(false);
    verify(k, times(1)).query(eq("test"), eq("write"), eq("createOrUpdate"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testDelete() throws URISyntaxException, IOException, JSONException, KuzzleException {
    KuzzleDocument mock = mock(KuzzleDocument.class);
    // Mocking getId()
    doc.setId("id-42");
    doc.delete();
    verify(k, times(1)).query(eq("test"), eq("write"), eq("delete"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testRefresh() throws IOException, JSONException {
    KuzzleDocument mock = mock(KuzzleDocument.class);
    doc.refresh(null);
    verify(k, never()).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class));
    when(mock.getId()).thenReturn("id-42");
    when(mock.getContent()).thenReturn(new JSONObject());

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("result", "foo");
        ((ResponseListener) invocation.getArguments()[4]).onSuccess(response);
        return null;
      }
    }).when(k).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class), any(ResponseListener.class));
    mock.refresh(null);
    verify(k, times(1)).query(eq("test"), eq("read"), eq("get"), any(JSONObject.class), any(ResponseListener.class));
    assertNotNull(mock.getContent());
  }

  @Test
  public void testSend() throws IOException, JSONException {
    doc.send();
    verify(k, times(1)).query(eq("test"), eq("write"), eq("create"), any(JSONObject.class));
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

}
