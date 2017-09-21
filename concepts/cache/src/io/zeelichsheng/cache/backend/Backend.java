package io.zeelichsheng.cache.backend;

/**
 * Defines the interface of basic cache operations.
 */
public interface Backend<K, V> {
  /**
   * Returns the size of the cache.
   * @return The size of the cache.
   */
  int size();

  /**
   * Returns true if the cache contains the item of the given key. False otherwise.
   * @param key The key of the item.
   * @return True if the cache contains the item of the given key. False otherwise.
   */
  boolean contains(K key);

  /**
   * Returns the item of the given key.
   * @param key The key of the item.
   * @return The item of the given key.
   */
  V get(K key);

  /**
   * Adds an item to the cache for a given key.
   * @param key The key of the item.
   * @param value The item.
   */
  void add(K key, V value);

  /**
   * Removes the item of a given key from the cache.
   * @param key The key of the item.
   */
  void remove(K key);

  /**
   * Updates the item of a given key in the cache.
   * @param key The key of the item.
   * @param value The new item of the key.
   */
  void update(K key, V value);
}
