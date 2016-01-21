package io.kuzzle.sdk;

import io.kuzzle.sdk.core.Kuzzle;

public class QueryArgsHelper {

  public static Kuzzle.QueryArgs  makeQueryArgs(final String controller, final String action) {
    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.controller = controller;
    args.action = action;
    return args;
  }

}
