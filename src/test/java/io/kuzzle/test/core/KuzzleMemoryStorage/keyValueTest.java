package io.kuzzle.test.core.KuzzleMemoryStorage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.util.KuzzleJSONObject;
import io.kuzzle.sdk.util.memoryStorage.Action;
import io.kuzzle.test.testUtils.MemoryStorageExtend;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class keyValueTest {

  Kuzzle kuzzle;
  MemoryStorageExtend ms;
  ArgumentCaptor argument;

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    ms = spy(new MemoryStorageExtend(kuzzle));
    argument = ArgumentCaptor.forClass(JSONObject.class);
  }

  private JSONObject  getBody(final JSONObject obj) {
    try {
      return obj.getJSONObject("body");
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  private void testIt(final Action action) throws JSONException {
    verify(ms).send(eq(action), (KuzzleJSONObject) argument.capture());
    assertEquals("id", ((JSONObject) argument.getValue()).getString("_id"));
    assertEquals("value", getBody((JSONObject) argument.getValue()).getString("value"));
  }

  @Test
  public void getsetTest() throws JSONException {
    ms.getset("id", "value");
    testIt(Action.getset);
  }

  @Test
  public void lpushxTest() throws JSONException {
    ms.lpushx("id", "value");
    testIt(Action.lpushx);
  }



}
