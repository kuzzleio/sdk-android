package io.kuzzle.sdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDataMapping;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleDataMappingTest {

  private Kuzzle k;
  private KuzzleDataCollection dataCollection;
  private KuzzleDataMapping dataMapping;

  @Before
  public void setUp() {
    k = mock(Kuzzle.class);
    when(k.getIndex()).thenReturn("index");
    dataCollection = new KuzzleDataCollection(k, "index", "test");
    dataMapping = new KuzzleDataMapping(dataCollection);
  }

  @Test
  public void testConstructor() throws JSONException {
    JSONObject mapping = new JSONObject();
    mapping.put("type", "string");
    dataMapping = new KuzzleDataMapping(dataCollection, mapping);
  }

  @Test(expected = RuntimeException.class)
  public void testApplyQueryException() throws JSONException {
    doThrow(JSONException.class).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    dataMapping.apply();
  }

  @Test(expected = RuntimeException.class)
  public void testApplyException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(mock(JSONObject.class));
        return null;
      }
    }).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    KuzzleResponseListener mockListener = mock(KuzzleResponseListener.class);
    doThrow(JSONException.class).when(mockListener).onSuccess(any(KuzzleDataMapping.class));
    dataMapping.apply(mockListener);
  }

  @Test
  public void testApply() throws JSONException {
    final JSONObject mockResponse = new JSONObject("{\n" +
        "    properties: {\n" +
        "      field1: {type: \"field type\"},\n" +
        "      field2: {type: \"field type\"},\n" +
        "      fieldn: {type: \"field type\"}\n" +
        "    }\n" +
        "  }");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(mockResponse);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(null);
        return null;
      }
    }).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    dataMapping.apply();
    dataMapping.apply(new KuzzleOptions());
    dataMapping.apply(mock(KuzzleResponseListener.class));
    dataMapping.apply(new KuzzleOptions(), mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(k, times(4)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "putMapping");
  }

  @Test(expected = RuntimeException.class)
  public void testException() throws JSONException {
    doThrow(JSONException.class).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    dataMapping.refresh();
  }

  @Test(expected = RuntimeException.class)
  public void testRefreshException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject("{\"index\": {\n" +
            "      \"mappings\": {\n" +
            "        \"users\": {\n" +
            "          \"properties\": {\n" +
            "            \"pos\": {\n" +
            "              \"type\": \"geo_point\"\n" +
            "            },\n" +
            "            \"sibling\": {\n" +
            "              \"type\": \"string\"\n" +
            "            },\n" +
            "            \"status\": {\n" +
            "              \"type\": \"string\"\n" +
            "            },\n" +
            "            \"type\": {\n" +
            "              \"type\": \"string\"\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }" +
            "}"));
        return null;
      }
    }).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    KuzzleResponseListener mockListener = mock(KuzzleResponseListener.class);
    doThrow(JSONException.class).when(mockListener).onSuccess(any(KuzzleDataMapping.class));
    dataMapping.refresh(mockListener);
  }

  @Test
  public void testRefresh() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject("{\"index\": {\"mappings\": {" +
            "        \"test\": {" +
            "          \"properties\": {" +
            "            \"available\": {" +
            "              \"type\": \"boolean\"" +
            "            }," +
            "            \"foo\": {" +
            "              \"type\": \"string\"" +
            "            }," +
            "            \"type\": {" +
            "              \"type\": \"string\"" +
            "            }," +
            "            \"userId\": {" +
            "              \"type\": \"string\"" +
            "            }" +
            "          }" +
            "        }}}}");
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(null);
        return null;
      }
    }).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    when(k.getIndex()).thenReturn("index");
    dataMapping.refresh();
    dataMapping.refresh(new KuzzleOptions());
    dataMapping.refresh(mock(KuzzleResponseListener.class));
    dataMapping.refresh(new KuzzleOptions(), mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(k, times(4)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getMapping");
  }

  @Test
  public void testRemove() throws JSONException {
    JSONObject mapping = new JSONObject();
    mapping.put("type", "string");
    dataMapping.set("foo", mapping);
    assertEquals(dataMapping.getMapping().getJSONObject("foo").getString("type"), "string");
    dataMapping.remove("foo");
    assertTrue(dataMapping.getMapping().isNull("foo"));
  }

  @Test
  public void testSetHeaders() throws JSONException {
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    dataMapping.setHeaders(headers, true);
    assertEquals(dataMapping.getHeaders().getString("foo"), "bar");
    headers.put("oof", "baz");
    dataMapping.setHeaders(headers);
    assertEquals(dataMapping.getHeaders().getString("foo"), "bar");
    assertEquals(dataMapping.getHeaders().getString("oof"), "baz");
  }

  @Test
  public void testGetHeaders() throws JSONException {
    dataMapping.setHeaders(null);
    assertNotNull(dataMapping.getHeaders());
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    dataMapping.setHeaders(headers);
    assertEquals(dataMapping.getHeaders().getString("foo"), "bar");
  }

}
