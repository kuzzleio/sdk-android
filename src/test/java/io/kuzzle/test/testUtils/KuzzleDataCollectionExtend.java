package io.kuzzle.test.testUtils;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;

public class KuzzleDataCollectionExtend extends KuzzleDataCollection {
  public KuzzleDataCollectionExtend(@NonNull final Kuzzle kuzzle, @NonNull final String index, @NonNull final String collection) {
    super(kuzzle, collection, index);
  }

  public KuzzleDataCollection deleteDocument(final String documentId, final JSONObject filter, final KuzzleOptions options, final KuzzleResponseListener<String> listener, final KuzzleResponseListener<String[]> listener2) {
    return super.deleteDocument(documentId, filter, options, listener, listener2);
  }
}
