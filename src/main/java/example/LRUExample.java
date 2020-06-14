package example;

import com.cache.LRUCache;
import java.util.Arrays;

public class LRUExample {
  public static void main(String[] args) throws InterruptedException {
    LRUCache<String, Integer> lruCache =
        LRUCache.newBuilder().maxItems(2).age(33).refreshExpireAfterGet(false).build();

    String[][] dataSource = {
      {"K", "V"},
      {"K1", "V1"},
      {"K2", "V2"},
      {"K3", "V3"},
      {"K4", "V4"},
      {"K5", "V5"},
      {"K6", "V6"},
      {"K7", "V7"},
      {"K8", "V8"},
    };

    Runnable addTask =
        () -> {
          Arrays.stream(dataSource).forEach((pair) -> System.out.println(pair[0]));
        };

    Thread thread = new Thread(addTask);
    thread.start();
    thread.join();
  }
}
