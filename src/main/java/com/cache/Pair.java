package com.cache;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Pair<K, V> {
  private K key;
  private V value;

  public Pair(K key, V value) {
    this.key = key;
    this.value = value;
  }
}
