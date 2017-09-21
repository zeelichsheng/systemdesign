package io.zeelichsheng.cache.backend;

import java.util.HashMap;

/**
 * Created by ysheng on 9/20/17.
 */
public class HashMapBackend<K, V> implements Backend<K, V> {
  private HashMap<K, V> hashTable;

  public HashMapBackend() {
    this.hashTable = new HashMap<>();
  }

  public int size() {
    return hashTable.size();
  }

  public boolean contains(K key) {
    return hashTable.containsKey(key);
  }

  public V get(K key) {
    return hashTable.get(key);
  }

  public void update(K key, V value) {
    hashTable.put(key, value);
  }

  public void add(K key, V value) {
    hashTable.put(key, value);
  }

  public void remove(K key) {
    hashTable.remove(key);
  }
}
