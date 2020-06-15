import com.cache.LRUCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LRUCacheThreadsTest {
  private LRUCache<String, Integer> lruCache;

  @Before
  public void init() {
    lruCache = LRUCache.newBuilder().maxItems(1_000_000).build();
  }

  /**
   * An implementation to check if 1000 threads update the same key A synchronization locks should
   * prevent the race condition in the cache
   *
   * @throws InterruptedException
   */
  @Test
  public void testCacheWhenMultiThreadWithTheSameKeys() throws InterruptedException {

    int size = 1000;
    Runnable addTask =
        () -> {
          for (int i = 0; i < size; ++i) {
            lruCache.set(String.valueOf(i), i);
          }
        };

    Thread[] thread = new Thread[size];
    for (int i = 0; i < size; ++i) {
      thread[i] = new Thread(addTask);
      thread[i].start();
    }

    for (int i = 0; i < size; ++i) {
      thread[i].join();
    }
    Assert.assertEquals(1_000, lruCache.size());
  }

  @Test
  public void testCacheWhenMultiThreadWithDifferentKeys() throws InterruptedException {
    int size = 30;
    Runnable addTask =
        () -> {
          String threadName = Thread.currentThread().getName();
          for (int i = 0; i < size; ++i) {
            // Generate unique key for each thread putting in the cache
            threadName += i;
            lruCache.set(threadName, i);
          }
        };

    Thread[] thread = new Thread[size];
    for (int i = 0; i < size; ++i) {
      thread[i] = new Thread(addTask);
      thread[i].start();
    }

    for (int i = 0; i < size; ++i) {
      thread[i].join();
    }
    Assert.assertEquals(900, lruCache.size());
  }

  @Test
  public void testCacheEvictionWhenMultiThread() throws InterruptedException {
    int size = 30;

    Runnable evictTask =
        () -> {
          lruCache.set("Something", 2);
          lruCache.evict("Something");
        };

    Thread[] thread = new Thread[size];
    for (int i = 0; i < size; ++i) {
      thread[i] = new Thread(evictTask);
      thread[i].start();
    }

    for (int i = 0; i < size; ++i) {
      thread[i].join();
    }

    Assert.assertNull(lruCache.get("Something"));
  }
}
