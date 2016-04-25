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
import io.kuzzle.sdk.util.memoryStorage.Position;

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
  public KuzzleMemoryStorage bitpos(String id, long bit) {
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
  public KuzzleMemoryStorage bitpos(String id, long bit, long start) {
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
  public KuzzleMemoryStorage blpop(final String key, long timeout) {
    try {
      return send(Action.blpop, new JSONObject()
          .put("body", new JSONObject()
              .put("_id", key)
              .put("timeout", timeout)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
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
  public KuzzleMemoryStorage decrby(String key, long integer) {
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
  public KuzzleMemoryStorage del(final String... keys) {
    JSONObject query = new JSONObject();
    try {
      if (keys.length == 1) {
        query.put("_id", keys[0]);
      } else {
        query.put("body", new JSONObject()
          .put("keys", keys));
      }
      return send(Action.del, query);
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
  public KuzzleMemoryStorage exists(final String key) {
    try {
      return send(Action.exists, new JSONObject()
        .put("_id", key));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KuzzleMemoryStorage exists(final String... keys) {
    try {
      return send(Action.exists, new JSONObject()
          .put("body", new JSONObject()
            .put("keys", keys)));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
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
      if (fields.length == 1) {
        query.put("body", new JSONObject()
            .put("field", fields[0]));
      } else {
        query.put("body", new JSONObject()
            .put("fields", fields));
      }
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
  public KuzzleMemoryStorage lrem(final String key, final long count, String value) {
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
  public KuzzleMemoryStorage lset(String key, long index, String value) {
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
  public KuzzleMemoryStorage ltrim(String key, long start, long end) {
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
  public KuzzleMemoryStorage mset(String... keysvalues) {
    return null;
  }

  /*
  @Override
  public KuzzleMemoryStorage set(final String key, final String value) {
    return send(Action., putKeyValue(key, value));
  }

  @Override
  public KuzzleMemoryStorage set(final String key, final String value, final SetParams params) {
    JSONObject kuzzleQuery;
    try {
      kuzzleQuery = new JSONObject().put(key, value);
      for (Map.Entry<String, Object> entry : params.getParams().entrySet()) {
        kuzzleQuery.put(entry.getKey(), entry.getValue());
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return send(Action., kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage get(String key) {

  }

  @Override
  public KuzzleMemoryStorage exists(String key) {

  }

  @Override
  public KuzzleMemoryStorage exists(String... keys) {

  }

  @Override
  public KuzzleMemoryStorage del(String... keys) {

  }

  @Override
  public KuzzleMemoryStorage type(String key) {

  }

  @Override
  public KuzzleMemoryStorage keys(String pattern) {

  }

  @Override
  public KuzzleMemoryStorage rename(String oldkey, String newkey) {

  }

  @Override
  public KuzzleMemoryStorage renamenx(String oldkey, String newkey) {

  }

  @Override
  public KuzzleMemoryStorage expire(String key, int seconds) {
    putKeyValue(key, seconds);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage expireAt(String key, long unixTime) {
    putKeyValue(key, unixTime);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage ttl(String key) {

  }

  @Override
  public KuzzleMemoryStorage setbit(String key, long offset, boolean value) {

  }

  @Override
  public KuzzleMemoryStorage setbit(String key, long offset, String value) {

  }

  @Override
  public KuzzleMemoryStorage getbit(String key, long offset) {
    putKeyValue(key, offset);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage setrange(String key, long offset, String value) {

  }

  @Override
  public KuzzleMemoryStorage getrange(String key, long startOffset, long endOffset) {

  }

  @Override
  public KuzzleMemoryStorage getSet(String key, String value) {
    putKeyValue(key, value);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage mget(String... keys) {

  }

  @Override
  public KuzzleMemoryStorage setnx(String key, String value) {
    putKeyValue(key, value);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage setex(String key, int seconds, String value) {

  }

  @Override
  public KuzzleMemoryStorage mset(String... keysvalues) {

  }

  @Override
  public KuzzleMemoryStorage msetnx(String... keysvalues) {

  }

  @Override
  public KuzzleMemoryStorage decrBy(String key, long integer) {
    putKeyValue(key, integer);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage decr(String key) {

  }

  @Override
  public KuzzleMemoryStorage incrBy(String key, long integer) {
    putKeyValue(key, integer);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage incrByFloat(String key, double value) {
    putKeyValue(key, value);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage incr(String key) {

  }

  @Override
  public KuzzleMemoryStorage substr(String key, int start, int end) {

  }

  @Override
  public KuzzleMemoryStorage hset(String key, String field, String value) {

  }

  @Override
  public KuzzleMemoryStorage hget(String key, String field) {
    putKeyValue(key, field);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage hsetnx(String key, String field, String value) {

  }

  @Override
  public KuzzleMemoryStorage hmset(String key, Map<String, String> hash) {

  }

  @Override
  public KuzzleMemoryStorage hmget(String key, String... fields) {

  }

  @Override
  public KuzzleMemoryStorage hincrBy(String key, String field, long value) {

  }

  @Override
  public KuzzleMemoryStorage hincrByFloat(String key, String field, double value) {

  }

  @Override
  public KuzzleMemoryStorage hexists(String key, String field) {
    putKeyValue(key, field);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage hdel(String key, String... fields) {

  }

  @Override
  public KuzzleMemoryStorage hlen(String key) {

  }

  @Override
  public KuzzleMemoryStorage hkeys(String key) {

  }

  @Override
  public KuzzleMemoryStorage hvals(String key) {

  }

  @Override
  public KuzzleMemoryStorage hgetAll(String key) {

  }

  @Override
  public KuzzleMemoryStorage rpush(String key, String... strings) {

  }

  @Override
  public KuzzleMemoryStorage lpush(String key, String... strings) {

  }

  @Override
  public KuzzleMemoryStorage llen(String key) {

  }

  @Override
  public KuzzleMemoryStorage lrange(String key, long start, long end) {

  }

  @Override
  public KuzzleMemoryStorage ltrim(String key, long start, long end) {

  }

  @Override
  public KuzzleMemoryStorage lindex(String key, long index) {
    putKeyValue(key, index);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage lset(String key, long index, String value) {

  }

  @Override
  public KuzzleMemoryStorage lrem(String key, long count, String value) {

  }

  @Override
  public KuzzleMemoryStorage lpop(String key) {

  }

  @Override
  public KuzzleMemoryStorage rpop(String key) {

  }

  @Override
  public KuzzleMemoryStorage rpoplpush(String srckey, String dstkey) {

  }

  @Override
  public KuzzleMemoryStorage sadd(String key, String... members) {

  }

  @Override
  public KuzzleMemoryStorage smembers(String key) {

  }

  @Override
  public KuzzleMemoryStorage srem(String key, String... member) {

  }

  @Override
  public KuzzleMemoryStorage spop(String key) {

  }

  @Override
  public KuzzleMemoryStorage spop(String key, long count) {
    putKeyValue(key, count);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage smove(String srckey, String dstkey, String member) {

  }

  @Override
  public KuzzleMemoryStorage scard(String key) {

  }

  @Override
  public KuzzleMemoryStorage sismember(String key, String member) {
    putKeyValue(key, member);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage sinter(String... keys) {

  }

  @Override
  public KuzzleMemoryStorage sinterstore(String dstkey, String... keys) {

  }

  @Override
  public KuzzleMemoryStorage sunion(String... keys) {

  }

  @Override
  public KuzzleMemoryStorage sunionstore(String dstkey, String... keys) {

  }

  @Override
  public KuzzleMemoryStorage sdiff(String... keys) {

  }

  @Override
  public KuzzleMemoryStorage sdiffstore(String dstkey, String... keys) {

  }

  @Override
  public KuzzleMemoryStorage srandmember(String key) {

  }

  @Override
  public KuzzleMemoryStorage zadd(String key, double score, String member) {

  }

  @Override
  public KuzzleMemoryStorage zadd(String key, double score, String member, ZAddParams params) {

  }

  @Override
  public KuzzleMemoryStorage zadd(String key, Map<String, Double> scoreMembers) {

  }

  @Override
  public KuzzleMemoryStorage zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {

  }

  @Override
  public KuzzleMemoryStorage zrange(String key, long start, long end) {

  }

  @Override
  public KuzzleMemoryStorage zrem(String key, String... members) {

  }

  @Override
  public KuzzleMemoryStorage zincrby(String key, double score, String member) {

  }

  @Override
  public KuzzleMemoryStorage zincrby(String key, double score, String member, ZIncrByParams params) {

  }

  @Override
  public KuzzleMemoryStorage zrank(String key, String member) {
    putKeyValue(key, member);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage zrevrank(String key, String member) {
    putKeyValue(key, member);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage zrevrange(String key, long start, long end) {

  }

  @Override
  public KuzzleMemoryStorage zrangeWithScores(String key, long start, long end) {

  }

  @Override
  public KuzzleMemoryStorage zrevrangeWithScores(String key, long start, long end) {

  }

  @Override
  public KuzzleMemoryStorage zcard(String key) {

  }

  @Override
  public KuzzleMemoryStorage zscore(String key, String member) {
    putKeyValue(key, member);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage watch(String... keys) {

  }

  @Override
  public KuzzleMemoryStorage sort(String key) {

  }

  @Override
  public KuzzleMemoryStorage sort(String key, SortingParams sortingParameters) {
    putKeyValue(key, sortingParameters);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage blpop(String[] args) {

  }

  @Override
  public KuzzleMemoryStorage sort(String key, SortingParams sortingParameters, String dstkey) {

  }

  @Override
  public KuzzleMemoryStorage sort(String key, String dstkey) {
    putKeyValue(key, dstkey);
    return send(Thread.currentThread().getStackTrace()[1].getClassName(), kuzzleQuery);
  }

  @Override
  public KuzzleMemoryStorage brpop(String[] args) {

  }

  @Override
  public KuzzleMemoryStorage brpoplpush(String source, String destination, int timeout) {

  }

  @Override
  public KuzzleMemoryStorage zcount(String key, double min, double max) {

  }

  @Override
  public KuzzleMemoryStorage zcount(String key, String min, String max) {

  }

  @Override
  public KuzzleMemoryStorage zrangeByScore(String key, double min, double max) {

  }

  @Override
  public KuzzleMemoryStorage zrangeByScore(String key, String min, String max) {

  }

  @Override
  public KuzzleMemoryStorage zrangeByScore(String key, double min, double max, int offset, int count) {

  }

  @Override
  public KuzzleMemoryStorage zrangeByScoreWithScores(String key, double min, double max) {

  }

  @Override
  public KuzzleMemoryStorage zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {

  }

  @Override
  public KuzzleMemoryStorage zrangeByScoreWithScores(String key, String min, String max) {

  }

  @Override
  public KuzzleMemoryStorage zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {

  }

  @Override
  public KuzzleMemoryStorage zrevrangeByScore(String key, double max, double min) {

  }

  @Override
  public KuzzleMemoryStorage zrevrangeByScore(String key, String max, String min) {

  }

  @Override
  public KuzzleMemoryStorage zrevrangeByScore(String key, double max, double min, int offset, int count) {

  }

  @Override
  public KuzzleMemoryStorage zrevrangeByScoreWithScores(String key, double max, double min) {

  }

  @Override
  public KuzzleMemoryStorage zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {

  }

  @Override
  public KuzzleMemoryStorage zrevrangeByScoreWithScores(String key, String max, String min) {

  }

  @Override
  public KuzzleMemoryStorage zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {

  }

  @Override
  public KuzzleMemoryStorage zremrangeByRank(String key, long start, long end) {

  }

  @Override
  public KuzzleMemoryStorage zremrangeByScore(String key, double start, double end) {

  }

  @Override
  public KuzzleMemoryStorage zremrangeByScore(String key, String start, String end) {

  }

  @Override
  public KuzzleMemoryStorage zunionstore(String dstkey, String... sets) {

  }

  @Override
  public KuzzleMemoryStorage zunionstore(String dstkey, ZParams params, String... sets) {

  }

  @Override
  public KuzzleMemoryStorage zinterstore(String dstkey, String... sets) {

  }

  @Override
  public KuzzleMemoryStorage zinterstore(String dstkey, ZParams params, String... sets) {

  }

  @Override
  public KuzzleMemoryStorage strlen(String key) {

  }

  @Override
  public KuzzleMemoryStorage lpushx(String key, String... string) {

  }

  @Override
  public KuzzleMemoryStorage persist(String key) {

  }

  @Override
  public KuzzleMemoryStorage rpushx(String key, String... string) {

  }

  @Override
  public KuzzleMemoryStorage linsert(String key, LIST_POSITION where, String pivot, String value) {

  }

  @Override
  public KuzzleMemoryStorage bgsave() {

  }

  @Override
  public KuzzleMemoryStorage lastsave() {

  }

  @Override
  public KuzzleMemoryStorage save() {

  }

  @Override
  public KuzzleMemoryStorage configSet(String parameter, String value) {
  }

  @Override
  public KuzzleMemoryStorage configGet(String pattern) {

  }

  @Override
  public KuzzleMemoryStorage configResetStat() {

  }

  @Override
  public KuzzleMemoryStorage multi() {

  }

  @Override
  public KuzzleMemoryStorage exec() {
    return null;
  }

  @Override
  public KuzzleMemoryStorage send() {

  }

  @Override
  public KuzzleMemoryStorage discard() {

  }

  @Override
  public KuzzleMemoryStorage objectRefcount(String key) {

  }

  @Override
  public KuzzleMemoryStorage objectIdletime(String key) {

  }

  @Override
  public KuzzleMemoryStorage objectEncoding(String key) {

  }

  @Override
  public KuzzleMemoryStorage bitcount(String key) {

  }

  @Override
  public KuzzleMemoryStorage bitcount(String key, long start, long end) {

  }

  @Override
  public KuzzleMemoryStorage bitop(BitOP op, String destKey, String... srcKeys) {

  }

  @Override
  public KuzzleMemoryStorage waitReplicas(int replicas, long timeout) {

  }
  */
}
