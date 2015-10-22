package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

/**
 * Created by kblondel on 13/10/15.
 */
public interface ResponseListener {
    void onSuccess(JSONObject object) throws Exception;
    void onError(JSONObject error) throws Exception;
}
