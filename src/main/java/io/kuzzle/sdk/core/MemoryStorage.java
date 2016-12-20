package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.util.KuzzleJSONObject;
import io.kuzzle.sdk.util.memoryStorage.Action;
import io.kuzzle.sdk.util.memoryStorage.BitOP;
import io.kuzzle.sdk.util.memoryStorage.KuzzleMemoryStorageCommands;
import io.kuzzle.sdk.util.memoryStorage.ObjectCommand;
import io.kuzzle.sdk.util.memoryStorage.Position;
import io.kuzzle.sdk.util.memoryStorage.SetParams;
import io.kuzzle.sdk.util.memoryStorage.ZParams;

/**
 * Kuzzle's memory storage is a separate data store from the database layer.
 * It is internaly based on Redis. You can access most of Redis functions (all
 * lowercased), excepting:
 *   * all cluster based functions
 *   * all script based functions
 *   * all cursors functions
 *
 */

public class MemoryStorage implements KuzzleMemoryStorageCommands {

  private Kuzzle  kuzzle;
  private Kuzzle.QueryArgs  queryArgs = new Kuzzle.QueryArgs();
  private KuzzleResponseListener<JSONObject> listener;
  private Options options;

  public MemoryStorage(@NonNull final Kuzzle kuzzle) {
    this.kuzzle = kuzzle;
  }

  protected MemoryStorage send(@NonNull final Action action) {
    return send(action, null);
  }

  protected MemoryStorage send(@NonNull final Action action, final KuzzleJSONObject query) {
    queryArgs.controller = "ms";
    queryArgs.action = action.toString();
    try {
      kuzzle.query(queryArgs, (query == null ? new KuzzleJSONObject() : query), options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            listener.onSuccess(response);
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public MemoryStorage setListener(final KuzzleResponseListener<JSONObject> listener) {
    this.listener = listener;
    return this;
  }

  public MemoryStorage setOptions(final Options options) {
    this.options = options;
    return this;
  }

  @Override
  public MemoryStorage append(final String key, final String value) {
    return send(Action.append, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", value));
  }

  @Override
  public MemoryStorage bgrewriteaof() {
    return send(Action.bgrewriteaof);
  }

  @Override
  public MemoryStorage bgsave() {
    return send(Action.bgsave);
  }

  @Override
  public MemoryStorage bitcount(final String key) {
    return send(Action.bitcount, new KuzzleJSONObject().put("_id", key));
  }

  @Override
  public MemoryStorage bitcount(final String key, final long start, final long end) {
      return send(Action.bitcount, new KuzzleJSONObject()
          .put("_id", key)
          .put("body", new KuzzleJSONObject()
            .put("start", start)
            .put("end", end)));
  }

  @Override
  public MemoryStorage bitop(final BitOP op, final String destKey, final String... srcKeys) {
    return send(Action.bitop, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
          .put("operation", op.toString())
          .put("destKey", destKey)
          .put("keys", srcKeys)));
  }

  @Override
  public MemoryStorage bitpos(final String id, final long bit) {
    return bitpos(new KuzzleJSONObject()
        .put("_id", id)
        .put("body", new KuzzleJSONObject()
          .put("bit", bit)));
  }

  @Override
  public MemoryStorage bitpos(final String id, final long bit, final long start) {
    return bitpos(new KuzzleJSONObject()
        .put("_id", id)
        .put("body", new KuzzleJSONObject()
          .put("bit", bit)
          .put("start", start)));
  }

  @Override
  public MemoryStorage bitpos(final String id, final long bit, final long start, final long end) {
    return bitpos(new KuzzleJSONObject()
        .put("_id", id)
        .put("body", new KuzzleJSONObject()
          .put("bit", bit)
          .put("start", start)
          .put("end", end)));
  }

  private MemoryStorage bitpos(final KuzzleJSONObject query) {
    return send(Action.bitpos, query);
  }

  @Override
  public MemoryStorage blpop(final String[] args, long timeout) {
    return send(Action.blpop, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
          .put("src", args)
          .put("timeout", timeout)));
  }

  @Override
  public MemoryStorage brpoplpush(final String source, final String destination, final int timeout) {
    return send(Action.brpoplpush, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
          .put("source", source)
          .put("destination", destination)
          .put("timeout", timeout)));
  }

  @Override
  public MemoryStorage dbsize() {
    return send(Action.dbsize);
  }

  @Override
  public MemoryStorage decrby(final String key, final long integer) {
    return send(Action.decrby, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
          .put("value", integer)));
  }

  @Override
  public MemoryStorage discard() {
    return send(Action.discard);
  }

  @Override
  public MemoryStorage exec() {
    return send(Action.exec);
  }

  @Override
  public MemoryStorage expire(final String key, int seconds) {
    return send(Action.expire, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
          .put("seconds", seconds)));
  }

  @Override
  public MemoryStorage expireat(final String key, final long timestamp) {
    return send(Action.expireat, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
          .put("timestamp", timestamp)));
  }

  @Override
  public MemoryStorage flushdb() {
    return send(Action.flushdb);
  }

  @Override
  public MemoryStorage getbit(final String key, final long offset) {
    return send(Action.getbit, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("offset", offset)));
  }

  @Override
  public MemoryStorage getrange(final String key, final long startOffset, final long endOffset) {
    return send(Action.getrange, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("start", startOffset)
            .put("end", endOffset)));
  }

  @Override
  public MemoryStorage hdel(final String key, final String... fields) {
    KuzzleJSONObject query = new KuzzleJSONObject();
    query.put("_id", key);
    query.put("body", new KuzzleJSONObject()
        .put("fields", fields));
    return send(Action.hdel, query);
  }

  @Override
  public MemoryStorage hexists(final String key, final String field) {
    return send(Action.hexists, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("field", field)));
  }

  @Override
  public MemoryStorage hincrby(final String key, final String field, final double value) {
    return send(Action.hincrby, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("field", field)
            .put("value", value)));
  }

  @Override
  public MemoryStorage hmset(final String key, final Map<String, String> hash) {
    KuzzleJSONObject  query = new KuzzleJSONObject();
    KuzzleJSONObject  values = new KuzzleJSONObject();
    query.put("_id", key);
    for(Map.Entry<String, String> entry : hash.entrySet()) {
      values.put(entry.getKey(), entry.getValue());
    }
    query.put("body", new KuzzleJSONObject().put("fields", values));
    return send(Action.hmset, query);
  }

  @Override
  public MemoryStorage hset(final String key, final String field, final String value) {
    return send(Action.hset, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
          .put("field", field)
          .put("value", value)));
  }

  @Override
  public MemoryStorage info(final String section) {
    return send(Action.info, new KuzzleJSONObject().put("body", new KuzzleJSONObject()
        .put("section", section)));
  }

  @Override
  public MemoryStorage keys(final String pattern) {
    return send(Action.keys, new KuzzleJSONObject().put("body", new KuzzleJSONObject()
        .put("pattern", pattern)));
  }

  @Override
  public MemoryStorage lastsave() {
    return send(Action.lastsave);
  }

  @Override
  public MemoryStorage lindex(final String key, final long index) {
    return send(Action.lindex, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
          .put("idx", index)));
  }

  @Override
  public MemoryStorage linsert(final String key, final Position where, final String pivot, final String value) {
    return send(Action.linsert, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("position", where.toString())
            .put("pivot", pivot)
            .put("value", value)));
  }

  @Override
  public MemoryStorage lpush(final String key, final String... values) {
    return send(Action.lpush, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
          .put("values", values)));
  }

  @Override
  public MemoryStorage lrange(final String key, final long start, final long end) {
    return send(Action.lrange, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("start", start)
            .put("end", end)));
  }

  @Override
  public MemoryStorage lrem(final String key, final long count, final String value) {
    return send(Action.lrem, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("count", count)
            .put("value", value)));
  }

  @Override
  public MemoryStorage lset(final String key, final long index, final String value) {
    return send(Action.lset, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("idx", index)
            .put("value", value)));
  }

  @Override
  public MemoryStorage ltrim(final String key, final long start, final long end) {
    return send(Action.ltrim, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("start", start)
            .put("stop", end)));
  }

  @Override
  public MemoryStorage mset(final String... keysvalues) {
    return send(Action.mset, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
          .put("values", keysvalues)));
  }

  @Override
  public MemoryStorage multi() {
    return send(Action.multi);
  }

  @Override
  public MemoryStorage object(final ObjectCommand subcommand, final String args) {
    return send(Action.object, new KuzzleJSONObject()
      .put("body", new KuzzleJSONObject()
        .put("subcommand", subcommand.toString())
        .put("args", args)));
  }

  @Override
  public MemoryStorage pexpire(final String key, final long milliseconds) {
    return send(Action.pexpire, new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("milliseconds", milliseconds)));
  }

  @Override
  public MemoryStorage pexpireat(final String key, final long timestamp) {
    return send(Action.pexpireat, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("timestamp", timestamp)));
  }

  @Override
  public MemoryStorage pfadd(final String key, final String... elements) {
    return send(Action.pfadd, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("elements", elements)));
  }

  @Override
  public MemoryStorage pfmerge(final String destKey, final String... sourceKeys) {
    return send(Action.pfmerge, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("destkey", destKey)
            .put("sourcekeys", sourceKeys)));
  }

  @Override
  public MemoryStorage ping() {
    return send(Action.ping);
  }

  @Override
  public MemoryStorage psetex(final String key, final long milliseconds, final String value) {
    return send(Action.psetex, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("milliseconds", milliseconds)
            .put("value", value)));
  }

  @Override
  public MemoryStorage publish(final String channel, final String message) {
    return send(Action.publish, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("channel", channel)
            .put("message", message)));
  }

  @Override
  public MemoryStorage randomkey() {
    return send(Action.randomkey);
  }

  @Override
  public MemoryStorage rename(final String oldkey, final String newkey) {
    return send(Action.rename, new KuzzleJSONObject()
        .put("_id", oldkey)
        .put("body", new KuzzleJSONObject()
            .put("newkey", newkey)));
  }

  @Override
  public MemoryStorage renamenx(final String oldkey, final String newkey) {
    return send(Action.renamenx, new KuzzleJSONObject()
        .put("_id", oldkey)
        .put("body", new KuzzleJSONObject()
            .put("newkey", newkey)));
  }

  @Override
  public MemoryStorage restore(final String key, final long ttl, final String content) {
    return send(Action.restore, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("ttl", ttl)
            .put("content", content)));
  }

  @Override
  public MemoryStorage rpoplpush(final String srckey, final String dstkey) {
    return send(Action.rpoplpush, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("source", srckey)
            .put("destination", dstkey)));
  }

  @Override
  public MemoryStorage sadd(final String key, final String... members) {
    return send(Action.sadd, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("members", members)));
  }

  @Override
  public MemoryStorage save() {
    return send(Action.save);
  }

  @Override
  public MemoryStorage sdiffstore(final String dstkey, final String... keys) {
    return send(Action.sdiffstore, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("destination", dstkey)
            .put("keys", keys)));
  }

  @Override
  public MemoryStorage set(final String key, final String value, final SetParams params) {
    KuzzleJSONObject kuzzleQuery;
    KuzzleJSONObject body = new KuzzleJSONObject();
    kuzzleQuery = new KuzzleJSONObject().put("_id", key);
    body.put("value", value);
    for (Map.Entry<String, Object> entry : params.getParams().entrySet()) {
      body.put(entry.getKey(), entry.getValue());
    }
    kuzzleQuery.put("body", body);
    return send(Action.set, kuzzleQuery);
  }

  @Override
  public MemoryStorage setbit(final String key, final long offset, final Object value) {
    return send(Action.setbit, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("offset", offset)
            .put("value", value)));
  }

  @Override
  public MemoryStorage setex(final String key, final int seconds, final String value) {
    return send(Action.setex, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("seconds", seconds)
            .put("value", value)));
  }

  @Override
  public MemoryStorage setrange(final String key, final long offset, final String value) {
    return send(Action.setrange, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("offset", offset)
            .put("value", value)));
  }

  @Override
  public MemoryStorage sinterstore(final String dstkey, final String... keys) {
    return send(Action.sinterstore, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("destination", dstkey)
            .put("keys", keys)));
  }

  @Override
  public MemoryStorage sismember(final String key, final String member) {
    return send(Action.sismember, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("member", member)));
  }

  @Override
  public MemoryStorage smove(final String srckey, final String dstkey, final String member) {
    return send(Action.smove, new KuzzleJSONObject()
        .put("_id", srckey)
        .put("body", new KuzzleJSONObject()
            .put("destination", dstkey)
            .put("member", member)));
  }

  @Override
  public MemoryStorage spop(final String key) {
    return send(Action.spop, new KuzzleJSONObject()
      .put("_id", key));
  }

  @Override
  public MemoryStorage spop(final String key, final long count) {
    return send(Action.spop, new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("count", count)));
  }

  @Override
  public MemoryStorage srem(final String key, final String... members) {
    return send(Action.srem, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("members", members)));
  }

  @Override
  public MemoryStorage sunionstore(final String dstkey, final String... keys) {
    return send(Action.sunionstore, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("destination", dstkey)
            .put("keys", keys)));
  }

  @Override
  public MemoryStorage unwatch() {
    return send(Action.unwatch);
  }

  @Override
  public MemoryStorage wait(final int replicas, final long timeout) {
    return send(Action.wait, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("numslaves", replicas)
            .put("timeout", timeout)));
  }

  @Override
  public MemoryStorage zcount(final String key, final Object min, final Object max) {
    return send(Action.zcount, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
          .put("min", min)
          .put("max", max)));
  }

  @Override
  public MemoryStorage zincrby(final String key, final double score, final String member) {
    return send(Action.zincrby, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("value", score)
            .put("member", member)));
  }

  @Override
  public MemoryStorage zinterstore(final String destination, final String[] sets, final ZParams.Aggregate aggregate, final Object... weights) {
    return send(Action.zinterstore, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
          .put("destination", destination)
          .put("keys", sets)
          .put("aggregate", aggregate.toString())
          .put("weights", weights)));
  }

  @Override
  public MemoryStorage zlexcount(final String key, final long min, final long max) {
    return send(Action.zlexcount, new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("min", min)
        .put("max", max)));
  }

  @Override
  public MemoryStorage zrange(final String key, final long start, final long end, final boolean withscores) {
    return send(Action.zrange, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("start", start)
            .put("stop", end)
            .put("withscores", withscores)));
  }

  @Override
  public MemoryStorage zrangebylex(final String key, final long min, final long max, final long offset, final long count) {
    return send(Action.zrangebylex, new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("min", min)
        .put("max", max)
        .put("offset", offset)
        .put("count", count)));
  }

  @Override
  public MemoryStorage zrangebyscore(final String key, final long min, final long max, final boolean withscores, final long offset, final long count) {
    return send(Action.zrangebyscore, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("min", min)
            .put("max", max)
            .put("withscores", withscores)
            .put("offset", offset)
            .put("count", count)));
  }

  @Override
  public MemoryStorage zrem(final String key, final String... members) {
    return send(Action.zrem, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("members", members)));
  }

  @Override
  public MemoryStorage zremrangebylex(final String key, final long min, final long max, final long offset, final long count) {
    return send(Action.zremrangebylex, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("min", min)
            .put("max", max)
            .put("offset", offset)
            .put("count", count)));
  }

  @Override
  public MemoryStorage zrevrangebyscore(final String key, final long min, final long max, final boolean withscores, final long offset, final long count) {
    return send(Action.zrevrangebyscore, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("min", min)
            .put("max", max)
            .put("withscores", withscores)
            .put("offset", offset)
            .put("count", count)));
  }

  @Override
  public MemoryStorage zrevrank(final String key, final String member) {
    return send(Action.zrevrank, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("member", member)));
  }

  // Unique Key argument methods

  private MemoryStorage sendUniqueKeyArgument(final Action action, final String key) {
    return send(action, new KuzzleJSONObject()
        .put("_id", key));
  }

  @Override
  public MemoryStorage decr(final String key) {
    return sendUniqueKeyArgument(Action.decr, key);
  }

  @Override
  public MemoryStorage get(final String key) {
    return sendUniqueKeyArgument(Action.get, key);
  }

  @Override
  public MemoryStorage dump(final String key) {
    return sendUniqueKeyArgument(Action.dump, key);
  }

  @Override
  public MemoryStorage hgetall(final String key) {
    return sendUniqueKeyArgument(Action.hgetall, key);
  }

  @Override
  public MemoryStorage hkeys(final String key) {
    return sendUniqueKeyArgument(Action.hkeys, key);
  }

  @Override
  public MemoryStorage hlen(final String key) {
    return sendUniqueKeyArgument(Action.hlen, key);
  }

  @Override
  public MemoryStorage hstrlen(final String key) {
    return sendUniqueKeyArgument(Action.hstrlen, key);
  }

  @Override
  public MemoryStorage hvals(final String key) {
    return sendUniqueKeyArgument(Action.hvals, key);
  }

  @Override
  public MemoryStorage incr(final String key) {
    return sendUniqueKeyArgument(Action.incr, key);
  }

  @Override
  public MemoryStorage llen(final String key) {
    return sendUniqueKeyArgument(Action.llen, key);
  }

  @Override
  public MemoryStorage lpop(final String key) {
    return sendUniqueKeyArgument(Action.lpop, key);
  }

  @Override
  public MemoryStorage persist(final String key) {
    return sendUniqueKeyArgument(Action.persist, key);
  }

  @Override
  public MemoryStorage pttl(final String key) {
    return sendUniqueKeyArgument(Action.pttl, key);
  }

  @Override
  public MemoryStorage rpop(final String key) {
    return sendUniqueKeyArgument(Action.rpop, key);
  }

  @Override
  public MemoryStorage scard(final String key) {
    return sendUniqueKeyArgument(Action.scard, key);
  }

  @Override
  public MemoryStorage smembers(final String key) {
    return sendUniqueKeyArgument(Action.smembers, key);
  }

  @Override
  public MemoryStorage strlen(final String key) {
    return sendUniqueKeyArgument(Action.strlen, key);
  }

  @Override
  public MemoryStorage ttl(final String key) {
    return sendUniqueKeyArgument(Action.ttl, key);
  }

  @Override
  public MemoryStorage type(final String key) {
    return sendUniqueKeyArgument(Action.type, key);
  }

  @Override
  public MemoryStorage zcard(final String key) {
    return sendUniqueKeyArgument(Action.zcard, key);
  }

  @Override
  public MemoryStorage getset(String key, String value) {
    return send(Action.getset, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("value", value)));
  }

  @Override
  public MemoryStorage lpushx(String key, String value) {
    return send(Action.lpushx, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("value", value)));
  }

  // key key
  private MemoryStorage keyKey(final Action action, final String... keys) {
    return send(action, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("keys", keys)));
  }

  @Override
  public MemoryStorage del(final String... keys) {
    return keyKey(Action.del, keys);
  }

  @Override
  public MemoryStorage mget(String... keys) {
    return keyKey(Action.mget, keys);
  }

  @Override
  public MemoryStorage pfcount(String... keys) {
    return keyKey(Action.pfcount, keys);
  }

  @Override
  public MemoryStorage exists(final String... keys) {
    return keyKey(Action.exists, keys);
  }

  @Override
  public MemoryStorage sdiff(final String... keys) {
    return keyKey(Action.sdiff, keys);
  }

  @Override
  public MemoryStorage sinter(final String... keys) {
    return keyKey(Action.sinter, keys);
  }

  @Override
  public MemoryStorage sunion(final String... keys) {
    return keyKey(Action.sunion, keys);
  }

  @Override
  public MemoryStorage watch(final String... keys) {
    return keyKey(Action.watch, keys);
  }

  @Override
  public MemoryStorage incrby(String key, long value) {
    return send(Action.incrby, new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)));
  }

  @Override
  public MemoryStorage incrbyfloat(String key, double value) {
    return send(Action.incrbyfloat, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("value", value)));
  }

  @Override
  public MemoryStorage brpop(final String[] args, long timeout) {
    return send(Action.brpop, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("src", args)
            .put("timeout", timeout)));
  }

  @Override
  public MemoryStorage hget(final String key, final String field) {
    return send(Action.hget, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("field", field)));
  }

  @Override
  public MemoryStorage hmget(final String key, final String... fields) {
    KuzzleJSONObject query = new KuzzleJSONObject();
    query.put("_id", key);
    query.put("body", new KuzzleJSONObject()
        .put("fields", fields));
    return send(Action.hmget, query);
  }

  @Override
  public MemoryStorage hsetnx(final String key, final String field, final String value) {
    return send(Action.hsetnx, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("field", field)
            .put("value", value)));
  }

  @Override
  public MemoryStorage msetnx(final String... keysvalues) {
    return send(Action.msetnx, new KuzzleJSONObject()
        .put("body", new KuzzleJSONObject()
            .put("values", keysvalues)));
  }

  @Override
  public MemoryStorage rpush(final String key, final String... values) {
    return send(Action.rpush, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("values", values)));
  }

  @Override
  public MemoryStorage hincrbyfloat(final String key, final String field, final double value) {
    return send(Action.hincrbyfloat, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("field", field)
            .put("value", value)));
  }

  @Override
  public MemoryStorage srandmember(final String key, final long count) {
    return send(Action.srandmember, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("count", count)));
  }

  @Override
  public MemoryStorage zrevrange(final String key, final long start, final long end, final boolean withscores) {
    return send(Action.zrevrange, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("start", start)
            .put("stop", end)
            .put("withscores", withscores)));
  }

  @Override
  public MemoryStorage zscore(final String key, final String member) {
    return send(Action.zscore, new KuzzleJSONObject()
        .put("_id", key)
        .put("body", new KuzzleJSONObject()
            .put("member", member)));
  }

}
