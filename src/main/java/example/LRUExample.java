package example;

import com.cach.LRU;

public class LRUExample {
  public static void main(String[] args) throws Exception {
    LRU<String, Integer> lru = LRU.newBuilder().maxItems(2).age(33).updateAgeOnGet(false).build();
    lru.set("Hello3", 10, 33);
    System.out.println(lru.get("Hello3"));
    Thread.sleep(30);
    System.out.println(lru.get("Hello3"));
    Thread.sleep(30);
    System.out.println(lru.get("Hello3"));
  }
}
