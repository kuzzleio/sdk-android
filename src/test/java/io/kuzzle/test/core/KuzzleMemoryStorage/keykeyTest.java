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

public class keykeyTest {

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

  public void testIt(final Action action) throws JSONException {
    verify(ms).send(eq(action), (KuzzleJSONObject) argument.capture());
    assertEquals("id1", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[0]);
    assertEquals("id2", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[1]);
  }

  @Test
  public void testMget() throws JSONException {
    ms.mget("id1", "id2");
    testIt(Action.mget);
  }

  @Test
  public void testDel() throws JSONException {
    ms.del("id1", "id2");
    testIt(Action.del);
  }

  @Test
  public void testExists() throws JSONException {
    ms.exists("id1", "id2");
    testIt(Action.exists);
  }

  @Test
  public void testPfCount() throws JSONException {
    ms.pfcount("id1", "id2");
    testIt(Action.pfcount);
  }

  @Test
  public void testSdiff() throws JSONException {
    ms.sdiff("id1", "id2");
    testIt(Action.sdiff);
  }

  @Test
  public void testSinter() throws JSONException {
    ms.sinter("id1", "id2");
    testIt(Action.sinter);
  }

  @Test
  public void testSunion() throws JSONException {
    ms.sunion("id1", "id2");
    testIt(Action.sunion);
  }

  @Test
  public void testWatch() throws JSONException {
    ms.watch("id1", "id2");
    testIt(Action.watch);
  }

}
