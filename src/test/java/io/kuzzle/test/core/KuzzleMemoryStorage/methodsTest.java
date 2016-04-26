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
import io.kuzzle.sdk.util.memoryStorage.ObjectCommand;
import io.kuzzle.sdk.util.memoryStorage.Position;
import io.kuzzle.sdk.util.memoryStorage.SetParams;
import io.kuzzle.sdk.util.memoryStorage.ZParams;
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
    ms.hdel("id", "field1", "field2");
    verify(ms).send(eq(Action.hdel), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
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

  @Test
  public void multiTest() {
    ms.multi();
    verify(ms).send(eq(Action.multi), any(JSONObject.class));
  }

  @Test
  public void objectTest() throws JSONException {
    ms.object(ObjectCommand.encoding, "test1");
    ms.object(ObjectCommand.idletime, "test2");
    ms.object(ObjectCommand.refcount, "test3");
    verify(ms, times(3)).send(eq(Action.object), (JSONObject) argument.capture());
    assertEquals(ObjectCommand.encoding.toString(), getBody((JSONObject) argument.getAllValues().get(0)).getString("subcommand"));
    assertEquals(ObjectCommand.idletime.toString(), getBody((JSONObject) argument.getAllValues().get(1)).getString("subcommand"));
    assertEquals(ObjectCommand.refcount.toString(), getBody((JSONObject) argument.getAllValues().get(2)).getString("subcommand"));
    assertEquals("test1", getBody((JSONObject) argument.getAllValues().get(0)).getString("args"));
    assertEquals("test2", getBody((JSONObject) argument.getAllValues().get(1)).getString("args"));
    assertEquals("test3", getBody((JSONObject) argument.getAllValues().get(2)).getString("args"));
  }

  @Test
  public void pexpireTest() throws JSONException {
    ms.pexpire("id", 42);
    verify(ms).send(eq(Action.pexpire), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("milliseconds"));
  }

  @Test
  public void pexpireatTest() throws JSONException {
    ms.pexpireat("id", 42);
    verify(ms).send(eq(Action.pexpireat), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("timestamp"));
  }

  @Test
  public void pfaddTest() throws JSONException {
    ms.pfadd("id", "value1", "value2");
    verify(ms).send(eq(Action.pfadd), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("value1", ((String[])getBody((JSONObject) argument.getValue()).get("elements"))[0]);
    assertEquals("value2", ((String[])getBody((JSONObject) argument.getValue()).get("elements"))[1]);
  }

  @Test
  public void pfmerge() throws JSONException {
    ms.pfmerge("dest", "src1", "src2");
    verify(ms).send(eq(Action.pfmerge), (JSONObject) argument.capture());
    assertEquals("dest", getBody((JSONObject) argument.getValue()).get("destkey"));
    assertEquals("src1", ((String[])getBody((JSONObject) argument.getValue()).get("sourcekeys"))[0]);
    assertEquals("src2", ((String[])getBody((JSONObject) argument.getValue()).get("sourcekeys"))[1]);
  }

  @Test
  public void pingTest() {
    ms.ping();
    verify(ms).send(eq(Action.ping), any(JSONObject.class));
  }

  @Test
  public void psetexTest() throws JSONException {
    ms.psetex("id", 42, "value");
    verify(ms).send(eq(Action.psetex), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("milliseconds"));
    assertEquals("value", getBody((JSONObject) argument.getValue()).getString("value"));
  }

  @Test
  public void publishTest() throws JSONException {
    ms.publish("channel", "message");
    verify(ms).send(eq(Action.publish), (JSONObject) argument.capture());
    assertEquals("channel", getBody((JSONObject) argument.getValue()).getString("channel"));
    assertEquals("message", getBody((JSONObject) argument.getValue()).getString("message"));
  }

  @Test
  public void randomkeyTest() {
    ms.randomkey();
    verify(ms).send(eq(Action.randomkey), any(JSONObject.class));
  }

  @Test
  public void renameTest() throws JSONException {
    ms.rename("oldkey", "newkey");
    verify(ms).send(eq(Action.rename), (JSONObject) argument.capture());
    assertEquals("oldkey", ((JSONObject) argument.getValue()).getString("_id"));
    assertEquals("newkey", getBody((JSONObject) argument.getValue()).getString("newkey"));
  }

  @Test
  public void renamenxTest() throws JSONException {
    ms.renamenx("oldkey", "newkey");
    verify(ms).send(eq(Action.renamenx), (JSONObject) argument.capture());
    assertEquals("oldkey", ((JSONObject) argument.getValue()).getString("_id"));
    assertEquals("newkey", getBody((JSONObject) argument.getValue()).getString("newkey"));
  }

  @Test
  public void restoreTest() throws JSONException {
    ms.restore("id", 42, "\nx17serializedcontent");
    verify(ms).send(eq(Action.restore), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("ttl"));
    assertEquals("\nx17serializedcontent", getBody((JSONObject) argument.getValue()).getString("content"));
  }

  @Test
  public void rpoplpushTest() throws JSONException {
    ms.rpoplpush("source", "dst");
    verify(ms).send(eq(Action.rpoplpush), (JSONObject) argument.capture());
    assertEquals("source", getBody((JSONObject) argument.getValue()).getString("source"));
    assertEquals("dst", getBody((JSONObject) argument.getValue()).getString("destination"));
  }

  @Test
  public void saddTest() throws JSONException {
    ms.sadd("id", "member1", "member2");
    verify(ms).send(eq(Action.sadd), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("member1", ((String[])getBody((JSONObject) argument.getValue()).get("members"))[0]);
    assertEquals("member2", ((String[])getBody((JSONObject) argument.getValue()).get("members"))[1]);
  }

  @Test
  public void saveTest() {
    ms.save();
    verify(ms).send(eq(Action.save), any(JSONObject.class));
  }


  @Test
  public void sdiffstoreTest() throws JSONException {
    ms.sdiffstore("dest", "key1", "key2");
    verify(ms).send(eq(Action.sdiffstore), (JSONObject) argument.capture());
    assertEquals("dest", getBody((JSONObject) argument.getValue()).getString("destination"));
    assertEquals("key1", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[0]);
    assertEquals("key2", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[1]);
  }

  @Test
  public void setTest() throws JSONException {
    SetParams sp = SetParams.setParams();
    sp.ex(42);
    sp.nx();
    ms.set("mykey", "value", sp);
    verify(ms).send(eq(Action.set), (JSONObject) argument.capture());
    assertEquals("mykey", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("ex"));
    assertEquals(true, getBody((JSONObject) argument.getValue()).getBoolean("nx"));
  }

  @Test
  public void setbitTest() throws JSONException {
    ms.setbit("id", 42, 1337);
    verify(ms).send(eq(Action.setbit), (JSONObject) argument.capture());
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("offset"));
    assertEquals(1337, getBody((JSONObject) argument.getValue()).getLong("value"));
  }

  @Test
  public void setexTest() throws JSONException {
    ms.setex("id", 42, "value");
    verify(ms).send(eq(Action.setbit), (JSONObject) argument.capture());
    assertEquals(42, getBody((JSONObject) argument.getValue()).getInt("seconds"));
    assertEquals("value", getBody((JSONObject) argument.getValue()).getString("value"));
  }

  @Test
  public void setrangeTest() throws JSONException {
    ms.setrange("id", 42, "value");
    verify(ms).send(eq(Action.setrange), (JSONObject) argument.capture());
    assertEquals(42, getBody((JSONObject) argument.getValue()).getInt("offset"));
    assertEquals("value", getBody((JSONObject) argument.getValue()).getString("value"));
  }

  @Test
  public void sinterstoreTest() throws JSONException {
    ms.sinterstore("dest", "key1", "key2");
    verify(ms).send(eq(Action.sinterstore), (JSONObject) argument.capture());
    assertEquals("dest", getBody((JSONObject) argument.getValue()).getString("destination"));
    assertEquals("key1", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[0]);
    assertEquals("key2", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[1]);
  }

  @Test
  public void sismemberTest() throws JSONException {
    ms.sismember("id", "member");
    verify(ms).send(eq(Action.sinterstore), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("member", getBody((JSONObject) argument.getValue()).getString("member"));
  }

  @Test
  public void smoveTest() throws JSONException {
    ms.smove("srckey", "dstkey", "member");
    verify(ms).send(eq(Action.smove), (JSONObject) argument.capture());
    assertEquals("srckey", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("dstkey", getBody((JSONObject) argument.getValue()).getString("destination"));
    assertEquals("member", getBody((JSONObject) argument.getValue()).getString("member"));
  }

  @Test
  public void spopTest() throws JSONException {
    ms.spop("id");
    ms.spop("idd", 42);
    verify(ms, times(2)).send(eq(Action.spop), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals("idd", ((JSONObject)argument.getAllValues().get(1)).getString("_id"));
    assertEquals(42, getBody((JSONObject)argument.getAllValues().get(1)).getLong("count"));
  }

  @Test
  public void sremTest() throws JSONException {
    ms.srem("id", "member1", "member2");
    verify(ms).send(eq(Action.srem), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals("member1", ((String[])getBody((JSONObject) argument.getValue()).get("members"))[0]);
    assertEquals("member2", ((String[])getBody((JSONObject) argument.getValue()).get("members"))[1]);
  }

  @Test
  public void sunionstoreTest() throws JSONException {
    ms.sunionstore("dst", "key1", "key2");
    verify(ms).send(eq(Action.sunionstore), (JSONObject) argument.capture());
    assertEquals("dst", getBody((JSONObject)argument.getValue()).getString("destination"));
    assertEquals("key1", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[0]);
    assertEquals("key2", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[1]);
  }

  @Test
  public void unwatchTest() {
    ms.unwatch();
    verify(ms).send(eq(Action.unwatch), any(JSONObject.class));
  }

  @Test
  public void waitTest() throws JSONException {
    ms.wait(24, (long)42);
    verify(ms).send(eq(Action.wait), (JSONObject) argument.capture());
    assertEquals(24, getBody((JSONObject)argument.getValue()).getInt("numslaves"));
    assertEquals(42, getBody((JSONObject)argument.getValue()).getLong("timeout"));
  }

  @Test
  public void zcountTest() throws JSONException {
    ms.zcount("id", "min", 42);
    verify(ms).send(eq(Action.zcount), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals("min", getBody((JSONObject)argument.getValue()).get("min"));
    assertEquals(42, getBody((JSONObject)argument.getValue()).get("max"));
  }

  @Test
  public void zincrbyTest() throws JSONException {
    ms.zincrby("id", 42, "one");
    verify(ms).send(eq(Action.zincrby), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals(42, getBody((JSONObject)argument.getValue()).getDouble("value"), 2);
    assertEquals("one", getBody((JSONObject)argument.getValue()).get("member"));
  }

  @Test
  public void zinterstoreTest() throws JSONException {
    ms.zinterstore("out", new String[]{"zset1", "zset2"}, ZParams.Aggregate.MAX, 2, 3);
    verify(ms).send(eq(Action.zinterstore), (JSONObject) argument.capture());
    assertEquals("out", getBody((JSONObject)argument.getValue()).getString("destination"));
    assertEquals("zset1", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[0]);
    assertEquals("zset2", ((String[])getBody((JSONObject) argument.getValue()).get("keys"))[1]);
    assertEquals(ZParams.Aggregate.MAX.toString(), getBody((JSONObject)argument.getValue()).getString("aggregate"));
    assertEquals(2, ((Object[])getBody((JSONObject) argument.getValue()).get("weights"))[0]);
    assertEquals(3, ((Object[])getBody((JSONObject) argument.getValue()).get("weights"))[1]);
  }

  @Test
  public void zlexcountTest() throws JSONException {
    ms.zlexcount("id", 24, 42);
    verify(ms).send(eq(Action.zlexcount), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("min"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("max"));
  }

  @Test
  public void zrangeTest() throws JSONException {
    ms.zrange("id", 24, 42, true);
    verify(ms).send(eq(Action.zrange), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("start"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("stop"));
    assertEquals(true, getBody((JSONObject) argument.getValue()).getBoolean("withscores"));
  }

  @Test
  public void zrangebylexTest() throws JSONException {
    ms.zrangebylex("id", 24, 42, 0xff, 1337);
    verify(ms).send(eq(Action.zrangebylex), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("min"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("max"));
    assertEquals(0xff, getBody((JSONObject) argument.getValue()).getLong("offset"));
    assertEquals(1337, getBody((JSONObject) argument.getValue()).getLong("count"));
  }

  @Test
  public void zrangebyscoreTest() throws JSONException {
    ms.zrangebyscore("id", 24, 42, true, 0xff, 1337);
    verify(ms).send(eq(Action.zrangebylex), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("min"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("max"));
    assertEquals(true, getBody((JSONObject) argument.getValue()).getBoolean("withscores"));
    assertEquals(0xff, getBody((JSONObject) argument.getValue()).getLong("offset"));
    assertEquals(1337, getBody((JSONObject) argument.getValue()).getLong("count"));
  }

  @Test
  public void zremTest() throws JSONException {
    ms.zrem("id", "member1", "member2");
    verify(ms).send(eq(Action.zrem), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals("member1", ((String[])getBody((JSONObject) argument.getValue()).get("members"))[0]);
    assertEquals("member2", ((String[])getBody((JSONObject) argument.getValue()).get("members"))[1]);
  }

  @Test
  public void zremrangebylexTest() throws JSONException {
    ms.zremrangebylex("id", 24, 42, 0xff, 1337);
    verify(ms).send(eq(Action.zremrangebylex), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("min"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("max"));
    assertEquals(0xff, getBody((JSONObject) argument.getValue()).getLong("offset"));
    assertEquals(1337, getBody((JSONObject) argument.getValue()).getLong("count"));
  }

  @Test
  public void zrevrangebyscoreTest() throws JSONException {
    ms.zrevrangebyscore("id", 24, 42, true, 0xff, 1337);
    verify(ms).send(eq(Action.zrevrangebyscore), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("min"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("max"));
    assertEquals(true, getBody((JSONObject) argument.getValue()).getBoolean("withscores"));
    assertEquals(0xff, getBody((JSONObject) argument.getValue()).getLong("offset"));
    assertEquals(1337, getBody((JSONObject) argument.getValue()).getLong("count"));
  }

  @Test
  public void zrevrankTest() throws JSONException {
    ms.zrevrank("id", "member");
    verify(ms).send(eq(Action.zrevrank), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("member", getBody((JSONObject) argument.getValue()).getString("member"));
  }

  @Test
  public void incrbyTest() throws JSONException {
    ms.incrby("id", 42);
    verify(ms).send(eq(Action.incrby), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42, getBody((JSONObject)argument.getValue()).getLong("value"));
  }

  @Test
  public void incrbyfloatTest() throws JSONException {
    ms.incrbyfloat("id", 42.42);
    verify(ms).send(eq(Action.incrbyfloat), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(42.42, getBody((JSONObject)argument.getValue()).getDouble("value"), 2);
  }

  @Test
  public void brpopTest() throws JSONException {
    ms.brpop(new String[]{"1", "2"}, 42);
    verify(ms).send(eq(Action.brpop), (JSONObject) argument.capture());
    assertEquals("1",((String[])getBody((JSONObject) argument.getValue()).get("src"))[0]);
    assertEquals("2",((String[])getBody((JSONObject) argument.getValue()).get("src"))[1]);
    assertEquals(42,getBody((JSONObject) argument.getValue()).getLong("timeout"));
  }

  @Test
  public void testHget() throws JSONException {
    ms.hget("id", "field");
    verify(ms).send(eq(Action.hget), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("field", getBody((JSONObject) argument.getValue()).getString("field"));
  }

  @Test
  public void testHmget() throws JSONException {
    ms.hmget("id", "field1", "field2");
    verify(ms).send(eq(Action.hmget), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals("field1", ((String[])getBody((JSONObject) argument.getValue()).get("fields"))[0]);
    assertEquals("field2", ((String[])getBody((JSONObject) argument.getValue()).get("fields"))[1]);
  }

  @Test
  public void hsetnxTest() throws JSONException {
    ms.hsetnx("id", "field", "value");
    verify(ms).send(eq(Action.hsetnx), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("field", getBody((JSONObject) argument.getValue()).getString("field"));
    assertEquals("value", getBody((JSONObject) argument.getValue()).getString("value"));
  }

  @Test
  public void msetnxTest() throws JSONException {
    ms.msetnx("key1", "value1", "key2", "value2");
    verify(ms).send(eq(Action.msetnx), (JSONObject) argument.capture());
    assertEquals("key1", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[0]);
    assertEquals("value1", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[1]);
    assertEquals("key2", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[2]);
    assertEquals("value2", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[3]);
  }

  @Test
  public void rpushTest() throws JSONException {
    ms.rpush("id", "value1", "value2");
    verify(ms).send(eq(Action.rpush), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("value1", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[0]);
    assertEquals("value2", ((String[])getBody((JSONObject) argument.getValue()).get("values"))[1]);
  }

  @Test
  public void hincrbyfloatTest() throws JSONException {
    ms.hincrbyfloat("id", "field", 42.42);
    verify(ms).send(eq(Action.hincrbyfloat), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("field", getBody((JSONObject) argument.getValue()).getString("field"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getDouble("value"), 2);
  }

  @Test
  public void srandmemberTest() throws JSONException {
    ms.srandmember("idd", 42);
    verify(ms).send(eq(Action.srandmember), (JSONObject) argument.capture());
    assertEquals("idd", ((JSONObject)argument.getAllValues().get(0)).getString("_id"));
    assertEquals(42, getBody((JSONObject)argument.getAllValues().get(0)).getLong("count"));
  }

  @Test
  public void zrevrangeTest() throws JSONException {
    ms.zrevrange("id", 24, 42, true);
    verify(ms).send(eq(Action.zrevrange), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals(24, getBody((JSONObject) argument.getValue()).getLong("start"));
    assertEquals(42, getBody((JSONObject) argument.getValue()).getLong("stop"));
    assertEquals(true, getBody((JSONObject) argument.getValue()).getBoolean("withscores"));
  }

  @Test
  public void zscoreTest() throws JSONException {
    ms.zscore("id", "member");
    verify(ms).send(eq(Action.zscore), (JSONObject) argument.capture());
    assertEquals("id", ((JSONObject)argument.getValue()).getString("_id"));
    assertEquals("member", getBody((JSONObject) argument.getValue()).getString("member"));
  }

}
