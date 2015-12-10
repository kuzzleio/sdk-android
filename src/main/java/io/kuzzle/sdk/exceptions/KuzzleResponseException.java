package io.kuzzle.sdk.exceptions;

import org.json.JSONException;
import org.json.JSONObject;

public class KuzzleResponseException extends Throwable {

  private int count;
  private String message;
  private String stack;

  public KuzzleResponseException(JSONObject error) {
    try {
      this.count = error.getInt("count");
      this.message = error.getString("message");
      this.stack = error.getString("stack");
    } catch (JSONException e) {
      throw new RuntimeException("JSONObject input error.");
    }
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getStack() {
    return stack;
  }
}
