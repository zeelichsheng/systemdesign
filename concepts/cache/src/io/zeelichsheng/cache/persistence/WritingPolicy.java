package io.zeelichsheng.cache.persistence;

/**
 * Defines operation of a writing policy.
 */
public interface WritingPolicy<K, V> {

  /**
   * Writes the data associated with the key.
   * @param key The key of the data.
   * @param value The data associated with the key.
   */
  void write(K key, V value);
}
