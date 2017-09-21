package io.zeelichsheng.cache.eviction;

/**
 * Defines the operations of a eviction policy.
 */
public interface EvictionPolicy<K> {

  /**
   * Refreshes the key when a cache hit happens.
   * @param key The key of the cached data.
   */
  void refresh(K key);

  /**
   * Returns the key of the eviction victim.
   * @return The key of the eviction victim.
   */
  K evict();

  /**
   * Inserts the key of the new cached data.
   * @param key The key of the cached data.
   */
  void insert(K key);
}
