import com.cache.LRUCache;
import com.cache.Pair;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LRUCacheTest {
  private LRUCache<String, Integer> lruCache;

  @Before
  public void init() {
    lruCache = LRUCache.newBuilder().maxItems(5).age(100).refreshExpireAfterGet(true).build();
  }

  @Test
  public void shouldOnlyHavingThreeItems() {
    lruCache.set("Hello1", 1);
    lruCache.set("Hello2", 2);
    lruCache.set("Hello3", 3);
    Assert.assertEquals("Should have three items", lruCache.size(), 3);
  }

  @Test
  public void shouldThrowExceptionWhenItemNotFound() throws Exception {
    lruCache.set("Hello1", 1);
    Assert.assertNull(lruCache.get("Hello2"));
  }

  @Test
  public void testCacheEmpty() {
    Assert.assertEquals("Cache is empty", lruCache.isEmpty(), true);
    lruCache.set("hello", 2);
    Assert.assertEquals("Cache is not empty", lruCache.isEmpty(), false);
  }

  @Test
  public void testFlushExpireItems() throws Exception {
    lruCache.set("Hello", 1, 10);
    int expected = lruCache.get("Hello");
    Assert.assertEquals(expected, 1);
    Assert.assertEquals(lruCache.size(), 1l);
    Assert.assertEquals(lruCache.isEmpty(), false);
    // Wait for the expire
    Thread.sleep(10);
    Assert.assertEquals(lruCache.isEmpty(), true);
    Assert.assertEquals(lruCache.size(), 0);
  }

  @Test(expected = Exception.class)
  public void shouldThrowExceptionWhenCacheIsFull() throws Exception {
    lruCache.set("Hello", 34, 10);
    lruCache.set("Hello1", 1, 10);
    lruCache.set("Hello2", 1, 10);
    lruCache.set("Hello3", 1, 10);
    lruCache.set("Hello4", 1, 10);
    lruCache.set("Hello5", 1, 10);
    int x = lruCache.get("Hello");
    System.out.println(x);
  }

  @Test
  public void testKFrequentItems() throws Exception {
    lruCache.set("Hello", 1);
    lruCache.set("Hello1", 1 );
    lruCache.set("Hello2", 1 );
    lruCache.set("Hello3", 1);
    lruCache.set("Hello4", 1);

    lruCache.get("Hello1");
    lruCache.get("Hello2");
    lruCache.get("Hello3");

    List<Pair<String, Integer>> res = lruCache.topKRecentItems(3);
    int count = 3;
    for (Pair<String, Integer> item : res) {
      Assert.assertEquals(item.getKey(), "Hello" + count);
      count--;
    }
  }

  @Test
  public void testCacheStats() throws Exception {
    lruCache.set("Hello", 1, 10);
    lruCache.set("Hello1", 1, 10);
    lruCache.set("Hello2", 1, 10);
    lruCache.set("Hello3", 1, 10);
    lruCache.set("Hello4", 1, 10);

    lruCache.get("Hello4");
    lruCache.get("Hello");

    try {
      lruCache.get("hee");
    } catch (Exception e) {
      Assert.assertEquals(1, lruCache.getCacheStats().getMissCount());
    }

    Assert.assertEquals(2, lruCache.getCacheStats().getHitCount());
  }

}
