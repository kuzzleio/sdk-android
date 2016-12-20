package io.kuzzle.test.core.KuzzleDataMapping;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataMapping;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class refreshTest {
  private Kuzzle k;
  private Collection dataCollection;
  private KuzzleDataMapping dataMapping;

  @Before
  public void setUp() {
    k = mock(Kuzzle.class);
    when(k.getDefaultIndex()).thenReturn("index");
    when(k.getHeaders()).thenReturn(new JSONObject());
    dataCollection = new Collection(k, "test", "index");
    dataMapping = new KuzzleDataMapping(dataCollection);
  }

  @Test
  public void checkSignaturesVariants() {
    dataMapping = spy(dataMapping);
    dataMapping.refresh(mock(KuzzleResponseListener.class));
    verify(dataMapping).refresh(any(KuzzleOptions.class), any(KuzzleResponseListener.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRefreshIllegalListener() {
    dataMapping.refresh(null);
  }

  @Test(expected = RuntimeException.class)
  public void testException() throws JSONException {
    doThrow(JSONException.class).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    dataMapping.refresh(mock(KuzzleResponseListener.class));
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
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    KuzzleResponseListener mockListener = mock(KuzzleResponseListener.class);
    doThrow(JSONException.class).when(mockListener).onSuccess(any(KuzzleDataMapping.class));
    dataMapping.refresh(mockListener);
  }

  @Test
  public void testRefresh() throws JSONException {
    final JSONObject mockMapping = new JSONObject("{\"index\": {\"mappings\": {" +
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

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = mockMapping;
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(null);
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    when(k.getDefaultIndex()).thenReturn("index");

    dataMapping.refresh(new KuzzleResponseListener<KuzzleDataMapping>() {
      @Override
      public void onSuccess(KuzzleDataMapping response) {
        assertNotEquals(dataMapping, response);
        try {
          assertEquals(
            mockMapping.getJSONObject("index").getJSONObject("mappings").getJSONObject("test"),
            response.getMapping()
          );
        }
        catch(JSONException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    dataMapping.refresh(new KuzzleOptions(), mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(k, times(2)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "collection");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "getMapping");
  }
}
