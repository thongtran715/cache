import com.cach.LRU;
import com.cach.Pair;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LRUTest {
  private LRU<String, Integer> lru;

  @Before
  public void init() {
    lru = LRU.newBuilder().maxItems(10).age(100).updateAgeOnGet(true).build();
  }

  @Test
  public void shouldOnlyHavingThreeItems() {
    lru.set("Hello1", 1);
    lru.set("Hello2", 2);
    lru.set("Hello3", 3);
    Assert.assertEquals("Should have three items", lru.length(), 3);
  }

  @Test(expected = Exception.class)
  public void shouldThrowExceptionWhenItemNotFound() throws Exception {
    lru.set("Hello1", 1);
    System.out.println(lru.get("Hello2"));
  }

  @Test
  public void testCacheEmpty() {
    Assert.assertEquals("Cache is empty", lru.isEmpty(), true);
    lru.set("hello", 2);
    Assert.assertEquals("Cache is not empty", lru.isEmpty(), false);
  }

  @Test
  public void testFlushExpireItems() throws Exception{
    lru.set("Hello", 1, 10);
    int expected = lru.get("Hello");
    Assert.assertEquals(expected, 1);
    Assert.assertEquals(lru.length(), 1l);
    Assert.assertEquals(lru.isEmpty(), false);
    // Wait for the expire
    Thread.sleep(10);
    Assert.assertEquals(lru.isEmpty(), true);
    Assert.assertEquals(lru.length(), 0);
  }

  @Test(expected = Exception.class)
  public void shouldThrowExceptionWhenCacheIsFull() throws Exception{
    lru.set("Hello", 1, 10);
    lru.set("Hello1", 1, 10);
    lru.set("Hello2", 1, 10);
    lru.set("Hello3", 1, 10);
    lru.set("Hello4", 1, 10);
    lru.get("Hello");
  }

  @Test
  public void testKFrequentItems() throws Exception{
    lru.set("Hello", 1, 10);
    lru.set("Hello1", 1, 10);
    lru.set("Hello2", 1, 10);
    lru.set("Hello3", 1, 10);
    lru.set("Hello4", 1, 10);

    lru.get("Hello1");
    lru.get("Hello2") ;
    lru.get("Hello3") ;

    List<Pair<String, Integer>> res = lru.topKFrequentItems(3);
    int count = 3;
    for (Pair<String, Integer> item: res) {
        Assert.assertEquals(item.getKey(), "Hello" + count);
        count--;
    }
  }
}
