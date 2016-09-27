package io.kuzzle.test.core.Kuzzle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.sdk.util.KuzzleQueryObject;
import io.kuzzle.sdk.util.KuzzleQueueFilter;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.kuzzle.test.testUtils.QueryArgsHelper;
import io.socket.client.Socket;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class constructorTest {
  private KuzzleExtend kuzzle;
  private Socket s;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    options.setPort(12345);
    options.setDefaultIndex("testIndex");

    s = mock(Socket.class);
    kuzzle = new KuzzleExtend("localhost", options, null);
    kuzzle.setSocket(s);

    listener = new KuzzleResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadUriConnection() throws URISyntaxException {
    new Kuzzle(null);
  }

  @Test
  public void checkSignaturesVariants() throws URISyntaxException {
    Kuzzle k = new Kuzzle("localhost");
    assertNotNull(k);
    k = new Kuzzle("localhost", new KuzzleOptions());
    assertNotNull(k);
    k = new Kuzzle("localhost", listener);
    assertNotNull(k);
  }

  @Test
  public void testKuzzleConstructor() throws URISyntaxException, JSONException {
    kuzzle = spy(kuzzle);
    assertEquals(kuzzle.getDefaultIndex(), "testIndex");
    assertNotNull(kuzzle);
    verify(kuzzle, never()).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testSetHeadersSignaturesVariants() throws JSONException {
    kuzzle = spy(kuzzle);
    kuzzle.setHeaders(new JSONObject());
    verify(kuzzle).setHeaders(any(JSONObject.class), eq(false));
  }

  @Test(expected = RuntimeException.class)
  public void testSetHeaders() throws JSONException {
    kuzzle.setHeaders(null, true);
    kuzzle.setHeaders(new JSONObject());
    assertNotNull(kuzzle.getHeaders());
    JSONObject content = new JSONObject();
    content.put("foo", "bar");
    kuzzle.setHeaders(content);
    assertEquals(kuzzle.getHeaders().getString("foo"), "bar");
    content.put("foo", "baz");
    kuzzle.setHeaders(content, true);
    assertEquals(kuzzle.getHeaders().getString("foo"), "baz");
    JSONObject fake = spy(new JSONObject());
    doThrow(JSONException.class).when(fake).keys();
    kuzzle.setHeaders(fake, false);
  }

  @Test(expected = RuntimeException.class)
  public void testAddHeaders() throws JSONException {
    JSONObject query = new JSONObject();
    JSONObject headers = new JSONObject();
    headers.put("testPurpose", "test");
    kuzzle.addHeaders(query, headers);
    assertEquals(query.get("testPurpose"), "test");
    JSONObject fake = mock(JSONObject.class);
    doThrow(JSONException.class).when(fake).keys();
    kuzzle.addHeaders(new JSONObject(), fake);
  }

  @Test
  public void testMetadataOptions() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", options, null);
    extended.setSocket(s);
    extended.setState(KuzzleStates.CONNECTED);


    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    options.setMetadata(meta);

    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");

    extended.query(QueryArgsHelper.makeQueryArgs("controller", "action"), jsonObj, options, null);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getJSONObject("metadata").getString("foo"), "bar");
  }

  @Test
  public void testMetadataInKuzzle() throws JSONException, URISyntaxException {
    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");
    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    KuzzleOptions options = new KuzzleOptions();
    options.setMetadata(meta);
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", options, null);
    extended.setSocket(s);
    extended.setState(KuzzleStates.CONNECTED);
    extended.query(QueryArgsHelper.makeQueryArgs("controller", "action"), jsonObj, options);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getJSONObject("metadata").getString("foo"), "bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetDefaultIndexIllegalIndex() {
    kuzzle.setDefaultIndex(null);
  }

  @Test
  public void exposeDefaultIndexGetterSetter() {
    assertEquals("testIndex", kuzzle.getDefaultIndex());
    assertThat(kuzzle.setDefaultIndex("foobar"), instanceOf(KuzzleExtend.class));
    assertEquals("foobar", kuzzle.getDefaultIndex());
  }

  @Test
  public void exposeJwtTokenGetter() {
    assertThat(kuzzle.setJwtToken("foobar"), instanceOf(KuzzleExtend.class));
    assertEquals("foobar", kuzzle.getJwtToken());
  }

  @Test
  public void exposeAutoQueueGetterSetter() {
    assertThat(kuzzle.setAutoQueue(true), instanceOf(KuzzleExtend.class));
    assertEquals(true, kuzzle.isAutoQueue());
  }

  @Test
  public void exposeAutoReconnectGetter() {
    assertThat(kuzzle.isAutoReconnect(), instanceOf(boolean.class));
  }

  @Test
  public void exposeAutoResubscribeGetterSetter() {
    assertThat(kuzzle.setAutoResubscribe(true), instanceOf(KuzzleExtend.class));
    assertEquals(true, kuzzle.isAutoResubscribe());
  }

  @Test
  public void exposeAutoReplayGetterSetter() {
    assertThat(kuzzle.setAutoReplay(true), instanceOf(KuzzleExtend.class));
    assertEquals(true, kuzzle.isAutoReplay());
  }

  @Test
  public void exposeOfflineQueueGetterSetter() {
    KuzzleQueryObject query = new KuzzleQueryObject();
    assertThat(kuzzle.setOfflineQueue(query), instanceOf(KuzzleExtend.class));
    assertEquals(kuzzle.getOfflineQueue().peek(), query);
  }

  @Test
  public void exposeQueueFilterGetterSetter() {
    KuzzleQueueFilter qf = new KuzzleQueueFilter() {
      @Override
      public boolean filter(JSONObject object) {
        return false;
      }
    };

    assertThat(kuzzle.setQueueFilter(qf), instanceOf(KuzzleExtend.class));
    assertEquals(qf, kuzzle.getQueueFilter());
  }

  @Test
  public void exposeQueueMaxSizeGetterSetter() {
    assertThat(kuzzle.setQueueMaxSize(123), instanceOf(KuzzleExtend.class));
    assertEquals(123, kuzzle.getQueueMaxSize());
  }

  @Test
  public void ensureQueueMaxSizeIsNeverBelowZero() {
    kuzzle.setQueueMaxSize(-4623);
    assertEquals(0, kuzzle.getQueueMaxSize());
  }

  @Test
  public void exposeQueueTTLGetterSetter() {
    assertThat(kuzzle.setQueueTTL(123), instanceOf(KuzzleExtend.class));
    assertEquals(123, kuzzle.getQueueTTL());
  }

  @Test
  public void ensureQueueTTLIsNeverBelowZero() {
    kuzzle.setQueueTTL(-12);
    assertEquals(0, kuzzle.getQueueTTL());
  }

  @Test
  public void exposeMetadataGetterSetter() {
    JSONObject meta = new JSONObject();
    assertThat(kuzzle.setMetadata(meta), instanceOf(KuzzleExtend.class));
    assertEquals(meta, kuzzle.getMetadata());
  }

  @Test
  public void exposeReplayIntervalGetterSetter() {
    assertThat(kuzzle.setReplayInterval(123), instanceOf(KuzzleExtend.class));
    assertEquals(123, kuzzle.getReplayInterval());
  }

  @Test
  public void ensureReplayIntervalIsNeverBelowZero() {
    kuzzle.setReplayInterval(-123);
    assertEquals(0, kuzzle.getReplayInterval());
  }

  @Test
  public void exposeReconnectionDelayGetter() {
    assertThat(kuzzle.getReconnectionDelay(), instanceOf(long.class));
  }

  @Test
  public void testGetPort() {
    assertEquals(kuzzle.getPort(), 12345);
  }

  @Test
  public void testSetPort() {
    kuzzle = spy(kuzzle);
    kuzzle.setPort(1234);
    assertEquals(kuzzle.getPort(), 1234);
  }

  @Test
  public void testGetHost() {
    assertEquals("localhost", kuzzle.getHost());
  }

  @Test public void testSetHost() {
    kuzzle = spy(kuzzle);
    kuzzle.setHost("foobar");
    assertEquals("foobar", kuzzle.getHost());
  }
}
