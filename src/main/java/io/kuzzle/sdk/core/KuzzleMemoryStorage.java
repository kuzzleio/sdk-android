package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
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

public class KuzzleMemoryStorage implements KuzzleMemoryStorageCommands {

  private Kuzzle  kuzzle;
  private Kuzzle.QueryArgs  queryArgs = new Kuzzle.QueryArgs();
  private KuzzleResponseListener<JSONObject> listener;
  private KuzzleOptions options;

  public KuzzleMemoryStorage(@NonNull final Kuzzle kuzzle) {
    this.kuzzle = kuzzle;
  }

  protected KuzzleMemoryStorage send(@NonNull final Action action) {
    return send(action, null);
  }

  protected KuzzleMemoryStorage send(@NonNull final Action action, final JSONObject query) {
    queryArgs.controller = "ms";
    queryArgs.action = action.toString();
    try {
      kuzzle.query(queryArgs, (query == null ? new JSONObject() : query), options, new OnQueryDoneListener() {
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

  public KuzzleMemoryStorage setListener(final KuzzleResponseListener<JSONObject> listener) {
    this.listener = listener;
    return this;
  }

  public KuzzleMemoryStorage setOptions(final KuzzleOptions options) {
    this.options = options;
    return this;
  }

  @Override
  public KuzzleMemoryStorage append(final String key, final String value) {
    try {
      return send(Action.append, new JSONObject()
          .put("_id", key)
          .put("body", value));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage bgrewriteaof() {
    return send(Action.bgrewriteaof);
  }

  @Override
  public KuzzleMemoryStorage bgsave() {
    return send(Action.bgsave);
  }

  @Override
  public KuzzleMemoryStorage bitcount(final String key) {
    try {
      return send(Action.bitcount, new JSONObject().put("_id", key));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage bitcount(final String key, final long start, final long end) {
    try {
      return send(Action.bitcount, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
            .put("start", start)
            .put("end", end)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage bitop(final BitOP op, final String destKey, final String... srcKeys) {
    try {
      return send(Action.bitop, new JSONObject()
          .put("body", new JSONObject()
            .put("operation", op.toString())
            .put("destKey", destKey)
            .put("keys", srcKeys)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage bitpos(final String id, final long bit) {
    try {
      return bitpos(new JSONObject()
          .put("_id", id)
          .put("body", new JSONObject()
            .put("bit", bit)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage bitpos(final String id, final long bit, final long start) {
    try {
      return bitpos(new JSONObject()
          .put("_id", id)
          .put("body", new JSONObject()
            .put("bit", bit)
            .put("start", start)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage bitpos(final String id, final long bit, final long start, final long end) {
    try {
      return bitpos(new JSONObject()
          .put("_id", id)
          .put("body", new JSONObject()
            .put("bit", bit)
            .put("start", start)
            .put("end", end)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  private KuzzleMemoryStorage bitpos(final JSONObject query) {
    return send(Action.bitpos, query);
  }

  @Override
  public KuzzleMemoryStorage blpop(final String[] args, long timeout) {
    try {
      return send(Action.blpop, new JSONObject()
          .put("body", new JSONObject()
            .put("src", args)
            .put("timeout", timeout)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage brpoplpush(final String source, final String destination, final int timeout) {
    try {
      return send(Action.brpoplpush, new JSONObject()
          .put("body", new JSONObject()
            .put("source", source)
            .put("destination", destination)
            .put("timeout", timeout)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage dbsize() {
    return send(Action.dbsize);
  }

  @Override
  public KuzzleMemoryStorage decrby(final String key, final long integer) {
    try {
      return send(Action.decrby, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
            .put("value", integer)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage discard() {
    return send(Action.discard);
  }

  @Override
  public KuzzleMemoryStorage  exec() {
    return send(Action.exec);
  }

  @Override
  public KuzzleMemoryStorage expire(final String key, int seconds) {
    try {
      return send(Action.expire, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
            .put("seconds", seconds)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage expireat(final String key, final long timestamp) {
    try {
      return send(Action.expireat, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
            .put("timestamp", timestamp)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage flushdb() {
    return send(Action.flushdb);
  }

  @Override
  public KuzzleMemoryStorage getbit(final String key, final long offset) {
    try {
      return send(Action.getbit, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("offset", offset)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage getrange(final String key, final long startOffset, final long endOffset) {
    try {
      return send(Action.getrange, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("start", startOffset)
              .put("end", endOffset)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage hdel(final String key, final String... fields) {
    JSONObject query = new JSONObject();
    try {
      query.put("_id", key);
      query.put("body", new JSONObject()
          .put("fields", fields));
      return send(Action.hdel, query);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage hexists(final String key, final String field) {
    try {
      return send(Action.hexists, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("field", field)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage hincrby(final String key, final String field, final double value) {
    try {
      return send(Action.hincrby, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("field", field)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage hmset(final String key, final Map<String, String> hash) {
    JSONObject  query = new JSONObject();
    JSONObject  values = new JSONObject();
    try {
      query.put("_id", key);
      for(Map.Entry<String, String> entry : hash.entrySet()) {
        values.put(entry.getKey(), entry.getValue());
      }
      query.put("body", new JSONObject().put("fields", values));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return send(Action.hmset, query);
  }

  @Override
  public KuzzleMemoryStorage hset(final String key, final String field, final String value) {
    try {
      return send(Action.hset, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
            .put("field", field)
            .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage info(final String section) {
    try {
      return send(Action.info, new JSONObject().put("body", new JSONObject()
          .put("section", section)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage keys(final String pattern) {
    try {
      return send(Action.keys, new JSONObject().put("body", new JSONObject()
          .put("pattern", pattern)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage lastsave() {
    return send(Action.lastsave);
  }

  @Override
  public KuzzleMemoryStorage lindex(final String key, final long index) {
    try {
      return send(Action.lindex, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
            .put("idx", index)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage linsert(final String key, final Position where, final String pivot, final String value) {
    try {
      return send(Action.linsert, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("position", where.toString())
              .put("pivot", pivot)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage lpush(final String key, final String... values) {
    try {
      return send(Action.lpush, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
            .put("values", values)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage lrange(final String key, final long start, final long end) {
    try {
      return send(Action.lrange, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("start", start)
              .put("end", end)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage lrem(final String key, final long count, final String value) {
    try {
      return send(Action.lrem, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("count", count)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage lset(final String key, final long index, final String value) {
    try {
      return send(Action.lset, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("idx", index)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage ltrim(final String key, final long start, final long end) {
    try {
      return send(Action.lrange, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("start", start)
              .put("stop", end)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage mset(final String... keysvalues) {
    try {
      return send(Action.mset, new JSONObject()
          .put("body", new JSONObject()
            .put("values", keysvalues)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage multi() {
    return send(Action.multi);
  }

  @Override
  public KuzzleMemoryStorage object(final ObjectCommand subcommand, final String args) {
    try {
      return send(Action.object, new JSONObject()
        .put("body", new JSONObject()
          .put("subcommand", subcommand.toString())
          .put("args", args)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage pexpire(final String key, final long milliseconds) {
    try {
      return send(Action.pexpire, new JSONObject()
        .put("_id", key)
        .put("body", new JSONObject()
          .put("milliseconds", milliseconds)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage pexpireat(final String key, final long timestamp) {
    try {
      return send(Action.pexpireat, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("timestamp", timestamp)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage pfadd(final String key, final String... elements) {
    try {
      return send(Action.pfadd, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("elements", elements)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage pfmerge(final String destKey, final String... sourceKeys) {
    try {
      return send(Action.pfmerge, new JSONObject()
          .put("body", new JSONObject()
              .put("destkey", destKey)
              .put("sourcekeys", sourceKeys)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage ping() {
    return send(Action.ping);
  }

  @Override
  public KuzzleMemoryStorage psetex(final String key, final long milliseconds, final String value) {
    try {
      return send(Action.psetex, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("milliseconds", milliseconds)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage publish(final String channel, final String message) {
    try {
      return send(Action.publish, new JSONObject()
          .put("body", new JSONObject()
              .put("channel", channel)
              .put("message", message)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage randomkey() {
    return send(Action.randomkey);
  }

  @Override
  public KuzzleMemoryStorage rename(final String oldkey, final String newkey) {
    try {
      return send(Action.rename, new JSONObject()
          .put("_id", oldkey)
          .put("body", new JSONObject()
              .put("newkey", newkey)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage renamenx(final String oldkey, final String newkey) {
    try {
      return send(Action.renamenx, new JSONObject()
          .put("_id", oldkey)
          .put("body", new JSONObject()
              .put("newkey", newkey)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage restore(final String key, final long ttl, final String content) {
    try {
      return send(Action.restore, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("ttl", ttl)
              .put("content", content)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage rpoplpush(final String srckey, final String dstkey) {
    try {
      return send(Action.rpoplpush, new JSONObject()
          .put("body", new JSONObject()
              .put("source", srckey)
              .put("destination", dstkey)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage sadd(final String key, final String... members) {
    try {
      return send(Action.sadd, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("members", members)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage save() {
    return send(Action.save);
  }

  @Override
  public KuzzleMemoryStorage sdiffstore(final String dstkey, final String... keys) {
    try {
      return send(Action.sdiffstore, new JSONObject()
          .put("body", new JSONObject()
              .put("destination", dstkey)
              .put("keys", keys)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage set(final String key, final String value, final SetParams params) {
    JSONObject kuzzleQuery;
    JSONObject body = new JSONObject();
    try {
      kuzzleQuery = new JSONObject().put("_id", key);
      body.put("value", value);
      for (Map.Entry<String, Object> entry : params.getParams().entrySet()) {
        body.put(entry.getKey(), entry.getValue());
      }
      kuzzleQuery.put("body", body);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return send(Action.set, kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage setbit(final String key, final long offset, final Object value) {
    try {
      return send(Action.setbit, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("offset", offset)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage setex(final String key, final int seconds, final String value) {
    try {
      return send(Action.setbit, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("seconds", seconds)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage setrange(final String key, final long offset, final String value) {
    try {
      return send(Action.setrange, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("offset", offset)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage sinterstore(final String dstkey, final String... keys) {
    try {
      return send(Action.sinterstore, new JSONObject()
          .put("body", new JSONObject()
              .put("destination", dstkey)
              .put("keys", keys)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage sismember(final String key, final String member) {
    try {
      return send(Action.sinterstore, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("member", member)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage smove(final String srckey, final String dstkey, final String member) {
    try {
      return send(Action.smove, new JSONObject()
          .put("_id", srckey)
          .put("body", new JSONObject()
              .put("destination", dstkey)
              .put("member", member)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage spop(final String key) {
    try {
      return send(Action.spop, new JSONObject()
        .put("_id", key));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage spop(final String key, final long count) {
    try {
      return send(Action.spop, new JSONObject()
        .put("_id", key)
        .put("body", new JSONObject()
          .put("count", count)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage srem(final String key, final String... members) {
    try {
      return send(Action.srem, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("members", members)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage sunionstore(final String dstkey, final String... keys) {
    try {
      return send(Action.sunionstore, new JSONObject()
          .put("body", new JSONObject()
              .put("destination", dstkey)
              .put("keys", keys)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage unwatch() {
    return send(Action.unwatch);
  }

  @Override
  public KuzzleMemoryStorage wait(final int replicas, final long timeout) {
    try {
      return send(Action.wait, new JSONObject()
          .put("body", new JSONObject()
              .put("numslaves", replicas)
              .put("timeout", timeout)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zcount(final String key, final Object min, final Object max) {
    try {
      return send(Action.zcount, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
            .put("min", min)
            .put("max", max)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zincrby(final String key, final double score, final String member) {
    try {
      return send(Action.zincrby, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("value", score)
              .put("member", member)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zinterstore(final String destination, final String[] sets, final ZParams.Aggregate aggregate, final Object... weights) {
    try {
      return send(Action.zinterstore, new JSONObject()
          .put("body", new JSONObject()
            .put("destination", destination)
            .put("keys", sets)
            .put("aggregate", aggregate.toString())
            .put("weights", weights)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zlexcount(final String key, final long min, final long max) {
    try {
      return send(Action.zlexcount, new JSONObject()
        .put("_id", key)
        .put("body", new JSONObject()
          .put("min", min)
          .put("max", max)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zrange(final String key, final long start, final long end, final boolean withscores) {
    try {
      return send(Action.zrange, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("start", start)
              .put("stop", end)
              .put("withscores", withscores)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zrangebylex(final String key, final long min, final long max, final long offset, final long count) {
    try {
      return send(Action.zrangebylex, new JSONObject()
        .put("_id", key)
        .put("body", new JSONObject()
          .put("min", min)
          .put("max", max)
          .put("offset", offset)
          .put("count", count)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zrangebyscore(final String key, final long min, final long max, final boolean withscores, final long offset, final long count) {
    try {
      return send(Action.zrangebylex, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("min", min)
              .put("max", max)
              .put("withscores", withscores)
              .put("offset", offset)
              .put("count", count)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zrem(final String key, final String... members) {
    try {
      return send(Action.zrem, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("members", members)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zremrangebylex(final String key, final long min, final long max, final long offset, final long count) {
    try {
      return send(Action.zremrangebylex, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("min", min)
              .put("max", max)
              .put("offset", offset)
              .put("count", count)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zrevrangebyscore(final String key, final long min, final long max, final boolean withscores, final long offset, final long count) {
    try {
      return send(Action.zrevrangebyscore, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("min", min)
              .put("max", max)
              .put("withscores", withscores)
              .put("offset", offset)
              .put("count", count)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zrevrank(final String key, final String member) {
    try {
      return send(Action.zrevrank, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("member", member)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  // Unique Key argument methods

  private KuzzleMemoryStorage sendUniqueKeyArgument(final Action action, final String key) {
    try {
      return send(action, new JSONObject()
          .put("_id", key));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage decr(final String key) {
    return sendUniqueKeyArgument(Action.decr, key);
  }

  @Override
  public KuzzleMemoryStorage get(final String key) {
    return sendUniqueKeyArgument(Action.get, key);
  }

  @Override
  public KuzzleMemoryStorage dump(final String key) {
    return sendUniqueKeyArgument(Action.dump, key);
  }

  @Override
  public KuzzleMemoryStorage hgetall(final String key) {
    return sendUniqueKeyArgument(Action.hgetall, key);
  }

  @Override
  public KuzzleMemoryStorage hkeys(final String key) {
    return sendUniqueKeyArgument(Action.hkeys, key);
  }

  @Override
  public KuzzleMemoryStorage hlen(final String key) {
    return sendUniqueKeyArgument(Action.hlen, key);
  }

  @Override
  public KuzzleMemoryStorage hstrlen(final String key) {
    return sendUniqueKeyArgument(Action.hstrlen, key);
  }

  @Override
  public KuzzleMemoryStorage hvals(final String key) {
    return sendUniqueKeyArgument(Action.hvals, key);
  }

  @Override
  public KuzzleMemoryStorage incr(final String key) {
    return sendUniqueKeyArgument(Action.incr, key);
  }

  @Override
  public KuzzleMemoryStorage llen(final String key) {
    return sendUniqueKeyArgument(Action.llen, key);
  }

  @Override
  public KuzzleMemoryStorage lpop(final String key) {
    return sendUniqueKeyArgument(Action.lpop, key);
  }

  @Override
  public KuzzleMemoryStorage persist(final String key) {
    return sendUniqueKeyArgument(Action.persist, key);
  }

  @Override
  public KuzzleMemoryStorage pttl(final String key) {
    return sendUniqueKeyArgument(Action.pttl, key);
  }

  @Override
  public KuzzleMemoryStorage rpop(final String key) {
    return sendUniqueKeyArgument(Action.rpop, key);
  }

  @Override
  public KuzzleMemoryStorage scard(final String key) {
    return sendUniqueKeyArgument(Action.scard, key);
  }

  @Override
  public KuzzleMemoryStorage smembers(final String key) {
    return sendUniqueKeyArgument(Action.smembers, key);
  }

  @Override
  public KuzzleMemoryStorage strlen(final String key) {
    return sendUniqueKeyArgument(Action.strlen, key);
  }

  @Override
  public KuzzleMemoryStorage ttl(final String key) {
    return sendUniqueKeyArgument(Action.ttl, key);
  }

  @Override
  public KuzzleMemoryStorage type(final String key) {
    return sendUniqueKeyArgument(Action.type, key);
  }

  @Override
  public KuzzleMemoryStorage zcard(final String key) {
    return sendUniqueKeyArgument(Action.zcard, key);
  }

  @Override
  public KuzzleMemoryStorage getset(String key, String value) {
    try {
      return send(Action.getset, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage lpushx(String key, String value) {
    try {
      return send(Action.lpushx, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  // key key
  private KuzzleMemoryStorage keyKey(final Action action, final String... keys) {
    try {
      return send(action, new JSONObject()
          .put("body", new JSONObject()
              .put("keys", keys)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage del(final String... keys) {
    return keyKey(Action.del, keys);
  }

  @Override
  public KuzzleMemoryStorage mget(String... keys) {
    return keyKey(Action.mget, keys);
  }

  @Override
  public KuzzleMemoryStorage pfcount(String... keys) {
    return keyKey(Action.pfcount, keys);
  }

  @Override
  public KuzzleMemoryStorage exists(final String... keys) {
    return keyKey(Action.exists, keys);
  }

  @Override
  public KuzzleMemoryStorage sdiff(final String... keys) {
    return keyKey(Action.sdiff, keys);
  }

  @Override
  public KuzzleMemoryStorage sinter(final String... keys) {
    return keyKey(Action.sinter, keys);
  }

  @Override
  public KuzzleMemoryStorage sunion(final String... keys) {
    return keyKey(Action.sunion, keys);
  }

  @Override
  public KuzzleMemoryStorage watch(final String... keys) {
    return keyKey(Action.watch, keys);
  }

  @Override
  public KuzzleMemoryStorage incrby(String key, long value) {
    try {
      return send(Action.incrby, new JSONObject()
        .put("_id", key)
        .put("body", new JSONObject()
          .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage incrbyfloat(String key, double value) {
    try {
      return send(Action.incrbyfloat, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage brpop(final String[] args, long timeout) {
    try {
      return send(Action.brpop, new JSONObject()
          .put("body", new JSONObject()
              .put("src", args)
              .put("timeout", timeout)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage hget(final String key, final String field) {
    try {
      return send(Action.hget, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("field", field)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage hmget(final String key, final String... fields) {
    JSONObject query = new JSONObject();
    try {
      query.put("_id", key);
      query.put("body", new JSONObject()
          .put("fields", fields));
      return send(Action.hmget, query);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage hsetnx(final String key, final String field, final String value) {
    try {
      return send(Action.hsetnx, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("field", field)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage msetnx(final String... keysvalues) {
    try {
      return send(Action.msetnx, new JSONObject()
          .put("body", new JSONObject()
              .put("values", keysvalues)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage rpush(final String key, final String... values) {
    try {
      return send(Action.rpush, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("values", values)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage hincrbyfloat(final String key, final String field, final double value) {
    try {
      return send(Action.hincrbyfloat, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("field", field)
              .put("value", value)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage srandmember(final String key, final long count) {
    try {
      return send(Action.srandmember, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("count", count)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zrevrange(final String key, final long start, final long end, final boolean withscores) {
    try {
      return send(Action.zrevrange, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("start", start)
              .put("stop", end)
              .put("withscores", withscores)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage zscore(final String key, final String member) {
    try {
      return send(Action.zscore, new JSONObject()
          .put("_id", key)
          .put("body", new JSONObject()
              .put("member", member)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

}
