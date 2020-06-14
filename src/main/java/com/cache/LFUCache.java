package com.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LFUCache<K, V> {

  private Map<K, Node> map;
  private Map<Long, LinkedList> countMap;
  private long capacity;
  private long min;
  private long currentSize = 0;

  private class Node {
    public K key;
    public V value;
    public Node next;
    public Node prev;
    public long count;

    public Node(K key, V value) {
      this.key = key;
      this.value = value;
      this.count = 1;
    }
  }

  private class LinkedList {
    public Node head, tail;
    public long size;

    public LinkedList() {
      head = new Node(null, null);
      tail = new Node(null, null);
      head.next = tail;
      tail.prev = head;
    }

    public void add(Node node) {
      head.next.prev = node;
      node.next = head.next;
      node.prev = head;
      head.next = node;
      size++;
    }

    public void remove(Node node) {
      node.prev.next = node.next;
      node.next.prev = node.prev;
      size--;
    }

    public Node removeLast() {
      if (size > 0) {
        Node node = tail.prev;
        remove(node);
        return node;
      } else return null;
    }
  }

  public LFUCache(int capacity) {
    this.capacity = capacity;
    map = new ConcurrentHashMap<>();
    countMap = new ConcurrentHashMap<>();
  }

  public V get(int key) {
    if (!map.containsKey(key)) {
      return null;
    }
    Node node = map.get(key);
    update(node);
    return node.value;
  }

  public void put(K key, V value) {
    if (capacity == 0) {
      return;
    }

    if (map.containsKey(key)) {
      // update stuff
      Node node = map.get(key);
      node.value = value;
      update(node);
    } else {
      if (capacity == currentSize) {
        // Remove the least frequency
        LinkedList dll = countMap.get(min);
        Node node = dll.removeLast();
        map.remove(node.key);
        currentSize--;
      }
      currentSize++;
      Node node = new Node(key, value);
      map.put(key, node);
      LinkedList dll = countMap.getOrDefault(node.count, new LinkedList());
      dll.add(node);
      min = 1;
      countMap.put(min, dll);
    }
  }

  private void update(Node node) {

    LinkedList dll = countMap.get(node.count);
    dll.remove(node);
    if (node.count == min && dll.head == null) {
      min++;
    }
    node.count += 1;
    LinkedList aList = countMap.getOrDefault(node.count, new LinkedList());
    aList.add(node);
    countMap.put(node.count, aList);
  }
}
