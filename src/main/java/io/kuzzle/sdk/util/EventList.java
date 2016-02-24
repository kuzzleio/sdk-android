package io.kuzzle.sdk.util;

import java.util.HashMap;

public class EventList extends HashMap<String, Event> {
  public long lastEmitted = 0;
}
