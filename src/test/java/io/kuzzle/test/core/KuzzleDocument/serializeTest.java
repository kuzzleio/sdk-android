package io.kuzzle.test.core.KuzzleDocument;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class serializeTest {

  private Kuzzle k;
  private KuzzleDocument doc;

  @Before
  public void setUp() throws URISyntaxException, JSONException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    extended.setState(KuzzleStates.CONNECTED);
    k = spy(extended);
    doc = new KuzzleDocument(new Collection(k, "test", "index"));
  }

  @Test(expected = RuntimeException.class)
  public void testException() {
    doThrow(JSONException.class).when(k).addHeaders(any(JSONObject.class), any(JSONObject.class));
    doc.serialize();
  }

  @Test
  public void testToString() throws JSONException {
    JSONObject o = new JSONObject()
        .put("foo", "bar");
    doc.setId("42");
    doc.setVersion(4242);
    doc.setContent(o);
    assertEquals(doc.toString(), "{\"_id\":\"42\",\"body\":{\"foo\":\"bar\"},\"_version\":4242}");
  }

}
