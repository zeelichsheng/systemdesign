package io.zeelichsheng.cache.persistence;

/**
 * Defines the operations of a persistence store.
 */
public interface PersistenceStore<K, V> {
  /**
   * Reads the data from persistence store by the given key.
   * @param key The key of the data.
   * @return The data associated with the key.
   */
  V read(K key);

  /**
   * Writes the data to persistence store by the given key, in a synchronous fashion.
   * @param key The key of the data.
   * @param value The data associated with the key.
   */
  void write(K key, V value);

  /**
   * Writes the data to persistence store by the given key, in a asynchronous fashion.
   * @param key The key of the data.
   * @param value The data associated with the key.
   */
  void writeAsync(K key, V value);
}
