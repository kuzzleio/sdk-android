package io.kuzzle.test.core.Kuzzle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleQueue;
import io.kuzzle.sdk.state.States;
import io.kuzzle.sdk.util.OfflineQueueLoader;
import io.kuzzle.sdk.util.QueryObject;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class offlineQueueLoaderTest {

  private KuzzleExtend kuzzleExtend;
  private Socket s;

  @Before
  public void setUp() throws URISyntaxException {
    Options options = new Options();
    options.setConnect(Mode.MANUAL);
    s = mock(Socket.class);
    KuzzleExtend extended = new KuzzleExtend("localhost", options, null);
    extended.setSocket(s);
    kuzzleExtend = extended;
  }

  private void mockAnswer(final String event) {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(event), any(Emitter.Listener.class));
  }


  @Test
  public void testOfflineQueueLoader() throws JSONException, URISyntaxException, InterruptedException {
    kuzzleExtend = spy(kuzzleExtend);
    kuzzleExtend.setAutoReplay(true);
    mockAnswer(Socket.EVENT_RECONNECT);

    Options opts = new Options().setQueuable(true);
    kuzzleExtend.setState(States.OFFLINE);
    kuzzleExtend.startQueuing();

    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.setAction("bar");
    kuzzleExtend.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));

    OfflineQueueLoader offlineQueueLoader = new OfflineQueueLoader() {
      @Override
      public KuzzleQueue<QueryObject> load() {
        KuzzleQueue<QueryObject> queue = new KuzzleQueue();
        QueryObject query = new QueryObject();
        try {
          query.setQuery(new JSONObject().put("action", "foo").put("requestId", "42").put("controller", "ctrl"));
        } catch (JSONException e) {
          e.printStackTrace();
        }
        queue.addToQueue(query);
        return queue;
      }
    };
    kuzzleExtend.setOfflineQueueLoader(offlineQueueLoader);

    mockAnswer(Socket.EVENT_RECONNECT);
    kuzzleExtend.connect();
    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    Thread.sleep(1000);
    verify(kuzzleExtend, times(2)).emitRequest((JSONObject) argument.capture(), any(OnQueryDoneListener.class));
    assertEquals(((JSONObject) argument.getAllValues().get(0)).getString("action"), "bar");
    assertEquals(((JSONObject) argument.getAllValues().get(1)).getString("action"), "foo");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOfflineQueueLoaderIllegalRequestId() throws JSONException, URISyntaxException {
    kuzzleExtend = spy(kuzzleExtend);
    kuzzleExtend.setAutoReplay(true);
    mockAnswer(Socket.EVENT_RECONNECT);

    Options opts = new Options().setQueuable(true);
    kuzzleExtend.setState(States.OFFLINE);
    kuzzleExtend.startQueuing();

    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.setAction("bar");
    kuzzleExtend.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));

    OfflineQueueLoader offlineQueueLoader = new OfflineQueueLoader() {
      @Override
      public KuzzleQueue<QueryObject> load() {
        KuzzleQueue<QueryObject> queue = new KuzzleQueue();
        QueryObject query = new QueryObject();
        try {
          query.setQuery(new JSONObject().put("action", "foo").put("controller", "ctrl"));
        } catch (JSONException e) {
          e.printStackTrace();
        }
        queue.addToQueue(query);
        return queue;
      }
    };
    kuzzleExtend.setOfflineQueueLoader(offlineQueueLoader);

    mockAnswer(Socket.EVENT_RECONNECT);
    kuzzleExtend.connect();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOfflineQueueLoaderIllegalController() throws JSONException, URISyntaxException {
    kuzzleExtend = spy(kuzzleExtend);
    kuzzleExtend.setAutoReplay(true);
    mockAnswer(Socket.EVENT_RECONNECT);

    Options opts = new Options().setQueuable(true);
    kuzzleExtend.setState(States.OFFLINE);
    kuzzleExtend.startQueuing();

    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.setAction("bar");
    kuzzleExtend.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));

    OfflineQueueLoader offlineQueueLoader = new OfflineQueueLoader() {
      @Override
      public KuzzleQueue<QueryObject> load() {
        KuzzleQueue<QueryObject> queue = new KuzzleQueue();
        QueryObject query = new QueryObject();
        try {
          query.setQuery(new JSONObject().put("action", "foo").put("requestId", "42"));
        } catch (JSONException e) {
          e.printStackTrace();
        }
        queue.addToQueue(query);
        return queue;
      }
    };
    kuzzleExtend.setOfflineQueueLoader(offlineQueueLoader);

    mockAnswer(Socket.EVENT_RECONNECT);
    kuzzleExtend.connect();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOfflineQueueLoaderIllegalAction() throws JSONException, URISyntaxException {
    kuzzleExtend = spy(kuzzleExtend);
    kuzzleExtend.setAutoReplay(true);
    mockAnswer(Socket.EVENT_RECONNECT);

    Options opts = new Options().setQueuable(true);
    kuzzleExtend.setState(States.OFFLINE);
    kuzzleExtend.startQueuing();

    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.setAction("bar");
    kuzzleExtend.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));

    OfflineQueueLoader offlineQueueLoader = new OfflineQueueLoader() {
      @Override
      public KuzzleQueue<QueryObject> load() {
        KuzzleQueue<QueryObject> queue = new KuzzleQueue();
        QueryObject query = new QueryObject();
        try {
          query.setQuery(new JSONObject().put("requestId", "42").put("controller", "ctrl"));
        } catch (JSONException e) {
          e.printStackTrace();
        }
        queue.addToQueue(query);
        return queue;
      }
    };
    kuzzleExtend.setOfflineQueueLoader(offlineQueueLoader);

    mockAnswer(Socket.EVENT_RECONNECT);
    kuzzleExtend.connect();
  }

  @Test
  public void testOfflineQueueDuplicateRequestId() throws JSONException, URISyntaxException, InterruptedException {
    kuzzleExtend = spy(kuzzleExtend);
    kuzzleExtend.setAutoReplay(true);
    mockAnswer(Socket.EVENT_RECONNECT);

    Options opts = new Options().setQueuable(true);
    kuzzleExtend.setState(States.OFFLINE);
    kuzzleExtend.startQueuing();

    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.setAction("bar");
    kuzzleExtend.query(args, new JSONObject().put("requestId", "42"), opts, mock(OnQueryDoneListener.class));

    OfflineQueueLoader offlineQueueLoader = new OfflineQueueLoader() {
      @Override
      public KuzzleQueue<QueryObject> load() {
        KuzzleQueue<QueryObject> queue = new KuzzleQueue();
        QueryObject query = new QueryObject();
        try {
          query.setQuery(new JSONObject().put("action", "foo").put("requestId", "42").put("controller", "ctrl"));
        } catch (JSONException e) {
          e.printStackTrace();
        }
        queue.addToQueue(query);
        return queue;
      }
    };
    kuzzleExtend.setOfflineQueueLoader(offlineQueueLoader);

    mockAnswer(Socket.EVENT_RECONNECT);
    kuzzleExtend.connect();
    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    Thread.sleep(1000);
    verify(kuzzleExtend, times(1)).emitRequest((JSONObject) argument.capture(), any(OnQueryDoneListener.class));
    assertEquals(((JSONObject) argument.getAllValues().get(0)).getString("action"), "bar");
  }

}
