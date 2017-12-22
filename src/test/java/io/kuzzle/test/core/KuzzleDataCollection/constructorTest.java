package io.kuzzle.test.core.KuzzleDataCollection;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.state.States;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class constructorTest {
  private Kuzzle kuzzle;
  private Collection collection;
  private ResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    Options opts = new Options();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    extended.setSocket(mock(Socket.class));
    extended.setState(States.CONNECTED);

    kuzzle = spy(extended);

    collection = new Collection(kuzzle, "test", "index");
    listener = mock(ResponseListener.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIfNoKuzzleInstanceProvided() {
    new Collection(null, "foo", "bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIfNoIndexProvided() {
    new Collection(mock(Kuzzle.class), "foo", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIfNoCollectionProvided() {
    new Collection(mock(Kuzzle.class), null, "foo");
  }
}
