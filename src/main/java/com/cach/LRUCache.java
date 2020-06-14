package com.cach;

import static com.base.Preconditions.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.Synchronized;

/**
 * Least Recently Used Cache
 *
 * @param <K>
 * @param <V>
 */
public class LRUCache<K, V> {

  /** Maximum number of items */
  private Long maxItems = 100L;
  /** Default age for all item in the cache */
  private long age = -1;

  /** Update expiration on item when using get method */
  private boolean updateAgeOnGet = false;

  /** Map holding Key Value Pair */
  private Map<K, V> map = new ConcurrentHashMap<>();

  /** Map holding Key and Node pair */
  private Map<K, Node> cacheMap = new ConcurrentHashMap<K, Node>();

  /** Double Linked List */
  private DoubleLinkedList dll = new DoubleLinkedList();

  /** Cache statistic */
  private CacheStats cacheStats;

  /** Step Builder */
  private LRUCache() {}

  public static MaxItemStep newBuilder() {
    return new LRUImpl();
  }

  public interface MaxItemStep {
    AgeSteps maxItems(long maxItems);
  }

  public interface AgeSteps {
    UpdateAgeStep age(long age);

    LRUCache build();
  }

  public interface UpdateAgeStep {
    FinalStep refreshExpireAfterGet(boolean update);

    LRUCache build();
  }

  public interface FinalStep {
    LRUCache build();
  }

  private static class LRUImpl implements MaxItemStep, AgeSteps, FinalStep, UpdateAgeStep {
    private long maxAge = -1;
    private long maxItems;
    private boolean updateAge = false;

    @Override
    public AgeSteps maxItems(long maxItems) {
      checkArgument(maxItems >= 0);
      this.maxItems = maxItems;
      return this;
    }

    @Override
    public FinalStep refreshExpireAfterGet(boolean update) {
      this.updateAge = update;
      return this;
    }

    @Override
    public UpdateAgeStep age(long age) {
      checkArgument(age >= 0);
      this.maxAge = age;
      return this;
    }

    @Override
    public LRUCache build() {
      LRUCache lruCache = new LRUCache();
      lruCache.age = this.maxAge;
      lruCache.maxItems = this.maxItems;
      lruCache.updateAgeOnGet = this.updateAge;
      lruCache.cacheStats = new CacheStats();
      return lruCache;
    }
  }

  private class Node {
    public long age;
    public V value;
    public K key;
    public Node next;
    public Node prev;
    public Date date;

    public Node(K key, V value) {
      this.key = key;
      this.value = value;
    }

    public Node(long age, K key, V value) {
      this(key, value);
      if (age != -1) {
        date = new Date();
        this.age = age;
      }
    }
  }

  private class DoubleLinkedList {
    private Node head;
    private Node tail;
    private long size;

    public DoubleLinkedList() {
      /** Dummy head and tail */
      head = new Node(null, null);
      tail = new Node(null, null);
      head.next = tail;
      tail.prev = head;
    }

    private Node addFirst(Node node) {
      node.next = head.next;
      head.next.prev = node;
      head.next = node;
      node.prev = head;
      size++;
      return head;
    }

    public Node getNode(Node aNode) {
      Node node = removeNode(aNode);
      if (node == null) {
        throw new NullPointerException("Item not found");
      }
      addFirst(node);
      return node;
    }

    private Node removeHelper(Node node) {
      Node prev = node.prev;
      Node next = node.next;
      prev.next = next;
      next.prev = prev;
      return node;
    }

    public Node removeNode(Node node) {
      if (size > 0) {
        size--;
        return removeHelper(node);
      }
      return null;
    }

    public Node removeLeastNode() {
      if (size > 0) {
        --size;
        Node node = tail;
        Node prev = tail.prev;
        prev.next = null;
        tail.prev = null;
        tail = prev;
        return node;
      } else {
        return null;
      }
    }
  }

  @Synchronized
  public void set(@NonNull K key, @NonNull V value) {
    set(key, value, this.age);
  }

  @Synchronized
  public void set(@NonNull K key, @NonNull V value, long age) {
    if (!cacheMap.containsKey(key)) {
      // Check if size is still available
      if (maxItems == 0) {
        maxItems++;
        Node node = dll.removeLeastNode();
        cacheMap.remove(node.key);
        map.remove(node.key);
      }
      Node newNode = new Node(age, key, value);
      cacheMap.put(key, newNode);
      map.put(key, value);
      dll.addFirst(newNode);
      --maxItems;
    } else {
      Node aNode = cacheMap.get(key);
      aNode.value = value;
      dll.getNode(aNode);
    }
  }

  private boolean isExpire(Node aNode) {
    Date date = aNode.date;
    if (date == null) {
      return false;
    }
    Long age = aNode.age;
    Date currentDate = new Date();
    return currentDate.getTime() - date.getTime() >= age;
  }

  @Synchronized
  public V get(@NonNull K key) {
    if (!cacheMap.containsKey(key)) {
      cacheStats.incrementMiss();
      return null;
    }
    Node aNode = cacheMap.get(key);

    // Check if the node is not expire
    if (aNode.age != -1) {
      if (isExpire(aNode)) {
        inValidate(aNode.key);
        cacheStats.incrementEviction();
        return null;
      } else {
        if (this.updateAgeOnGet) {
          aNode.date = new Date();
        }
      }
    }
    cacheStats.incrementHit();
    dll.getNode(aNode);
    return aNode.value;
  }

  @Synchronized
  public boolean isEmpty() {
    flushAllExpireItems();
    return cacheMap.isEmpty();
  }

  @Synchronized
  public long size() {
    flushAllExpireItems();
    return cacheMap.size();
  }

  @Synchronized
  public Set<K> keySet() {
    flushAllExpireItems();
    return map.keySet();
  }

  @Synchronized
  public Collection<V> values() {
    flushAllExpireItems();
    return map.values();
  }

  @Synchronized
  public List<Pair<K, V>> toList() {
    flushAllExpireItems();
    List<Pair<K, V>> list = new ArrayList<>();
    Iterator<Node> iterator = cacheMap.values().iterator();
    while (iterator.hasNext()) {
      Node node = iterator.next();
      list.add(new Pair(node.key, node.value));
    }
    return list;
  }

  @Synchronized
  public List<Pair<K, V>> topKRecentItems(long k) {
    checkArgument(k < size(), "K can't be greater than number of available items");
    checkArgument(k > 0, "K can't be negative");
    List<Pair<K, V>> list = new ArrayList<>();
    Node curr = this.dll.head.next;
    while (curr != null && k != 0) {
      list.add(new Pair(curr.key, curr.value));
      k--;
      curr = curr.next;
    }
    return list;
  }

  /**
   * @param key - Key in the cache to invalidate
   * @return true/false if invalidate successfully
   */
  public boolean inValidate(@NonNull K key) {
    if (!cacheMap.containsKey(key)) {
      return false;
    } else {
      Node aNode = cacheMap.get(key);
      cacheMap.remove(key);
      map.remove(key);
      dll.removeNode(aNode);
      --maxItems;
      return true;
    }
  }

  private void flushAllExpireItems() {
    for (K key : cacheMap.keySet()) {
      Node aNode = cacheMap.get(key);
      if (isExpire(aNode)) {
        inValidate(key);
      }
    }
  }

  public CacheStats getCacheStats() {
    return this.cacheStats;
  }

  /** Evict the key */
  public V evict(K key) {
    if (!cacheMap.containsKey(key)) {
      return null;
    }
    Node node = cacheMap.get(key);
    cacheMap.remove(key);
    map.remove(key);
    dll.removeNode(node);
    return node.value;
  }
}
