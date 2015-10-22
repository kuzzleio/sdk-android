package io.kuzzle.sdk.util;

/**
 * Created by kblondel on 15/10/15.
 */
public class Options {

    private boolean persist = false;
    private boolean updateIfExist = false;

    public boolean isPersist() {
        return persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    public boolean isUpdateIfExist() {
        return updateIfExist;
    }

    public void setUpdateIfExist(boolean updateIfExist) {
        this.updateIfExist = updateIfExist;
    }
}
