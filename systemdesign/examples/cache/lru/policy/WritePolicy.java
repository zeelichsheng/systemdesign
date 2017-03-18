package lru.policy;

import lru.model.LinkedList;
import lru.model.LinkedListNode;

import java.util.Map;

/**
 * This interface defines the write policy contract for a LRU cache.
 */
public interface WritePolicy<K, V> {

  /**
   * Writes the value based on the given key.
   */
  void write(K key,
             V value,
             Map<K, LinkedListNode<V>> hashTable,
             LinkedList<V> cache);
}
