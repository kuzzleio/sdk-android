package io.kuzzle.test.responses;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Document;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.responses.SearchResult;
import io.kuzzle.sdk.security.Profile;
import io.kuzzle.sdk.state.States;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchResultTest {
  private Collection collection;
  private Kuzzle kuzzle;
  private ResponseListener listener;
  private long total;
  private ArrayList<Document> documents;
  private String documentId;
  private JSONObject documentContent;
  private Options options;
  private String scrollId;
  private String scroll;

  @Before
  public void setUp() throws URISyntaxException {
    options = new Options();
    options.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", options, null);
    extended.setSocket(mock(Socket.class));
    extended.setState(States.CONNECTED);
    kuzzle = spy(extended);
    when(kuzzle.getHeaders()).thenReturn(new JSONObject());

    collection = new Collection(kuzzle, "test", "index");
    listener = mock(ResponseListener.class);
    total = (long) 42;
    documentId = "someDocumentId";
    documents = new ArrayList<Document>();
    scrollId = "someScrollId";
    scroll = "someScroll";
    try {
      documentContent = new JSONObject().put("some", "content");
      documents.add(new Document(collection, documentId, documentContent));
    } catch(JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void checkConstructorArguments() {
    SearchResult searchResult = new SearchResult(collection, total, documents, null, options, null);
    assertEquals(searchResult.getCollection(), collection);
    assertEquals(searchResult.getTotal(), total);
    assertEquals(searchResult.getDocuments(), documents);
    assertEquals(searchResult.getAggregations(), null);
    assertEquals(searchResult.getOptions(), options);
    assertEquals(searchResult.getFilters(), null);
    assertEquals(searchResult.getFetchedDocument(), (long) 1);
  }

  @Test
  public void fetchNextByScroll() throws JSONException {
    Options localOptions = new Options(options);
    localOptions
      .setScroll(scroll)
      .setScrollId(scrollId);

    collection = spy(collection);
    SearchResult searchResult = new SearchResult(collection, total, documents, null, localOptions, null);

    searchResult.fetchNext(listener);

    verify(collection).scroll(eq(scrollId), eq(scroll), any(Options.class), eq((JSONObject) null), eq(listener));
  }

  @Test
  public void fetchNextBySearch() throws JSONException {
    Options localOptions = new Options(options);
    localOptions
      .setFrom((long) 0)
      .setSize((long) 2);

    collection = spy(collection);
    SearchResult searchResult = new SearchResult(collection, total, documents, null, localOptions, new JSONObject());

    searchResult.fetchNext(listener);

    verify(collection).search(any(JSONObject.class), any(Options.class), eq(listener));
  }

  @Test(expected = RuntimeException.class)
  public void fetchNextOnError() throws JSONException {
    Options localOptions = new Options(options);

    collection = spy(collection);
    SearchResult searchResult = new SearchResult(collection, total, documents, null, localOptions, new JSONObject());

    searchResult.fetchNext(listener);
  }
}
