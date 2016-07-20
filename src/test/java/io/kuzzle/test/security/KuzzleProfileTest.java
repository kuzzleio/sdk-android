package io.kuzzle.test.security;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.security.KuzzleProfile;
import io.kuzzle.sdk.security.KuzzleRole;
import io.kuzzle.sdk.security.KuzzleSecurity;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class KuzzleProfileTest {
  private Kuzzle kuzzle;
  private KuzzleProfile stubProfile;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws JSONException {
    kuzzle = mock(Kuzzle.class);
    kuzzle.security = new KuzzleSecurity(kuzzle);
    listener = mock(KuzzleResponseListener.class);
    stubProfile = new KuzzleProfile(kuzzle, "foo", null);
  }

  @Test
  public void testConstructorNoContent() throws JSONException {
    KuzzleProfile profile = new KuzzleProfile(kuzzle, "foo", null);
    assertEquals(profile.id, "foo");
    assertEquals(profile.getPolicies().length(), 0);
    assertThat(profile.content, instanceOf(JSONObject.class));
    assertEquals(profile.content.length(), 0);
  }

  @Test
  public void testConstructorContentWithIDs() throws JSONException {
    JSONObject content = new JSONObject(
      "{" +
        "\"policies\": [{\"roleId\": \"foo\"}, {\"roleId\": \"bar\"}, {\"roleId\": \"baz\"}]" +
      "}"
    );
    KuzzleProfile profile = new KuzzleProfile(kuzzle, "foo", content);
    assertEquals(profile.id, "foo");
    assertEquals(profile.getPolicies().length(), 3);
    assertEquals(profile.getPolicies().getJSONObject(2).getString("roleId"), "baz");
    assertThat(profile.content, instanceOf(JSONObject.class));
    assertEquals(profile.content.length(), 0);
  }

  @Test
  public void testConstructorContentWithRoles() throws JSONException {
    JSONObject content = new JSONObject(
      "{" +
        "\"policies\": [" +
          "{\"roleId\": \"foo\"}, " +
          "{\"roleId\": \"bar\"}, " +
          "{\"roleId\": \"baz\"}" +
        "]" +
      "}"
    );
    KuzzleProfile profile = new KuzzleProfile(kuzzle, "foo", content);
    assertEquals(profile.id, "foo");
    assertEquals(profile.getPolicies().length(), 3);
    assertEquals(profile.getPolicies().getJSONObject(2).getString("roleId"), "baz");
    assertThat(profile.content, instanceOf(JSONObject.class));
    assertEquals(profile.content.length(), 0);
  }

  @Test
  public void testAddPolicyObject() throws JSONException {
    stubProfile.addPolicy(new JSONObject().put("roleId", "some role"));
    assertEquals(stubProfile.getPolicies().length(), 1);
    assertTrue(stubProfile.getPolicies().getJSONObject(0).getString("roleId") == "some role");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddNullPolicyObject() throws JSONException {
    stubProfile.addPolicy(new JSONObject().put("roleId", null));
    doThrow(IllegalArgumentException.class).when(stubProfile).addPolicy(any(JSONObject.class));
  }

  @Test
  public void testAddPolicyID() throws JSONException {
    stubProfile.addPolicy("another role");
    assertEquals(stubProfile.getPolicies().length(), 1);
    assertTrue(stubProfile.getPolicies().getJSONObject(0).getString("roleId") == "another role");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveNoRole() throws JSONException {
    stubProfile.save();
  }

  @Test
  public void testSaveNoListener() throws JSONException {
    stubProfile.addPolicy("baz");
    stubProfile.save();
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplaceProfile");
  }

  @Test
  public void testSave() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject());
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    stubProfile.addPolicy("baz");
    stubProfile.save(new KuzzleResponseListener<KuzzleProfile>() {
      @Override
      public void onSuccess(KuzzleProfile response) {
        assertEquals(response, stubProfile);
      }

      @Override
      public void onError(JSONObject error) {
        try {
          assertEquals(error.getString("error"), "stub");
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }
    });
    stubProfile.save(mock(KuzzleOptions.class));

    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplaceProfile");
  }

  @Test
  public void testSetPoliciesObjectList() throws JSONException {
    JSONArray policies = new JSONArray("[{\"roleId\": \"bar\"},{\"roleId\": \"baz\"},{\"roleId\": \"qux\"}]");

    stubProfile.addPolicy("foo");
    stubProfile.setPolicies(policies);
    assertEquals(stubProfile.getPolicies().length(), 3);
    assertEquals(stubProfile.getPolicies().getJSONObject(0).getString("roleId"), "bar");
    assertEquals(stubProfile.getPolicies().getJSONObject(1).getString("roleId"), "baz");
    assertEquals(stubProfile.getPolicies().getJSONObject(2).getString("roleId"), "qux");
  }

  @Test
  public void testSetRolesIDs() throws JSONException {
    String[] policies = {"bar", "baz", "qux"};

    stubProfile.addPolicy("foo");
    stubProfile.setPolicies(policies);
    assertEquals(stubProfile.getPolicies().length(), 4);
    assertEquals(stubProfile.getPolicies().getJSONObject(0).getString("roleId"), "foo");
    assertEquals(stubProfile.getPolicies().getJSONObject(1).getString("roleId"), "bar");
    assertEquals(stubProfile.getPolicies().getJSONObject(2).getString("roleId"), "baz");
    assertEquals(stubProfile.getPolicies().getJSONObject(3).getString("roleId"), "qux");
  }
}
