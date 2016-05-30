package io.kuzzle.test.security.KuzzleSecurity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.enums.KuzzlePolicies;
import io.kuzzle.sdk.security.KuzzleSecurity;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class isActionAllowedTest {

  private Kuzzle kuzzle;
  private KuzzleSecurity kuzzleSecurity;
  private JSONArray policies;

  private JSONObject addProperties(final String ctrl, final String action, final String idx, final String collection, final String value) throws JSONException {
    return new JSONObject()
        .put("controller", ctrl)
        .put("action", action)
        .put("index", idx)
        .put("collection", collection)
        .put("value", value);
  }

  @Before
  public void setUp() throws JSONException {
    kuzzle = mock(Kuzzle.class);
    kuzzleSecurity = new KuzzleSecurity(kuzzle);
    policies = new JSONArray()
        .put(addProperties("read", "get", "*", "*", KuzzlePolicies.allowed.toString()))
        .put(addProperties("read", "count", "*", "*", KuzzlePolicies.allowed.toString()))
        .put(addProperties("read", "search", "*", "*", KuzzlePolicies.allowed.toString()))
        .put(addProperties("read", "*", "index1", "collection1", KuzzlePolicies.allowed.toString()))
        .put(addProperties("read", "*", "index1", "collection2", KuzzlePolicies.allowed.toString()))
        .put(addProperties("write", "update", "*", "*", KuzzlePolicies.allowed.toString()))
        .put(addProperties("write", "create", "*", "*", KuzzlePolicies.allowed.toString()))
        .put(addProperties("write", "createOrReplace", "*", "*", KuzzlePolicies.allowed.toString()))
        .put(addProperties("write", "delete", "*", "*", KuzzlePolicies.conditional.toString()))
        .put(addProperties("write", "publish", "index2", "*", KuzzlePolicies.allowed.toString()))
        .put(addProperties("security", "searchUsers", "*", "*", KuzzlePolicies.allowed.toString()))
        .put(addProperties("security", "updateUser", "*", "*", KuzzlePolicies.conditional.toString()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullPolicies() {
    kuzzleSecurity.isActionAllowed(null, "ctrl", "action");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullController() {
    kuzzleSecurity.isActionAllowed(policies, null, "action");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullAction() {
    kuzzleSecurity.isActionAllowed(policies, "ctrl", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyController() {
    kuzzleSecurity.isActionAllowed(policies, "", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyAction() {
    kuzzleSecurity.isActionAllowed(policies, "ctrl", "");
  }

  @Test
  public void testControllerActionAllowed() {
    assertEquals(KuzzlePolicies.allowed, kuzzleSecurity.isActionAllowed(policies, "read", "get"));
  }

  @Test
  public void testControllerActionIndexAllowed() {
    assertEquals(KuzzlePolicies.allowed, kuzzleSecurity.isActionAllowed(policies, "read", "count", "myIndex"));
  }

  @Test
  public void testControllerActionIndexCollectionAllowed() {
    assertEquals(KuzzlePolicies.allowed, kuzzleSecurity.isActionAllowed(policies, "read", "search", "index1", "collection1"));
  }

  @Test
  public void testControllerActionIndexCollection2Allowed() {
    assertEquals(KuzzlePolicies.allowed, kuzzleSecurity.isActionAllowed(policies, "read", "search", "index1", "collection2"));
  }

  @Test
  public void testControllerActionDenied() {
    assertEquals(KuzzlePolicies.denied, kuzzleSecurity.isActionAllowed(policies, "read", "replace"));
  }

  @Test
  public void testControllerActionIndexDenied() {
    assertEquals(KuzzlePolicies.denied, kuzzleSecurity.isActionAllowed(policies, "read", "listIndexes", "index2"));
  }

  @Test
  public void testControllerActionConditional() {
    assertEquals(KuzzlePolicies.conditional, kuzzleSecurity.isActionAllowed(policies, "security", "updateUser"));
  }

  @Test
  public void testControllerActionIndexCollectionConditional() {
    assertEquals(KuzzlePolicies.conditional, kuzzleSecurity.isActionAllowed(policies, "write", "delete", "index1", "collection1"));
  }

  @Test(expected = RuntimeException.class)
  public void testJsonException() {
    policies = spy(policies);
    doThrow(JSONException.class).when(policies).length();
    kuzzleSecurity.isActionAllowed(policies, "write", "delete", "index1", "collection1");
  }

}
