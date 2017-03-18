package lru.database;

/**
 * This interface defines the database that stores cold data.
 */
public interface Database<K, V> {

  /**
   * Gets the value based on the given key.
   */
  V get(K key);

  /**
   * Sets the value based on the given key.
   */
  void set(K key, V value);
}
