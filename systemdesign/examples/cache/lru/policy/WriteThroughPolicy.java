package lru.policy;

import lru.database.Database;
import lru.model.LinkedList;
import lru.model.LinkedListNode;

import java.util.Map;

/**
 * This class defines a write through policy.
 */
public class WriteThroughPolicy<K, V> implements WritePolicy<K, V> {

  private Database<K, V> database;

  public WriteThroughPolicy(Database<K, V> database) {
    this.database = database;
  }

  /**
   * Writes the resource using write through policy.
   */
  public void write(K key,
                    V value,
                    Map<K, LinkedListNode<V>> hashTable,
                    LinkedList<V> cache) {
    LinkedListNode<V> node;

    if (hashTable.containsKey(key)) {
      node = hashTable.get(key);
      node.setValue(value);
      cache.remove(node);
      cache.add(node);
    } else {
      node = new LinkedListNode<V>(value);
      cache.add(node);
      hashTable.put(key, node);
    }

    database.set(key, value);
  }
}
