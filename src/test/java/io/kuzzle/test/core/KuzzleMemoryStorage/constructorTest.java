package io.kuzzle.test.core.KuzzleMemoryStorage;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.test.testUtils.KuzzleMemoryStorageExtend;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class constructorTest {

  Kuzzle kuzzle;
  KuzzleMemoryStorageExtend ms;

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    ms = spy(new KuzzleMemoryStorageExtend(kuzzle));
  }

  @Test
  public void testConstructor() throws JSONException {
    //@todo
  }

}
