package io.kuzzle.test.core.KuzzleMemoryStorage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.util.KuzzleJSONObject;
import io.kuzzle.test.testUtils.MemoryStorageExtend;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class uniqueKeyArgumentMethodsTest {

  Kuzzle kuzzle;
  MemoryStorageExtend ms;
  ArgumentCaptor argument;
  ArgumentCaptor argumentAction;

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    ms = spy(new MemoryStorageExtend(kuzzle));
    argument = ArgumentCaptor.forClass(JSONObject.class);
    argumentAction = ArgumentCaptor.forClass(Action.class);
  }

  private JSONObject  getBody(final JSONObject obj) {
    try {
      return obj.getJSONObject("body");
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void uniqueKeyArgumentTest() throws JSONException {
    ms.decr("key");
    ms.get("key");
    ms.dump("key");
    ms.hgetall("key");
    ms.hkeys("key");
    ms.hlen("key");
    ms.hstrlen("key");
    ms.hvals("key");
    ms.incr("key");
    ms.llen("key");
    ms.lpop("key");
    ms.persist("key");
    ms.pttl("key");
    ms.rpop("key");
    ms.scard("key");
    ms.smembers("key");
    ms.strlen("key");
    ms.ttl("key");
    ms.type("key");
    ms.zcard("key");
    verify(ms, times(20)).send((Action) argumentAction.capture(), (KuzzleJSONObject) argument.capture());
    assertEquals(Action.decr, argumentAction.getAllValues().get(0));
    assertEquals(Action.get, argumentAction.getAllValues().get(1));
    assertEquals(Action.dump, argumentAction.getAllValues().get(2));
    assertEquals(Action.hgetall, argumentAction.getAllValues().get(3));
    assertEquals(Action.hkeys, argumentAction.getAllValues().get(4));
    assertEquals(Action.hlen, argumentAction.getAllValues().get(5));
    assertEquals(Action.hstrlen, argumentAction.getAllValues().get(6));
    assertEquals(Action.hvals, argumentAction.getAllValues().get(7));
    assertEquals(Action.incr, argumentAction.getAllValues().get(8));
    assertEquals(Action.llen, argumentAction.getAllValues().get(9));
    assertEquals(Action.lpop, argumentAction.getAllValues().get(10));
    assertEquals(Action.persist, argumentAction.getAllValues().get(11));
    assertEquals(Action.pttl, argumentAction.getAllValues().get(12));
    assertEquals(Action.rpop, argumentAction.getAllValues().get(13));
    assertEquals(Action.scard, argumentAction.getAllValues().get(14));
    assertEquals(Action.smembers, argumentAction.getAllValues().get(15));
    assertEquals(Action.strlen, argumentAction.getAllValues().get(16));
    assertEquals(Action.ttl, argumentAction.getAllValues().get(17));
    assertEquals(Action.type, argumentAction.getAllValues().get(18));
    assertEquals(Action.zcard, argumentAction.getAllValues().get(19));
    assertEquals("key", ((JSONObject) argument.getAllValues().get(0)).getString("_id"));
  }

}
