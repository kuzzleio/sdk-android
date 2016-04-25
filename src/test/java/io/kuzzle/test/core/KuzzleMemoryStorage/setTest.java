package io.kuzzle.test.core.KuzzleMemoryStorage;

import org.junit.Before;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.test.testUtils.KuzzleMemoryStorageExtend;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class setTest {

  Kuzzle kuzzle;
  KuzzleMemoryStorageExtend ms;

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    ms = spy(new KuzzleMemoryStorageExtend(kuzzle));
  }

  /*
  @Test
  public void testSet() throws JSONException {
    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    ms.set("foo", "bar");
    ms.set("foo", "bar", SetParams.setParams().ex(42));
    verify(ms, times(2)).send(eq("set"), (JSONObject) argument.capture());
    assertEquals(42, ((JSONObject)argument.getAllValues().get(1)).getLong("ex"));
  }
*/

}
