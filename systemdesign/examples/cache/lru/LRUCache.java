package lru;

import lru.model.LinkedList;
import lru.model.LinkedListNode;
import lru.policy.ReadPolicy;
import lru.policy.WritePolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implements a simple LRU cache.
 */
public class LRUCache<K, V> {

  private Map<K, LinkedListNode<V>> hashTable;
  private LinkedList<V> cache;

  private ReadPolicy<K, V> readPolicy;
  private WritePolicy<K, V> writePolicy;

  public LRUCache(int capacity,
                  ReadPolicy<K, V> readPolicy,
                  WritePolicy<K, V> writePolicy) {
    hashTable = new HashMap<K, LinkedListNode<V>>();
    cache = new LinkedList<V>(capacity);

    this.readPolicy = readPolicy;
    this.writePolicy = writePolicy;
  }

  /**
   * Reads the value based on the given key.
   */
  public V read(K key) {
    return readPolicy.read(key, hashTable, cache);
  }

  /**
   * Writes the value based on the given key.
   */
  public void write(K key, V value) {
    writePolicy.write(key, value, hashTable, cache);
  }
}
