package io.kuzzle.sdk.test;

import org.json.JSONObject;

public class KuzzleOptions {

    private boolean     autoReconnect = false;
    private JSONObject  headers;
    private boolean     updateIfExists = false;
    private JSONObject  metadata;

    /**
     * Is auto reconnect boolean.
     *
     * @return the boolean
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    /**
     * Sets auto reconnect.
     *
     * @param autoReconnect the auto reconnect
     */
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    /**
     * Gets headers.
     *
     * @return the headers
     */
    public JSONObject getHeaders() {
        return headers;
    }

    /**
     * Sets headers.
     *
     * @param headers the headers
     */
    public void setHeaders(JSONObject headers) {
        this.headers = headers;
    }

    /**
     * Is update if exists boolean.
     *
     * @return the boolean
     */
    public boolean isUpdateIfExists() {
        return updateIfExists;
    }

    /**
     * Sets update if exists.
     *
     * @param updateIfExists the update if exists
     */
    public void setUpdateIfExists(boolean updateIfExists) {
        this.updateIfExists = updateIfExists;
    }

    /**
     * Gets metadata.
     *
     * @return the metadata
     */
    public JSONObject getMetadata() {
        return metadata;
    }

    /**
     * Sets metadata.
     *
     * @param metadata the metadata
     */
    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }
}
