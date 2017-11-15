package io.kuzzle.sdk.core

import org.json.JSONException
import org.json.JSONObject
import org.json.JSONArray

import java.util.ArrayList
import java.util.Arrays

import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener
import io.kuzzle.sdk.util.KuzzleJSONObject

/**
 * Kuzzle's memory storage is a separate data store from the database layer.
 * It is internaly based on Redis. You can access most of Redis functions (all
 * lowercased), excepting:
 * * all cluster based functions
 * * all script based functions
 * * all cursors functions
 *
 */
 class MemoryStorage(private val kuzzle:Kuzzle) {
private val queryArgs = Kuzzle.QueryArgs()

init{
queryArgs.controller = "ms"
}

protected fun assignGeoradiusOptions(query:JSONObject, options:Options?) {
if (options != null)
{
val opts = JSONArray()

if (options.withcoord)
{
opts.put("withcoord")
}

if (options.withdist)
{
opts.put("withdist")
}

if (options.count != null)
{
opts.put("count").put(options.count)
}

if (options.sort != null)
{
opts.put(options.sort)
}

if (opts.length() > 0)
{
try
{
query.put("options", opts)
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}
}
}

@Throws(JSONException::class)
protected fun mapGeoradiusResults(points:JSONArray):Array<JSONObject> {
val mapped = arrayOfNulls<JSONObject>(points.length())

 // Simple array of point names (no options provided)
    if (points.get(0) is String)
{
for (i in 0 until points.length())
{
mapped[i] = JSONObject().put("name", points.getString(i))
}

return mapped
}

for (i in 0 until points.length())
{
val rawPoint = points.getJSONArray(i)
val p = JSONObject().put("name", rawPoint.getString(0))

for (j in 1 until rawPoint.length())
{
 // withcoord results are stored in an array...
        if (rawPoint.get(j) is JSONArray)
{
val coords = rawPoint.getJSONArray(j)

p
.put("coordinates", JSONArray()
.put(java.lang.Double.parseDouble(coords.getString(0)))
.put(java.lang.Double.parseDouble(coords.getString(1)))
)
}
else
{
 // ... while withdist results are not
          p.put("distance", java.lang.Double.parseDouble(rawPoint.getString(j)))
}
}

mapped[i] = p
}

return mapped
}

protected fun mapZrangeResults(members:JSONArray):Array<JSONObject> {
val mapped = ArrayList<JSONObject>(members.length() / 2)

try
{
var i = 0
while (i < members.length())
{
mapped.add(JSONObject()
.put("member", members.getString(i))
.put("score", java.lang.Double.parseDouble(members.getString(i + 1)))
)
i += 2
}
}
catch (e:JSONException) {
throw RuntimeException(e)
}

return mapped.toTypedArray()
}

protected fun send(action:String, query:KuzzleJSONObject, options:Options?, listener:ResponseListener<JSONObject>?) {
queryArgs.action = action

try
{
if (listener != null)
{
kuzzle.query(queryArgs, query, options, object:OnQueryDoneListener {
override fun onSuccess(response:JSONObject) {
listener.onSuccess(response)
}

override fun onError(error:JSONObject) {
listener.onError(error)
}
})
}
else
{
kuzzle.query(queryArgs, query, options!!)
}
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

protected fun getCallbackLong(listener:ResponseListener<Long>):ResponseListener<JSONObject> {
return object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(response.getLong("result"))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
}

protected fun getCallbackInt(listener:ResponseListener<Int>):ResponseListener<JSONObject> {
return object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(response.getInt("result"))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
}

protected fun getCallbackString(listener:ResponseListener<String>):ResponseListener<JSONObject> {
return object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(response.getString("result"))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
}

protected fun getCallbackDouble(listener:ResponseListener<Double>):ResponseListener<JSONObject> {
return object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(java.lang.Double.parseDouble(response.getString("result")))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
}

protected fun getCallbackStringArray(listener:ResponseListener<Array<String>>):ResponseListener<JSONObject> {
return object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
val arr = response.getJSONArray("result")
val result = arrayOfNulls<String>(arr.length())

for (i in 0 until arr.length())
{
result[i] = arr.getString(i)
}

listener.onSuccess(result)
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
}

protected fun getCallbackScanResult(listener:ResponseListener<JSONObject>):ResponseListener<JSONObject> {
return object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
val arr = response.getJSONArray("result")
val result = JSONObject()

try
{
result
.put("cursor", Integer.parseInt(arr.getString(0)))
.put("values", arr.getJSONArray(1))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

listener.onSuccess(result)
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
}

/**
 * [.append]
 */
   fun append(key:String, value:String, listener:ResponseListener<Long>):MemoryStorage {
return append(key, value, null, listener)
}

/**
 * Append a value to a key
 *
 * @param key  Key ID
 * @param  value  Value to append
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun append(key:String, value:String, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
)

send("append", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.bitcount]
 */
   fun bitcount(key:String, listener:ResponseListener<Long>) {
bitcount(key, null, listener)
}

/**
 * Counts the number of set bits (population counting)
 *
 * @param key       Key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun bitcount(key:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key)

if (options != null)
{
if (options.start != null)
{
query.put("start", options.start)
}

if (options.end != null)
{
query.put("end", options.end)
}
}

send("bitcount", query, options, getCallbackLong(listener))
}

/**
 * [.bitop]
 */
  @Throws(JSONException::class)
 fun bitop(key:String, operation:String, keys:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return bitop(key, operation, keys, null, listener)
}

/**
 * Performs a bitwise operation between multiple keys (containing string values) and stores the result in the destination key.
 * @param  key            Destination Key ID
 * @param  operation      Bit operation name
 * @param  keys           Keys on which to perform the bitwise operation
 * @param options Request options
 * @param  listener  Response callback listener
 * @return this
 * @throws JSONException
 */
  @Throws(JSONException::class)
@JvmOverloads  fun bitop(key:String, operation:String, keys:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("operation", operation)
.put("keys", JSONArray(Arrays.asList(*keys)))
)

send("bitop", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.bitpos]
 */
   fun bitpos(key:String, bit:Int, listener:ResponseListener<Long>) {
bitpos(key, bit, null, listener)
}

/**
 * Returns the position of the first bit set to 1 or 0 in a string, or in a substring
 * @param key       Key ID
 * @param bit       Bit to look for
 * @param options Request options
 * @param listener Response callback listener
 */
     fun bitpos(key:String, bit:Int, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("bit", bit)

if (options != null)
{
if (options.start != null)
{
query.put("start", options.start)
}

if (options.end != null)
{
query.put("end", options.end)
}
}

send("bitpos", query, options, getCallbackLong(listener))
}

/**
 * [.dbsize]
 */
   fun dbsize(listener:ResponseListener<Long>) {
dbsize(null, listener)
}

/**
 * Returns the number of keys in the application database.
 * @param options Request options
 * @param listener Response callback listener
 */
   fun dbsize(options:Options?, listener:ResponseListener<Long>) {
send("dbsize", KuzzleJSONObject(), options, getCallbackLong(listener))
}

/**
 * [.decr]
 */
   fun decr(key:String, listener:ResponseListener<Long>):MemoryStorage {
return decr(key, null, listener)
}

/**
 * Decrements the value of a key by 1
 * @param  key  Key ID
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun decr(key:String, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject().put("_id", key)

send("decr", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.decrby]
 */
   fun decrby(key:String, value:Long, listener:ResponseListener<Long>):MemoryStorage {
return decrby(key, value, null, listener)
}

/**
 * Decrements the value of a key by a given value
 * @param  key    Key ID
 * @param  value  Decrement value
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun decrby(key:String, value:Long, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
)

send("decrby", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.del]
 */
   fun del(keys:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return del(keys, null, listener)
}

/**
 * Delete keys
 * @param  keys  Key IDs to delete
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun del(keys:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("body", KuzzleJSONObject().put("keys", JSONArray(Arrays.asList(*keys))))

send("del", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.exists]
 */
   fun exists(keys:Array<String>, listener:ResponseListener<Long>) {
exists(keys, null, listener)
}

/**
 * Check if the specified keys exist
 *
 * @param keys      Key IDs
 * @param options Request options
 * @param listener Response callback listener
 */
   fun exists(keys:Array<String>, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("keys", JSONArray(Arrays.asList(*keys)))

send("exists", query, options, getCallbackLong(listener))
}

/**
 * [.expire]
 */
   fun expire(key:String, seconds:Long, listener:ResponseListener<Int>):MemoryStorage {
return expire(key, seconds, null, listener)
}

/**
 * Set an expiration timeout on a key
 * @param  key      Key ID
 * @param  seconds  Timeout (in seconds)
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun expire(key:String, seconds:Long, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("seconds", seconds)
)

send("expire", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.expireat]
 */
   fun expireat(key:String, timestamp:Long, listener:ResponseListener<Int>):MemoryStorage {
return expireat(key, timestamp, null, listener)
}

/**
 * Set an expiration timestamp to a key
 * @param  key        Key ID
 * @param  timestamp  Expiration timestamp (Epoch Time)
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun expireat(key:String, timestamp:Long, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("timestamp", timestamp)
)

send("expireat", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.flushdb]
 */
   fun flushdb(listener:ResponseListener<String>):MemoryStorage {
return flushdb(null, listener)
}

/**
 * Delete all keys from the database
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun flushdb(options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
send("flushdb", KuzzleJSONObject(), options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.geoadd]
 */
   fun geoadd(key:String, points:Array<JSONObject>, listener:ResponseListener<Long>):MemoryStorage {
return geoadd(key, points, null, listener)
}

/**
 * Add geospatial points to a key
 * @param  key     Key ID
 * @param  points  Geospatial points to add
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun geoadd(key:String, points:Array<JSONObject>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("points", JSONArray(Arrays.asList(*points)))
)

send("geoadd", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.geodist]
 */
   fun geodist(key:String, member1:String, member2:String, listener:ResponseListener<Double>) {
geodist(key, member1, member2, null, listener)
}

/**
 * Get the distance between two geospatial members of a key (see geoadd)
 * @param key       Key ID
 * @param member1   Geospatial point 1
 * @param member2   Geospatial point 2
 * @param options Request options
 * @param listener Response callback listener
 */
   fun geodist(key:String, member1:String, member2:String, options:Options?, listener:ResponseListener<Double>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("member1", member1)
.put("member2", member2)

if (options != null)
{
if (options.unit != null)
{
query.put("unit", options.unit)
}
}

send("geodist", query, options, getCallbackDouble(listener))
}

/**
 * [.geohash]
 */
   fun geohash(key:String, members:Array<String>, listener:ResponseListener<Array<String>>) {
geohash(key, members, null, listener)
}

/**
 * Return the geohash values for the provided key's members
 * @param key       Key ID
 * @param members   List of geospatial members
 * @param options Request options
 * @param listener Response callback listener
 */
   fun geohash(key:String, members:Array<String>, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("members", JSONArray(Arrays.asList(*members)))

send("geohash", query, options, getCallbackStringArray(listener))
}

/**
 * [.geopos]
 */
   fun geopos(key:String, members:Array<String>, listener:ResponseListener<Array<Array<Double>>>) {
geopos(key, members, null, listener)
}

/**
 * Return the longitude/latitude values for the provided key's members
 * @param key       Key ID
 * @param members   List of geospatial members
 * @param options Request options
 * @param listener Response callback listener
 */
   fun geopos(key:String, members:Array<String>, options:Options?, listener:ResponseListener<Array<Array<Double>>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("members", JSONArray(Arrays.asList(*members)))

send(
"geopos",
query,
options,
object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
 /*
             Converts the resulting array of arrays of strings,
             into an array of arrays of doubles
             */
            val raw = response.getJSONArray("result")
val result = Array<Array<Double>>(raw.length() ){ arrayOfNulls(2) }

for (i in 0 until raw.length())
{
val rawPos = raw.getJSONArray(i)

for (j in 0 until rawPos.length())
{
result[i][j] = java.lang.Double.parseDouble(rawPos.getString(j))
}
}

listener.onSuccess(result)
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
)
}

/**
 * [.georadius]
 */
   fun georadius(key:String, lon:Double, lat:Double, distance:Double, unit:String, listener:ResponseListener<Array<JSONObject>>) {
georadius(key, lon, lat, distance, unit, null, listener)
}

/**
 * Return the geospatial members of a key inside the provided radius
 * @param key       Key ID
 * @param lon       Longitude value for the radius center
 * @param lat       Latitude value for the radius center
 * @param distance  Radius value
 * @param unit      Radius distance unit
 * @param options Request options
 * @param listener Response callback listener
 */
   fun georadius(key:String, lon:Double, lat:Double, distance:Double, unit:String, options:Options?, listener:ResponseListener<Array<JSONObject>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("lon", lon)
.put("lat", lat)
.put("distance", distance)
.put("unit", unit)

assignGeoradiusOptions(query, options)

send(
"georadius",
query,
options,
object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(mapGeoradiusResults(response.getJSONArray("result")))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
)
}

/**
 * [.georadiusbymember]
 */
   fun georadiusbymember(key:String, member:String, distance:Double, unit:String, listener:ResponseListener<Array<JSONObject>>) {
georadiusbymember(key, member, distance, unit, null, listener)
}

/**
 * Returns the members (added with geoadd) of a given key inside
 * the provided geospatial radius, centered around one of a
 * key's member.
 *
 * @param key       Key ID
 * @param member    Geospatial member, center of the radius
 * @param distance  Radius value
 * @param unit      Radius distance unit
 * @param options Request options
 * @param listener Response callback listener
 */
   fun georadiusbymember(key:String, member:String, distance:Double, unit:String, options:Options?, listener:ResponseListener<Array<JSONObject>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("member", member)
.put("distance", distance)
.put("unit", unit)

assignGeoradiusOptions(query, options)

send(
"georadiusbymember",
query,
options,
object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(mapGeoradiusResults(response.getJSONArray("result")))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
)
}

/**
 * [.get]
 */
   operator fun get(key:String, listener:ResponseListener<String>) {
get(key, null, listener)
}


/**
 * Get a key's value
 * @param key       Key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   operator fun get(key:String, options:Options?, listener:ResponseListener<String>) {
val query = KuzzleJSONObject().put("_id", key)

send("get", query, options, getCallbackString(listener))
}

/**
 * [.getbit]
 */
   fun getbit(key:String, offset:Long, listener:ResponseListener<Int>) {
getbit(key, offset, null, listener)
}

/**
 * Returns the bit value at offset, in the string value stored in a key.
 * @param key       Key ID
 * @param offset    Offset value
 * @param options Request options
 * @param listener Response callback listener
 */
   fun getbit(key:String, offset:Long, options:Options?, listener:ResponseListener<Int>) {
val query = KuzzleJSONObject().put("_id", key).put("offset", offset)

send("getbit", query, options, getCallbackInt(listener))
}

/**
 * [.getrange]
 */
   fun getrange(key:String, start:Long, end:Long, listener:ResponseListener<String>) {
getrange(key, start, end, null, listener)
}

/**
 * Returns a substring of a key's value (index starts at position 0).
 * @param key       Key ID
 * @param start     Substring starting position
 * @param end       Substring ending position
 * @param options Request options
 * @param listener Response callback listener
 */
   fun getrange(key:String, start:Long, end:Long, options:Options?, listener:ResponseListener<String>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("start", start)
.put("end", end)

send("getrange", query, options, getCallbackString(listener))
}

/**
 * [.getset]
 */
   fun getset(key:String, value:String, listener:ResponseListener<String>):MemoryStorage {
return getset(key, value, null, listener)
}

/**
 * Sets a new value for a key and returns its previous value.
 * @param  key    Key ID
 * @param  value  New value
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun getset(key:String, value:String, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
)

send("getset", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.hdel]
 */
   fun hdel(key:String, fields:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return hdel(key, fields, null, listener)
}

/**
 * Remove fields from a hash
 * @param  key     Hash key ID
 * @param  fields  Fields to remove
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun hdel(key:String, fields:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("fields", JSONArray(Arrays.asList(*fields)))
)

send("hdel", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.hexists]
 */
   fun hexists(key:String, field:String, listener:ResponseListener<Int>) {
hexists(key, field, null, listener)
}

/**
 * Check if a field exists in a hash
 * @param key       Hash key ID
 * @param field     Field name
 * @param options Request options
 * @param listener Response callback listener
 */
   fun hexists(key:String, field:String, options:Options?, listener:ResponseListener<Int>) {
val query = KuzzleJSONObject().put("_id", key).put("field", field)

send("hexists", query, options, getCallbackInt(listener))
}

/**
 * [.hget]
 */
   fun hget(key:String, field:String, listener:ResponseListener<String>) {
hget(key, field, null, listener)
}

/**
 * Return the field's value of a hash
 * @param key       Hash key ID
 * @param field     Field name
 * @param options Request options
 * @param listener Response callback listener
 */
   fun hget(key:String, field:String, options:Options?, listener:ResponseListener<String>) {
val query = KuzzleJSONObject().put("_id", key).put("field", field)

send("hget", query, options, getCallbackString(listener))
}

/**
 * [.hgetall]
 */
   fun hgetall(key:String, listener:ResponseListener<JSONObject>) {
hgetall(key, null, listener)
}

/**
 * Return all fields and values of a hash
 * @param key       Hash key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun hgetall(key:String, options:Options?, listener:ResponseListener<JSONObject>) {
val query = KuzzleJSONObject().put("_id", key)

send(
"hgetall",
query,
options,
object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(response.getJSONObject("result"))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
)
}

/**
 * [.hincrby]
 */
   fun hincrby(key:String, field:String, value:Long, listener:ResponseListener<Long>):MemoryStorage {
return hincrby(key, field, value, null, listener)
}

/**
 * Increments the number stored in a hash field by the provided integer value.
 * @param  key    Hash Key ID
 * @param  field  Field to increment
 * @param  value  Increment value
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun hincrby(key:String, field:String, value:Long, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("field", field)
.put("value", value)
)

send("hincrby", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.hincrbyfloat]
 */
   fun hincrbyfloat(key:String, field:String, value:Double, listener:ResponseListener<Double>):MemoryStorage {
return hincrbyfloat(key, field, value, null, listener)
}

/**
 * Increments the number stored in a hash field by the provided float value.
 * @param  key    Hash key ID
 * @param  field  Field to increment
 * @param  value  Increment value
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun hincrbyfloat(key:String, field:String, value:Double, options:Options? = null, listener:ResponseListener<Double>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("field", field)
.put("value", value)
)

send("hincrbyfloat", query, options, if (listener != null) getCallbackDouble(listener) else null)

return this
}

/**
 * [.hkeys]
 */
   fun hkeys(key:String, listener:ResponseListener<Array<String>>) {
hkeys(key, null, listener)
}

/**
 * Return all the field names contained in a hash
 * @param key       Hash key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun hkeys(key:String, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject().put("_id", key)

send("hkeys", query, options, getCallbackStringArray(listener))
}

/**
 * [.hlen]
 */
   fun hlen(key:String, listener:ResponseListener<Long>) {
hlen(key, null, listener)
}

/**
 * Return the number of members of a hash
 * @param key       Hash key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun hlen(key:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key)

send("hlen", query, options, getCallbackLong(listener))
}

/**
 * [.hmget]
 */
   fun hmget(key:String, fields:Array<String>, listener:ResponseListener<Array<String>>) {
hmget(key, fields, null, listener)
}

/**
 * Returns the values of the specified hash’s fields.
 * @param key       Hash key ID
 * @param fields    Field names to return
 * @param options Request options
 * @param listener Response callback listener
 */
   fun hmget(key:String, fields:Array<String>, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("fields", JSONArray(Arrays.asList(*fields)))

send("hmget", query, options, getCallbackStringArray(listener))
}

/**
 * [.hmset]
 */
   fun hmset(key:String, entries:Array<JSONObject>, listener:ResponseListener<String>):MemoryStorage {
return hmset(key, entries, null, listener)
}

/**
 * Sets multiple fields at once in a hash.
 * @param  key      Hash key ID
 * @param  entries  Name-Value pairs to set
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun hmset(key:String, entries:Array<JSONObject>, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("entries", JSONArray(Arrays.asList(*entries)))
)

send("hmset", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.hscan]
 */
   fun hscan(key:String, cursor:Long, listener:ResponseListener<JSONObject>) {
hscan(key, cursor, null, listener)
}

/**
 * Identical to scan, except that hscan iterates the fields contained in a hash.
 * @param key       Hash key ID
 * @param cursor    Cursor position (0 to start a new scan)
 * @param options Request options
 * @param listener Response callback listener
 */
   fun hscan(key:String, cursor:Long, options:Options?, listener:ResponseListener<JSONObject>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("cursor", cursor)

if (options != null)
{
if (options.count != null)
{
query.put("count", options.count)
}

if (options.match != null)
{
query.put("match", options.match)
}
}

send("hscan", query, options, getCallbackScanResult(listener))
}

/**
 * [.hset]
 */
   fun hset(key:String, field:String, value:String, listener:ResponseListener<Int>):MemoryStorage {
return hset(key, field, value, null, listener)
}

/**
 * Sets a field and its value in a hash.
 * If the key does not exist, a new key holding a hash is created.
 * @param  key    Hash key ID
 * @param  field  Field name to set
 * @param  value  Value
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun hset(key:String, field:String, value:String, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("field", field)
.put("value", value)
)

send("hset", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.hsetnx]
 */
   fun hsetnx(key:String, field:String, value:String, listener:ResponseListener<Int>):MemoryStorage {
return hsetnx(key, field, value, null, listener)
}

/**
 * Sets a field and its value in a hash, only if the field does not already exist.
 * @param  key    Hash key ID
 * @param  field  Field name to set
 * @param  value  Value
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun hsetnx(key:String, field:String, value:String, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("field", field)
.put("value", value)
)

send("hsetnx", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.hstrlen]
 */
   fun hstrlen(key:String, field:String, listener:ResponseListener<Long>) {
hstrlen(key, field, null, listener)
}

/**
 * Returns the string length of a field’s value in a hash.
 * @param key       Hash key ID
 * @param field     Field name
 * @param options Request options
 * @param listener Response callback listener
 */
   fun hstrlen(key:String, field:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key).put("field", field)

send("hstrlen", query, options, getCallbackLong(listener))
}

/**
 * [.hvals]
 */
   fun hvals(key:String, listener:ResponseListener<Array<String>>) {
hvals(key, null, listener)
}

/**
 * Returns all values contained in a hash.
 * @param key       Hash key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun hvals(key:String, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject().put("_id", key)

send("hvals", query, options, getCallbackStringArray(listener))
}

/**
 * [.incr]
 */
   fun incr(key:String, listener:ResponseListener<Long>):MemoryStorage {
return incr(key, null, listener)
}

/**
 * Increments the number stored at key by 1.
 * If the key does not exist, it is set to 0 before performing the operation.
 * @param  key  Key ID
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun incr(key:String, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject().put("_id", key)

send("incr", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.incrby]
 */
   fun incrby(key:String, value:Long, listener:ResponseListener<Long>):MemoryStorage {
return incrby(key, value, null, listener)
}

/**
 * Increments the number stored at key by the provided integer value.
 * If the key does not exist, it is set to 0 before performing the operation.
 * @param  key    Key ID
 * @param  value  Increment value
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun incrby(key:String, value:Long, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
)

send("incrby", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.incrbyfloat]
 */
   fun incrbyfloat(key:String, value:Double, listener:ResponseListener<Double>):MemoryStorage {
return incrbyfloat(key, value, null, listener)
}

/**
 * Increments the number stored at key by the provided float value.
 * If the key does not exist, it is set to 0 before performing the operation.
 * @param  key    Key ID
 * @param  value  Increment value
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun incrbyfloat(key:String, value:Double, options:Options? = null, listener:ResponseListener<Double>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
)

send("incrbyfloat", query, options, if (listener != null) getCallbackDouble(listener) else null)

return this
}

/**
 * [.keys]
 */
   fun keys(pattern:String, listener:ResponseListener<Array<String>>) {
keys(pattern, null, listener)
}

/**
 * Returns all keys matching the provided pattern.
 * @param pattern   Match pattern
 * @param options Request options
 * @param listener Response callback listener
 */
   fun keys(pattern:String, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject().put("pattern", pattern)

send("keys", query, options, getCallbackStringArray(listener))
}

/**
 * [.lindex]
 */
   fun lindex(key:String, index:Long, listener:ResponseListener<String>) {
lindex(key, index, null, listener)
}

/**
 * Returns the element at the provided index in a list.
 * @param key       List ID
 * @param index     Index position
 * @param  options [description]
 * @param listener Response callback listener
 */
   fun lindex(key:String, index:Long, options:Options?, listener:ResponseListener<String>) {
val query = KuzzleJSONObject().put("_id", key).put("index", index)

send("lindex", query, options, getCallbackString(listener))
}

/**
 * [.linsert]
 */
   fun linsert(key:String, position:String, pivot:String, value:String, listener:ResponseListener<Long>):MemoryStorage {
return linsert(key, position, pivot, value, null, listener)
}


/**
 * Inserts a value in a list, either before or after the reference pivot value.
 * @param  key       List ID
 * @param  position  Either "after" or "before"
 * @param  pivot     Pivot value in the list
 * @param  value     Value to insert
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun linsert(key:String, position:String, pivot:String, value:String, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("position", position)
.put("pivot", pivot)
.put("value", value)
)

send("linsert", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.llen]
 */
   fun llen(key:String, listener:ResponseListener<Long>) {
llen(key, null, listener)
}

/**
 * Counts the number of items in a list.
 * @param key       List ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun llen(key:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key)

send("llen", query, options, getCallbackLong(listener))
}

/**
 * [.lpop]
 */
   fun lpop(key:String, listener:ResponseListener<String>):MemoryStorage {
return lpop(key, null, listener)
}

/**
 * Removes and returns the first element of a list.
 * @param  key  List ID
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun lpop(key:String, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject().put("_id", key)

send("lpop", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.lpush]
 */
   fun lpush(key:String, values:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return lpush(key, values, null, listener)
}

/**
 * Prepends the specified values to a list.
 * If the key does not exist, it is created holding
 * an empty list before performing the operation.
 * @param  key     List ID
 * @param  values  Values to prepend
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun lpush(key:String, values:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("values", JSONArray(Arrays.asList(*values)))
)

send("lpush", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.lpushx]
 */
   fun lpushx(key:String, value:String, listener:ResponseListener<Long>):MemoryStorage {
return lpushx(key, value, null, listener)
}

/**
 * Prepends the specified value to a list,
 * only if the key already exists and if it holds a list.
 * @param  key    List ID
 * @param  value  Value to prepend
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun lpushx(key:String, value:String, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
)

send("lpushx", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.lrange]
 */
   fun lrange(key:String, start:Long, stop:Long, listener:ResponseListener<Array<String>>) {
lrange(key, start, stop, null, listener)
}

/**
 * Returns the list elements between the start and stop positions (inclusive).
 * @param key       List ID
 * @param start     Start position
 * @param stop      End position
 * @param options Request options
 * @param listener Response callback listener
 */
   fun lrange(key:String, start:Long, stop:Long, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("start", start)
.put("stop", stop)

send("lrange", query, options, getCallbackStringArray(listener))
}

/**
 * [.lrem]
 */
   fun lrem(key:String, count:Long, value:String, listener:ResponseListener<Long>):MemoryStorage {
return lrem(key, count, value, null, listener)
}

/**
 * Removes the first count occurences of elements equal to value from a list.
 * @param  key    List ID
 * @param  count  Number of elements to remove
 * @param  value  Value to remove
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun lrem(key:String, count:Long, value:String, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("count", count)
.put("value", value)
)

send("lrem", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.lset]
 */
   fun lset(key:String, index:Long, value:String, listener:ResponseListener<String>):MemoryStorage {
return lset(key, index, value, null, listener)
}

/**
 * Sets the list element at index with the provided value.
 * @param  key    List ID
 * @param  index  Index of the element to set
 * @param  value  Element new value
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun lset(key:String, index:Long, value:String, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("index", index)
.put("value", value)
)

send("lset", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.ltrim]
 */
   fun ltrim(key:String, start:Long, stop:Long, listener:ResponseListener<String>):MemoryStorage {
return ltrim(key, start, stop, null, listener)
}

/**
 * Trims an existing list so that it will
 * contain only the specified range of elements specified.
 * @param  key    List ID
 * @param  start  Start position
 * @param  stop   End position
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun ltrim(key:String, start:Long, stop:Long, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("start", start)
.put("stop", stop)
)

send("ltrim", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.mget]
 */
   fun mget(keys:Array<String>, listener:ResponseListener<Array<String>>) {
mget(keys, null, listener)
}

/**
 * Returns the values of the provided keys.
 * @param keys      List of key identifiers
 * @param options Request options
 * @param listener Response callback listener
 */
   fun mget(keys:Array<String>, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject().put("keys", JSONArray(Arrays.asList(*keys)))

send("mget", query, options, getCallbackStringArray(listener))
}

/**
 * [.mset]
 */
   fun mset(entries:Array<JSONObject>, listener:ResponseListener<String>):MemoryStorage {
return mset(entries, null, listener)
}

/**
 * Sets the provided keys to their respective values.
 * If a key does not exist, it is created. Otherwise, the key’s value is overwritten.
 * @param  entries  Key-Value pairs
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun mset(entries:Array<JSONObject>, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("body", KuzzleJSONObject().put("entries", JSONArray(Arrays.asList(*entries))))

send("mset", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.msetnx]
 */
   fun msetnx(entries:Array<JSONObject>, listener:ResponseListener<Int>):MemoryStorage {
return msetnx(entries, null, listener)
}

/**
 * Sets the provided keys to their respective values, only if they do not exist.
 * If a key exists, then the whole operation is aborted and no key is set
 * @param  entries  Key-Value pairs to set
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun msetnx(entries:Array<JSONObject>, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("body", KuzzleJSONObject().put("entries", JSONArray(Arrays.asList(*entries))))

send("msetnx", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.object]
 */
   fun `object`(key:String, subcommand:String, listener:ResponseListener<String>) {
`object`(key, subcommand, null, listener)
}

/**
 * Inspects the low-level properties of a key.
 * @param key         Key ID
 * @param subcommand  Name of the low-level property to get
 * @param options Request options
 * @param listener Response callback listener
 */
   fun `object`(key:String, subcommand:String, options:Options?, listener:ResponseListener<String>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("subcommand", subcommand)

send("object", query, options, getCallbackString(listener))
}

/**
 * [.persist]
 */
   fun persist(key:String, listener:ResponseListener<Int>):MemoryStorage {
return persist(key, null, listener)
}

/**
 * Removes the expiration delay or timestamp from a key, making it persistent.
 * @param  key  Key ID
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun persist(key:String, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject().put("_id", key)

send("persist", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.pexpire]
 */
   fun pexpire(key:String, milliseconds:Long, listener:ResponseListener<Int>):MemoryStorage {
return pexpire(key, milliseconds, null, listener)
}

/**
 * Sets a timeout (in milliseconds) on a key.
 * After the timeout has expired, the key will automatically be deleted.
 * @param  key           Key ID
 * @param  milliseconds  Expiration time in milliseconds
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun pexpire(key:String, milliseconds:Long, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("milliseconds", milliseconds)
)

send("pexpire", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.pexpireat]
 */
   fun pexpireat(key:String, timestamp:Long, listener:ResponseListener<Int>):MemoryStorage {
return pexpireat(key, timestamp, null, listener)
}

/**
 * Sets an expiration timestamp on a key.
 * After the timestamp has been reached, the key will automatically be deleted.
 * @param  key        Key ID
 * @param  timestamp  Expiration timestamp (Epoch-time in milliseconds)
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun pexpireat(key:String, timestamp:Long, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("timestamp", timestamp)
)

send("pexpireat", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.pfadd]
 */
   fun pfadd(key:String, elements:Array<String>, listener:ResponseListener<Int>):MemoryStorage {
return pfadd(key, elements, null, listener)
}

/**
 * Adds elements to an HyperLogLog data structure.
 * @param  key       HyperLogLog ID
 * @param  elements  Array of elements to add
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun pfadd(key:String, elements:Array<String>, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("elements", JSONArray(Arrays.asList(*elements)))
)

send("pfadd", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.pfcount]
 */
   fun pfcount(keys:Array<String>, listener:ResponseListener<Long>) {
pfcount(keys, null, listener)
}

/**
 * Returns the probabilistic cardinality of a HyperLogLog data structure,
 * or of the merged HyperLogLog structures if more than 1 is provided (see pfadd).
 * @param keys      HyperLogLog ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun pfcount(keys:Array<String>, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("keys", JSONArray(Arrays.asList(*keys)))

send("pfcount", query, options, getCallbackLong(listener))
}

/**
 * [.pfmerge]
 */
   fun pfmerge(key:String, sources:Array<String>, listener:ResponseListener<String>):MemoryStorage {
return pfmerge(key, sources, null, listener)
}

/**
 * Merges multiple HyperLogLog data structures into an unique HyperLogLog structure
 * stored at key, approximating the cardinality of the union of the source structures.
 * @param  key      Destination key ID
 * @param  sources  Array of HyperLogLog ID to merge
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun pfmerge(key:String, sources:Array<String>, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("sources", JSONArray(Arrays.asList(*sources)))
)

send("pfmerge", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.ping]
 */
   fun ping(listener:ResponseListener<String>) {
ping(null, listener)
}

/**
 * Pings the memory storage database.
 * @param options Request options
 * @param listener Response callback listener
 */
   fun ping(options:Options?, listener:ResponseListener<String>) {
send("ping", KuzzleJSONObject(), options, getCallbackString(listener))
}

/**
 * [.psetex]
 */
   fun psetex(key:String, value:String, milliseconds:Long, listener:ResponseListener<String>):MemoryStorage {
return psetex(key, value, milliseconds, null, listener)
}

/**
 * Sets a key with the provided value, and an expiration delay
 * expressed in milliseconds. If the key does not exist, it is created beforehand.
 * @param  key           Key ID
 * @param  value         Value to set
 * @param  milliseconds  Expiration delay in milliseconds
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun psetex(key:String, value:String, milliseconds:Long, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
.put("milliseconds", milliseconds)
)

send("psetex", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.pttl]
 */
   fun pttl(key:String, listener:ResponseListener<Long>) {
pttl(key, null, listener)
}

/**
 * Returns the remaining time to live of a key, in milliseconds.
 * @param key       Key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun pttl(key:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key)

send("pttl", query, options, getCallbackLong(listener))
}

/**
 * [.randomkey]
 */
   fun randomkey(listener:ResponseListener<String>) {
randomkey(null, listener)
}

/**
 * Returns a random key from the memory storage.
 * @param options Request options
 * @param listener Response callback listener
 */
   fun randomkey(options:Options?, listener:ResponseListener<String>) {
send("randomkey", KuzzleJSONObject(), options, getCallbackString(listener))
}

/**
 * [.rename]
 */
   fun rename(key:String, newkey:String, listener:ResponseListener<String>):MemoryStorage {
return rename(key, newkey, null, listener)
}

/**
 * Renames a key to newkey. If newkey already exists, it is overwritten.
 * @param  key     Key ID
 * @param  newkey  New key ID
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun rename(key:String, newkey:String, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("newkey", newkey)
)

send("rename", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.renamenx]
 */
   fun renamenx(key:String, newkey:String, listener:ResponseListener<Int>):MemoryStorage {
return renamenx(key, newkey, null, listener)
}

/**
 * Renames a key to newkey, only if newkey does not already exist.
 * @param key      Key ID
 * @param newkey   New key ID
 * @param options  Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun renamenx(key:String, newkey:String, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("newkey", newkey)
)

send("renamenx", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.rpop]
 */
   fun rpop(key:String, listener:ResponseListener<String>):MemoryStorage {
return rpop(key, null, listener)
}

/**
 * Removes and returns the last element of a list.
 * @param  key  Key ID
 * @param options Request options
 * @param listener Response callback listener
 * @return this
 */
  @JvmOverloads  fun rpop(key:String, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject().put("_id", key)

send("rpop", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.rpoplpush]
 */
   fun rpoplpush(source:String, destination:String, listener:ResponseListener<String>):MemoryStorage {
return rpoplpush(source, destination, null, listener)
}

/**
 * Removes the last element of the list at source and
 * pushes it back at the start of the list at destination.
 * @param  source       Source key ID
 * @param  destination  Destination key ID
 * @param  options     [description]
 * @param  listener    [description]
 * @return             [description]
 */
  @JvmOverloads  fun rpoplpush(source:String, destination:String, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("body", KuzzleJSONObject()
.put("source", source)
.put("destination", destination)
)

send("rpoplpush", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.rpush]
 */
   fun rpush(key:String, values:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return rpush(key, values, null, listener)
}

/**
 * Appends the specified values at the end of a list.
 * If the key does not exist, it is created holding an empty
 * list before performing the operation.
 * @param  key       List ID
 * @param  values    Values to append
 * @param  options  [description]
 * @param  listener [description]
 * @return          [description]
 */
  @JvmOverloads  fun rpush(key:String, values:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("values", JSONArray(Arrays.asList(*values)))
)

send("rpush", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.rpushx]
 */
   fun rpushx(key:String, value:String, listener:ResponseListener<Long>):MemoryStorage {
return rpushx(key, value, null, listener)
}

/**
 * Appends the specified value at the end of a list,
 * only if the key already exists and if it holds a list.
 * @param  key       List ID
 * @param  value     Value to append
 * @param  options  [description]
 * @param  listener [description]
 * @return          [description]
 */
  @JvmOverloads  fun rpushx(key:String, value:String, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
)

send("rpushx", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.sadd]
 */
   fun sadd(key:String, members:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return sadd(key, members, null, listener)
}

/**
 * Adds members to a set of unique values stored at key.
 * If the key does not exist, it is created beforehand.
 * @param  key       Set ID
 * @param  members   Members to add
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun sadd(key:String, members:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("members", JSONArray(Arrays.asList(*members)))
)

send("sadd", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.scan]
 */
   fun scan(cursor:Long, listener:ResponseListener<JSONObject>) {
scan(cursor, null, listener)
}

/**
 * Iterates incrementally the set of keys in the database using a cursor.
 *
 * @param cursor    Cursor position (0 to start an iteration)
 * @param options Request options
 * @param listener Response callback listener
 */
   fun scan(cursor:Long, options:Options?, listener:ResponseListener<JSONObject>) {
val query = KuzzleJSONObject().put("cursor", cursor)

if (options != null)
{
if (options.count != null)
{
query.put("count", options.count)
}

if (options.match != null)
{
query.put("match", options.match)
}
}

send("scan", query, options, getCallbackScanResult(listener))
}

/**
 * [.scard]
 */
   fun scard(key:String, listener:ResponseListener<Long>) {
scard(key, null, listener)
}

/**
 * Returns the number of members stored in a set of unique values.
 * @param key      Set ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun scard(key:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key)

send("scard", query, options, getCallbackLong(listener))
}

/**
 * [.sdiff]
 */
   fun sdiff(key:String, keys:Array<String>, listener:ResponseListener<Array<String>>) {
sdiff(key, keys, null, listener)
}

/**
 * Returns the difference between the set of unique
 * values stored at key and the other provided sets.
 * @param key      Reference set ID
 * @param keys     Set IDs to compare
 * @param options Request options
 * @param listener Response callback listener
 */
   fun sdiff(key:String, keys:Array<String>, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("keys", JSONArray(Arrays.asList(*keys)))

send("sdiff", query, options, getCallbackStringArray(listener))
}

/**
 * [.sdiffstore]
 */
   fun sdiffstore(key:String, keys:Array<String>, destination:String, listener:ResponseListener<Long>):MemoryStorage {
return sdiffstore(key, keys, destination, null, listener)
}

/**
 * Computes the difference between the set of unique values stored at key and
 * the other provided sets, and stores the result in the key stored at destination.
 * @param  key         Reference set ID
 * @param  keys        Set IDs to compare
 * @param  destination Destination set ID
 * @param  options     [description]
 * @param  listener    [description]
 * @return this
 */
  @JvmOverloads  fun sdiffstore(key:String, keys:Array<String>, destination:String, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("destination", destination)
.put("keys", JSONArray(Arrays.asList(*keys)))
)

send("sdiffstore", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.set]
 */
   operator fun set(key:String, value:String, listener:ResponseListener<String>):MemoryStorage {
return set(key, value, null, listener)
}

/**
 * Creates a key holding the provided value, or overwrites it if it already exists.
 * @param  key      Key ID
 * @param  value    Value to set
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun set(key:String, value:String, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject().put("_id", key)
val body = KuzzleJSONObject().put("value", value)

if (options != null)
{
if (options.ex != null)
{
body.put("ex", options.ex)
}

if (options.px != null)
{
body.put("px", options.px)
}

body.put("nx", options.nx)
body.put("xx", options.xx)
}

query.put("body", body)

send("set", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.setex]
 */
   fun setex(key:String, value:String, seconds:Long, listener:ResponseListener<String>):MemoryStorage {
return setex(key, value, seconds, null, listener)
}

/**
 * Sets a key with the provided value, and an expiration delay
 * expressed in seconds. If the key does not exist, it is created beforehand.
 * @param  key      Key ID
 * @param  value    Value to set
 * @param  seconds  Expiration delay, in seconds
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun setex(key:String, value:String, seconds:Long, options:Options? = null, listener:ResponseListener<String>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
.put("seconds", seconds)
)

send("setex", query, options, if (listener != null) getCallbackString(listener) else null)

return this
}

/**
 * [.setnx]
 */
   fun setnx(key:String, value:String, listener:ResponseListener<Int>):MemoryStorage {
return setnx(key, value, null, listener)
}

/**
 * Sets a value on a key, only if it does not already exist.
 * @param  key      Key ID
 * @param  value    Value to set
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun setnx(key:String, value:String, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("value", value)
)

send("setnx", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.sinter]
 */
   fun sinter(keys:Array<String>, listener:ResponseListener<Array<String>>) {
sinter(keys, null, listener)
}

/**
 * Returns the intersection of the provided sets of unique values.
 * @param keys     Array of set IDs to intersect
 * @param options Request options
 * @param listener Response callback listener
 */
   fun sinter(keys:Array<String>, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject().put("keys", JSONArray(Arrays.asList(*keys)))

send("sinter", query, options, getCallbackStringArray(listener))
}

/**
 * [.sinterstore]
 */
   fun sinterstore(destination:String, keys:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return sinterstore(destination, keys, null, listener)
}

/**
 * Computes the intersection of the provided sets of unique values
 * and stores the result in the destination key.
 * @param  destination Destination key ID
 * @param  keys        Array of set IDs to intersect
 * @param  options     [description]
 * @param  listener    [description]
 * @return             [description]
 */
  @JvmOverloads  fun sinterstore(destination:String, keys:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("body", KuzzleJSONObject()
.put("destination", destination)
.put("keys", JSONArray(Arrays.asList(*keys)))
)

send("sinterstore", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.sismember]
 */
   fun sismember(key:String, member:String, listener:ResponseListener<Int>) {
sismember(key, member, null, listener)
}

/**
 * Checks if member is a member of the set of unique values stored at key.
 * @param key      Set ID
 * @param member   Member name
 * @param options Request options
 * @param listener Response callback listener
 */
   fun sismember(key:String, member:String, options:Options?, listener:ResponseListener<Int>) {
val query = KuzzleJSONObject().put("_id", key).put("member", member)

send("sismember", query, options, getCallbackInt(listener))
}

/**
 * [.smembers]
 */
   fun smembers(key:String, listener:ResponseListener<Array<String>>) {
smembers(key, null, listener)
}

/**
 * Returns the members of a set of unique values.
 * @param key      Set ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun smembers(key:String, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject().put("_id", key)

send("smembers", query, options, getCallbackStringArray(listener))
}

/**
 * [.smove]
 */
   fun smove(key:String, destination:String, member:String, listener:ResponseListener<Int>):MemoryStorage {
return smove(key, destination, member, null, listener)
}

/**
 * Moves a member from a set of unique values to another.
 * @param  key         Source set ID
 * @param  destination Destination set ID
 * @param  member      Member to move
 * @param  options     [description]
 * @param  listener    [description]
 * @return this
 */
  @JvmOverloads  fun smove(key:String, destination:String, member:String, options:Options? = null, listener:ResponseListener<Int>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("destination", destination)
.put("member", member)
)

send("smove", query, options, if (listener != null) getCallbackInt(listener) else null)

return this
}

/**
 * [.sort]
 */
   fun sort(key:String, listener:ResponseListener<Array<String>>) {
sort(key, null, listener)
}

/**
 * Sorts and returns elements contained in a list, a set of unique values or a
 * sorted set. By default, sorting is numeric and elements are compared by their
 * value interpreted as double precision floating point number.
 * @param key      ID of the key to sort
 * @param options Request options
 * @param listener Response callback listener
 */
   fun sort(key:String, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject().put("_id", key)

if (options != null)
{
if (options.by != null)
{
query.put("by", options.by)
}

if (options.direction != null)
{
query.put("direction", options.direction)
}

if (options.get != null)
{
query.put("get", JSONArray(Arrays.asList(*options.get!!)))
}

if (options.limit != null)
{
query.put("limit", JSONArray(Arrays.asList(*options.limit!!)))
}

query.put("alpha", options.alpha)
}

send("sort", query, options, getCallbackStringArray(listener))
}

/**
 * [.spop]
 */
   fun spop(key:String, listener:ResponseListener<Array<String>>):MemoryStorage {
return spop(key, null, listener)
}

/**
 * Removes and returns one or more elements at random from a set of unique values.
 * @param  key      Set ID
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun spop(key:String, options:Options? = null, listener:ResponseListener<Array<String>>? = null):MemoryStorage {
var callback:ResponseListener<JSONObject>? = null
val query = KuzzleJSONObject().put("_id", key)

if (options != null && options.count != null)
{
query.put("body", KuzzleJSONObject().put("count", options.count))
}

if (listener != null)
{
callback = object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
if (response.get("result") is String)
{
listener.onSuccess(arrayOf(response.getString("result")))
}
else
{
val arr = response.getJSONArray("result")
val elements = arrayOfNulls<String>(arr.length())

for (i in 0 until arr.length())
{
elements[i] = arr.getString(i)
}

listener.onSuccess(elements)
}
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
}

send("spop", query, options, callback)

return this
}

/**
 * [.srandmember]
 */
   fun srandmember(key:String, listener:ResponseListener<Array<String>>):MemoryStorage {
return srandmember(key, null, listener)
}

/**
 * Returns one or more members of a set of unique values, at random.
 * @param  key      Set ID
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
   fun srandmember(key:String, options:Options?, listener:ResponseListener<Array<String>>?):MemoryStorage {
var callback:ResponseListener<JSONObject>? = null
val query = KuzzleJSONObject().put("_id", key)

if (options != null && options.count != null)
{
query.put("count", options.count)
}

if (listener != null)
{
callback = object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
if (response.get("result") is String)
{
listener.onSuccess(arrayOf(response.getString("result")))
}
else
{
val arr = response.getJSONArray("result")
val elements = arrayOfNulls<String>(arr.length())

for (i in 0 until arr.length())
{
elements[i] = arr.getString(i)
}

listener.onSuccess(elements)
}
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
}

send("srandmember", query, options, callback)

return this
}

/**
 * [.srem]
 */
   fun srem(key:String, members:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return srem(key, members, null, listener)
}

/**
 * Removes members from a set of unique values.
 * @param  key      Set ID
 * @param  members  Members to remove
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun srem(key:String, members:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("members", JSONArray(Arrays.asList(*members)))
)

send("srem", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.sscan]
 */
   fun sscan(key:String, cursor:Long, listener:ResponseListener<JSONObject>) {
sscan(key, cursor, null, listener)
}

/**
 * Identical to scan, except that sscan iterates the members contained in a set
 * @param key       Set ID
 * @param cursor    Cursor position (0 to start a new scan)
 * @param options Request options
 * @param listener Response callback listener
 */
   fun sscan(key:String, cursor:Long, options:Options?, listener:ResponseListener<JSONObject>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("cursor", cursor)

if (options != null)
{
if (options.count != null)
{
query.put("count", options.count)
}

if (options.match != null)
{
query.put("match", options.match)
}
}

send("sscan", query, options, getCallbackScanResult(listener))
}

/**
 * [.strlen]
 */
   fun strlen(key:String, listener:ResponseListener<Long>) {
strlen(key, null, listener)
}

/**
 * Returns the length of a value stored at key.
 * @param key      Key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun strlen(key:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key)

send("strlen", query, options, getCallbackLong(listener))
}

/**
 * [.sunion]
 */
   fun sunion(keys:Array<String>, listener:ResponseListener<Array<String>>) {
sunion(keys, null, listener)
}

/**
 * Returns the union of the provided sets of unique values.
 * @param keys     Set IDs
 * @param options Request options
 * @param listener Response callback listener
 */
   fun sunion(keys:Array<String>, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject().put("keys", JSONArray(Arrays.asList(*keys)))

send("sunion", query, options, getCallbackStringArray(listener))
}

/**
 * [.sunionstore]
 */
   fun sunionstore(destination:String, keys:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return sunionstore(destination, keys, null, listener)
}

/**
 * Computes the union of the provided sets of unique values
 * and stores the result in the destination key.
 * @param  destination Destination set ID
 * @param  keys        Array of set IDs
 * @param  options     [description]
 * @param  listener    [description]
 * @return this
 */
  @JvmOverloads  fun sunionstore(destination:String, keys:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("body", KuzzleJSONObject()
.put("destination", destination)
.put("keys", JSONArray(Arrays.asList(*keys)))
)

send("sunionstore", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.time]
 */
   fun time(listener:ResponseListener<Array<Long>>) {
time(null, listener)
}

/**
 * Returns the current server time.
 * @param options Request options
 * @param listener Response callback listener
 */
   fun time(options:Options?, listener:ResponseListener<Array<Long>>) {
send(
"time",
KuzzleJSONObject(),
options,
object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
val raw = response.getJSONArray("result")
listener.onSuccess(arrayOf(java.lang.Long.parseLong(raw.getString(0)), java.lang.Long.parseLong(raw.getString(1))))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
)
}

/**
 * [.touch]
 */
   fun touch(keys:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return touch(keys, null, listener)
}

/**
 * Alters the last access time of one or multiple keys.
 * A key is ignored if it does not exist.
 * @param  keys     Array of key IDs to alter
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun touch(keys:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("body", KuzzleJSONObject().put("keys", JSONArray(Arrays.asList(*keys))))

send("touch", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.ttl]
 */
   fun ttl(key:String, listener:ResponseListener<Long>) {
ttl(key, null, listener)
}

/**
 * Returns the remaining time to live of a key, in seconds, or a negative value
 * if the key does not exist or if it is persistent.
 * @param key      Key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun ttl(key:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key)

send("ttl", query, options, getCallbackLong(listener))
}

/**
 * [.type]
 */
   fun type(key:String, listener:ResponseListener<String>) {
type(key, null, listener)
}

/**
 * Returns the type of the value held by a key.
 * @param key      Key ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun type(key:String, options:Options?, listener:ResponseListener<String>) {
val query = KuzzleJSONObject().put("_id", key)

send("type", query, options, getCallbackString(listener))
}

/**
 * [.zadd]
 */
   fun zadd(key:String, elements:Array<JSONObject>, listener:ResponseListener<Long>):MemoryStorage {
return zadd(key, elements, null, listener)
}

/**
 * Adds the specified elements to the sorted set stored at key.
 * If the key does not exist, it is created, holding an empty sorted set.
 * If it already exists and does not hold a sorted set, an error is returned.
 * @param  key      Sorted set ID
 * @param  elements Elements to add
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun zadd(key:String, elements:Array<JSONObject>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject().put("_id", key)
val body = KuzzleJSONObject().put("elements", JSONArray(Arrays.asList(*elements)))

if (options != null)
{
body.put("nx", options.nx)
body.put("xx", options.xx)
body.put("ch", options.ch)
body.put("incr", options.incr)
}

query.put("body", body)

send("zadd", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.zcard]
 */
   fun zcard(key:String, listener:ResponseListener<Long>) {
zcard(key, null, listener)
}

/**
 * Returns the number of elements held by a sorted set.
 * @param key      Sorted set ID
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zcard(key:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key)

send("zcard", query, options, getCallbackLong(listener))
}

/**
 * [.zcount]
 */
   fun zcount(key:String, min:Long, max:Long, listener:ResponseListener<Long>) {
zcount(key, min, max, null, listener)
}

/**
 * Returns the number of elements held by a sorted set
 * with a score between the provided min and max values.
 * @param key      Sorted set ID
 * @param min      Minimum score
 * @param max      Maximum score
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zcount(key:String, min:Long, max:Long, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("min", min)
.put("max", max)

send("zcount", query, options, getCallbackLong(listener))
}

/**
 * [.zincrby]
 */
   fun zincrby(key:String, member:String, value:Double, listener:ResponseListener<Double>):MemoryStorage {
return zincrby(key, member, value, null, listener)
}

/**
 * Increments the score of a member in a sorted set by the provided value.
 * @param  key      Sorted set ID
 * @param  member   Member to increment
 * @param  value    Increment value
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun zincrby(key:String, member:String, value:Double, options:Options? = null, listener:ResponseListener<Double>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("member", member)
.put("value", value)
)


send("zincrby", query, options, if (listener != null) getCallbackDouble(listener) else null)

return this
}

/**
 * [.zinterstore]
 */
   fun zinterstore(destination:String, keys:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return zinterstore(destination, keys, null, listener)
}

/**
 * Computes the intersection of the provided sorted sets and
 * stores the result in the destination key.
 * @param  destination Destination ID
 * @param  keys        Array of sorted set IDs
 * @param  options     [description]
 * @param  listener    [description]
 * @return this
 */
  @JvmOverloads  fun zinterstore(destination:String, keys:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject().put("_id", destination)
val body = KuzzleJSONObject().put("keys", JSONArray(Arrays.asList(*keys)))

if (options != null)
{
if (options.aggregate != null)
{
body.put("aggregate", options.aggregate)
}

if (options.weights != null)
{
body.put("weights", JSONArray(Arrays.asList(*options.weights!!)))
}
}

query.put("body", body)

send("zinterstore", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.zlexcount]
 */
   fun zlexcount(key:String, min:String, max:String, listener:ResponseListener<Long>) {
zlexcount(key, min, max, null, listener)
}

/**
 * Counts elements in a sorted set where all members have equal
 * score, using lexicographical ordering.
 * @param key      Sorted set ID
 * @param min      Minimum value
 * @param max      Maximum value
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zlexcount(key:String, min:String, max:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("min", min)
.put("max", max)

send("zlexcount", query, options, getCallbackLong(listener))
}

/**
 * [.zrange]
 */
   fun zrange(key:String, start:Long, stop:Long, listener:ResponseListener<Array<JSONObject>>) {
zrange(key, start, stop, null, listener)
}

/**
 * Returns elements from a sorted set depending on their position in the set,
 * from a start position index to a stop position index (inclusives).
 * @param key      Sorted set ID
 * @param start    Start position
 * @param stop     End position
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zrange(key:String, start:Long, stop:Long, options:Options?, listener:ResponseListener<Array<JSONObject>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("start", start)
.put("stop", stop)
.put("options", JSONArray().put("withscores"))

send(
"zrange",
query,
options,
object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(mapZrangeResults(response.getJSONArray("result")))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
)
}

/**
 * [.zrangebylex]
 */
   fun zrangebylex(key:String, min:String, max:String, listener:ResponseListener<Array<String>>) {
zrangebylex(key, min, max, null, listener)
}

/**
 * Returns elements in a sorted set where all members have equal
 * score, using lexicographical ordering.
 * @param key      Sorted set ID
 * @param min      Minimum value
 * @param max      Maximum value
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zrangebylex(key:String, min:String, max:String, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("min", min)
.put("max", max)

if (options != null)
{
if (options.limit != null)
{
query.put("limit", JSONArray(Arrays.asList(*options.limit!!)))
}
}

send("zrangebylex", query, options, getCallbackStringArray(listener))
}

/**
 * [.zrangebyscore]
 */
   fun zrangebyscore(key:String, min:Double, max:Double, listener:ResponseListener<Array<JSONObject>>) {
zrangebyscore(key, min, max, null, listener)
}

/**
 * Returns all the elements in the sorted set at key with a
 * score between min and max (inclusive). T
 * @param key      Sorted set ID
 * @param min      Minimum score
 * @param max      Maximum score
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zrangebyscore(key:String, min:Double, max:Double, options:Options?, listener:ResponseListener<Array<JSONObject>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("min", min)
.put("max", max)
.put("options", JSONArray().put("withscores"))

if (options != null)
{
if (options.limit != null)
{
query.put("limit", JSONArray(Arrays.asList(*options.limit!!)))
}
}

send(
"zrangebyscore",
query,
options,
object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(mapZrangeResults(response.getJSONArray("result")))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
)
}

/**
 * [.zrank]
 */
   fun zrank(key:String, member:String, listener:ResponseListener<Long>) {
zrank(key, member, null, listener)
}

/**
 * Returns the position of an element in a sorted set, with scores in ascending order.
 * @param key      Sorted set ID
 * @param member   Member value
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zrank(key:String, member:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key).put("member", member)

send("zrank", query, options, getCallbackLong(listener))
}

/**
 * [.zrem]
 */
   fun zrem(key:String, members:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return zrem(key, members, null, listener)
}

/**
 * Removes members from a sorted set.
 * @param  key      Sorted set ID
 * @param  members  Members to remove
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun zrem(key:String, members:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("members", JSONArray(Arrays.asList(*members)))
)

send("zrem", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.zremrangebylex]
 */
   fun zremrangebylex(key:String, min:String, max:String, listener:ResponseListener<Long>):MemoryStorage {
return zremrangebylex(key, min, max, null, listener)
}

/**
 * Removes members from a sorted set where all elements have
 * the same score, using lexicographical ordering.
 * @param  key      Sorted set ID
 * @param  min      Minimum value
 * @param  max      Maximum value
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun zremrangebylex(key:String, min:String, max:String, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("min", min)
.put("max", max)
)

send("zremrangebylex", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.zremrangebyrank]
 */
   fun zremrangebyrank(key:String, min:Long, max:Long, listener:ResponseListener<Long>):MemoryStorage {
return zremrangebyrank(key, min, max, null, listener)
}

/**
 * Removes members from a sorted set with their position
 * in the set
 * @param  key      Sorted set ID
 * @param  min      Start position
 * @param  max      End position
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun zremrangebyrank(key:String, min:Long, max:Long, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("start", min)
.put("stop", max)
)

send("zremrangebyrank", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.zremrangebyscore]
 */
   fun zremrangebyscore(key:String, min:Double, max:Double, listener:ResponseListener<Long>):MemoryStorage {
return zremrangebyscore(key, min, max, null, listener)
}

/**
 * Removes members from a sorted set with a score
 * @param  key      Sorted set ID
 * @param  min      Minimum score
 * @param  max      Maximum score
 * @param  options  [description]
 * @param  listener [description]
 * @return this
 */
  @JvmOverloads  fun zremrangebyscore(key:String, min:Double, max:Double, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject()
.put("_id", key)
.put("body", KuzzleJSONObject()
.put("min", min)
.put("max", max)
)

send("zremrangebyscore", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}

/**
 * [.zrevrange]
 */
   fun zrevrange(key:String, start:Long, stop:Long, listener:ResponseListener<Array<JSONObject>>) {
zrevrange(key, start, stop, null, listener)
}

/**
 * Identical to zrange, except that the sorted set is traversed in descending order.
 * @param key      Sorted set ID
 * @param start    Start position
 * @param stop     End position
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zrevrange(key:String, start:Long, stop:Long, options:Options?, listener:ResponseListener<Array<JSONObject>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("start", start)
.put("stop", stop)
.put("options", JSONArray().put("withscores"))

send(
"zrevrange",
query,
options,
object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(mapZrangeResults(response.getJSONArray("result")))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
)
}

/**
 * [.zrevrangebylex]
 */
   fun zrevrangebylex(key:String, min:String, max:String, listener:ResponseListener<Array<String>>) {
zrevrangebylex(key, min, max, null, listener)
}

/**
 * Identical to zrangebylex except that the sorted set is traversed in descending order.
 * @param key      Sorted set ID
 * @param min      Minimum value
 * @param max      Maximum value
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zrevrangebylex(key:String, min:String, max:String, options:Options?, listener:ResponseListener<Array<String>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("min", min)
.put("max", max)

if (options != null)
{
if (options.limit != null)
{
query.put("limit", JSONArray(Arrays.asList(*options.limit!!)))
}
}

send("zrevrangebylex", query, options, getCallbackStringArray(listener))
}

/**
 * [.zrevrangebyscore]
 */
   fun zrevrangebyscore(key:String, min:Double, max:Double, listener:ResponseListener<Array<JSONObject>>) {
zrevrangebyscore(key, min, max, null, listener)
}

/**
 * Identical to zrangebyscore except that the sorted set is traversed in descending order.
 * @param key      Sorted set ID
 * @param min      Minimum score
 * @param max      Maximum score
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zrevrangebyscore(key:String, min:Double, max:Double, options:Options?, listener:ResponseListener<Array<JSONObject>>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("min", min)
.put("max", max)
.put("options", JSONArray().put("withscores"))

if (options != null)
{
if (options.limit != null)
{
query.put("limit", JSONArray(Arrays.asList(*options.limit!!)))
}
}

send(
"zrevrangebyscore",
query,
options,
object:ResponseListener<JSONObject> {
override fun onSuccess(response:JSONObject) {
try
{
listener.onSuccess(mapZrangeResults(response.getJSONArray("result")))
}
catch (e:JSONException) {
throw RuntimeException(e)
}

}

override fun onError(error:JSONObject) {
listener.onError(error)
}
}
)
}

/**
 * [.zrevrank]
 */
   fun zrevrank(key:String, member:String, listener:ResponseListener<Long>) {
zrevrank(key, member, null, listener)
}

/**
 * Returns the position of an element in a sorted set, with scores in descending order.
 * @param key      Sorted set ID
 * @param member   Member value
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zrevrank(key:String, member:String, options:Options?, listener:ResponseListener<Long>) {
val query = KuzzleJSONObject().put("_id", key).put("member", member)

send("zrevrank", query, options, getCallbackLong(listener))
}

/**
 * [.zscan]
 */
   fun zscan(key:String, cursor:Long, listener:ResponseListener<JSONObject>) {
zscan(key, cursor, null, listener)
}

/**
 * Identical to scan, except that zscan iterates the members held by a sorted set.
 * @param key      Sorted set ID
 * @param cursor   Cursor position (0 to start an iteration)
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zscan(key:String, cursor:Long, options:Options?, listener:ResponseListener<JSONObject>) {
val query = KuzzleJSONObject()
.put("_id", key)
.put("cursor", cursor)

if (options != null)
{
if (options.count != null)
{
query.put("count", options.count)
}

if (options.match != null)
{
query.put("match", options.match)
}
}

send("zscan", query, options, getCallbackScanResult(listener))
}

/**
 * [.zscore]
 */
   fun zscore(key:String, member:String, listener:ResponseListener<Double>) {
zscore(key, member, null, listener)
}

/**
 * Returns the score of a member in a sorted set.
 * @param key      Sorted set ID
 * @param member   Member value
 * @param options Request options
 * @param listener Response callback listener
 */
   fun zscore(key:String, member:String, options:Options?, listener:ResponseListener<Double>) {
val query = KuzzleJSONObject().put("_id", key).put("member", member)

send("zscore", query, options, getCallbackDouble(listener))
}

/**
 * [.zunionstore]
 */
   fun zunionstore(destination:String, keys:Array<String>, listener:ResponseListener<Long>):MemoryStorage {
return zunionstore(destination, keys, null, listener)
}

/**
 * Computes the union of the provided sorted sets and stores the result in the destination key.
 * @param  destination Destination key ID
 * @param  keys        Array of sorted set IDs
 * @param  options     [description]
 * @param  listener    [description]
 * @return this
 */
  @JvmOverloads  fun zunionstore(destination:String, keys:Array<String>, options:Options? = null, listener:ResponseListener<Long>? = null):MemoryStorage {
val query = KuzzleJSONObject().put("_id", destination)
val body = KuzzleJSONObject().put("keys", JSONArray(Arrays.asList(*keys)))

if (options != null)
{
if (options.aggregate != null)
{
body.put("aggregate", options.aggregate)
}

if (options.weights != null)
{
body.put("weights", JSONArray(Arrays.asList(*options.weights!!)))
}
}

query.put("body", body)

send("zunionstore", query, options, if (listener != null) getCallbackLong(listener) else null)

return this
}
}/**
 * [.append]
 *//**
 * [.append]
 *//**
 * [.bitop]
 *//**
 * [.bitop]
 *//**
 * [.decr]
 *//**
 * [.decr]
 *//**
 * [.decrby]
 *//**
 * [.decrby]
 *//**
 * [.del]
 *//**
 * [.del]
 *//**
 * [.expire]
 *//**
 * [.expire]
 *//**
 * [.expireat]
 *//**
 * [.expireat]
 *//**
 * [.flushdb]
 *//**
 * [.flushdb]
 *//**
 * [.geoadd]
 *//**
 * [.geoadd]
 *//**
 * [.getset]
 *//**
 * [.getset]
 *//**
 * [.hdel]
 *//**
 * [.hdel]
 *//**
 * [.hincrby]
 *//**
 * [.hincrby]
 *//**
 * [.hincrbyfloat]
 *//**
 * [.hincrbyfloat]
 *//**
 * [.hmset]
 *//**
 * [.hmset]
 *//**
 * [.hset]
 *//**
 * [.hset]
 *//**
 * [.hsetnx]
 *//**
 * [.hsetnx]
 *//**
 * [.incr]
 *//**
 * [.incr]
 *//**
 * [.incrby]
 *//**
 * [.incrby]
 *//**
 * [.incrbyfloat]
 *//**
 * [.incrbyfloat]
 *//**
 * [.linsert]
 *//**
 * [.linsert]
 *//**
 * [.lpop]
 *//**
 * [.lpop]
 *//**
 * [.lpush]
 *//**
 * [.lpush]
 *//**
 * [.lpushx]
 *//**
 * [.lpushx]
 *//**
 * [.lrem]
 *//**
 * [.lrem]
 *//**
 * [.lset]
 *//**
 * [.lset]
 *//**
 * [.ltrim]
 *//**
 * [.ltrim]
 *//**
 * [.mset]
 *//**
 * [.mset]
 *//**
 * [.msetnx]
 *//**
 * [.msetnx]
 *//**
 * [.persist]
 *//**
 * [.persist]
 *//**
 * [.pexpire]
 *//**
 * [.pexpire]
 *//**
 * [.pexpireat]
 *//**
 * [.pexpireat]
 *//**
 * [.pfadd]
 *//**
 * [.pfadd]
 *//**
 * [.pfmerge]
 *//**
 * [.pfmerge]
 *//**
 * [.psetex]
 *//**
 * [.psetex]
 *//**
 * [.rename]
 *//**
 * [.rename]
 *//**
 * [.renamenx]
 *//**
 * [.renamenx]
 *//**
 * [.rpop]
 *//**
 * [.rpop]
 *//**
 * [.rpoplpush]
 *//**
 * [.rpoplpush]
 *//**
 * [.rpush]
 *//**
 * [.rpush]
 *//**
 * [.rpushx]
 *//**
 * [.rpushx]
 *//**
 * [.sadd]
 *//**
 * [.sadd]
 *//**
 * [.sdiffstore]
 *//**
 * [.sdiffstore]
 *//**
 * [.set]
 *//**
 * [.set]
 *//**
 * [.setex]
 *//**
 * [.setex]
 *//**
 * [.setnx]
 *//**
 * [.setnx]
 *//**
 * [.sinterstore]
 *//**
 * [.sinterstore]
 *//**
 * [.smove]
 *//**
 * [.smove]
 *//**
 * [.spop]
 *//**
 * [.spop]
 *//**
 * [.srem]
 *//**
 * [.srem]
 *//**
 * [.sunionstore]
 *//**
 * [.sunionstore]
 *//**
 * [.touch]
 *//**
 * [.touch]
 *//**
 * [.zadd]
 *//**
 * [.zadd]
 *//**
 * [.zincrby]
 *//**
 * [.zincrby]
 *//**
 * [.zinterstore]
 *//**
 * [.zinterstore]
 *//**
 * [.zrem]
 *//**
 * [.zrem]
 *//**
 * [.zremrangebylex]
 *//**
 * [.zremrangebylex]
 *//**
 * [.zremrangebyrank]
 *//**
 * [.zremrangebyrank]
 *//**
 * [.zremrangebyscore]
 *//**
 * [.zremrangebyscore]
 *//**
 * [.zunionstore]
 *//**
 * [.zunionstore]
 */
