package io.kuzzle.sdk;

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

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;

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
    System.out.println(profile.content);
    assertEquals(profile.id, "foo");
    assertEquals(profile.getRoles().length, 0);
    assertThat(profile.content, instanceOf(JSONObject.class));
    assertEquals(profile.content.length(), 0);
  }

  @Test
  public void testConstructorContentWithIDs() throws JSONException {
    JSONObject content = new JSONObject(
      "{" +
        "\"roles\": [{\"_id\": \"foo\"}, {\"_id\": \"bar\"}, {\"_id\": \"baz\"}]" +
      "}"
    );
    KuzzleProfile profile = new KuzzleProfile(kuzzle, "foo", content);
    assertEquals(profile.id, "foo");
    assertEquals(profile.getRoles().length, 3);
    assertEquals(profile.getRoles()[2].id, "baz");
    assertThat(profile.content, instanceOf(JSONObject.class));
    assertEquals(profile.content.length(), 0);
  }

  @Test
  public void testConstructorContentWithRoles() throws JSONException {
    JSONObject content = new JSONObject(
      "{" +
        "\"roles\": [" +
          "{\"_id\": \"foo\", \"_source\": {}}, " +
          "{\"_id\": \"bar\", \"_source\": {}}, " +
          "{\"_id\": \"baz\", \"_source\": {}}" +
        "]" +
      "}"
    );
    KuzzleProfile profile = new KuzzleProfile(kuzzle, "foo", content);
    assertEquals(profile.id, "foo");
    assertEquals(profile.getRoles().length, 3);
    assertEquals(profile.getRoles()[2].id, "baz");
    assertThat(profile.content, instanceOf(JSONObject.class));
    assertEquals(profile.content.length(), 0);
  }

  @Test
  public void testAddRoleObject() throws JSONException {
    stubProfile.addRole(new KuzzleRole(kuzzle, "some role", null));
    assertEquals(stubProfile.getRoles().length, 1);
    assertEquals(stubProfile.getRoles()[0].id, "some role");
  }

  @Test
  public void testAddRoleID() throws JSONException {
    stubProfile.addRole("another role");
    assertEquals(stubProfile.getRoles().length, 1);
    assertEquals(stubProfile.getRoles()[0].id, "another role");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveNoRole() throws JSONException {
    stubProfile.save();
  }

  @Test
  public void testSaveNoListener() throws JSONException {
    stubProfile.addRole("baz");
    stubProfile.save();
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplaceProfile");
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
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    stubProfile.addRole("baz");
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

    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplaceProfile");
  }

  @Test
  public void testSetRolesObjectList() throws JSONException {
    KuzzleRole[] roles = {
      new KuzzleRole(kuzzle, "bar", null),
      new KuzzleRole(kuzzle, "baz", null),
      new KuzzleRole(kuzzle, "qux", null)
    };

    stubProfile.addRole("foo");
    stubProfile.setRoles(roles);
    assertEquals(stubProfile.getRoles().length, 3);
    assertEquals(stubProfile.getRoles()[0].id, "bar");
    assertEquals(stubProfile.getRoles()[1].id, "baz");
    assertEquals(stubProfile.getRoles()[2].id, "qux");
  }

  @Test
  public void testSetRolesIDs() throws JSONException {
    String[] roles = {"bar", "baz", "qux" };

    stubProfile.addRole("foo");
    stubProfile.setRoles(roles);
    assertEquals(stubProfile.getRoles().length, 3);
    assertEquals(stubProfile.getRoles()[0].id, "bar");
    assertEquals(stubProfile.getRoles()[1].id, "baz");
    assertEquals(stubProfile.getRoles()[2].id, "qux");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHydrateNoListener() throws JSONException {
    stubProfile.hydrate(null, null);
  }

  @Test
  public void testHydrateValid() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
              "\"hits\": [" +
                "{" +
                  "\"_id\": \"qux\"," +
                  "\"_source\": {" +
                    "\"_id\": \"qux\"," +
                    "\"some\": \"content\"" +
                  "}" +
                "}" +
              "]," +
              "\"total\": 1" +
            "}" +
          "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    stubProfile.addRole("qux");

    stubProfile.hydrate(new KuzzleResponseListener<KuzzleProfile>() {
      @Override
      public void onSuccess(KuzzleProfile response) {
        assertEquals(response.getRoles().length, 1);
        assertEquals(response.getRoles()[0].id, "qux");

        try {
          assertEquals(response.getRoles()[0].content.getString("some"), "content");
        }
        catch(JSONException e) {
          throw new RuntimeException(e);
        }
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

    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "mGetRoles");
  }

  @Test(expected = RuntimeException.class)
  public void testHydrateBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    stubProfile.addRole("qux");
    stubProfile.hydrate(listener);
  }

}
