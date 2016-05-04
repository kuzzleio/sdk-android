package io.kuzzle.sdk.util.memoryStorage;

import java.util.Map;

import io.kuzzle.sdk.core.KuzzleMemoryStorage;

public interface KuzzleMemoryStorageCommands {

  KuzzleMemoryStorage append(final String key, final String value);

  KuzzleMemoryStorage bgrewriteaof();

  KuzzleMemoryStorage bgsave();

  KuzzleMemoryStorage bitcount(final String key);

  KuzzleMemoryStorage bitcount(final String key, long start, long end);

  KuzzleMemoryStorage bitop(final BitOP op, final String destKey, final String... srcKeys);

  KuzzleMemoryStorage bitpos(final String id, final long bit);

  KuzzleMemoryStorage bitpos(final String id, final long bit, final long start);

  KuzzleMemoryStorage bitpos(final String id, final long bit, final long start, final long end);

  KuzzleMemoryStorage blpop(final String[] args, final long timeout);

  KuzzleMemoryStorage brpoplpush(final String source, final String destination, final int timeout);

  KuzzleMemoryStorage dbsize();

  KuzzleMemoryStorage decrby(final String key, final long integer);

  KuzzleMemoryStorage discard();

  KuzzleMemoryStorage exec();

  KuzzleMemoryStorage expire(final String key, final int seconds);

  KuzzleMemoryStorage expireat(final String key, final long unixTime);

  KuzzleMemoryStorage flushdb();

  KuzzleMemoryStorage getbit(String key, long offset);

  KuzzleMemoryStorage getrange(String key, long startOffset, long endOffset);

  KuzzleMemoryStorage hdel(final String key, final String... fields);

  KuzzleMemoryStorage hexists(final String key, final String field);

  KuzzleMemoryStorage hincrby(final String key, final String field, final double value);

  KuzzleMemoryStorage hmset(final String key, final Map<String, String> hash);

  KuzzleMemoryStorage hset(final String key, final String field, final String value);

  KuzzleMemoryStorage info(final String section);

  KuzzleMemoryStorage keys(final String pattern);

  KuzzleMemoryStorage lastsave();

  KuzzleMemoryStorage lindex(final String key, final long index);

  KuzzleMemoryStorage linsert(final String key, final Position where, final String pivot, final String value);

  KuzzleMemoryStorage lpush(final String key, final String... values);

  KuzzleMemoryStorage lrange(final String key, final long start, final long end);

  KuzzleMemoryStorage lrem(final String key, final long count, final String value);

  KuzzleMemoryStorage lset(final String key, final long index, final String value);

  KuzzleMemoryStorage ltrim(final String key, final long start, final long end);

  KuzzleMemoryStorage mset(final String... keysvalues);

  KuzzleMemoryStorage multi();

  KuzzleMemoryStorage object(final ObjectCommand subcommand, final String args);

  KuzzleMemoryStorage pexpire(final String key, final long milliseconds);

  KuzzleMemoryStorage pexpireat(final String key, final long timestamp);

  KuzzleMemoryStorage pfadd(final String key, final String... elements);

  KuzzleMemoryStorage pfmerge(final String destKey, final String... sourceKeys);

  KuzzleMemoryStorage ping();

  KuzzleMemoryStorage psetex(final String key, final long milliseconds, final String value);

  KuzzleMemoryStorage publish(final String channel, final String message);

  KuzzleMemoryStorage randomkey();

  KuzzleMemoryStorage rename(final String oldkey, final String newkey);

  KuzzleMemoryStorage renamenx(final String oldkey, final String newkey);

  KuzzleMemoryStorage restore(final String key, final long ttl, final String content);

  KuzzleMemoryStorage rpoplpush(final String srckey, final String dstkey);

  KuzzleMemoryStorage sadd(final String key, final String... members);

  KuzzleMemoryStorage save();

  KuzzleMemoryStorage sdiffstore(final String dstkey, final String... keys);

  KuzzleMemoryStorage set(final String key, final String value, final SetParams params);

  KuzzleMemoryStorage setbit(final String key, final long offset, final Object value);

  KuzzleMemoryStorage setex(final String key, final int seconds, final String value);

  KuzzleMemoryStorage setrange(final String key, final long offset, final String value);

  KuzzleMemoryStorage sinterstore(final String dstkey, final String... keys);

  KuzzleMemoryStorage sismember(final String key, final String member);

  KuzzleMemoryStorage smove(final String srckey, final String dstkey, final String member);

  KuzzleMemoryStorage spop(final String key);

  KuzzleMemoryStorage spop(final String key, final long count);

  KuzzleMemoryStorage srem(final String key, final String... members);

  KuzzleMemoryStorage sunionstore(final String dstkey, final String... keys);

  KuzzleMemoryStorage unwatch();

  KuzzleMemoryStorage wait(final int replicas, final long timeout);

  KuzzleMemoryStorage zcount(final String key, final Object min, final Object max);

  KuzzleMemoryStorage zincrby(final String key, final double score, final String member);

  KuzzleMemoryStorage zinterstore(final String destination, final String[] sets, final ZParams.Aggregate aggregate, final Object... weights);

  KuzzleMemoryStorage zlexcount(final String key, final long min, final long max);

  KuzzleMemoryStorage zrange(final String key, final long start, final long end, boolean withscores);

  KuzzleMemoryStorage zrangebylex(final String key, final long min, final long max, final long offset, final long count);

  KuzzleMemoryStorage zrangebyscore(final String key, final long min, final long max, final boolean withscores, final long offset, final long count);

  KuzzleMemoryStorage zrem(final String key, final String... members);

  KuzzleMemoryStorage zremrangebylex(final String key, final long min, final long max, final long offset, final long count);

  KuzzleMemoryStorage zrevrangebyscore(final String key, final long min, final long max, final boolean withscores, final long offset, final long count);

  KuzzleMemoryStorage zrevrank(final String key, final String member);

  // Unique argument key
  KuzzleMemoryStorage decr(final String key);

  KuzzleMemoryStorage get(final String key);

  KuzzleMemoryStorage dump(final String key);

  KuzzleMemoryStorage hgetall(final String key);

  KuzzleMemoryStorage hkeys(final String key);

  KuzzleMemoryStorage hlen(final String key);

  KuzzleMemoryStorage hstrlen(final String key);

  KuzzleMemoryStorage hvals(final String key);

  KuzzleMemoryStorage incr(final String key);

  KuzzleMemoryStorage llen(final String key);

  KuzzleMemoryStorage lpop(final String key);

  KuzzleMemoryStorage persist(final String key);

  KuzzleMemoryStorage pttl(final String key);

  KuzzleMemoryStorage rpop(final String key);

  KuzzleMemoryStorage scard(final String key);

  KuzzleMemoryStorage smembers(final String key);

  KuzzleMemoryStorage strlen(final String key);

  KuzzleMemoryStorage ttl(final String key);

  KuzzleMemoryStorage type(final String key);

  KuzzleMemoryStorage zcard(final String key);

  // key value
  KuzzleMemoryStorage getset(final String key, final String value);

  KuzzleMemoryStorage lpushx(final String key, final String value);

  // key key...
  KuzzleMemoryStorage mget(final String... keys);

  KuzzleMemoryStorage pfcount(final String... keys);

  KuzzleMemoryStorage del(final String... keys);

  KuzzleMemoryStorage exists(final String... keys);

  KuzzleMemoryStorage sdiff(final String... keys);

  KuzzleMemoryStorage sinter(final String... keys);

  KuzzleMemoryStorage sunion(final String... keys);

  KuzzleMemoryStorage watch(final String... keys);


  KuzzleMemoryStorage incrby(final String key, final long value);

  KuzzleMemoryStorage incrbyfloat(final String key, final double value);

  KuzzleMemoryStorage brpop(final String[] args, final long timeout);

  KuzzleMemoryStorage hget(final String key, final String field);

  KuzzleMemoryStorage hmget(final String key, final String... fields);

  KuzzleMemoryStorage hsetnx(final String key, final String field, final String value);

  KuzzleMemoryStorage msetnx(final String... keysvalues);

  KuzzleMemoryStorage rpush(final String key, final String... strings);

  KuzzleMemoryStorage hincrbyfloat(final String key, final String field, final double value);

  KuzzleMemoryStorage srandmember(final String key, final long count);

  KuzzleMemoryStorage zrevrange(final String key, final long start, final long end, boolean withscores);

  KuzzleMemoryStorage zscore(final String key, final String member);

}