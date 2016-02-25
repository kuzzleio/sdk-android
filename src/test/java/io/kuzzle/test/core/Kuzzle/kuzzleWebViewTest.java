package io.kuzzle.test.core.Kuzzle;

import android.webkit.WebView;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class kuzzleWebViewTest {

  private KuzzleExtend kuzzle;
  private KuzzleExtend.KuzzleWebViewClient webViewClient;
  private Socket s;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    options.setDefaultIndex("testIndex");

    s = mock(Socket.class);
    kuzzle = new KuzzleExtend("http://localhost:7512", options, null);
    kuzzle.setSocket(s);
    webViewClient = kuzzle.getKuzzleWebViewClient();

    listener = new KuzzleResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
  }

  @Test
  public void testShouldLoadFinalUrl() {
    WebView view = mock(WebView.class);
    webViewClient.shouldOverrideUrlLoading(view, "url");
    verify(view).loadUrl(eq("url"));
  }
}