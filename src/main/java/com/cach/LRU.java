package com.cach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class LRU<K, V> {

  /** Maximum number of items */
  private Long maxItems = 100L;
  /** Default age for all item in the cache */
  private Long age;

  /** Update expiration on item when using get method */
  private boolean updateAgeOnGet = false;

  /** Map holding Key Value Pair */
  private HashMap<K, V> map = new HashMap<>();

  /** Map holding Key and Node pair */
  private HashMap<K, Node> cacheMap = new HashMap<K, Node>();

  /** Double Linked List */
  private DoubleLinkedList dll = new DoubleLinkedList();

  /** Step Builder */
  private LRU() {}

  public static MaxItemStep newBuilder() {
    return new LRUImpl();
  }

  public interface MaxItemStep {
    AgeSteps maxItems(long maxItems);
  }

  public interface AgeSteps {
    UpdateAgeStep age(long age);

    LRU build();
  }

  public interface UpdateAgeStep {
    FinalStep updateAgeOnGet(boolean update);

    LRU build();
  }

  public interface FinalStep {
    LRU build();
  }

  private static class LRUImpl implements MaxItemStep, AgeSteps, FinalStep, UpdateAgeStep {
    private Long maxAge;
    private long maxItems;
    private boolean updateAge = false;

    @Override
    public AgeSteps maxItems(long maxItems) {
      this.maxItems = maxItems;
      return this;
    }

    @Override
    public FinalStep updateAgeOnGet(boolean update) {
      this.updateAge = update;
      return this;
    }

    @Override
    public UpdateAgeStep age(long age) {
      this.maxAge = age;
      return this;
    }

    @Override
    public LRU build() {
      LRU lru = new LRU();
      lru.age = this.maxAge;
      lru.maxItems = this.maxItems;
      lru.updateAgeOnGet = this.updateAge;
      return lru;
    }
  }

  private class Node {
    public Long age;
    public V value;
    public K key;
    public Node next;
    public Node prev;
    public Date date;

    public Node(K key, V value) {
      this.key = key;
      this.value = value;
    }

    public Node(Long age, K key, V value) {
      this(key, value);
      if (age != null) {
        date = new Date();
        this.age = age;
      }
    }
  }

  private class DoubleLinkedList {
    private Node head;
    private Node tail;

    private Node addFirst(Node node) {
      if (head == null) {
        head = node;
        tail = head;
      } else {
        node.next = head;
        head.prev = node;
        head = node;
      }
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

    private Node removeHelper(Node toDelete) {
      if (toDelete == tail) {
        Node prev = tail.prev;
        prev.next = null;
        tail.prev = null;
        tail = prev;
        return toDelete;
      }
      Node prev = toDelete.prev;
      Node next = toDelete.next;
      prev.next = next;
      next.prev = prev;
      toDelete.next = null;
      toDelete.prev = null;
      return toDelete;
    }

    public Node removeNode(Node node) {
      if (head == null) {
        throw new NullPointerException("Cache is empty");
      }
      if (head == node) {
        Node aNode = head.next;
        head.next = null;
        if (aNode != null) {
          aNode.prev = null;
          head = aNode;
        }
        return head;
      }
      return removeHelper(node);
    }

    public Node removeLeastNode() {
      if (head == null) {
        throw new NullPointerException("Cache is empty");
      }
      Node node = tail;
      Node prev = tail.prev;
      prev.next = null;
      tail.prev = null;
      tail = prev;
      return node;
    }
  }

  public void set(K key, V value) {
    set(key, value, this.age);
  }

  public void set(K key, V value, long age) {
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

  public V get(K key) throws Exception {
    if (!cacheMap.containsKey(key)) {
      throw new Exception("Item not found");
    }
    Node aNode = cacheMap.get(key);

    // Check if the node is not expire
    if (aNode.age != null) {
      if (isExpire(aNode)) {
        inValidate(aNode.key);
        throw new Exception("Item is expired, therefore have been removed");
      } else {
        if (this.updateAgeOnGet) {
          aNode.date = new Date();
        }
      }
    }
    dll.getNode(aNode);
    return aNode.value;
  }

  public boolean isEmpty() {
    flushAllExpireItems();
    return cacheMap.isEmpty();
  }

  public long length() {
    flushAllExpireItems();
    return cacheMap.size();
  }

  public Set<K> keySet() {
    flushAllExpireItems();
    return map.keySet();
  }

  public Collection<V> values() {
    flushAllExpireItems();
    return map.values();
  }

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

  public List<Pair<K, V>> topKFrequentItems(long k) {
    if (k > length()) {
      throw new RuntimeException("K is not valid");
    }
    List<Pair<K, V>> list = new ArrayList<>();
    Node curr = this.dll.head;
    while (curr != null && k != 0){
      list.add(new Pair(curr.key, curr.value)) ;
      k--;
      curr = curr.next;
    }
    return list;
  }

  /**
   * @param key - Key in the cache to invalidate
   * @return true/false if invalidate successfully
   */
  public boolean inValidate(K key) {
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
}
