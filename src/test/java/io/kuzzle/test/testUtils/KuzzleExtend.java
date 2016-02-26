package io.kuzzle.test.testUtils;

import android.support.annotation.NonNull;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.sdk.util.EventList;
import io.socket.client.Socket;

import static org.mockito.Mockito.spy;

public class KuzzleExtend extends Kuzzle {

  public KuzzleResponseListener loginCallback;

  public class KuzzleWebViewClient extends Kuzzle.KuzzleWebViewClient {
    public boolean shouldOverrideUrlLoading(WebView view, final String url) {
      return super.shouldOverrideUrlLoading(view, url);
    }
  }

  public KuzzleExtend.KuzzleWebViewClient getKuzzleWebViewClient() {
    return new KuzzleWebViewClient();
  }

  public KuzzleExtend(@NonNull final String url, final KuzzleOptions options, final KuzzleResponseListener<Void> connectionCallback) throws URISyntaxException {
    super(url, options, connectionCallback);
  }

  public void setState(KuzzleStates newState) {
    this.state = newState;
  }

  public void setSocket(Socket s) {
    this.socket = s;
  }

  public void setListener(KuzzleResponseListener listener) {
    this.connectionCallback = listener;
  }


  public Kuzzle deleteSubscription(final String roomId, final String id) {
    return super.deleteSubscription(roomId, id);
  }

  /**
   * * Returns all registered listeners on a given event
   *
   * @param event
   */
  public EventList getEventListeners(KuzzleEvent event) {
    return this.eventListeners.get(event);
  }

  /**
   * Get the subscription object from a Kuzzle instance
   *
   * @return
   */
  public Map<String, ConcurrentHashMap<String, KuzzleRoom>> getSubscriptions() {
    return this.subscriptions;
  }

  /**
   * Gets the internal socket instance from the kuzzle object
   * @return
   */
  public Socket getSocket() {
    return this.socket;
  }

  public void isValid() {
    super.isValid();
  }

  public void emitRequest(final JSONObject request, final OnQueryDoneListener listener) throws JSONException {
    super.emitRequest(request, listener);
  }

  public Kuzzle deletePendingSubscription(final String id) {
    return super.deletePendingSubscription(id);
  }

  public Map<String, Date> getRequestHistory() {
    return super.getRequestHistory();
  }

  public Map<String, KuzzleRoom> getPendingSubscriptions() {
    return super.getPendingSubscriptions();
  }

  public boolean isValidState() {
    return super.isValidState();
  }

  public KuzzleResponseListener<Void> spyAndGetConnectionCallback() {
    super.connectionCallback = spy(super.connectionCallback);
    return super.connectionCallback;
  }

  public void setSuperDefaultIndex(final String index) {
    super.defaultIndex = index;
  }

  public void emitEvent(final KuzzleEvent event, final Object... args) {
    super.emitEvent(event, args);
  }

  public void renewSubscriptions() {
    super.renewSubscriptions();
  }

}
