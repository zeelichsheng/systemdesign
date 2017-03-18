package lru.policy;

import lru.model.LinkedList;
import lru.model.LinkedListNode;

import java.util.Map;

/**
 * This interface defines the read policy contract for a LRU cache.
 */
public interface ReadPolicy<K, V> {

  /**
   * Reads the value based on the given key.
   */
  V read(K key,
         Map<K, LinkedListNode<V>> hashTable,
         LinkedList<V> cache);
}
