package io.kuzzle.sdk.core;

import org.json.JSONObject;

/**
 * Created by kblondel on 13/10/15.
 */
public class KuzzleRoomOption {

    private boolean subscribeToSelf = false;
    private boolean listeningToConnections = false;
    private boolean listeningToDisconnections = false;

    public boolean isSubscribeToSelf() {
        return subscribeToSelf;
    }

    public void setSubscribeToSelf(boolean subscribeToSelf) {
        this.subscribeToSelf = subscribeToSelf;
    }

    public boolean isListeningToConnections() {
        return listeningToConnections;
    }

    public void setListeningToConnections(boolean listeningToConnections) {
        this.listeningToConnections = listeningToConnections;
    }

    public boolean isListeningToDisconnections() {
        return listeningToDisconnections;
    }

    public void setListeningToDisconnections(boolean listeningToDisconnections) {
        this.listeningToDisconnections = listeningToDisconnections;
    }
}
