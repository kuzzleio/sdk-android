package io.kuzzle.sdk.util.memoryStorage;

import java.util.Map;

import io.kuzzle.sdk.core.MemoryStorage;

public interface KuzzleMemoryStorageCommands {

  MemoryStorage append(final String key, final String value);

  MemoryStorage bgrewriteaof();

  MemoryStorage bgsave();

  MemoryStorage bitcount(final String key);

  MemoryStorage bitcount(final String key, long start, long end);

  MemoryStorage bitop(final BitOP op, final String destKey, final String... srcKeys);

  MemoryStorage bitpos(final String id, final long bit);

  MemoryStorage bitpos(final String id, final long bit, final long start);

  MemoryStorage bitpos(final String id, final long bit, final long start, final long end);

  MemoryStorage blpop(final String[] args, final long timeout);

  MemoryStorage brpoplpush(final String source, final String destination, final int timeout);

  MemoryStorage dbsize();

  MemoryStorage decrby(final String key, final long integer);

  MemoryStorage discard();

  MemoryStorage exec();

  MemoryStorage expire(final String key, final int seconds);

  MemoryStorage expireat(final String key, final long unixTime);

  MemoryStorage flushdb();

  MemoryStorage getbit(String key, long offset);

  MemoryStorage getrange(String key, long startOffset, long endOffset);

  MemoryStorage hdel(final String key, final String... fields);

  MemoryStorage hexists(final String key, final String field);

  MemoryStorage hincrby(final String key, final String field, final double value);

  MemoryStorage hmset(final String key, final Map<String, String> hash);

  MemoryStorage hset(final String key, final String field, final String value);

  MemoryStorage info(final String section);

  MemoryStorage keys(final String pattern);

  MemoryStorage lastsave();

  MemoryStorage lindex(final String key, final long index);

  MemoryStorage linsert(final String key, final Position where, final String pivot, final String value);

  MemoryStorage lpush(final String key, final String... values);

  MemoryStorage lrange(final String key, final long start, final long end);

  MemoryStorage lrem(final String key, final long count, final String value);

  MemoryStorage lset(final String key, final long index, final String value);

  MemoryStorage ltrim(final String key, final long start, final long end);

  MemoryStorage mset(final String... keysvalues);

  MemoryStorage multi();

  MemoryStorage object(final ObjectCommand subcommand, final String args);

  MemoryStorage pexpire(final String key, final long milliseconds);

  MemoryStorage pexpireat(final String key, final long timestamp);

  MemoryStorage pfadd(final String key, final String... elements);

  MemoryStorage pfmerge(final String destKey, final String... sourceKeys);

  MemoryStorage ping();

  MemoryStorage psetex(final String key, final long milliseconds, final String value);

  MemoryStorage publish(final String channel, final String message);

  MemoryStorage randomkey();

  MemoryStorage rename(final String oldkey, final String newkey);

  MemoryStorage renamenx(final String oldkey, final String newkey);

  MemoryStorage restore(final String key, final long ttl, final String content);

  MemoryStorage rpoplpush(final String srckey, final String dstkey);

  MemoryStorage sadd(final String key, final String... members);

  MemoryStorage save();

  MemoryStorage sdiffstore(final String dstkey, final String... keys);

  MemoryStorage set(final String key, final String value, final SetParams params);

  MemoryStorage setbit(final String key, final long offset, final Object value);

  MemoryStorage setex(final String key, final int seconds, final String value);

  MemoryStorage setrange(final String key, final long offset, final String value);

  MemoryStorage sinterstore(final String dstkey, final String... keys);

  MemoryStorage sismember(final String key, final String member);

  MemoryStorage smove(final String srckey, final String dstkey, final String member);

  MemoryStorage spop(final String key);

  MemoryStorage spop(final String key, final long count);

  MemoryStorage srem(final String key, final String... members);

  MemoryStorage sunionstore(final String dstkey, final String... keys);

  MemoryStorage unwatch();

  MemoryStorage wait(final int replicas, final long timeout);

  MemoryStorage zcount(final String key, final Object min, final Object max);

  MemoryStorage zincrby(final String key, final double score, final String member);

  MemoryStorage zinterstore(final String destination, final String[] sets, final ZParams.Aggregate aggregate, final Object... weights);

  MemoryStorage zlexcount(final String key, final long min, final long max);

  MemoryStorage zrange(final String key, final long start, final long end, boolean withscores);

  MemoryStorage zrangebylex(final String key, final long min, final long max, final long offset, final long count);

  MemoryStorage zrangebyscore(final String key, final long min, final long max, final boolean withscores, final long offset, final long count);

  MemoryStorage zrem(final String key, final String... members);

  MemoryStorage zremrangebylex(final String key, final long min, final long max, final long offset, final long count);

  MemoryStorage zrevrangebyscore(final String key, final long min, final long max, final boolean withscores, final long offset, final long count);

  MemoryStorage zrevrank(final String key, final String member);

  // Unique argument key
  MemoryStorage decr(final String key);

  MemoryStorage get(final String key);

  MemoryStorage dump(final String key);

  MemoryStorage hgetall(final String key);

  MemoryStorage hkeys(final String key);

  MemoryStorage hlen(final String key);

  MemoryStorage hstrlen(final String key);

  MemoryStorage hvals(final String key);

  MemoryStorage incr(final String key);

  MemoryStorage llen(final String key);

  MemoryStorage lpop(final String key);

  MemoryStorage persist(final String key);

  MemoryStorage pttl(final String key);

  MemoryStorage rpop(final String key);

  MemoryStorage scard(final String key);

  MemoryStorage smembers(final String key);

  MemoryStorage strlen(final String key);

  MemoryStorage ttl(final String key);

  MemoryStorage type(final String key);

  MemoryStorage zcard(final String key);

  // key value
  MemoryStorage getset(final String key, final String value);

  MemoryStorage lpushx(final String key, final String value);

  // key key...
  MemoryStorage mget(final String... keys);

  MemoryStorage pfcount(final String... keys);

  MemoryStorage del(final String... keys);

  MemoryStorage exists(final String... keys);

  MemoryStorage sdiff(final String... keys);

  MemoryStorage sinter(final String... keys);

  MemoryStorage sunion(final String... keys);

  MemoryStorage watch(final String... keys);


  MemoryStorage incrby(final String key, final long value);

  MemoryStorage incrbyfloat(final String key, final double value);

  MemoryStorage brpop(final String[] args, final long timeout);

  MemoryStorage hget(final String key, final String field);

  MemoryStorage hmget(final String key, final String... fields);

  MemoryStorage hsetnx(final String key, final String field, final String value);

  MemoryStorage msetnx(final String... keysvalues);

  MemoryStorage rpush(final String key, final String... strings);

  MemoryStorage hincrbyfloat(final String key, final String field, final double value);

  MemoryStorage srandmember(final String key, final long count);

  MemoryStorage zrevrange(final String key, final long start, final long end, boolean withscores);

  MemoryStorage zscore(final String key, final String member);

}