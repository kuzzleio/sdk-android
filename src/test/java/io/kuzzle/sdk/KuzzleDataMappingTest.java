package io.kuzzle.sdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDataMapping;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.listeners.ResponseListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class KuzzleDataMappingTest {

  private Kuzzle k;
  private KuzzleDataCollection dataCollection;
  private KuzzleDataMapping dataMapping;

  @Before
  public void setUp() {
    k = mock(Kuzzle.class);
    dataCollection = new KuzzleDataCollection(k, "test");
    dataMapping = new KuzzleDataMapping(dataCollection);
  }

  @Test
  public void testConstructor() throws JSONException {
    JSONObject mapping = new JSONObject();
    mapping.put("type", "string");
    dataMapping = new KuzzleDataMapping(dataCollection, mapping);
  }

  @Test
  public void testApply() throws IOException, JSONException, KuzzleException {
    dataMapping.apply();
    dataMapping.apply(new KuzzleOptions());
    dataMapping.apply(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    dataMapping.apply(new KuzzleOptions(), new ResponseListener() {
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
  public void testRefresh() throws IOException, JSONException, KuzzleException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject("{\"mainindex\": {\"mappings\": {" +
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
        ((ResponseListener) invocation.getArguments()[5]).onSuccess(response);
        ((ResponseListener) invocation.getArguments()[5]).onError(null);
        return null;
      }
    }).when(k).query(eq("test"), eq("admin"), eq("getMapping"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));

    dataMapping.refresh();
    dataMapping.refresh(new KuzzleOptions());
    dataMapping.refresh(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          assertEquals(object.getJSONObject("test").getJSONObject("properties").getJSONObject("foo").getString("type"), "string");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    dataMapping.refresh(new KuzzleOptions(), new ResponseListener() {
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
  public void testRemove() throws JSONException {
    JSONObject mapping = new JSONObject();
    mapping.put("type", "string");
    dataMapping.set("foo", mapping);
    assertEquals(dataMapping.getMapping().getJSONObject("foo").getString("type"), "string");
    dataMapping.remove("foo");
    assertTrue(dataMapping.getMapping().isNull("foo"));
  }

  @Test
  public void testSetHeaders() throws JSONException, KuzzleException {
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
  public void testGetHeaders() throws JSONException, IOException, KuzzleException {
    dataMapping.setHeaders(null);
    assertNotNull(dataMapping.getHeaders());
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    dataMapping.setHeaders(headers);
    assertEquals(dataMapping.getHeaders().getString("foo"), "bar");
  }

}
