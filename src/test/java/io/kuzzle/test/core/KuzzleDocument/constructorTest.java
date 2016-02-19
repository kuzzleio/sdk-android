package io.kuzzle.test.core.KuzzleDocument;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class constructorTest {
  private Kuzzle k;
  private KuzzleDocument doc;

  @Before
  public void setUp() throws URISyntaxException, JSONException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("http://localhost:7512", opts, null);
    extended.setState(KuzzleStates.CONNECTED);
    k = spy(extended);
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "index", "test"));
  }

  @Test
  public void testConstructor() throws JSONException {
    doc = new KuzzleDocument(new KuzzleDataCollection(k, "index", "test"), "42");
    assertEquals(doc.getId(), "42");
  }

  @Test
  public void testCollection() throws JSONException {
    Kuzzle k = mock(Kuzzle.class);
    when(k.getHeaders()).thenReturn(new JSONObject());
    KuzzleDataCollection collection = new KuzzleDataCollection(k, "index", "test");
    KuzzleDocument doc = new KuzzleDocument(collection);
    assertEquals(doc.getCollection(), collection.getCollection());
  }

  @Test
  public void testDocumentWithContent() throws JSONException {
    JSONObject content = new JSONObject();
    content.put("foo", "bar");

    doc = new KuzzleDocument(new KuzzleDataCollection(k, "index", "test"), content);
    assertEquals(doc.getContent().getString("foo"), "bar");
  }

  @Test(expected = RuntimeException.class)
  public void testSetContentPutException() throws JSONException {
    doc = spy(doc);
    doc.setContent(mock(JSONObject.class));
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

  @Test
  public void testGetContent() throws JSONException {
    doc.setContent(null);
    assertNotNull(doc.getContent());
    doc.setContent("foo", "bar");
    assertEquals(doc.getContent().getString("foo"), "bar");
    assertNull(doc.getContent("!exist"));
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
  public void testGetHeaders() throws JSONException {
    doc.setHeaders(null);
    assertNotNull(doc.getHeaders());
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    doc.setHeaders(headers);
    assertEquals(doc.getHeaders().getString("foo"), "bar");
  }

  @Test
  public void testGetVersion() throws JSONException {
    assertEquals(-1, doc.getVersion());
    doc.setVersion(42);
    assertEquals(42, doc.getVersion());
  }
}
