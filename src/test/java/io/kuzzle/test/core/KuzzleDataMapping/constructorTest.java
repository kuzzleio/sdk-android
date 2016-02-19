package io.kuzzle.test.core.KuzzleDataMapping;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDataMapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class constructorTest {
  private Kuzzle k;
  private KuzzleDataCollection dataCollection;
  private KuzzleDataMapping dataMapping;

  @Before
  public void setUp() {
    k = mock(Kuzzle.class);
    when(k.getDefaultIndex()).thenReturn("index");
    when(k.getHeaders()).thenReturn(new JSONObject());
    dataCollection = new KuzzleDataCollection(k, "index", "test");
    dataMapping = new KuzzleDataMapping(dataCollection);
  }

  @Test
  public void testConstructor() throws JSONException {
    JSONObject mapping = new JSONObject();
    mapping.put("type", "string");
    dataMapping = new KuzzleDataMapping(dataCollection, mapping);
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
