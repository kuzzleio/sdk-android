package io.kuzzle.sdk.core;

import org.json.JSONObject;

/**
 * Created by kblondel on 13/10/15.
 */
public class KuzzleOption {

    private boolean     autoReconnect = false;
    private JSONObject  headers;

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public JSONObject getHeaders() {
        return headers;
    }

    public void setHeaders(JSONObject headers) {
        this.headers = headers;
    }
}
