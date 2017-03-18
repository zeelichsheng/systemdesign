package lru.policy;

import lru.database.Database;
import lru.model.LinkedList;
import lru.model.LinkedListNode;

import java.util.Map;

/**
 * This class defines a read through policy.
 */
public class ReadThroughPolicy<K, V> implements ReadPolicy<K, V> {

  private Database<K, V> database;

  public ReadThroughPolicy(Database<K, V> database) {
    this.database = database;
  }

  /**
   * Read the resource using read through policy.
   */
  public V read(K key,
                Map<K, LinkedListNode<V>> hashTable,
                LinkedList<V> cache) {
    LinkedListNode<V> node;

    if (hashTable.containsKey(key)) {
      node = hashTable.get(key);
      cache.remove(node);
      cache.add(node);
    } else {
      V value = database.get(key);
      node = new LinkedListNode<V>(value);
      cache.add(node);
      hashTable.put(key, node);
    }

    return node.getValue();

  }
}
