package io.kuzzle.test.core.KuzzleMemoryStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;

import java.lang.reflect.Method;
import java.util.Arrays;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.MemoryStorage;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.util.KuzzleJSONObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class methodsTest {
  private Kuzzle kuzzle;
  private Kuzzle.QueryArgs queryArgs = new Kuzzle.QueryArgs();
  private MemoryStorage ms;
  private ArgumentCaptor capturedQueryArgs;
  private ArgumentCaptor capturedQuery;

  private static <T> T[] concat(T[] first, T[] second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  private void validate(String command, JSONObject expected, boolean withOpts) throws JSONException {
    assertEquals(((Kuzzle.QueryArgs) capturedQueryArgs.getValue()).controller, "ms");
    assertEquals(((Kuzzle.QueryArgs) capturedQueryArgs.getValue()).action, command);

    /*
     if options are provided, the expected result should come first
     as the assertion expects the second argument to contain at least
     the JSON properties contained in the first one

     And vice versa without options provided
     */
    if (withOpts) {
      JSONAssert.assertEquals(expected, (JSONObject)capturedQuery.getValue(), false);
    }
    else {
      JSONAssert.assertEquals(capturedQuery.getValue().toString(), expected, false);
    }
  }

  private ResponseListener<Long> verifyResultLong(long returnValue, final long expected) throws JSONException {
    mockResult(new KuzzleJSONObject().put("result", returnValue));

    return new ResponseListener<Long>() {
      @Override
      public void onSuccess(Long response) {
        assertEquals((long)response, expected);
      }

      @Override
      public void onError(JSONObject error) {
      }
    };
  }

  private ResponseListener<Integer> verifyResultInt(int returnValue, final int expected) throws JSONException {
    mockResult(new KuzzleJSONObject().put("result", returnValue));

    return new ResponseListener<Integer>() {
      @Override
      public void onSuccess(Integer response) {
        assertEquals((int)response, expected);
      }

      @Override
      public void onError(JSONObject error) {
      }
    };
  }

  private ResponseListener<String> verifyResultString(String returnValue, final String expected) throws JSONException {
    mockResult(new KuzzleJSONObject().put("result", returnValue));

    return new ResponseListener<String>() {
      @Override
      public void onSuccess(String response) {
        assertEquals(response, expected);
      }

      @Override
      public void onError(JSONObject error) {
      }
    };
  }

  private ResponseListener<JSONArray> verifyResultArray(JSONArray returnValue, final JSONArray expected) throws JSONException {
    mockResult(new KuzzleJSONObject().put("result", returnValue));

    return new ResponseListener<JSONArray>() {
      @Override
      public void onSuccess(JSONArray response) {
        try {
          JSONAssert.assertEquals(response, expected, false);
        }
        catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(JSONObject error) {
      }
    };
  }

  private ResponseListener<Double> verifyResultDouble(String returnValue, final double expected) throws JSONException {
    mockResult(new KuzzleJSONObject().put("result", returnValue));

    return new ResponseListener<Double>() {
      @Override
      public void onSuccess(Double response) {
        assertEquals(response, expected, 10e-12);
      }

      @Override
      public void onError(JSONObject error) {
      }
    };
  }

  private void mockResult(final KuzzleJSONObject raw) throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(raw);
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
  }

  private void testReadMethod(String command, Class[] proto, Object[] args, Options opts, JSONObject expected) throws Exception {
    Class[] overloadedProto;
    Object[] overloadedArgs;
    Method mscommand;

    // With options and listener
    overloadedProto = concat(proto, new Class[]{Options.class, ResponseListener.class});
    overloadedArgs = concat(args, new Object[]{opts, mock(ResponseListener.class)});
    mscommand = MemoryStorage.class.getDeclaredMethod(command, overloadedProto);
    mscommand.invoke(ms, overloadedArgs);
    verify(kuzzle).query((Kuzzle.QueryArgs) capturedQueryArgs.capture(), (JSONObject)capturedQuery.capture(), eq(opts), any(OnQueryDoneListener.class));
    validate(command, expected, true);

    // Without options, with listener
    overloadedProto = concat(proto, new Class[]{ResponseListener.class});
    overloadedArgs = concat(args, new Object[]{mock(ResponseListener.class)});
    mscommand = MemoryStorage.class.getDeclaredMethod(command, overloadedProto);
    mscommand.invoke(ms, overloadedArgs);
    verify(kuzzle).query((Kuzzle.QueryArgs) capturedQueryArgs.capture(), (JSONObject)capturedQuery.capture(), eq((Options)null), any(OnQueryDoneListener.class));
    validate(command, expected, false);
  }

  private void testWriteMethod(String command, Class[] proto, Object[] args, Options opts, JSONObject expected) throws Exception {
    Class[] overloadedProto;
    Object[] overloadedArgs;
    Method mscommand = MemoryStorage.class.getDeclaredMethod(command, proto);

    // Without options nor listener
    mscommand.invoke(ms, args);
    verify(kuzzle).query((Kuzzle.QueryArgs) capturedQueryArgs.capture(), (JSONObject)capturedQuery.capture(), eq((Options)null));
    validate(command, expected, false);

    // With options, without listener
    overloadedProto = concat(proto, new Class[]{Options.class});
    overloadedArgs = concat(args, new Object[]{opts});
    mscommand = MemoryStorage.class.getDeclaredMethod(command, overloadedProto);
    mscommand.invoke(ms, overloadedArgs);
    verify(kuzzle).query((Kuzzle.QueryArgs) capturedQueryArgs.capture(), (JSONObject)capturedQuery.capture(), eq(opts));
    validate(command, expected, true);

    // With options and listener
    overloadedProto = concat(overloadedProto, new Class[]{ResponseListener.class});
    overloadedArgs = concat(overloadedArgs, new Object[]{mock(ResponseListener.class)});
    mscommand = MemoryStorage.class.getDeclaredMethod(command, overloadedProto);
    mscommand.invoke(ms, overloadedArgs);
    verify(kuzzle).query((Kuzzle.QueryArgs) capturedQueryArgs.capture(), (JSONObject)capturedQuery.capture(), eq(opts), any(OnQueryDoneListener.class));
    validate(command, expected, true);

    // Without options, with listener
    overloadedProto = concat(proto, new Class[]{ResponseListener.class});
    overloadedArgs = concat(args, new Object[]{mock(ResponseListener.class)});
    mscommand = MemoryStorage.class.getDeclaredMethod(command, overloadedProto);
    mscommand.invoke(ms, overloadedArgs);
    verify(kuzzle).query((Kuzzle.QueryArgs) capturedQueryArgs.capture(), (JSONObject)capturedQuery.capture(), eq((Options)null), any(OnQueryDoneListener.class));
    validate(command, expected, false);
  }

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    ms = new MemoryStorage(kuzzle);
    capturedQueryArgs = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    capturedQuery = ArgumentCaptor.forClass(io.kuzzle.sdk.util.KuzzleJSONObject.class);
    queryArgs.controller = "ms";
  }

  @Test
  public void append() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"foo", "bar"};
    JSONObject expected = new JSONObject()
      .put("_id", "foo")
      .put("body", new JSONObject()
        .put("value", "bar")
      );

    this.testWriteMethod("append", new Class[]{String.class, String.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.append("foo", "bar", listener);
  }

  @Test
  public void bitcount() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"foo"};
    JSONObject expected = new JSONObject()
      .put("_id", "foo");

    this.testReadMethod("bitcount", new Class[]{String.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.bitcount("foo", listener);
  }

  @Test
  public void bitop() throws Exception {
    Options opts = new Options().setQueuable(true);
    JSONArray keys = new JSONArray().put("foo").put("bar");
    Object[] args = new Object[]{"foo", "xor", keys};
    JSONObject expected = new JSONObject()
      .put("_id", "foo")
      .put("body", new JSONObject()
        .put("operation", "xor")
        .put("keys", keys)
      );

    this.testWriteMethod("bitop", new Class[]{String.class, String.class, JSONArray.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.bitop("foo", "bar", keys, listener);
  }

  @Test
  public void bitpos() throws Exception {
    Options opts = new Options()
      .setQueuable(true)
      .setStart((long)13)
      .setEnd((long)42);
    Object[] args = new Object[]{"foo", 1};
    JSONObject expected = new JSONObject()
      .put("_id", "foo")
      .put("bit", 1)
      .put("start", 13)
      .put("end", 42);

    this.testReadMethod("bitpos", new Class[]{String.class, int.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.bitpos("foo", 1, listener);
  }

  @Test
  public void dbsize() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{};
    JSONObject expected = new JSONObject();

    this.testReadMethod("dbsize", new Class[]{}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.dbsize(listener);
  }

  @Test
  public void decr() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"foo"};
    JSONObject expected = new JSONObject().put("_id", "foo");

    this.testWriteMethod("decr", new Class[]{String.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.decr("foo", listener);
  }

  @Test
  public void decrby() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"foo", -42};
    JSONObject expected = new JSONObject()
      .put("_id", "foo")
      .put("body", new JSONObject()
        .put("value", -42)
      );

    this.testWriteMethod("decrby", new Class[]{String.class, long.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.decrby("foo", -42, listener);
  }

  @Test
  public void del() throws Exception {
    Options opts = new Options().setQueuable(true);
    JSONArray keys = new JSONArray().put("foo").put("bar").put("baz");
    Object[] args = new Object[]{keys};
    JSONObject expected = new JSONObject()
      .put("body", new JSONObject()
        .put("keys", keys)
      );

    this.testWriteMethod("del", new Class[]{JSONArray.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.del(keys, listener);
  }

  @Test
  public void exists() throws Exception {
    Options opts = new Options().setQueuable(true);
    JSONArray keys = new JSONArray().put("foo").put("bar").put("baz");
    Object[] args = new Object[]{keys};
    JSONObject expected = new JSONObject()
      .put("keys", keys);

    this.testReadMethod("exists", new Class[]{JSONArray.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.exists(keys, listener);
  }

  @Test
  public void expire() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"foo", 42};
    JSONObject expected = new JSONObject()
      .put("_id", "foo")
      .put("body", new JSONObject()
        .put("seconds", 42)
      );

    this.testWriteMethod("expire", new Class[]{String.class, long.class}, args, opts, expected);

    ResponseListener<Integer> listener = verifyResultInt(123, 123);
    ms.expire("foo", 42, listener);
  }

  @Test
  public void expireat() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"foo", 42};
    JSONObject expected = new JSONObject()
      .put("_id", "foo")
      .put("body", new JSONObject()
        .put("timestamp", 42)
      );

    this.testWriteMethod("expireat", new Class[]{String.class, long.class}, args, opts, expected);

    ResponseListener<Integer> listener = verifyResultInt(123, 123);
    ms.expireat("foo", 42, listener);
  }

  @Test
  public void flushdb() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{};
    JSONObject expected = new JSONObject();

    this.testWriteMethod("flushdb", new Class[]{}, args, opts, expected);

    ResponseListener<String> listener = verifyResultString("OK", "OK");
    ms.flushdb(listener);
  }

  @Test
  public void geoadd() throws Exception {
    Options opts = new Options().setQueuable(true);
    JSONArray points = new JSONArray()
      .put(new JSONObject()
        .put("lon", 13.361389)
        .put("lat", 38.115556)
        .put("name", "Palermo")
      )
      .put(new JSONObject()
        .put("lon", 15.087269)
        .put("lat", 37.502669)
        .put("name", "Catania")
      );
    Object[] args = new Object[]{"foo", points};
    JSONObject expected = new JSONObject()
      .put("_id", "foo")
      .put("body", new JSONObject()
        .put("points", points)
      );

    this.testWriteMethod("geoadd", new Class[]{String.class, JSONArray.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.geoadd("foo", points, listener);
  }

  @Test
  public void geodist() throws Exception {
    Options opts = new Options().setQueuable(true).setUnit("ft");
    Object[] args = new Object[]{"key", "Palermo", "Catania"};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("member1", "Palermo")
      .put("member2", "Catania")
      .put("unit", "ft");

    this.testReadMethod("geodist", new Class[]{String.class, String.class, String.class}, args, opts, expected);

    ResponseListener<Double> listener = verifyResultDouble("166274.1516", 166274.1516);
    ms.geodist("foo", "Palermo", "Catania", listener);
  }

  @Test
  public void geohash() throws Exception {
    Options opts = new Options().setQueuable(true);
    JSONArray members = new JSONArray().put("foo").put("bar").put("baz");
    Object[] args = new Object[]{"key", members};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("members", members);

    this.testReadMethod("geohash", new Class[]{String.class, JSONArray.class}, args, opts, expected);

    JSONArray expectedResult = new JSONArray().put("sqc8b49rny0").put("sqdtr74hyu0");
    ResponseListener<JSONArray> listener = verifyResultArray(expectedResult, expectedResult);
    ms.geohash("key", members, listener);
  }

  @Test
  public void geopos() throws Exception {
    Options opts = new Options().setQueuable(true);
    JSONArray members = new JSONArray().put("foo").put("bar").put("baz");
    Object[] args = new Object[]{"key", members};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("members", members);

    this.testReadMethod("geopos", new Class[]{String.class, JSONArray.class}, args, opts, expected);

    JSONArray rawResult = new JSONArray()
      .put(new JSONArray().put("13.361389").put("38.115556"))
      .put(new JSONArray().put("15.087269").put("37.502669"));

    JSONArray expectedResult = new JSONArray()
      .put(new JSONArray().put(13.361389).put(38.115556))
      .put(new JSONArray().put(15.087269).put(37.502669));

    ResponseListener<JSONArray> listener = verifyResultArray(rawResult, expectedResult);
    ms.geopos("key", members, listener);
  }

  @Test
  public void georadius() throws Exception {
    Options opts = new Options()
      .setQueuable(true)
      .setCount((long)10)
      .setSort("asc")
      .setWithcoord(true)
      .setWithdist(true);
    Object[] args = new Object[]{"key", 15, 37, 200, "km"};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("lon", 15)
      .put("lat", 37)
      .put("distance", 200)
      .put("unit", "km");

    this.testReadMethod("georadius", new Class[]{String.class, double.class, double.class, double.class, String.class}, args, opts, expected);

    // without coordinates nor distance
    opts.setWithcoord(false).setWithdist(false);
    ResponseListener<JSONArray> listener = verifyResultArray(
      new JSONArray().put("Palermo").put("Catania"),
      new JSONArray()
        .put(new JSONObject().put("name", "Palermo"))
        .put(new JSONObject().put("name", "Catania"))
    );
    ms.georadius("key", 15, 37, 200, "km", opts, listener);

    // with coordinates, without distance
    opts.setWithcoord(true).setWithdist(false);
    listener = verifyResultArray(
      new JSONArray().put(
        new JSONArray()
          .put("Palermo").put("190.4424")
      ).put(
        new JSONArray()
          .put("Catania").put("56.4413")
      ),
      new JSONArray()
        .put(new JSONObject().put("name", "Palermo").put("distance", 190.4424))
        .put(new JSONObject().put("name", "Catania").put("distance", 56.4413))
    );
    ms.georadius("key", 15, 37, 200, "km", opts, listener);

    // without coordinates, with distance
    opts.setWithcoord(false).setWithdist(true);
    listener = verifyResultArray(
      new JSONArray().put(
        new JSONArray()
          .put("Palermo").put(new JSONArray().put("13.3613").put("38.1155"))
      ).put(
        new JSONArray()
          .put("Catania").put(new JSONArray().put("15.0872").put("37.5026"))
      ),
      new JSONArray()
        .put(new JSONObject().put("name", "Palermo")
          .put("coordinates", new JSONArray().put(13.3613).put(38.1155))
        )
        .put(new JSONObject().put("name", "Catania")
          .put("coordinates", new JSONArray().put(15.0872).put(37.5026))
        )
    );
    ms.georadius("key", 15, 37, 200, "km", opts, listener);

    // with coordinates, with distance
    opts.setWithcoord(true).setWithdist(true);
    listener = verifyResultArray(
      new JSONArray().put(
        new JSONArray()
          .put("Palermo").put(new JSONArray().put("13.3613").put("38.1155")).put("190.4424")
      ).put(
        new JSONArray()
          .put("Catania").put("56.4413").put(new JSONArray().put("15.0872").put("37.5026"))
      ),
      new JSONArray()
        .put(new JSONObject().put("name", "Palermo")
          .put("distance", 190.4424)
          .put("coordinates", new JSONArray().put(13.3613).put(38.1155))
        )
        .put(new JSONObject().put("name", "Catania")
          .put("distance", 56.4413)
          .put("coordinates", new JSONArray().put(15.0872).put(37.5026))
        )
    );
    ms.georadius("key", 15, 37, 200, "km", opts, listener);
  }

  @Test
  public void georadiusbymember() throws Exception {
    Options opts = new Options()
      .setQueuable(true)
      .setCount((long)10)
      .setSort("asc")
      .setWithcoord(true)
      .setWithdist(true);
    Object[] args = new Object[]{"key", "Palermo", 200, "km"};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("member", "Palermo")
      .put("distance", 200)
      .put("unit", "km");

    this.testReadMethod("georadiusbymember", new Class[]{String.class, String.class, double.class, String.class}, args, opts, expected);

    // without coordinates nor distance
    opts.setWithcoord(false).setWithdist(false);
    ResponseListener<JSONArray> listener = verifyResultArray(
      new JSONArray().put("Palermo").put("Catania"),
      new JSONArray()
        .put(new JSONObject().put("name", "Palermo"))
        .put(new JSONObject().put("name", "Catania"))
    );
    ms.georadiusbymember("key", "Palermo", 200, "km", opts, listener);

    // with coordinates, without distance
    opts.setWithcoord(true).setWithdist(false);
    listener = verifyResultArray(
      new JSONArray().put(
        new JSONArray()
          .put("Palermo").put("190.4424")
      ).put(
        new JSONArray()
          .put("Catania").put("56.4413")
      ),
      new JSONArray()
        .put(new JSONObject().put("name", "Palermo").put("distance", 190.4424))
        .put(new JSONObject().put("name", "Catania").put("distance", 56.4413))
    );
    ms.georadiusbymember("key", "Palermo", 200, "km", opts, listener);

    // without coordinates, with distance
    opts.setWithcoord(false).setWithdist(true);
    listener = verifyResultArray(
      new JSONArray().put(
        new JSONArray()
          .put("Palermo").put(new JSONArray().put("13.3613").put("38.1155"))
      ).put(
        new JSONArray()
          .put("Catania").put(new JSONArray().put("15.0872").put("37.5026"))
      ),
      new JSONArray()
        .put(new JSONObject().put("name", "Palermo")
          .put("coordinates", new JSONArray().put(13.3613).put(38.1155))
        )
        .put(new JSONObject().put("name", "Catania")
          .put("coordinates", new JSONArray().put(15.0872).put(37.5026))
        )
    );
    ms.georadiusbymember("key", "Palermo", 200, "km", opts, listener);

    // with coordinates, with distance
    opts.setWithcoord(true).setWithdist(true);
    listener = verifyResultArray(
      new JSONArray().put(
        new JSONArray()
          .put("Palermo").put(new JSONArray().put("13.3613").put("38.1155")).put("190.4424")
      ).put(
        new JSONArray()
          .put("Catania").put("56.4413").put(new JSONArray().put("15.0872").put("37.5026"))
      ),
      new JSONArray()
        .put(new JSONObject().put("name", "Palermo")
          .put("distance", 190.4424)
          .put("coordinates", new JSONArray().put(13.3613).put(38.1155))
        )
        .put(new JSONObject().put("name", "Catania")
          .put("distance", 56.4413)
          .put("coordinates", new JSONArray().put(15.0872).put(37.5026))
        )
    );
    ms.georadiusbymember("key", "Palermo", 200, "km", opts, listener);
  }

  @Test
  public void get() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key"};
    JSONObject expected = new JSONObject()
      .put("_id", "key");

    this.testReadMethod("get", new Class[]{String.class}, args, opts, expected);

    ResponseListener<String> listener = verifyResultString("foobar", "foobar");
    ms.get("key", listener);
  }

  @Test
  public void getbit() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key", 10};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("offset", 10);

    this.testReadMethod("getbit", new Class[]{String.class, long.class}, args, opts, expected);

    ResponseListener<Integer> listener = verifyResultInt(123, 123);
    ms.getbit("key", 10, listener);
  }

  @Test
  public void getrange() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key", 13, 42};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("start", 13)
      .put("end", 42);

    this.testReadMethod("getrange", new Class[]{String.class, long.class, long.class}, args, opts, expected);

    ResponseListener<String> listener = verifyResultString("foobar", "foobar");
    ms.getrange("key", 13, 42, listener);
  }

  @Test
  public void getset() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key", "value"};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("body", new JSONObject().put("value", "value"));

    this.testWriteMethod("getset", new Class[]{String.class, String.class}, args, opts, expected);

    ResponseListener<String> listener = verifyResultString("foobar", "foobar");
    ms.getset("key", "value", listener);
  }

  @Test
  public void hdel() throws Exception {
    Options opts = new Options().setQueuable(true);
    JSONArray fields = new JSONArray().put("foo").put("bar").put("baz");
    Object[] args = new Object[]{"key", fields};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("body", new JSONObject().put("fields", fields));

    this.testWriteMethod("hdel", new Class[]{String.class, JSONArray.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.hdel("key", fields, listener);
  }

  @Test
  public void hexists() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key", "foobar"};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("field", "foobar");

    this.testReadMethod("hexists", new Class[]{String.class, String.class}, args, opts, expected);

    ResponseListener<Integer> listener = verifyResultInt(1, 1);
    ms.hexists("key", "foobar", listener);
  }

  @Test
  public void hget() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key", "foo"};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("field", "foo");

    this.testReadMethod("hget", new Class[]{String.class, String.class}, args, opts, expected);

    ResponseListener<String> listener = verifyResultString("bar", "bar");
    ms.hget("key", "foo", listener);
  }

  @Test
  public void hgetall() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key"};
    JSONObject expected = new JSONObject()
      .put("_id", "key");

    this.testReadMethod("hgetall", new Class[]{String.class}, args, opts, expected);

    final KuzzleJSONObject result = new KuzzleJSONObject().put("foo", "bar");
    mockResult(new KuzzleJSONObject().put("result", result));

    ResponseListener<JSONObject> listener =new ResponseListener<JSONObject>() {
      @Override
      public void onSuccess(JSONObject response) {
        assertEquals(response, result);
      }

      @Override
      public void onError(JSONObject error) {
      }
    };

    ms.hgetall("key", listener);
  }

  @Test
  public void hincrby() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key", "foo", 42};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("body", new JSONObject()
        .put("value", 42)
        .put("field", "foo")
      );

    this.testWriteMethod("hincrby", new Class[]{String.class, String.class, long.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.hincrby("foo", "bar", 42, listener);
  }

  @Test
  public void hincrbyfloat() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key", "foo", 3.14159};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("body", new JSONObject()
        .put("value", 3.14159)
        .put("field", "foo")
      );

    this.testWriteMethod("hincrbyfloat", new Class[]{String.class, String.class, double.class}, args, opts, expected);

    ResponseListener<Double> listener = verifyResultDouble("48.14159", 48.14159);
    ms.hincrbyfloat("foo", "bar", 3.14159, listener);
  }

  @Test
  public void hkeys() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key"};
    JSONObject expected = new JSONObject()
      .put("_id", "key");

    this.testReadMethod("hkeys", new Class[]{String.class}, args, opts, expected);

    JSONArray result = new JSONArray().put("foo").put("bar").put("baz");
    ResponseListener<JSONArray> listener = verifyResultArray(result, result);
    ms.hkeys("key", listener);
  }

  @Test
  public void hlen() throws Exception {
    Options opts = new Options().setQueuable(true);
    Object[] args = new Object[]{"key"};
    JSONObject expected = new JSONObject()
      .put("_id", "key");

    this.testReadMethod("hlen", new Class[]{String.class}, args, opts, expected);

    ResponseListener<Long> listener = verifyResultLong(123, 123);
    ms.hlen("key", listener);
  }

  @Test
  public void hmget() throws Exception {
    Options opts = new Options().setQueuable(true);
    JSONArray fields = new JSONArray().put("foo").put("bar").put("baz");
    Object[] args = new Object[]{"key", fields};
    JSONObject expected = new JSONObject()
      .put("_id", "key")
      .put("fields", fields);

    this.testReadMethod("hmget", new Class[]{String.class, JSONArray.class}, args, opts, expected);

    JSONArray result = new JSONArray().put("foo").put("bar").put("baz");
    ResponseListener<JSONArray> listener = verifyResultArray(result, result);
    ms.hmget("key", fields, listener);
  }
}
