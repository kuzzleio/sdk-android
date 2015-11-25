package io.kuzzle.sdk.util;

/**
 * The type Options.
 */
public class Options {

  private boolean persist = false;
  private boolean updateIfExist = false;

  /**
   * Is persist boolean.
   *
   * @return the boolean
   */
  public boolean isPersist() {
    return persist;
  }

  /**
   * Sets persist.
   *
   * @param persist the persist
   */
  public void setPersist(boolean persist) {
    this.persist = persist;
  }

  /**
   * Is update if exist boolean.
   *
   * @return the boolean
   */
  public boolean isUpdateIfExist() {
    return updateIfExist;
  }

  /**
   * Sets update if exist.
   *
   * @param updateIfExist the update if exist
   */
  public void setUpdateIfExist(boolean updateIfExist) {
    this.updateIfExist = updateIfExist;
  }
}
