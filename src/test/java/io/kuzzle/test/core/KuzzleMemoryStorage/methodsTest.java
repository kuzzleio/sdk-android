package io.kuzzle.test.core.KuzzleMemoryStorage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.util.memoryStorage.Action;
import io.kuzzle.sdk.util.memoryStorage.BitOP;
import io.kuzzle.sdk.util.memoryStorage.Position;
import io.kuzzle.test.testUtils.KuzzleMemoryStorageExtend;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class methodsTest {

  Kuzzle kuzzle;
  KuzzleMemoryStorageExtend ms;
  ArgumentCaptor argument;

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    ms = spy(new KuzzleMemoryStorageExtend(kuzzle));
    argument = ArgumentCaptor.forClass(JSONObject.class);
  }

  private JSONObject  getBody(final JSONObject obj) {
    try {
      return obj.getJSONObject("body");
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAppend() throws JSONException {
    ms.append("foo", "bar");
    verify(ms).send(eq(Action.append), (JSONObject) argument.capture());
    assertEquals("bar", ((JSONObject)argument.getValue()).getString("body"));
  }

  @Test
  public void testBgrewriteaof() {
    ms.bgrewriteaof();
    verify(ms).send(eq(Action.bgrewriteaof), any(JSONObject.class));
  }

  @Test
  public void testBgsave() {
    ms.bgsave();
    verify(ms).send(eq(Action.bgsave), any(JSONObject.class));
  }

  @Test
  public void testBitcount() throws JSONException {
    ms.bitcount("foo");
    ms.bitcount("foo", 24, 42);
    verify(ms, times(2)).send(eq(Action.bitcount), (JSONObject) argument.capture());
    assertEquals(24, getBody((JSONObject)argument.getAllValues().get(1)).getLong("start"));
    assertEquals(42, getBody((JSONObject)argument.getAllValues().get(1)).getLong("end"));
  }

  @Test
  public void testBitOP() throws JSONException {
    ms.bitop(BitOP.AND, "destKey", "src1", "src2");
    verify(ms).send(eq(Action.bitop), (JSONObject) argument.capture());
    assertEquals("AND", getBody((JSONObject) argument.getValue()).getString("operation"));
    assertEquals("destKey", getBody((JSONObject) argument.getValue()).getString("destKey"));
    assertEquals("src1", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[0]);
    assertEquals("src2", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[1]);
  }

  @Test
  public void testBitPos() throws JSONException {
    ms.bitpos("id", 42);
    ms.bitpos("id", 42, 24);
    ms.bitpos("id", 42, 24, 1337);
    verify(ms, times(3)).send(eq(Action.bitpos), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject) argument.getAllValues().get(0)).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getAllValues().get(2)).getLong("bit"));
    assertEquals(24, getBody((JSONObject) argument.getAllValues().get(2)).getLong("start"));
    assertEquals(1337, getBody((JSONObject) argument.getAllValues().get(2)).getLong("end"));
  }

  @Test
  public void testBlpop() throws JSONException {
    ms.blpop(new String[]{"1", "2"}, 42);
    verify(ms).send(eq(Action.blpop), (JSONObject) argument.capture());
    assertEquals("1",((String[])getBody((JSONObject) argument.getValue()).get("src"))[0]);
    assertEquals("2",((String[])getBody((JSONObject) argument.getValue()).get("src"))[1]);
    assertEquals(42,getBody((JSONObject) argument.getValue()).getLong("timeout"));
  }

  @Test
  public void testBrpoplpush() throws JSONException {
    ms.brpoplpush("source", "destination", 42);
    verify(ms).send(eq(Action.brpoplpush), (JSONObject) argument.capture());
    assertEquals("source", getBody((JSONObject) argument.getValue()).get("source"));
    assertEquals("destination", getBody((JSONObject) argument.getValue()).get("destination"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("timeout"));
  }

  @Test
  public void testDbsize() {
    ms.dbsize();
    verify(ms).send(eq(Action.dbsize), any(JSONObject.class));
  }

  @Test
  public void testDecrby() throws JSONException {
    ms.decrby("id", 42);
    verify(ms).send(eq(Action.decrby), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject) argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("value"));
  }

  @Test
  public void testDel() throws JSONException {
    ms.del("id");
    ms.del("id1", "id2");
    verify(ms, times(2)).send(eq(Action.del), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals("id1", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[0]);
    assertEquals("id2", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[1]);
  }

  @Test
  public void testDiscard() {
    ms.discard();
    verify(ms).send(eq(Action.discard), any(JSONObject.class));
  }

  @Test
  public void testExec() {
    ms.exec();
    verify(ms).send(eq(Action.exec), any(JSONObject.class));
  }

  @Test
  public void testExists() throws JSONException {
    ms.exists("id");
    ms.exists("id1", "id2");
    verify(ms, times(2)).send(eq(Action.exists), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals("id1", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[0]);
    assertEquals("id2", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[1]);
  }

  @Test
  public void testExpire() throws JSONException {
    ms.expire("id", 42);
    verify(ms).send(eq(Action.expire), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("seconds"));
  }

  @Test
  public void testExpireAt() throws JSONException {
    ms.expireat("id", 42);
    verify(ms).send(eq(Action.expireat), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("timestamp"));
  }

  @Test
  public void testFlushdb() {
    ms.flushdb();
    verify(ms).send(eq(Action.flushdb), any(JSONObject.class));
  }

  @Test
  public void testGetbit() throws JSONException {
    ms.getbit("id", 42);
    verify(ms).send(eq(Action.getbit), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("offset"));
  }

  @Test
  public void testGetrange() throws JSONException {
    ms.getrange("id", 24, 42);
    verify(ms).send(eq(Action.getrange), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("start"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("end"));
  }

  @Test
  public void testHdel() throws JSONException {
    ms.hdel("id", "field");
    ms.hdel("id", "field1", "field2");
    verify(ms, times(2)).send(eq(Action.hdel), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals("field", getBody((JSONObject) argument.getAllValues().get(0)).getString("field"));
    assertEquals("field1", ((String[])getBody((JSONObject) argument.getValue()).get("fields"))[0]);
    assertEquals("field2", ((String[])getBody((JSONObject) argument.getValue()).get("fields"))[1]);
  }

  @Test
  public void testHexists() throws JSONException {
    ms.hexists("id", "field");
    verify(ms).send(eq(Action.hexists), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("field", getBody((JSONObject) argument.getValue()).getString("field"));
  }

  @Test
  public void hincrby() throws JSONException {
    ms.hincrby("id", "field", 42);
    verify(ms).send(eq(Action.hincrby), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("field", getBody((JSONObject) argument.getValue()).getString("field"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getDouble("value"), 1);
  }

  @Test
  public void hmsetTest() throws JSONException {
    Map m = new HashMap<String, String>();
    m.put("one", "one");
    m.put("second", "two");
    ms.hmset("id", m);
    verify(ms).send(eq(Action.hmset), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("one", getBody((JSONObject) argument.getValue()).getJSONObject("fields").getString("one"));
    assertEquals("two", getBody((JSONObject) argument.getValue()).getJSONObject("fields").getString("second"));
  }

  @Test
  public void hsetTest() throws JSONException {
    ms.hset("id", "field", "value");
    verify(ms).send(eq(Action.hset), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("field", getBody((JSONObject) argument.getValue()).getString("field"));
    assertEquals("value", getBody((JSONObject) argument.getValue()).getString("value"));
  }

  @Test
  public void infoTest() throws JSONException {
    ms.info("section");
    verify(ms).send(eq(Action.info), (JSONObject) argument.capture());
    assertEquals("section", getBody((JSONObject) argument.getValue()).getString("section"));
  }

  @Test
  public void keysTest() throws JSONException {
    ms.keys("pattern");
    verify(ms).send(eq(Action.keys), (JSONObject) argument.capture());
    assertEquals("pattern", getBody((JSONObject) argument.getValue()).getString("pattern"));
  }

  @Test
  public void lastSaveTest() {
    ms.lastsave();
    verify(ms).send(eq(Action.lastsave), any(JSONObject.class));
  }

  @Test
  public void lindexTest() throws JSONException {
    ms.lindex("id", 42);
    verify(ms).send(eq(Action.lindex), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("idx"));
  }

  @Test
  public void linsertTest() throws JSONException {
    ms.linsert("id", Position.AFTER, "pivot", "value");
    verify(ms).send(eq(Action.linsert), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("pivot", getBody((JSONObject) argument.getValue()).getString("pivot"));
    assertEquals("value", getBody((JSONObject) argument.getValue()).getString("value"));
  }

  @Test
  public void lpushTest() throws JSONException {
    ms.lpush("id", "value1", "value2");
    verify(ms).send(eq(Action.lpush), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("value1", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[0]);
    assertEquals("value2", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[1]);
  }

  @Test
  public void lrangeTest() throws JSONException {
    ms.lrange("id", 24, 42);
    verify(ms).send(eq(Action.lrange), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("start"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("end"));
  }

  @Test
  public void lremTest() throws JSONException {
    ms.lrem("id", 42, "value");
    verify(ms).send(eq(Action.lrem), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("count"));
    assertEquals("value", getBody((JSONObject) argument.getValue()).getString("value"));
  }

  @Test
  public void lsetTest() throws JSONException {
    ms.lset("id", 42, "value");
    verify(ms).send(eq(Action.lset), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("idx"));
    assertEquals("value", getBody((JSONObject) argument.getValue()).getString("value"));
  }

  @Test
  public void ltrimTest() throws JSONException {
    ms.ltrim("id", 24, 42);
    verify(ms).send(eq(Action.lrange), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("start"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("stop"));
  }

  @Test
  public void msetTest() throws JSONException {
    ms.mset("key1", "value1", "key2", "value2");
    verify(ms).send(eq(Action.mset), (JSONObject) argument.capture());
    assertEquals("key1", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[0]);
    assertEquals("value1", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[1]);
    assertEquals("key2", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[2]);
    assertEquals("value2", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[3]);
  }

}
