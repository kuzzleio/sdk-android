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

  KuzzleMemoryStorage blpop(final String key, final long timeout);

  KuzzleMemoryStorage blpop(final String[] args, final long timeout);

  KuzzleMemoryStorage brpoplpush(final String source, final String destination, final int timeout);

  KuzzleMemoryStorage dbsize();

  KuzzleMemoryStorage decrby(final String key, final long integer);

  KuzzleMemoryStorage del(final String... keys);

  KuzzleMemoryStorage discard();

  KuzzleMemoryStorage exec();

  KuzzleMemoryStorage exists(final String key);

  KuzzleMemoryStorage exists(final String... keys);

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
/*
  KuzzleMemoryStorage set(final String key, final String value);

  KuzzleMemoryStorage set(final String key, final String value, SetParams params);

  KuzzleMemoryStorage get(final String key);

  KuzzleMemoryStorage type(final String key);

  KuzzleMemoryStorage rename(final String oldkey, final String newkey);

  KuzzleMemoryStorage renamenx(final String oldkey, final String newkey);

  KuzzleMemoryStorage ttl(final String key);

  KuzzleMemoryStorage setbit(String key, long offset, boolean value);

  KuzzleMemoryStorage setbit(String key, long offset, String value);

  KuzzleMemoryStorage setrange(String key, long offset, String value);

  KuzzleMemoryStorage getSet(final String key, final String value);

  KuzzleMemoryStorage mget(final String... keys);

  KuzzleMemoryStorage setnx(final String key, final String value);

  KuzzleMemoryStorage setex(final String key, final int seconds, final String value);

  KuzzleMemoryStorage msetnx(final String... keysvalues);

  KuzzleMemoryStorage decr(final String key);

  KuzzleMemoryStorage incrBy(final String key, final long integer);

  KuzzleMemoryStorage incrByFloat(final String key, final double value);

  KuzzleMemoryStorage incr(final String key);

  KuzzleMemoryStorage substr(final String key, final int start, final int end);

  KuzzleMemoryStorage hget(final String key, final String field);

  KuzzleMemoryStorage hsetnx(final String key, final String field, final String value);

  KuzzleMemoryStorage hmget(final String key, final String... fields);

  KuzzleMemoryStorage hincrBy(final String key, final String field, final long value);

  KuzzleMemoryStorage hlen(final String key);

  KuzzleMemoryStorage hkeys(final String key);

  KuzzleMemoryStorage hvals(final String key);

  KuzzleMemoryStorage hgetAll(final String key);

  KuzzleMemoryStorage rpush(final String key, final String... strings);

  KuzzleMemoryStorage llen(final String key);

  KuzzleMemoryStorage lpop(final String key);

  KuzzleMemoryStorage rpop(final String key);

  KuzzleMemoryStorage rpoplpush(final String srckey, final String dstkey);

  KuzzleMemoryStorage sadd(final String key, final String... members);

  KuzzleMemoryStorage smembers(final String key);

  KuzzleMemoryStorage srem(final String key, final String... member);

  KuzzleMemoryStorage spop(final String key);

  KuzzleMemoryStorage spop(final String key, final long count);

  KuzzleMemoryStorage smove(final String srckey, final String dstkey, final String member);

  KuzzleMemoryStorage scard(final String key);

  KuzzleMemoryStorage sismember(final String key, final String member);

  KuzzleMemoryStorage sinter(final String... keys);

  KuzzleMemoryStorage sinterstore(final String dstkey, final String... keys);

  KuzzleMemoryStorage sunion(final String... keys);

  KuzzleMemoryStorage sunionstore(final String dstkey, final String... keys);

  KuzzleMemoryStorage sdiff(final String... keys);

  KuzzleMemoryStorage sdiffstore(final String dstkey, final String... keys);

  KuzzleMemoryStorage srandmember(final String key);

  KuzzleMemoryStorage zadd(final String key, final double score, final String member);

  KuzzleMemoryStorage zadd(final String key, final double score, final String member, final ZAddParams params);

  KuzzleMemoryStorage zadd(final String key, final Map<String, Double> scoreMembers);

  KuzzleMemoryStorage zadd(final String key, final Map<String, Double> scoreMembers, final ZAddParams params);

  KuzzleMemoryStorage zrange(final String key, final long start, final long end);

  KuzzleMemoryStorage zrem(final String key, final String... members);

  KuzzleMemoryStorage zincrby(final String key, final double score, final String member);

  KuzzleMemoryStorage zincrby(final String key, final double score, final String member, final ZIncrByParams params);

  KuzzleMemoryStorage zrank(final String key, final String member);

  KuzzleMemoryStorage zrevrank(final String key, final String member);

  KuzzleMemoryStorage zrevrange(final String key, final long start, final long end);

  KuzzleMemoryStorage zrangeWithScores(final String key, final long start, final long end);

  KuzzleMemoryStorage zrevrangeWithScores(final String key, final long start, final long end);

  KuzzleMemoryStorage zcard(final String key);

  KuzzleMemoryStorage zscore(final String key, final String member);

  KuzzleMemoryStorage watch(final String... keys);

  KuzzleMemoryStorage sort(final String key);

  KuzzleMemoryStorage sort(final String key, final SortingParams sortingParameters);

  KuzzleMemoryStorage sort(final String key, final SortingParams sortingParameters, final String dstkey);

  KuzzleMemoryStorage sort(final String key, final String dstkey);

  KuzzleMemoryStorage brpop(final String[] args);

  KuzzleMemoryStorage zcount(final String key, final double min, final double max);

  KuzzleMemoryStorage zcount(final String key, final String min, final String max);

  KuzzleMemoryStorage zrangeByScore(final String key, final double min, final double max);

  KuzzleMemoryStorage zrangeByScore(final String key, final String min, final String max);

  KuzzleMemoryStorage zrangeByScore(final String key, final double min, final double max, final int offset,
                     int count);

  KuzzleMemoryStorage zrangeByScoreWithScores(final String key, final double min, final double max);

  KuzzleMemoryStorage zrangeByScoreWithScores(final String key, final double min, final double max,
                               final int offset, final int count);

  KuzzleMemoryStorage zrangeByScoreWithScores(final String key, final String min, final String max);

  KuzzleMemoryStorage zrangeByScoreWithScores(final String key, final String min, final String max,
                               final int offset, final int count);

  KuzzleMemoryStorage zrevrangeByScore(final String key, final double max, final double min);

  KuzzleMemoryStorage zrevrangeByScore(final String key, final String max, final String min);

  KuzzleMemoryStorage zrevrangeByScore(final String key, final double max, final double min, final int offset,
                        int count);

  KuzzleMemoryStorage zrevrangeByScoreWithScores(final String key, final double max, final double min);

  KuzzleMemoryStorage zrevrangeByScoreWithScores(final String key, final double max, final double min,
                                  final int offset, final int count);

  KuzzleMemoryStorage zrevrangeByScoreWithScores(final String key, final String max, final String min);

  KuzzleMemoryStorage zrevrangeByScoreWithScores(final String key, final String max, final String min,
                                  final int offset, final int count);

  KuzzleMemoryStorage zremrangeByRank(final String key, final long start, final long end);

  KuzzleMemoryStorage zremrangeByScore(final String key, final double start, final double end);

  KuzzleMemoryStorage zremrangeByScore(final String key, final String start, final String end);

  KuzzleMemoryStorage zunionstore(final String dstkey, final String... sets);

  KuzzleMemoryStorage zunionstore(final String dstkey, final ZParams params, final String... sets);

  KuzzleMemoryStorage zinterstore(final String dstkey, final String... sets);

  KuzzleMemoryStorage zinterstore(final String dstkey, final ZParams params, final String... sets);

  KuzzleMemoryStorage strlen(final String key);

  KuzzleMemoryStorage lpushx(final String key, final String... string);

  KuzzleMemoryStorage persist(final String key);

  KuzzleMemoryStorage rpushx(final String key, final String... string);

  KuzzleMemoryStorage bgsave();

  KuzzleMemoryStorage save();

  KuzzleMemoryStorage configSet(final String parameter, final String value);

  KuzzleMemoryStorage configGet(final String pattern);

  KuzzleMemoryStorage configResetStat();

  KuzzleMemoryStorage multi();

  KuzzleMemoryStorage objectRefcount(String key);

  KuzzleMemoryStorage objectIdletime(String key);

  KuzzleMemoryStorage objectEncoding(String key);

  KuzzleMemoryStorage waitReplicas(int replicas, long timeout);
  */
}