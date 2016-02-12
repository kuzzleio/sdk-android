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
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleSecurityDocumentList;
import io.kuzzle.sdk.security.*;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class KuzzleSecurityTest {
  private Kuzzle kuzzle;
  private KuzzleSecurity kuzzleSecurity;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    kuzzleSecurity = new KuzzleSecurity(kuzzle);
    listener = mock(KuzzleResponseListener.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetRoleNoID() {
    kuzzleSecurity.getRole(null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetRoleNoListener() {
    kuzzleSecurity.getRole("foobar", null);
  }

  @Test
  public void testGetRoleValid() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
              "\"_id\": \"foobar\"," +
              "\"_source\": {" +
                "\"indexes\": {}" +
              "}" +
            "}" +
          "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.getRole("foobar", null, new KuzzleResponseListener<KuzzleRole>() {
      @Override
      public void onSuccess(KuzzleRole response) {
        assertEquals(response.id, "foobar");
      }

      @Override
      public void onError(JSONObject error) {
        try {
          assertEquals(error.getString("error"), "stub");
        }
        catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }
    });

    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getRole");
  }

  @Test(expected = RuntimeException.class)
  public void testGetRoleInvalid() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzleSecurity.getRole("foobar", null, listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetRoleInvalidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.getRole("foobar", null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchRolesNoFilters() throws JSONException {
    kuzzleSecurity.searchRoles(null, null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchRolesNoListener() throws JSONException {
    kuzzleSecurity.searchRoles(new JSONObject(), null);
  }

  @Test
  public void testSearchRolesValid() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
              "\"hits\": [" +
                "{" +
                  "\"_id\": \"foobar\"," +
                  "\"_source\": {" +
                    "\"_id\": \"foobar\"," +
                    "\"indexes\": {}" +
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

    kuzzleSecurity.searchRoles(new JSONObject(), null, new KuzzleResponseListener<KuzzleSecurityDocumentList>() {
      @Override
      public void onSuccess(KuzzleSecurityDocumentList response) {
        assertEquals(response.getTotal(), 1);
        assertEquals(response.getDocuments().get(0).id, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "searchRoles");
  }

  @Test(expected = RuntimeException.class)
  public void testSearchRolesBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.searchRoles(new JSONObject(), null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateRoleNoID() throws JSONException {
    kuzzleSecurity.createRole(null, new JSONObject());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateRoleNoContent() throws JSONException {
    kuzzleSecurity.createRole("foo", null);
  }

  @Test
  public void testCreateRoleNoListener() throws JSONException {
    kuzzleSecurity.createRole("foo", new JSONObject(), new KuzzleOptions());
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createRole");
  }

  @Test
  public void testCreateRoleReplaceIfExists() throws JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setUpdateIfExists(true);

    kuzzleSecurity.createRole("foo", new JSONObject(), options);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplaceRole");
  }

  @Test
  public void testCreateRoleValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"_id\": \"foobar\"," +
            "\"_source\": {" +
            "\"indexes\": {}" +
            "}" +
            "}" +
            "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.createRole("foobar", new JSONObject(), new KuzzleResponseListener<KuzzleRole>() {
      @Override
      public void onSuccess(KuzzleRole response) {
        assertEquals(response.id, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createRole");
  }

  @Test(expected = RuntimeException.class)
  public void testCreateRoleBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.createRole("foobar", new JSONObject(), listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeleteRoleNoID() throws JSONException {
    kuzzleSecurity.deleteRole(null);
  }

  @Test
  public void testDeleteRoleNoListener() throws JSONException {
    kuzzleSecurity.deleteRole("foo", new KuzzleOptions());
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "deleteRole");
  }

  @Test
  public void testDeleteRoleValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
              "\"_id\": \"foobar\"" +
            "}" +
          "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.deleteRole("foobar", new KuzzleResponseListener<String>() {
      @Override
      public void onSuccess(String response) {
        assertEquals(response, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "deleteRole");
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteRoleBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.deleteRole("foobar", new KuzzleOptions(), listener);
  }

  @Test
  public void testRoleFactory() throws JSONException {
    assertThat(kuzzleSecurity.roleFactory("id"), instanceOf(KuzzleRole.class));
    assertThat(kuzzleSecurity.roleFactory("id", new JSONObject()), instanceOf(KuzzleRole.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetProfileNoID() throws JSONException {
    kuzzleSecurity.getProfile(null, new KuzzleOptions(), listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetProfileNoListener() throws JSONException {
    kuzzleSecurity.getProfile("foo", null);
  }

  @Test
  public void testgetProfileValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
              "\"_id\": \"foobar\"," +
              "\"_source\": {}" +
            "}" +
          "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.getProfile("foobar", new KuzzleResponseListener<KuzzleProfile>() {
      @Override
      public void onSuccess(KuzzleProfile response) {
        assertEquals(response.id, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getProfile");
  }

  @Test(expected = RuntimeException.class)
  public void testgetProfileBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.getProfile("foobar", listener);
  }


  @Test(expected = IllegalArgumentException.class)
  public void testSearchProfilesNoFilters() throws JSONException {
    kuzzleSecurity.searchProfiles(null, null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchProfilesNoListener() throws JSONException {
    kuzzleSecurity.searchProfiles(new JSONObject(), null);
  }

  @Test
  public void testSearchProfilesValid() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"hits\": [" +
            "{" +
            "\"_id\": \"foobar\"," +
            "\"_source\": {" +
            "\"_id\": \"foobar\"," +
            "\"indexes\": {}" +
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

    kuzzleSecurity.searchProfiles(new JSONObject(), null, new KuzzleResponseListener<KuzzleSecurityDocumentList>() {
      @Override
      public void onSuccess(KuzzleSecurityDocumentList response) {
        assertEquals(response.getTotal(), 1);
        assertEquals(response.getDocuments().get(0).id, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "searchProfiles");
  }

  @Test(expected = RuntimeException.class)
  public void testSearchProfilesBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.searchProfiles(new JSONObject(), null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateProfileNoID() throws JSONException {
    kuzzleSecurity.createProfile(null, new JSONObject());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateProfileNoContent() throws JSONException {
    kuzzleSecurity.createProfile("foo", null);
  }

  @Test
  public void testCreateProfileNoListener() throws JSONException {
    kuzzleSecurity.createProfile("foo", new JSONObject(), new KuzzleOptions());
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createProfile");
  }

  @Test
  public void testCreateProfileReplaceIfExists() throws JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setUpdateIfExists(true);

    kuzzleSecurity.createProfile("foo", new JSONObject(), options);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplaceProfile");
  }

  @Test
  public void testCreateProfileValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"_id\": \"foobar\"," +
            "\"_source\": {" +
            "\"indexes\": {}" +
            "}" +
            "}" +
            "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.createProfile("foobar", new JSONObject(), new KuzzleResponseListener<KuzzleProfile>() {
      @Override
      public void onSuccess(KuzzleProfile response) {
        assertEquals(response.id, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createProfile");
  }

  @Test(expected = RuntimeException.class)
  public void testCreateProfileBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.createProfile("foobar", new JSONObject(), listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeleteProfileNoID() throws JSONException {
    kuzzleSecurity.deleteProfile(null);
  }

  @Test
  public void testDeleteProfileNoListener() throws JSONException {
    kuzzleSecurity.deleteProfile("foo", new KuzzleOptions());
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "deleteProfile");
  }

  @Test
  public void testDeleteProfileValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"_id\": \"foobar\"" +
            "}" +
            "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.deleteProfile("foobar", new KuzzleResponseListener<String>() {
      @Override
      public void onSuccess(String response) {
        assertEquals(response, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "deleteProfile");
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteProfileBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.deleteProfile("foobar", new KuzzleOptions(), listener);
  }

  @Test
  public void testProfileFactory() throws JSONException {
    assertThat(kuzzleSecurity.profileFactory("id"), instanceOf(KuzzleProfile.class));
    assertThat(kuzzleSecurity.profileFactory("id", new JSONObject()), instanceOf(KuzzleProfile.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetUserNoID() throws JSONException {
    kuzzleSecurity.getUser(null, new KuzzleOptions(), listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetUserNoListener() throws JSONException {
    kuzzleSecurity.getUser("foo", null);
  }

  @Test
  public void testgetUserValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"_id\": \"foobar\"," +
            "\"_source\": {}" +
            "}" +
            "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.getUser("foobar", new KuzzleResponseListener<KuzzleUser>() {
      @Override
      public void onSuccess(KuzzleUser response) {
        assertEquals(response.id, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getUser");
  }

  @Test(expected = RuntimeException.class)
  public void testgetUserBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.getUser("foobar", listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchUsersNoFilters() throws JSONException {
    kuzzleSecurity.searchUsers(null, null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchUsersNoListener() throws JSONException {
    kuzzleSecurity.searchUsers(new JSONObject(), null);
  }

  @Test
  public void testSearchUsersValid() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"hits\": [" +
            "{" +
            "\"_id\": \"foobar\"," +
            "\"_source\": {" +
            "\"_id\": \"foobar\"," +
            "\"indexes\": {}" +
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

    kuzzleSecurity.searchUsers(new JSONObject(), null, new KuzzleResponseListener<KuzzleSecurityDocumentList>() {
      @Override
      public void onSuccess(KuzzleSecurityDocumentList response) {
        assertEquals(response.getTotal(), 1);
        assertEquals(response.getDocuments().get(0).id, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "searchUsers");
  }

  @Test(expected = RuntimeException.class)
  public void testSearchUsersBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.searchUsers(new JSONObject(), null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateUserNoID() throws JSONException {
    kuzzleSecurity.createUser(null, new JSONObject());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateUserNoContent() throws JSONException {
    kuzzleSecurity.createUser("foo", null);
  }

  @Test
  public void testCreateUserNoListener() throws JSONException {
    kuzzleSecurity.createUser("foo", new JSONObject(), new KuzzleOptions());
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createUser");
  }

  @Test
  public void testCreateUserReplaceIfExists() throws JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setUpdateIfExists(true);

    kuzzleSecurity.createUser("foo", new JSONObject(), options);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createOrReplaceUser");
  }

  @Test
  public void testCreateUserValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"_id\": \"foobar\"," +
            "\"_source\": {" +
            "\"indexes\": {}" +
            "}" +
            "}" +
            "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.createUser("foobar", new JSONObject(), new KuzzleResponseListener<KuzzleUser>() {
      @Override
      public void onSuccess(KuzzleUser response) {
        assertEquals(response.id, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "createUser");
  }

  @Test(expected = RuntimeException.class)
  public void testCreateUserBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.createUser("foobar", new JSONObject(), listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeleteUserNoID() throws JSONException {
    kuzzleSecurity.deleteUser(null);
  }

  @Test
  public void testDeleteUserNoListener() throws JSONException {
    kuzzleSecurity.deleteUser("foo", new KuzzleOptions());
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "deleteUser");
  }

  @Test
  public void testDeleteUserValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"_id\": \"foobar\"" +
            "}" +
            "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.deleteUser("foobar", new KuzzleResponseListener<String>() {
      @Override
      public void onSuccess(String response) {
        assertEquals(response, "foobar");
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
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "deleteUser");
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteUserBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.deleteUser("foobar", new KuzzleOptions(), listener);
  }

  @Test
  public void testUserFactory() throws JSONException {
    assertThat(kuzzleSecurity.userFactory("id"), instanceOf(KuzzleUser.class));
    assertThat(kuzzleSecurity.userFactory("id", new JSONObject()), instanceOf(KuzzleUser.class));
  }
}
