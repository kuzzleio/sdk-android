package io.kuzzle.sdk.util;

import org.json.JSONObject;

import io.kuzzle.sdk.listeners.ResponseListener;

/**
 * Created by kblondel on 14/10/15.
 */
public class QueryObject {

    private JSONObject  object;
    private String      controller;
    private ResponseListener cb;

    public JSONObject getObject() {
        return object;
    }

    public void setObject(JSONObject object) {
        this.object = object;
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public ResponseListener getCb() {
        return cb;
    }

    public void setCb(ResponseListener cb) {
        this.cb = cb;
    }
}
