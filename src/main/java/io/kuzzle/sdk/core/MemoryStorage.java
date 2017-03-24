package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.util.KuzzleJSONObject;

/**
 * Kuzzle's memory storage is a separate data store from the database layer.
 * It is internaly based on Redis. You can access most of Redis functions (all
 * lowercased), excepting:
 *   * all cluster based functions
 *   * all script based functions
 *   * all cursors functions
 *
 */

public class MemoryStorage {
  private Kuzzle  kuzzle;
  private Kuzzle.QueryArgs queryArgs = new Kuzzle.QueryArgs();

  public MemoryStorage(@NonNull final Kuzzle kuzzle) {
    this.kuzzle = kuzzle;
    queryArgs.controller = "ms";
  }

  protected void assignGeoradiusOptions(@NonNull JSONObject query, Options options) {
    if (options != null) {
      JSONArray opts = new JSONArray();

      if (options.getWithcoord()) {
        opts.put("withcoord");
      }

      if (options.getWithdist()) {
        opts.put("withdist");
      }

      if (options.getCount() != null) {
        opts.put("count").put(options.getCount());
      }

      if (options.getSort() != null) {
        opts.put(options.getSort());
      }

      if (opts.length() > 0) {
        try {
          query.put("options", opts);
        }
        catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  protected JSONArray mapGeoradiusResults(@NonNull JSONArray points) throws JSONException {
    JSONArray mapped = new JSONArray();

    // Simple array of point names (no options provided)
    if (points.get(0) instanceof String) {
      for (int i = 0; i < points.length(); i++) {
        mapped.put(new JSONObject().put("name", points.getString(i)));
      }

      return mapped;
    }

    for (int i = 0; i < points.length(); i++) {
      JSONArray rawPoint = points.getJSONArray(i);
      JSONObject p = new JSONObject().put("name", rawPoint.getString(0));

      for (int j = 1; j < rawPoint.length(); j++) {
        // withcoord results are stored in an array...
        if (rawPoint.get(j) instanceof JSONArray) {
          JSONArray coords = rawPoint.getJSONArray(j);

          p
            .put("coordinates", new JSONArray()
              .put(Double.parseDouble(coords.getString(0)))
              .put(Double.parseDouble(coords.getString(1)))
            );
        }
        else {
          // ... while withdist results are not
          p.put("distance", Double.parseDouble(rawPoint.getString(j)));
        }
      }

      mapped.put(p);
    }

    return mapped;
  }

  protected JSONArray mapZrangeResults(@NonNull JSONArray members) {
    String buffer = null;
    JSONArray mapped = new JSONArray();

    try {
      for (int i = 0; i < members.length(); i++) {
        if (buffer == null) {
          buffer = members.getString(i);
        }
        else {
          mapped.put(new JSONObject()
            .put("member", buffer)
            .put("score", Double.parseDouble(members.getString(i)))
          );
          buffer = null;
        }
      }
    }
    catch(JSONException e) {
      throw new RuntimeException(e);
    }

    return mapped;
  }

  protected void send(@NonNull String action, final KuzzleJSONObject query, Options options, final ResponseListener<JSONObject> listener) {
    queryArgs.action = action;

    try {
      if (listener != null) {
          kuzzle.query(queryArgs, query, options, new OnQueryDoneListener() {
            @Override
            public void onSuccess(JSONObject response) {
              listener.onSuccess(response);
            }

            @Override
            public void onError(JSONObject error) {
              listener.onError(error);
            }
          });
      }
      else {
        kuzzle.query(queryArgs, query, options);
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  protected ResponseListener<JSONObject> getCallbackLong(final ResponseListener<Long> listener) {
    ResponseListener<JSONObject> callback = new ResponseListener<JSONObject>() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          listener.onSuccess(response.getLong("result"));
        }
        catch(JSONException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(JSONObject error) {
        listener.onError(error);
      }
    };

    return callback;
  }

  protected ResponseListener<JSONObject> getCallbackInt(final ResponseListener<Integer> listener) {
    ResponseListener<JSONObject> callback = new ResponseListener<JSONObject>() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          listener.onSuccess(response.getInt("result"));
        }
        catch(JSONException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(JSONObject error) {
        listener.onError(error);
      }
    };

    return callback;
  }

  protected ResponseListener<JSONObject> getCallbackString(final ResponseListener<String> listener) {
    ResponseListener<JSONObject> callback = new ResponseListener<JSONObject>() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          listener.onSuccess(response.getString("result"));
        }
        catch(JSONException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(JSONObject error) {
        listener.onError(error);
      }
    };

    return callback;
  }

  protected ResponseListener<JSONObject> getCallbackDouble(final ResponseListener<Double> listener) {
    ResponseListener<JSONObject> callback = new ResponseListener<JSONObject>() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          listener.onSuccess(Double.parseDouble(response.getString("result")));
        }
        catch(JSONException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(JSONObject error) {
        listener.onError(error);
      }
    };

    return callback;
  }

  protected ResponseListener<JSONObject> getCallbackArray(final ResponseListener<JSONArray> listener) {
    ResponseListener<JSONObject> callback = new ResponseListener<JSONObject>() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          listener.onSuccess(response.getJSONArray("result"));
        }
        catch(JSONException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(JSONObject error) {
        listener.onError(error);
      }
    };

    return callback;
  }

  public MemoryStorage append(@NonNull String key, @NonNull final String value) {
    return append(key, value, null, null);
  }

  public MemoryStorage append(@NonNull String key, @NonNull final String value, Options options) {
    return append(key, value, options, null);
  }

  public MemoryStorage append(@NonNull String key, @NonNull final String value, final ResponseListener<Long> listener) {
    return append(key, value, null, listener);
  }

  public MemoryStorage append(@NonNull String key, @NonNull final String value, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
      );

    send("append", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void bitcount(@NonNull String key, @NonNull final ResponseListener<Long> listener) {
    bitcount(key, null, listener);
  }

  public void bitcount(@NonNull String key, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    if (options != null) {
      if (options.getStart() != null) {
        query.put("start", options.getStart());
      }

      if (options.getEnd() != null) {
        query.put("end", options.getEnd());
      }
    }

    send("bitcount", query, options, getCallbackLong(listener));
  }

  public MemoryStorage bitop(@NonNull String key, @NonNull String operation, @NonNull final JSONArray keys) {
    return bitop(key, operation, keys, null, null);
  }

  public MemoryStorage bitop(@NonNull String key, @NonNull String operation, @NonNull final JSONArray keys, Options options) {
    return bitop(key, operation, keys, options, null);
  }

  public MemoryStorage bitop(@NonNull String key, @NonNull String operation, @NonNull final JSONArray keys, final ResponseListener<Long> listener) {
    return bitop(key, operation, keys, null, listener);
  }

  public MemoryStorage bitop(@NonNull String key, @NonNull String operation, @NonNull final JSONArray keys, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("operation", operation)
        .put("keys", keys)
      );

    send("bitop", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void bitpos(@NonNull String key, int bit, @NonNull final ResponseListener<Long> listener) {
    bitpos(key, bit, null, listener);
  }

  public void bitpos(@NonNull String key, int bit, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("bit", bit);

    if (options != null) {
      if (options.getStart() != null) {
        query.put("start", options.getStart());
      }

      if (options.getEnd() != null) {
        query.put("end", options.getEnd());
      }
    }

    send("bitpos", query, options, getCallbackLong(listener));
  }

  public void dbsize(@NonNull final ResponseListener<Long> listener) {
    dbsize(null, listener);
  }

  public void dbsize(Options options, @NonNull final ResponseListener<Long> listener) {
    send("dbsize", new KuzzleJSONObject(), options, getCallbackLong(listener));
  }

  public MemoryStorage decr(@NonNull String key) {
    return decr(key, null, null);
  }

  public MemoryStorage decr(@NonNull String key, Options options) {
    return decr(key, options, null);
  }

  public MemoryStorage decr(@NonNull String key, final ResponseListener<Long> listener) {
    return decr(key, null, listener);
  }

  public MemoryStorage decr(@NonNull String key, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("decr", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage decrby(@NonNull String key, long value) {
    return decrby(key, value, null, null);
  }

  public MemoryStorage decrby(@NonNull String key, long value, Options options) {
    return decrby(key, value, options, null);
  }

  public MemoryStorage decrby(@NonNull String key, long value, final ResponseListener<Long> listener) {
    return decrby(key, value, null, listener);
  }

  public MemoryStorage decrby(@NonNull String key, long value, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
      );

    send("decrby", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage del(@NonNull JSONArray keys) {
    return del(keys, null, null);
  }

  public MemoryStorage del(@NonNull JSONArray keys, Options options) {
    return del(keys, options, null);
  }

  public MemoryStorage del(@NonNull JSONArray keys, final ResponseListener<Long> listener) {
    return del(keys, null, listener);
  }

  public MemoryStorage del(@NonNull JSONArray keys, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("body", new KuzzleJSONObject().put("keys", keys));

    send("del", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void exists(@NonNull JSONArray keys, @NonNull final ResponseListener<Long> listener) {
    exists(keys, null, listener);
  }

  public void exists(@NonNull JSONArray keys, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("keys", keys);

    send("exists", query, options, getCallbackLong(listener));
  }

  public MemoryStorage expire(@NonNull String key, long seconds) {
    return expire(key, seconds, null, null);
  }

  public MemoryStorage expire(@NonNull String key, long seconds, Options options) {
    return expire(key, seconds, options, null);
  }

  public MemoryStorage expire(@NonNull String key, long seconds, final ResponseListener<Integer> listener) {
    return expire(key, seconds, null, listener);
  }

  public MemoryStorage expire(@NonNull String key, long seconds, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("seconds", seconds)
      );

    send("expire", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public MemoryStorage expireat(@NonNull String key, long timestamp) {
    return expireat(key, timestamp, null, null);
  }

  public MemoryStorage expireat(@NonNull String key, long timestamp, Options options) {
    return expireat(key, timestamp, options, null);
  }

  public MemoryStorage expireat(@NonNull String key, long timestamp, final ResponseListener<Integer> listener) {
    return expireat(key, timestamp, null, listener);
  }

  public MemoryStorage expireat(@NonNull String key, long timestamp, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("timestamp", timestamp)
      );

    send("expireat", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public MemoryStorage flushdb() {
    return flushdb(null, null);
  }

  public MemoryStorage flushdb(final ResponseListener<String> listener) {
    return flushdb(null, listener);
  }

  public MemoryStorage flushdb(Options options) {
    return flushdb(options, null);
  }

  public MemoryStorage flushdb(Options options, final ResponseListener<String> listener) {
    send("flushdb", new KuzzleJSONObject(), options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage geoadd(@NonNull String key, @NonNull JSONArray points) {
    return geoadd(key, points, null, null);
  }

  public MemoryStorage geoadd(@NonNull String key, @NonNull JSONArray points, final ResponseListener<Long> listener) {
    return geoadd(key, points, null, listener);
  }

  public MemoryStorage geoadd(@NonNull String key, @NonNull JSONArray points, Options options) {
    return geoadd(key, points, options, null);
  }

  public MemoryStorage geoadd(@NonNull String key, @NonNull JSONArray points, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("points", points)
      );

    send("geoadd", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void geodist(@NonNull String key, @NonNull String member1, @NonNull String member2, @NonNull final ResponseListener<Double> listener) {
    geodist(key, member1, member2, null, listener);
  }

  public void geodist(@NonNull String key, @NonNull String member1, @NonNull String member2, Options options, @NonNull final ResponseListener<Double> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("member1", member1)
      .put("member2", member2);

    if (options != null) {
      if (options.getUnit() != null) {
        query.put("unit", options.getUnit());
      }
    }

    send("geodist", query, options, getCallbackDouble(listener));
  }

  public void geohash(@NonNull String key, @NonNull JSONArray members, @NonNull final ResponseListener<JSONArray> listener) {
    geohash(key, members, null, listener);
  }

  public void geohash(@NonNull String key, @NonNull JSONArray members, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("members", members);

    send("geohash", query, options, getCallbackArray(listener));
  }

  public void geopos(@NonNull String key, @NonNull JSONArray members, @NonNull final ResponseListener<JSONArray> listener) {
    geopos(key, members, null, listener);
  }

  public void geopos(@NonNull String key, @NonNull JSONArray members, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("members", members);

    send(
      "geopos",
      query,
      options,
      new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            /*
             Converts the resulting array of arrays of strings,
             into an array of arrays of doubles
             */
            JSONArray
              raw = response.getJSONArray("result"),
              result = new JSONArray();

            for (int i = 0; i < raw.length(); i++) {
              JSONArray
                rawPos = raw.getJSONArray(i),
                pos = new JSONArray();

              for (int j = 0; j < rawPos.length(); j++) {
                pos.put(Double.parseDouble(rawPos.getString(j)));
              }

              result.put(pos);
            }

            listener.onSuccess(result);
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      }
    );
  }

  public void georadius(@NonNull String key, double lon, double lat, double distance, @NonNull String unit, @NonNull final ResponseListener<JSONArray> listener) {
    georadius(key, lon, lat, distance, unit, null, listener);
  }

  public void georadius(@NonNull String key, double lon, double lat, double distance, @NonNull String unit, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("lon", lon)
      .put("lat", lat)
      .put("distance", distance)
      .put("unit", unit);

    assignGeoradiusOptions(query, options);

    send(
      "georadius",
      query,
      options,
      new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(mapGeoradiusResults(response.getJSONArray("result")));
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      }
    );
  }

  public void georadiusbymember(@NonNull String key, @NonNull String member, double distance, @NonNull String unit, @NonNull  final ResponseListener<JSONArray> listener) {
    georadiusbymember(key, member, distance, unit, null, listener);
  }

  public void georadiusbymember(@NonNull String key, @NonNull String member, double distance, @NonNull String unit, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("member", member)
      .put("distance", distance)
      .put("unit", unit);

    assignGeoradiusOptions(query, options);

    send(
      "georadiusbymember",
      query,
      options,
      new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(mapGeoradiusResults(response.getJSONArray("result")));
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      }
    );
  }

  public void get(@NonNull String key, @NonNull final ResponseListener<String> listener) {
    get(key, null, listener);
  }

  public void get(@NonNull String key, Options options, @NonNull final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("get", query, options, getCallbackString(listener));
  }

  public void getbit(@NonNull String key, long offset, @NonNull final ResponseListener<Integer> listener) {
    getbit(key, offset, null, listener);
  }

  public void getbit(@NonNull String key, long offset, Options options, @NonNull final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key).put("offset", offset);

    send("getbit", query, options, getCallbackInt(listener));
  }

  public void getrange(@NonNull String key, long start, long end, @NonNull final ResponseListener<String> listener) {
    getrange(key, start, end, null, listener);
  }

  public void getrange(@NonNull String key, long start, long end, Options options, @NonNull final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("start", start)
      .put("end", end);

    send("getrange", query, options, getCallbackString(listener));
  }

  public MemoryStorage getset(@NonNull String key, @NonNull String value) {
    return getset(key, value, null, null);
  }

  public MemoryStorage getset(@NonNull String key, @NonNull String value, final ResponseListener<String> listener) {
    return getset(key, value, null, listener);
  }

  public MemoryStorage getset(@NonNull String key, @NonNull String value, Options options) {
    return getset(key, value, options, null);
  }

  public MemoryStorage getset(@NonNull String key, @NonNull String value, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
      );

    send("getset", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage hdel(@NonNull String key, @NonNull JSONArray fields) {
    return hdel(key, fields, null, null);
  }

  public MemoryStorage hdel(@NonNull String key, @NonNull JSONArray fields, final ResponseListener<Long> listener) {
    return hdel(key, fields, null, listener);
  }

  public MemoryStorage hdel(@NonNull String key, @NonNull JSONArray fields, Options options) {
    return hdel(key, fields, options, null);
  }

  public MemoryStorage hdel(@NonNull String key, @NonNull JSONArray fields, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("fields", fields)
      );

    send("hdel", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void hexists(@NonNull String key, @NonNull String field, @NonNull final ResponseListener<Integer> listener) {
    hexists(key, field, null, listener);
  }

  public void hexists(@NonNull String key, @NonNull String field, Options options, @NonNull final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key).put("field", field);

    send("hexists", query, options, getCallbackInt(listener));
  }

  public void hget(@NonNull String key, @NonNull String field, @NonNull final ResponseListener<String> listener) {
    hget(key, field, null, listener);
  }

  public void hget(@NonNull String key, @NonNull String field, Options options, @NonNull final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key).put("field", field);

    send("hget", query, options, getCallbackString(listener));
  }

  public void hgetall(@NonNull String key, @NonNull final ResponseListener<JSONObject> listener) {
    hgetall(key, null, listener);
  }

  public void hgetall(@NonNull String key, Options options, @NonNull final ResponseListener<JSONObject> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send(
      "hgetall",
      query,
      options,
      new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result"));
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      }
    );
  }

  public MemoryStorage hincrby(@NonNull String key, @NonNull String field, long value) {
    return hincrby(key, field, value, null, null);
  }

  public MemoryStorage hincrby(@NonNull String key, @NonNull String field, long value, final ResponseListener<Long> listener) {
    return hincrby(key, field, value, null, listener);
  }

  public MemoryStorage hincrby(@NonNull String key, @NonNull String field, long value, Options options) {
    return hincrby(key, field, value, options, null);
  }

  public MemoryStorage hincrby(@NonNull String key, @NonNull String field, long value, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("field", field)
        .put("value", value)
      );

    send("hincrby", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage hincrbyfloat(@NonNull String key, @NonNull String field, double value) {
    return hincrbyfloat(key, field, value, null, null);
  }

  public MemoryStorage hincrbyfloat(@NonNull String key, @NonNull String field, double value, final ResponseListener<Double> listener) {
    return hincrbyfloat(key, field, value, null, listener);
  }

  public MemoryStorage hincrbyfloat(@NonNull String key, @NonNull String field, double value, Options options) {
    return hincrbyfloat(key, field, value, options, null);
  }

  public MemoryStorage hincrbyfloat(@NonNull String key, @NonNull String field, double value, Options options, final ResponseListener<Double> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("field", field)
        .put("value", value)
      );

    send("hincrbyfloat", query, options, listener != null ? getCallbackDouble(listener) : null);

    return this;
  }

  public void hkeys(@NonNull String key, @NonNull final ResponseListener<JSONArray> listener) {
    hkeys(key, null, listener);
  }

  public void hkeys(@NonNull String key, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("hkeys", query, options, getCallbackArray(listener));
  }

  public void hlen(@NonNull String key, @NonNull final ResponseListener<Long> listener) {
    hlen(key, null, listener);
  }

  public void hlen(@NonNull String key, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("hlen", query, options, getCallbackLong(listener));
  }

  public void hmget(@NonNull String key, @NonNull JSONArray fields, @NonNull final ResponseListener<JSONArray> listener) {
    hmget(key, fields, null, listener);
  }

  public void hmget(@NonNull String key, @NonNull JSONArray fields, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("fields", fields);

    send("hmget", query, options, getCallbackArray(listener));
  }

  public MemoryStorage hmset(@NonNull String key, @NonNull JSONArray entries) {
    return hmset(key, entries, null, null);
  }

  public MemoryStorage hmset(@NonNull String key, @NonNull JSONArray entries, final ResponseListener<String> listener) {
    return hmset(key, entries, null, listener);
  }

  public MemoryStorage hmset(@NonNull String key, @NonNull JSONArray entries, Options options) {
    return hmset(key, entries, options, null);
  }

  public MemoryStorage hmset(@NonNull String key, @NonNull JSONArray entries, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("entries", entries)
      );

    send("hmset", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public void hscan(@NonNull String key, long cursor, @NonNull final ResponseListener<JSONArray> listener) {
    hscan(key, cursor, null, listener);
  }

  public void hscan(@NonNull String key, long cursor, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("cursor", cursor);

    if (options != null) {
      if (options.getCount() != null) {
        query.put("count", options.getCount());
      }

      if (options.getMatch() != null) {
        query.put("match", options.getMatch());
      }
    }

    send("hscan", query, options, getCallbackArray(listener));
  }

  public MemoryStorage hset(@NonNull String key, @NonNull String field, @NonNull String value) {
    return hset(key, field, value, null, null);
  }

  public MemoryStorage hset(@NonNull String key, @NonNull String field, @NonNull String value, final ResponseListener<Integer> listener) {
    return hset(key, field, value, null, listener);
  }

  public MemoryStorage hset(@NonNull String key, @NonNull String field, @NonNull String value, Options options) {
    return hset(key, field, value, options, null);
  }

  public MemoryStorage hset(@NonNull String key, @NonNull String field, @NonNull String value, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("field", field)
        .put("value", value)
      );

    send("hset", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public MemoryStorage hsetnx(@NonNull String key, @NonNull String field, @NonNull String value) {
    return hsetnx(key, field, value, null, null);
  }

  public MemoryStorage hsetnx(@NonNull String key, @NonNull String field, @NonNull String value, final ResponseListener<Integer> listener) {
    return hsetnx(key, field, value, null, listener);
  }

  public MemoryStorage hsetnx(@NonNull String key, @NonNull String field, @NonNull String value, Options options) {
    return hsetnx(key, field, value, options, null);
  }

  public MemoryStorage hsetnx(@NonNull String key, @NonNull String field, @NonNull String value, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("field", field)
        .put("value", value)
      );

    send("hsetnx", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public void hstrlen(@NonNull String key, @NonNull String field, @NonNull final ResponseListener<Long> listener) {
    hstrlen(key, field, null, listener);
  }

  public void hstrlen(@NonNull String key, @NonNull String field, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key).put("field", field);

    send("hstrlen", query, options, getCallbackLong(listener));
  }

  public void hvals(@NonNull String key, @NonNull final ResponseListener<JSONArray> listener) {
    hvals(key, null, listener);
  }

  public void hvals(@NonNull String key, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("hvals", query, options, getCallbackArray(listener));
  }

  public MemoryStorage incr(@NonNull String key) {
    return incr(key, null, null);
  }

  public MemoryStorage incr(@NonNull String key, Options options) {
    return incr(key, options, null);
  }

  public MemoryStorage incr(@NonNull String key, final ResponseListener<Long> listener) {
    return incr(key, null, listener);
  }

  public MemoryStorage incr(@NonNull String key, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("incr", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage incrby(@NonNull String key, long value) {
    return incrby(key, value, null, null);
  }

  public MemoryStorage incrby(@NonNull String key, long value, Options options) {
    return incrby(key, value, options, null);
  }

  public MemoryStorage incrby(@NonNull String key, long value, final ResponseListener<Long> listener) {
    return incrby(key, value, null, listener);
  }

  public MemoryStorage incrby(@NonNull String key, long value, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
      );

    send("incrby", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage incrbyfloat(@NonNull String key, double value) {
    return incrbyfloat(key, value, null, null);
  }

  public MemoryStorage incrbyfloat(@NonNull String key, double value, final ResponseListener<Double> listener) {
    return incrbyfloat(key, value, null, listener);
  }

  public MemoryStorage incrbyfloat(@NonNull String key, double value, Options options) {
    return incrbyfloat(key, value, options, null);
  }

  public MemoryStorage incrbyfloat(@NonNull String key, double value, Options options, final ResponseListener<Double> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
      );

    send("incrbyfloat", query, options, listener != null ? getCallbackDouble(listener) : null);

    return this;
  }

  public void keys(@NonNull String pattern, @NonNull final ResponseListener<JSONArray> listener) {
    keys(pattern, null, listener);
  }

  public void keys(@NonNull String pattern, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("pattern", pattern);

    send("keys", query, options, getCallbackArray(listener));
  }

  public void lindex(@NonNull String key, long index, @NonNull final ResponseListener<String> listener) {
    lindex(key, index, null, listener);
  }

  public void lindex(@NonNull String key, long index, Options options, @NonNull final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key).put("index", index);

    send("lindex", query, options, getCallbackString(listener));
  }

  public MemoryStorage linsert(@NonNull String key, @NonNull String position, @NonNull String pivot, @NonNull String value) {
    return linsert(key, position, pivot, value, null, null);
  }

  public MemoryStorage linsert(@NonNull String key, @NonNull String position, @NonNull String pivot, @NonNull String value, final ResponseListener<Long> listener) {
    return linsert(key, position, pivot, value, null, listener);
  }

  public MemoryStorage linsert(@NonNull String key, @NonNull String position, @NonNull String pivot, @NonNull String value, Options options) {
    return linsert(key, position, pivot, value, options, null);
  }

  public MemoryStorage linsert(@NonNull String key, @NonNull String position, @NonNull String pivot, @NonNull String value, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("position", position)
        .put("pivot", pivot)
        .put("value", value)
      );

    send("linsert", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void llen(@NonNull String key, @NonNull final ResponseListener<Long> listener) {
    llen(key, null, listener);
  }

  public void llen(@NonNull String key, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("llen", query, options, getCallbackLong(listener));
  }

  public MemoryStorage lpop(@NonNull String key) {
    return lpop(key, null, null);
  }

  public MemoryStorage lpop(@NonNull String key, final ResponseListener<String> listener) {
    return lpop(key, null, listener);
  }

  public MemoryStorage lpop(@NonNull String key, Options options) {
    return lpop(key, options, null);
  }

  public MemoryStorage lpop(@NonNull String key, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("lpop", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage lpush(@NonNull String key, @NonNull JSONArray values) {
    return lpush(key, values, null, null);
  }

  public MemoryStorage lpush(@NonNull String key, @NonNull JSONArray values, final ResponseListener<Long> listener) {
    return lpush(key, values, null, listener);
  }

  public MemoryStorage lpush(@NonNull String key, @NonNull JSONArray values, Options options) {
    return lpush(key, values, options, null);
  }

  public MemoryStorage lpush(@NonNull String key, @NonNull JSONArray values, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("values", values)
      );

    send("lpush", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage lpushx(@NonNull String key, @NonNull final String value) {
    return lpushx(key, value, null, null);
  }

  public MemoryStorage lpushx(@NonNull String key, @NonNull final String value, Options options) {
    return lpushx(key, value, options, null);
  }

  public MemoryStorage lpushx(@NonNull String key, @NonNull final String value, final ResponseListener<Long> listener) {
    return lpushx(key, value, null, listener);
  }

  public MemoryStorage lpushx(@NonNull String key, @NonNull final String value, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
      );

    send("lpushx", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void lrange(@NonNull String key, long start, long stop, @NonNull final ResponseListener<JSONArray> listener) {
    lrange(key, start, stop, null, listener);
  }

  public void lrange(@NonNull String key, long start, long stop, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("start", start)
      .put("stop", stop);

    send("lrange", query, options, getCallbackArray(listener));
  }

  public MemoryStorage lrem(@NonNull String key, long count, @NonNull String value) {
    return lrem(key, count, value, null, null);
  }

  public MemoryStorage lrem(@NonNull String key, long count, @NonNull String value, final ResponseListener<Long> listener) {
    return lrem(key, count, value, null, listener);
  }

  public MemoryStorage lrem(@NonNull String key, long count, @NonNull String value, Options options) {
    return lrem(key, count, value, options, null);
  }

  public MemoryStorage lrem(@NonNull String key, long count, @NonNull String value, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("count", count)
        .put("value", value)
      );

    send("lrem", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage lset(@NonNull String key, long index, @NonNull String value) {
    return lset(key, index, value, null, null);
  }

  public MemoryStorage lset(@NonNull String key, long index, @NonNull String value, final ResponseListener<String> listener) {
    return lset(key, index, value, null, listener);
  }

  public MemoryStorage lset(@NonNull String key, long index, @NonNull String value, Options options) {
    return lset(key, index, value, options, null);
  }

  public MemoryStorage lset(@NonNull String key, long index, @NonNull String value, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("index", index)
        .put("value", value)
      );

    send("lset", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage ltrim(@NonNull String key, long start, long stop) {
    return ltrim(key, start, stop, null, null);
  }

  public MemoryStorage ltrim(@NonNull String key, long start, long stop, final ResponseListener<String> listener) {
    return ltrim(key, start, stop, null, listener);
  }

  public MemoryStorage ltrim(@NonNull String key, long start, long stop, Options options) {
    return ltrim(key, start, stop, options, null);
  }

  public MemoryStorage ltrim(@NonNull String key, long start, long stop, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("start", start)
        .put("stop", stop)
      );

    send("ltrim", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public void mget(@NonNull JSONArray keys, @NonNull final ResponseListener<JSONArray> listener) {
    mget(keys, null, listener);
  }

  public void mget(@NonNull JSONArray keys, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("keys", keys);

    send("mget", query, options, getCallbackArray(listener));
  }

  public MemoryStorage mset(@NonNull JSONArray entries) {
    return mset(entries, null, null);
  }

  public MemoryStorage mset(@NonNull JSONArray entries, Options options) {
    return mset(entries, options, null);
  }

  public MemoryStorage mset(@NonNull JSONArray entries, final ResponseListener<String> listener) {
    return mset(entries, null, listener);
  }

  public MemoryStorage mset(@NonNull JSONArray entries, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("body", new KuzzleJSONObject().put("entries", entries));

    send("mset", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage msetnx(@NonNull JSONArray entries) {
    return msetnx(entries, null, null);
  }

  public MemoryStorage msetnx(@NonNull JSONArray entries, Options options) {
    return msetnx(entries, options, null);
  }

  public MemoryStorage msetnx(@NonNull JSONArray entries, final ResponseListener<Integer> listener) {
    return msetnx(entries, null, listener);
  }

  public MemoryStorage msetnx(@NonNull JSONArray entries, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("body", new KuzzleJSONObject().put("entries", entries));

    send("msetnx", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public void object(@NonNull String key, @NonNull String subcommand, @NonNull final ResponseListener<String> listener) {
    object(key, subcommand, null, listener);
  }

  public void object(@NonNull String key, @NonNull String subcommand, Options options, @NonNull final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("subcommand", subcommand);

    send("object", query, options, getCallbackString(listener));
  }

  public MemoryStorage persist(@NonNull String key) {
    return persist(key, null, null);
  }

  public MemoryStorage persist(@NonNull String key, final ResponseListener<Integer> listener) {
    return persist(key, null, listener);
  }

  public MemoryStorage persist(@NonNull String key, Options options) {
    return persist(key, options, null);
  }

  public MemoryStorage persist(@NonNull String key, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("persist", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public MemoryStorage pexpire(@NonNull String key, long milliseconds) {
    return pexpire(key, milliseconds, null, null);
  }

  public MemoryStorage pexpire(@NonNull String key, long milliseconds, Options options) {
    return pexpire(key, milliseconds, options, null);
  }

  public MemoryStorage pexpire(@NonNull String key, long milliseconds, final ResponseListener<Integer> listener) {
    return pexpire(key, milliseconds, null, listener);
  }

  public MemoryStorage pexpire(@NonNull String key, long milliseconds, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("milliseconds", milliseconds)
      );

    send("pexpire", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public MemoryStorage pexpireat(@NonNull String key, long timestamp) {
    return pexpireat(key, timestamp, null, null);
  }

  public MemoryStorage pexpireat(@NonNull String key, long timestamp, Options options) {
    return pexpireat(key, timestamp, options, null);
  }

  public MemoryStorage pexpireat(@NonNull String key, long timestamp, final ResponseListener<Integer> listener) {
    return pexpireat(key, timestamp, null, listener);
  }

  public MemoryStorage pexpireat(@NonNull String key, long timestamp, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("timestamp", timestamp)
      );

    send("pexpireat", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public MemoryStorage pfadd(@NonNull String key, @NonNull JSONArray elements) {
    return pfadd(key, elements, null, null);
  }

  public MemoryStorage pfadd(@NonNull String key, @NonNull JSONArray elements, final ResponseListener<Integer> listener) {
    return pfadd(key, elements, null, listener);
  }

  public MemoryStorage pfadd(@NonNull String key, @NonNull JSONArray elements, Options options) {
    return pfadd(key, elements, options, null);
  }

  public MemoryStorage pfadd(@NonNull String key, @NonNull JSONArray elements, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("elements", elements)
      );

    send("pfadd", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public void pfcount(@NonNull JSONArray keys, @NonNull final ResponseListener<Long> listener) {
    pfcount(keys, null, listener);
  }

  public void pfcount(@NonNull JSONArray keys, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("keys", keys);

    send("pfcount", query, options, getCallbackLong(listener));
  }

  public MemoryStorage pfmerge(@NonNull String key, @NonNull JSONArray sources) {
    return pfmerge(key, sources, null, null);
  }

  public MemoryStorage pfmerge(@NonNull String key, @NonNull JSONArray sources, final ResponseListener<String> listener) {
    return pfmerge(key, sources, null, listener);
  }

  public MemoryStorage pfmerge(@NonNull String key, @NonNull JSONArray sources, Options options) {
    return pfmerge(key, sources, options, null);
  }

  public MemoryStorage pfmerge(@NonNull String key, @NonNull JSONArray sources, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("sources", sources)
      );

    send("pfmerge", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public void ping(@NonNull final ResponseListener<String> listener) {
    ping(null, listener);
  }

  public void ping(Options options, @NonNull final ResponseListener<String> listener) {
    send("ping", new KuzzleJSONObject(), options, getCallbackString(listener));
  }

  public MemoryStorage psetex(@NonNull String key, @NonNull String value, long milliseconds) {
    return psetex(key, value, milliseconds, null, null);
  }

  public MemoryStorage psetex(@NonNull String key, @NonNull String value, long milliseconds, final ResponseListener<String> listener) {
    return psetex(key, value, milliseconds, null, listener);
  }

  public MemoryStorage psetex(@NonNull String key, @NonNull String value, long milliseconds, Options options) {
    return psetex(key, value, milliseconds, options, null);
  }

  public MemoryStorage psetex(@NonNull String key, @NonNull String value, long milliseconds, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
        .put("milliseconds", milliseconds)
      );

    send("psetex", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public void pttl(@NonNull String key, @NonNull final ResponseListener<Long> listener) {
    pttl(key, null, listener);
  }

  public void pttl(@NonNull String key, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("pttl", query, options, getCallbackLong(listener));
  }

  public void randomkey(@NonNull final ResponseListener<String> listener) {
    randomkey(null, listener);
  }

  public void randomkey(Options options, @NonNull final ResponseListener<String> listener) {
    send("randomkey", new KuzzleJSONObject(), options, getCallbackString(listener));
  }

  public MemoryStorage rename(@NonNull String key, @NonNull String newkey) {
    return rename(key, newkey, null, null);
  }

  public MemoryStorage rename(@NonNull String key, @NonNull String newkey, final ResponseListener<String> listener) {
    return rename(key, newkey, null, listener);
  }

  public MemoryStorage rename(@NonNull String key, @NonNull String newkey, Options options) {
    return rename(key, newkey, options, null);
  }

  public MemoryStorage rename(@NonNull String key, @NonNull String newkey, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("newkey", newkey)
      );

    send("rename", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage renamenx(@NonNull String key, @NonNull String newkey) {
    return renamenx(key, newkey, null, null);
  }

  public MemoryStorage renamenx(@NonNull String key, @NonNull String newkey, final ResponseListener<String> listener) {
    return renamenx(key, newkey, null, listener);
  }

  public MemoryStorage renamenx(@NonNull String key, @NonNull String newkey, Options options) {
    return renamenx(key, newkey, options, null);
  }

  public MemoryStorage renamenx(@NonNull String key, @NonNull String newkey, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("newkey", newkey)
      );

    send("renamenx", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage rpop(@NonNull String key) {
    return rpop(key, null, null);
  }

  public MemoryStorage rpop(@NonNull String key, final ResponseListener<String> listener) {
    return rpop(key, null, listener);
  }

  public MemoryStorage rpop(@NonNull String key, Options options) {
    return rpop(key, options, null);
  }

  public MemoryStorage rpop(@NonNull String key, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("rpop", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage rpoplpush(@NonNull String source, @NonNull String destination) {
    return rpoplpush(source, destination, null, null);
  }

  public MemoryStorage rpoplpush(@NonNull String source, @NonNull String destination, final ResponseListener<String> listener) {
    return rpoplpush(source, destination, null, listener);
  }

  public MemoryStorage rpoplpush(@NonNull String source, @NonNull String destination, Options options) {
    return rpoplpush(source, destination, options, null);
  }

  public MemoryStorage rpoplpush(@NonNull String source, @NonNull String destination, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("body", new KuzzleJSONObject()
        .put("source", source)
        .put("destination", destination)
      );

    send("rpoplpush", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage rpush(@NonNull String key, @NonNull JSONArray values) {
    return rpush(key, values, null, null);
  }

  public MemoryStorage rpush(@NonNull String key, @NonNull JSONArray values, final ResponseListener<Long> listener) {
    return rpush(key, values, null, listener);
  }

  public MemoryStorage rpush(@NonNull String key, @NonNull JSONArray values, Options options) {
    return rpush(key, values, options, null);
  }

  public MemoryStorage rpush(@NonNull String key, @NonNull JSONArray values, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("values", values)
      );

    send("rpush", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage rpushx(@NonNull String key, @NonNull final String value) {
    return rpushx(key, value, null, null);
  }

  public MemoryStorage rpushx(@NonNull String key, @NonNull final String value, Options options) {
    return rpushx(key, value, options, null);
  }

  public MemoryStorage rpushx(@NonNull String key, @NonNull final String value, final ResponseListener<Long> listener) {
    return rpushx(key, value, null, listener);
  }

  public MemoryStorage rpushx(@NonNull String key, @NonNull final String value, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
      );

    send("rpushx", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage sadd(@NonNull String key, @NonNull JSONArray members) {
    return sadd(key, members, null, null);
  }

  public MemoryStorage sadd(@NonNull String key, @NonNull JSONArray members, final ResponseListener<Long> listener) {
    return sadd(key, members, null, listener);
  }

  public MemoryStorage sadd(@NonNull String key, @NonNull JSONArray members, Options options) {
    return sadd(key, members, options, null);
  }

  public MemoryStorage sadd(@NonNull String key, @NonNull JSONArray members, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("members", members)
      );

    send("sadd", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void scan(long cursor, @NonNull final ResponseListener<JSONArray> listener) {
    scan(cursor, null, listener);
  }

  public void scan(long cursor, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("cursor", cursor);

    if (options != null) {
      if (options.getCount() != null) {
        query.put("count", options.getCount());
      }

      if (options.getMatch() != null) {
        query.put("match", options.getMatch());
      }
    }

    send("scan", query, options, getCallbackArray(listener));
  }

  public void scard(@NonNull String key, @NonNull final ResponseListener<Long> listener) {
    scard(key, null, listener);
  }

  public void scard(@NonNull String key, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("scard", query, options, getCallbackLong(listener));
  }

  public void sdiff(@NonNull String key, @NonNull JSONArray keys, @NonNull final ResponseListener<JSONArray> listener) {
    sdiff(key, keys, null, listener);
  }

  public void sdiff(@NonNull String key, @NonNull JSONArray keys, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("keys", keys);

    send("sdiff", query, options, getCallbackArray(listener));
  }

  public MemoryStorage sdiffstore(@NonNull String key, @NonNull JSONArray keys, @NonNull String destination) {
    return sdiffstore(key, keys, destination, null, null);
  }

  public MemoryStorage sdiffstore(@NonNull String key, @NonNull JSONArray keys, @NonNull String destination, final ResponseListener<Long> listener) {
    return sdiffstore(key, keys, destination, null, listener);
  }

  public MemoryStorage sdiffstore(@NonNull String key, @NonNull JSONArray keys, @NonNull String destination, Options options) {
    return sdiffstore(key, keys, destination, options, null);
  }

  public MemoryStorage sdiffstore(@NonNull String key, @NonNull JSONArray keys, @NonNull String destination, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("destination", destination)
        .put("keys", keys)
      );

    send("sdiffstore", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage set(@NonNull String key, @NonNull String value) {
    return set(key, value, null, null);
  }

  public MemoryStorage set(@NonNull String key, @NonNull String value, final ResponseListener<String> listener) {
    return set(key, value, null, listener);
  }

  public MemoryStorage set(@NonNull String key, @NonNull String value, Options options) {
    return set(key, value, options, null);
  }

  public MemoryStorage set(@NonNull String key, @NonNull String value, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);
    KuzzleJSONObject body = new KuzzleJSONObject().put("value", value);

    if (options != null) {
      if (options.getEx() != null) {
        body.put("ex", options.getEx());
      }

      if (options.getPx() != null) {
        body.put("px", options.getPx());
      }

      body.put("nx", options.getNx());
      body.put("xx", options.getXx());
    }

    query.put("body", body);

    send("set", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage setex(@NonNull String key, @NonNull String value, long seconds) {
    return setex(key, value, seconds, null, null);
  }

  public MemoryStorage setex(@NonNull String key, @NonNull String value, long seconds, final ResponseListener<String> listener) {
    return setex(key, value, seconds, null, listener);
  }

  public MemoryStorage setex(@NonNull String key, @NonNull String value, long seconds, Options options) {
    return setex(key, value, seconds, options, null);
  }

  public MemoryStorage setex(@NonNull String key, @NonNull String value, long seconds, Options options, final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
        .put("seconds", seconds)
      );

    send("setex", query, options, listener != null ? getCallbackString(listener) : null);

    return this;
  }

  public MemoryStorage setnx(@NonNull String key, @NonNull String value) {
    return setnx(key, value, null, null);
  }

  public MemoryStorage setnx(@NonNull String key, @NonNull String value, final ResponseListener<Integer> listener) {
    return setnx(key, value, null, listener);
  }

  public MemoryStorage setnx(@NonNull String key, @NonNull String value, Options options) {
    return setnx(key, value, options, null);
  }

  public MemoryStorage setnx(@NonNull String key, @NonNull String value, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("value", value)
      );

    send("setnx", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public void sinter(@NonNull JSONArray keys, @NonNull final ResponseListener<JSONArray> listener) {
    sinter(keys, null, listener);
  }

  public void sinter(@NonNull JSONArray keys, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("keys", keys);

    send("sinter", query, options, getCallbackArray(listener));
  }

  public MemoryStorage sinterstore(@NonNull String destination, @NonNull JSONArray keys) {
    return sinterstore(destination, keys, null, null);
  }

  public MemoryStorage sinterstore(@NonNull String destination, @NonNull JSONArray keys, final ResponseListener<Long> listener) {
    return sinterstore(destination, keys, null, listener);
  }

  public MemoryStorage sinterstore(@NonNull String destination, @NonNull JSONArray keys, Options options) {
    return sinterstore(destination, keys, options, null);
  }

  public MemoryStorage sinterstore(@NonNull String destination, @NonNull JSONArray keys, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("body", new KuzzleJSONObject()
        .put("destination", destination)
        .put("keys", keys)
      );

    send("sinterstore", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void sismember(@NonNull String key, @NonNull String member, @NonNull final ResponseListener<Integer> listener) {
    sismember(key, member, null, listener);
  }

  public void sismember(@NonNull String key, @NonNull String member, Options options, @NonNull final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key).put("member", member);

    send("sismember", query, options, getCallbackInt(listener));
  }

  public void smembers(@NonNull String key, @NonNull final ResponseListener<JSONArray> listener) {
    smembers(key, null, listener);
  }

  public void smembers(@NonNull String key, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("smembers", query, options, getCallbackArray(listener));
  }

  public MemoryStorage smove(@NonNull String key, @NonNull String destination, @NonNull String member) {
    return smove(key, destination, member, null, null);
  }

  public MemoryStorage smove(@NonNull String key, @NonNull String destination, @NonNull String member, final ResponseListener<Integer> listener) {
    return smove(key, destination, member, null, listener);
  }

  public MemoryStorage smove(@NonNull String key, @NonNull String destination, @NonNull String member, Options options) {
    return smove(key, destination, member, options, null);
  }

  public MemoryStorage smove(@NonNull String key, @NonNull String destination, @NonNull String member, Options options, final ResponseListener<Integer> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("destination", destination)
        .put("member", member)
      );

    send("smove", query, options, listener != null ? getCallbackInt(listener) : null);

    return this;
  }

  public void sort(@NonNull String key, @NonNull final ResponseListener<JSONArray> listener) {
    sort(key, null, listener);
  }

  public void sort(@NonNull String key, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    if (options != null) {
      if (options.getBy() != null) {
        query.put("by", options.getBy());
      }

      if (options.getDirection() != null) {
        query.put("direction", options.getDirection());
      }

      if (options.getGet() != null) {
        query.put("get", options.getGet());
      }

      if (options.getLimit() != null) {
        query.put("limit", options.getLimit());
      }

      query.put("alpha", options.getAlpha());
    }

    send("sort", query, options, getCallbackArray(listener));
  }

  public MemoryStorage spop(@NonNull String key) {
    return spop(key, null, null);
  }

  public MemoryStorage spop(@NonNull String key, final ResponseListener<JSONArray> listener) {
    return spop(key, null, listener);
  }

  public MemoryStorage spop(@NonNull String key, Options options) {
    return spop(key, options, null);
  }

  public MemoryStorage spop(@NonNull String key, Options options, final ResponseListener<JSONArray> listener) {
    ResponseListener<JSONObject> callback = null;
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    if (options != null && options.getCount() != null) {
      query.put("body", new KuzzleJSONObject().put("count", options.getCount()));
    }

    if (listener != null) {
      callback = new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            if (response.get("result") instanceof String) {
              listener.onSuccess(new JSONArray().put(response.getString("result")));
            }
            else {
              listener.onSuccess(response.getJSONArray("result"));
            }
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      };
    }

    send("spop", query, options, callback);

    return this;
  }

  public MemoryStorage srandmember(@NonNull String key) {
    return srandmember(key, null, null);
  }

  public MemoryStorage srandmember(@NonNull String key, final ResponseListener<JSONArray> listener) {
    return srandmember(key, null, listener);
  }

  public MemoryStorage srandmember(@NonNull String key, Options options) {
    return srandmember(key, options, null);
  }

  public MemoryStorage srandmember(@NonNull String key, Options options, final ResponseListener<JSONArray> listener) {
    ResponseListener<JSONObject> callback = null;
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    if (options != null && options.getCount() != null) {
      query.put("count", options.getCount());
    }

    if (listener != null) {
      callback = new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            if (response.get("result") instanceof String) {
              listener.onSuccess(new JSONArray().put(response.getString("result")));
            }
            else {
              listener.onSuccess(response.getJSONArray("result"));
            }
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      };
    }

    send("srandmember", query, options, callback);

    return this;
  }

  public MemoryStorage srem(@NonNull String key, @NonNull JSONArray members) {
    return srem(key, members, null, null);
  }

  public MemoryStorage srem(@NonNull String key, @NonNull JSONArray members, final ResponseListener<Long> listener) {
    return srem(key, members, null, listener);
  }

  public MemoryStorage srem(@NonNull String key, @NonNull JSONArray members, Options options) {
    return srem(key, members, options, null);
  }

  public MemoryStorage srem(@NonNull String key, @NonNull JSONArray members, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("members", members)
      );

    send("srem", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void sscan(@NonNull String key, long cursor, @NonNull final ResponseListener<JSONArray> listener) {
    sscan(key, cursor, null, listener);
  }

  public void sscan(@NonNull String key, long cursor, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("cursor", cursor);

    if (options != null) {
      if (options.getCount() != null) {
        query.put("count", options.getCount());
      }

      if (options.getMatch() != null) {
        query.put("match", options.getMatch());
      }
    }

    send("sscan", query, options, getCallbackArray(listener));
  }

  public void strlen(@NonNull String key, @NonNull final ResponseListener<Long> listener) {
    strlen(key, null, listener);
  }

  public void strlen(@NonNull String key, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("strlen", query, options, getCallbackLong(listener));
  }

  public void sunion(@NonNull JSONArray keys, @NonNull final ResponseListener<JSONArray> listener) {
    sunion(keys, null, listener);
  }

  public void sunion(@NonNull JSONArray keys, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("keys", keys);

    send("sunion", query, options, getCallbackArray(listener));
  }

  public MemoryStorage sunionstore(@NonNull String destination, @NonNull JSONArray keys) {
    return sunionstore(destination, keys, null, null);
  }

  public MemoryStorage sunionstore(@NonNull String destination, @NonNull JSONArray keys, final ResponseListener<Long> listener) {
    return sunionstore(destination, keys, null, listener);
  }

  public MemoryStorage sunionstore(@NonNull String destination, @NonNull JSONArray keys, Options options) {
    return sunionstore(destination, keys, options, null);
  }

  public MemoryStorage sunionstore(@NonNull String destination, @NonNull JSONArray keys, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("body", new KuzzleJSONObject()
        .put("destination", destination)
        .put("keys", keys)
      );

    send("sunionstore", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void time(@NonNull final ResponseListener<JSONArray> listener) {
    time(null, listener);
  }

  public void time(Options options, @NonNull final ResponseListener<JSONArray> listener) {
    send(
      "time",
      new KuzzleJSONObject(),
      options,
      new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONArray raw = response.getJSONArray("result");
            JSONArray result = new JSONArray()
              .put(Integer.parseInt(raw.getString(0)))
              .put(Integer.parseInt(raw.getString(1)));

            listener.onSuccess(result);
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      }
    );
  }

  public MemoryStorage touch(@NonNull JSONArray keys) {
    return touch(keys, null, null);
  }

  public MemoryStorage touch(@NonNull JSONArray keys, Options options) {
    return touch(keys, options, null);
  }

  public MemoryStorage touch(@NonNull JSONArray keys, final ResponseListener<Long> listener) {
    return touch(keys, null, listener);
  }

  public MemoryStorage touch(@NonNull JSONArray keys, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("body", new KuzzleJSONObject().put("keys", keys));

    send("touch", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void ttl(@NonNull String key, @NonNull final ResponseListener<Long> listener) {
    ttl(key, null, listener);
  }

  public void ttl(@NonNull String key, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("ttl", query, options, getCallbackLong(listener));
  }

  public void type(@NonNull String key, @NonNull final ResponseListener<String> listener) {
    type(key, null, listener);
  }

  public void type(@NonNull String key, Options options, @NonNull final ResponseListener<String> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("type", query, options, getCallbackString(listener));
  }

  public MemoryStorage zadd(@NonNull String key, @NonNull JSONArray elements) {
    return zadd(key, elements, null, null);
  }

  public MemoryStorage zadd(@NonNull String key, @NonNull JSONArray elements, final ResponseListener<Long> listener) {
    return zadd(key, elements, null, listener);
  }

  public MemoryStorage zadd(@NonNull String key, @NonNull JSONArray elements, Options options) {
    return zadd(key, elements, options, null);
  }

  public MemoryStorage zadd(@NonNull String key, @NonNull JSONArray elements, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);
    KuzzleJSONObject body = new KuzzleJSONObject().put("elements", elements);

    if (options != null) {
      body.put("nx", options.getNx());
      body.put("xx", options.getXx());
      body.put("ch", options.getCh());
      body.put("incr", options.getIncr());
    }

    query.put("body", body);

    send("zadd", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void zcard(@NonNull String key, @NonNull final ResponseListener<Long> listener) {
    zcard(key, null, listener);
  }

  public void zcard(@NonNull String key, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key);

    send("zcard", query, options, getCallbackLong(listener));
  }

  public void zcount(@NonNull String key, long min, long max, @NonNull final ResponseListener<Long> listener) {
    zcount(key, min, max, null, listener);
  }

  public void zcount(@NonNull String key, long min, long max, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("min", min)
      .put("max", max);

    send("zcount", query, options, getCallbackLong(listener));
  }

  public MemoryStorage zincrby(@NonNull String key, @NonNull String member, double value) {
    return zincrby(key, member, value, null, null);
  }

  public MemoryStorage zincrby(@NonNull String key, @NonNull String member, double value, final ResponseListener<Double> listener) {
    return zincrby(key, member, value, null, listener);
  }

  public MemoryStorage zincrby(@NonNull String key, @NonNull String member, double value, Options options) {
    return zincrby(key, member, value, options, null);
  }

  public MemoryStorage zincrby(@NonNull String key, @NonNull String member, double value, Options options, final ResponseListener<Double> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("member", member)
        .put("value", value)
      );


    send("zincrby", query, options, listener != null ? getCallbackDouble(listener) : null);

    return this;
  }

  public MemoryStorage zinterstore(@NonNull String destination, @NonNull JSONArray keys) {
    return zinterstore(destination, keys, null, null);
  }

  public MemoryStorage zinterstore(@NonNull String destination, @NonNull JSONArray keys, final ResponseListener<Long> listener) {
    return zinterstore(destination, keys, null, listener);
  }

  public MemoryStorage zinterstore(@NonNull String destination, @NonNull JSONArray keys, Options options) {
    return zinterstore(destination, keys, options, null);
  }

  public MemoryStorage zinterstore(@NonNull String destination, @NonNull JSONArray keys, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", destination);
    KuzzleJSONObject body = new KuzzleJSONObject().put("keys", keys);

    if (options != null) {
      if (options.getAggregate() != null) {
        body.put("aggregate", options.getAggregate());
      }

      if (options.getWeights() != null) {
        body.put("weights", options.getWeights());
      }
    }

    query.put("body", body);

    send("zinterstore", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void zlexcount(@NonNull String key, @NonNull String min, @NonNull String max, @NonNull final ResponseListener<Long> listener) {
    zlexcount(key, min, max, null, listener);
  }

  public void zlexcount(@NonNull String key, @NonNull String min, @NonNull String max, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("min", min)
      .put("max", max);

    send("zlexcount", query, options, getCallbackLong(listener));
  }

  public void zrange(@NonNull String key, long start, long stop, @NonNull final ResponseListener<JSONArray> listener) {
    zrange(key, start, stop, null, listener);
  }

  public void zrange(@NonNull String key, long start, long stop, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("start", start)
      .put("stop", stop)
      .put("options", new JSONArray().put("withscores"));

    send(
      "zrange",
      query,
      options,
      new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(mapZrangeResults(response.getJSONArray("result")));
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      }
    );
  }

  public void zrangebylex(@NonNull String key, @NonNull String min, @NonNull String max, @NonNull final ResponseListener<JSONArray> listener) {
    zrangebylex(key, min, max, null, listener);
  }

  public void zrangebylex(@NonNull String key, @NonNull String min, @NonNull String max, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("min", min)
      .put("max", max);

    if (options != null) {
      if (options.getLimit() != null) {
        query.put("limit", options.getLimit());
      }
    }

    send("zrangebylex", query, options, getCallbackArray(listener));
  }

  public void zrangebyscore(@NonNull String key, double min, double max, @NonNull final ResponseListener<JSONArray> listener) {
    zrangebyscore(key, min, max, null, listener);
  }

  public void zrangebyscore(@NonNull String key, double min, double max, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("min", min)
      .put("max", max)
      .put("options", new JSONArray().put("withscores"));

    if (options != null) {
      if (options.getLimit() != null) {
        query.put("limit", options.getLimit());
      }
    }

    send(
      "zrangebyscore",
      query,
      options,
      new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(mapZrangeResults(response.getJSONArray("result")));
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      }
    );
  }

  public void zrank(@NonNull String key, @NonNull String member, @NonNull final ResponseListener<Long> listener) {
    zrank(key, member, null, listener);
  }

  public void zrank(@NonNull String key, @NonNull String member, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key).put("member", member);

    send("zrank", query, options, getCallbackLong(listener));
  }

  public MemoryStorage zrem(@NonNull String key, @NonNull JSONArray members) {
    return zrem(key, members, null, null);
  }

  public MemoryStorage zrem(@NonNull String key, @NonNull JSONArray members, final ResponseListener<Long> listener) {
    return zrem(key, members, null, listener);
  }

  public MemoryStorage zrem(@NonNull String key, @NonNull JSONArray members, Options options) {
    return zrem(key, members, options, null);
  }

  public MemoryStorage zrem(@NonNull String key, @NonNull JSONArray members, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("members", members)
      );

    send("zrem", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage zremrangebylex(@NonNull String key, @NonNull String min, @NonNull String max) {
    return zremrangebylex(key, min, max, null, null);
  }

  public MemoryStorage zremrangebylex(@NonNull String key, @NonNull String min, @NonNull String max, final ResponseListener<Long> listener) {
    return zremrangebylex(key, min, max, null, listener);
  }

  public MemoryStorage zremrangebylex(@NonNull String key, @NonNull String min, @NonNull String max, Options options) {
    return zremrangebylex(key, min, max, options, null);
  }

  public MemoryStorage zremrangebylex(@NonNull String key, @NonNull String min, @NonNull String max, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("min", min)
        .put("max", max)
      );

    send("zremrangebylex", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage zremrangebyrank(@NonNull String key, long min, long max) {
    return zremrangebyrank(key, min, max, null, null);
  }

  public MemoryStorage zremrangebyrank(@NonNull String key, long min, long max, final ResponseListener<Long> listener) {
    return zremrangebyrank(key, min, max, null, listener);
  }

  public MemoryStorage zremrangebyrank(@NonNull String key, long min, long max, Options options) {
    return zremrangebyrank(key, min, max, options, null);
  }

  public MemoryStorage zremrangebyrank(@NonNull String key, long min, long max, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("start", min)
        .put("stop", max)
      );

    send("zremrangebyrank", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public MemoryStorage zremrangebyscore(@NonNull String key, double min, double max) {
    return zremrangebyscore(key, min, max, null, null);
  }

  public MemoryStorage zremrangebyscore(@NonNull String key, double min, double max, final ResponseListener<Long> listener) {
    return zremrangebyscore(key, min, max, null, listener);
  }

  public MemoryStorage zremrangebyscore(@NonNull String key, double min, double max, Options options) {
    return zremrangebyscore(key, min, max, options, null);
  }

  public MemoryStorage zremrangebyscore(@NonNull String key, double min, double max, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("body", new KuzzleJSONObject()
        .put("min", min)
        .put("max", max)
      );

    send("zremrangebyscore", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }

  public void zrevrange(@NonNull String key, long start, long stop, @NonNull final ResponseListener<JSONArray> listener) {
    zrevrange(key, start, stop, null, listener);
  }

  public void zrevrange(@NonNull String key, long start, long stop, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("start", start)
      .put("stop", stop)
      .put("options", new JSONArray().put("withscores"));

    send(
      "zrevrange",
      query,
      options,
      new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(mapZrangeResults(response.getJSONArray("result")));
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      }
    );
  }

  public void zrevrangebylex(@NonNull String key, @NonNull String min, @NonNull String max, @NonNull final ResponseListener<JSONArray> listener) {
    zrevrangebylex(key, min, max, null, listener);
  }

  public void zrevrangebylex(@NonNull String key, @NonNull String min, @NonNull String max, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("min", min)
      .put("max", max);

    if (options != null) {
      if (options.getLimit() != null) {
        query.put("limit", options.getLimit());
      }
    }

    send("zrevrangebylex", query, options, getCallbackArray(listener));
  }

  public void zrevrangebyscore(@NonNull String key, double min, double max, @NonNull final ResponseListener<JSONArray> listener) {
    zrevrangebyscore(key, min, max, null, listener);
  }

  public void zrevrangebyscore(@NonNull String key, double min, double max, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("min", min)
      .put("max", max)
      .put("options", new JSONArray().put("withscores"));

    if (options != null) {
      if (options.getLimit() != null) {
        query.put("limit", options.getLimit());
      }
    }

    send(
      "zrevrangebyscore",
      query,
      options,
      new ResponseListener<JSONObject>() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(mapZrangeResults(response.getJSONArray("result")));
          }
          catch(JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      }
    );
  }

  public void zrevrank(@NonNull String key, @NonNull String member, @NonNull final ResponseListener<Long> listener) {
    zrevrank(key, member, null, listener);
  }

  public void zrevrank(@NonNull String key, @NonNull String member, Options options, @NonNull final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key).put("member", member);

    send("zrevrank", query, options, getCallbackLong(listener));
  }

  public void zscan(@NonNull String key, long cursor, @NonNull final ResponseListener<JSONArray> listener) {
    zscan(key, cursor, null, listener);
  }

  public void zscan(@NonNull String key, long cursor, Options options, @NonNull final ResponseListener<JSONArray> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject()
      .put("_id", key)
      .put("cursor", cursor);

    if (options != null) {
      if (options.getCount() != null) {
        query.put("count", options.getCount());
      }

      if (options.getMatch() != null) {
        query.put("match", options.getMatch());
      }
    }

    send("zscan", query, options, getCallbackArray(listener));
  }

  public void zscore(@NonNull String key, @NonNull String member, @NonNull final ResponseListener<Double> listener) {
    zscore(key, member, null, listener);
  }

  public void zscore(@NonNull String key, @NonNull String member, Options options, @NonNull final ResponseListener<Double> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", key).put("member", member);

    send("zscore", query, options, getCallbackDouble(listener));
  }

  public MemoryStorage zunionstore(@NonNull String destination, @NonNull JSONArray keys) {
    return zunionstore(destination, keys, null, null);
  }

  public MemoryStorage zunionstore(@NonNull String destination, @NonNull JSONArray keys, final ResponseListener<Long> listener) {
    return zunionstore(destination, keys, null, listener);
  }

  public MemoryStorage zunionstore(@NonNull String destination, @NonNull JSONArray keys, Options options) {
    return zunionstore(destination, keys, options, null);
  }

  public MemoryStorage zunionstore(@NonNull String destination, @NonNull JSONArray keys, Options options, final ResponseListener<Long> listener) {
    KuzzleJSONObject query = new KuzzleJSONObject().put("_id", destination);
    KuzzleJSONObject body = new KuzzleJSONObject().put("keys", keys);

    if (options != null) {
      if (options.getAggregate() != null) {
        body.put("aggregate", options.getAggregate());
      }

      if (options.getWeights() != null) {
        body.put("weights", options.getWeights());
      }
    }

    query.put("body", body);

    send("zunionstore", query, options, listener != null ? getCallbackLong(listener) : null);

    return this;
  }
}
