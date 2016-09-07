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
import io.kuzzle.sdk.security.KuzzleSecurity;
import io.kuzzle.sdk.security.KuzzleUser;

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

public class KuzzleUserTest {
  private Kuzzle kuzzle;
  private KuzzleUser stubUser;
  private KuzzleResponseListener listener;
  JSONObject stubProfile;

  @Before
  public void setUp() throws JSONException {
    stubProfile = new JSONObject(
      "{" +
        "\"profile\": {" +
        "\"_id\": \"bar\"," +
        "\"_source\": {}" +
        "}," +
        "\"someuseless\": \"field\"" +
        "}"
    );

    kuzzle = mock(Kuzzle.class);
    kuzzle.security = new KuzzleSecurity(kuzzle);
    listener = mock(KuzzleResponseListener.class);
    stubUser = new KuzzleUser(kuzzle, "foo", null);
  }

  @Test
  public void testKuzzleUserConstructorNoContent() throws JSONException {
    KuzzleUser user = new KuzzleUser(kuzzle, "foo", null);
    assertEquals(user.id, "foo");
    assertEquals(user.getProfiles(), null);
    assertThat(user.content, instanceOf(JSONObject.class));
  }

  @Test
  public void testKuzzleUserConstructorWithEmptyProfile() throws JSONException {
    JSONObject stubProfile = new JSONObject(
      "{" +
        "\"profileIds\": [\"bar\"]," +
        "\"someuseless\": \"field\"" +
      "}"
    );
    KuzzleUser user = new KuzzleUser(kuzzle, "foo", stubProfile);
    assertEquals(user.id, "foo");
    assertEquals(user.getProfiles().getString(0), "bar");
    assertThat(user.content, instanceOf(JSONObject.class));
    assertEquals(user.content.getString("someuseless"), "field");
  }

  @Test
  public void testKuzzleUserConstructorProfileWithContent() throws JSONException {
    JSONObject stubProfile = new JSONObject("{\"profileIds\": [\"bar\"]}");
    KuzzleUser user = new KuzzleUser(kuzzle, "foo", stubProfile);
    assertEquals(user.id, "foo");
    assertThat(user.getProfiles(), instanceOf(JSONArray.class));
    assertEquals(user.getProfiles().getString(0), "bar");
    assertThat(user.content, instanceOf(JSONObject.class));
  }

  @Test
  public void testSetProfileWithKuzzleProfile() throws JSONException {
    String[] ids = new String[1];
    ids[0] = "foo";
    stubUser.setProfiles(ids);
    assertEquals(stubUser.getProfiles().getString(0), "foo");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetProfileNullID() throws JSONException {
    String[] ids = null;
    stubUser.setProfiles(ids);
    doThrow(IllegalArgumentException.class).when(stubUser).setProfiles(any(String[].class));
  }

  @Test
  public void testSaveNoListener() throws JSONException {
    stubUser.save();
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplaceUser");
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

    stubUser.save(new KuzzleResponseListener<KuzzleUser>() {
      @Override
      public void onSuccess(KuzzleUser response) {
        assertEquals(response, stubUser);
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
    stubUser.save(mock(KuzzleOptions.class));

    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplaceUser");
  }

  @Test
  public void testSerializeNoProfile() throws JSONException {
    stubUser.content.put("foo", "bar");
    JSONObject serialized = stubUser.serialize();
    assertEquals(serialized.getString("_id"), stubUser.id);
    assertEquals(serialized.getJSONObject("body").toString(), stubUser.content.toString());
    assertEquals(serialized.has("profile"), false);
  }

  @Test
  public void testSerializeWithProfile() throws JSONException {
    stubUser.content.put("foo", "bar");
    stubUser.setProfiles(new String[]{"profile"});
    JSONObject serialized = stubUser.serialize();
    assertEquals(serialized.getString("_id"), stubUser.id);
    assertEquals(serialized.getJSONObject("body").getString("foo"), "bar");
    assertEquals(serialized.getJSONObject("body").getJSONArray("profileIds").getString(0), stubUser.getProfiles().getString(0));
  }

  @Test
  public void testGetProfiles() throws JSONException {
    JSONObject stubProfile = new JSONObject(
            "{\"profileIds\": [\"bar\"]}"
    );
    KuzzleUser user = new KuzzleUser(kuzzle, "foo", stubProfile);
    assertEquals(user.getProfiles().getString(0), "bar");
  }

  @Test
  public void testAddProfile() throws JSONException {
    JSONObject stubProfile = new JSONObject(
            "{\"profileIds\": [\"bar\"]}"
    );
    KuzzleUser user = new KuzzleUser(kuzzle, "foo", stubProfile);
    user.addProfile("new profile");
    assertEquals(user.getProfiles().getString(1), "new profile");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddNullProfile() throws JSONException {
    JSONObject stubProfile = new JSONObject(
            "{\"profileIds\": [\"bar\"]}"
    );
    KuzzleUser user = new KuzzleUser(kuzzle, "foo", stubProfile);
    user.addProfile(null);
    doThrow(IllegalArgumentException.class).when(user).addProfile(eq((String)null));
  }
}
