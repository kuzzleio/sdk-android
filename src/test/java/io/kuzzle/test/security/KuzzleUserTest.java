package io.kuzzle.test.security;

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
import io.kuzzle.sdk.security.KuzzleSecurity;
import io.kuzzle.sdk.security.KuzzleUser;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
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
    assertEquals(user.profile, null);
    assertThat(user.content, instanceOf(JSONObject.class));
  }

  @Test
  public void testKuzzleUserConstructorWithEmptyProfile() throws JSONException {
    JSONObject stubProfile = new JSONObject(
      "{" +
        "\"profile\": {" +
          "\"_id\": \"bar\"," +
        "}," +
        "\"someuseless\": \"field\"" +
      "}"
    );
    KuzzleUser user = new KuzzleUser(kuzzle, "foo", stubProfile);
    assertEquals(user.id, "foo");
    assertThat(user.profile, instanceOf(KuzzleProfile.class));
    assertEquals(user.profile.id, "bar");
    assertEquals(user.profile.content.length(), 0);
    assertThat(user.content, instanceOf(JSONObject.class));
    assertEquals(user.content.getString("someuseless"), "field");
  }

  @Test
  public void testKuzzleUserConstructorProfileWithContent() throws JSONException {
    JSONObject stubProfile = new JSONObject(
      "{" +
        "\"profile\": {" +
          "\"_id\": \"bar\"," +
          "\"_source\": {" +
            "\"bohemian\": \"rhapsody\"" +
          "}" +
        "}" +
      "}"
    );
    KuzzleUser user = new KuzzleUser(kuzzle, "foo", stubProfile);
    assertEquals(user.id, "foo");
    assertThat(user.profile, instanceOf(KuzzleProfile.class));
    assertEquals(user.profile.id, "bar");
    assertThat(user.profile.content, instanceOf(JSONObject.class));
    assertEquals(user.profile.content.getString("bohemian"), "rhapsody");
    assertThat(user.content, instanceOf(JSONObject.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHydrateNoListener() throws JSONException {
    stubUser.hydrate(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHydrateNoProfile() throws JSONException {
    stubUser.hydrate(null, listener);
  }

  @Test
  public void testHydrateValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject().put("result", stubProfile.getJSONObject("profile"));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    stubUser.profile = new KuzzleProfile(kuzzle, "bar", null);
    stubUser.hydrate(new KuzzleResponseListener<KuzzleUser>() {
      @Override
      public void onSuccess(KuzzleUser response) {
        assertEquals(response.id, "foo");
        assertThat(response.profile, instanceOf(KuzzleProfile.class));
        assertEquals(response.profile.id, "bar");
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

    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "getProfile");
  }

  @Test(expected = RuntimeException.class)
  public void testHydrateBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject());
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    stubUser.profile = new KuzzleProfile(kuzzle, "bar", null);
    stubUser.hydrate(listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetProfileNullProfile() {
    stubUser.setProfile((KuzzleProfile) null);
  }

  @Test
  public void testSetProfileWithKuzzleProfile() throws JSONException {
    stubUser.setProfile(new KuzzleProfile(kuzzle, "some profile", null));
    assertThat(stubUser.profile, instanceOf(KuzzleProfile.class));
    assertEquals(stubUser.profile.id, "some profile");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetProfileNullID() throws JSONException {
    stubUser.setProfile((String) null);
  }

  @Test
  public void testSetProfileWithStringID() throws JSONException {
    stubUser.setProfile("some profile");
    assertThat(stubUser.profile, instanceOf(KuzzleProfile.class));
    assertEquals(stubUser.profile.id, "some profile");
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

    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
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
    stubUser.profile = new KuzzleProfile(kuzzle, "bar", null);
    JSONObject serialized = stubUser.serialize();
    assertEquals(serialized.getString("_id"), stubUser.id);
    assertEquals(serialized.getJSONObject("body").getString("foo"), "bar");
    assertEquals(serialized.getJSONObject("body").getString("profile"), stubUser.profile.id);
  }
}
