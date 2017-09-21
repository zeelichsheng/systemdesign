package io.zeelichsheng.cache;

import io.zeelichsheng.cache.backend.Backend;
import io.zeelichsheng.cache.eviction.EvictionPolicy;
import io.zeelichsheng.cache.persistence.PersistenceStore;
import io.zeelichsheng.cache.persistence.WritingPolicy;

/**
 * Implements a cache.
 */
public class Cache<K, V> {
  private int capacity;
  private Backend<K, V> backend;
  private EvictionPolicy<K> evictionPolicy;
  private WritingPolicy<K, V> writingPolicy;
  private PersistenceStore<K, V> persistenceStore;

  public Cache(int capacity,
               Backend backend,
               EvictionPolicy evictionPolicy,
               WritingPolicy writingPolicy,
               PersistenceStore persistenceStore) {
    this.capacity = capacity;
    this.backend = backend;
    this.evictionPolicy = evictionPolicy;
    this.writingPolicy = writingPolicy;
    this.persistenceStore = persistenceStore;
  }

  /**
   * Reads an item from cache by the given key. If the read operation hits the cache,
   * returns the item directly from cache and refresh the key. Otherwise reads the
   * item from persistence store and save it in the cache.
   * @param key The key of the item.
   * @return The item associated with the key.
   */
  public V read(K key) {
    if (backend.contains(key)) {
      evictionPolicy.refresh(key);
      return backend.get(key);
    } else {
      if (backend.size() == capacity) {
        K evictionKey = evictionPolicy.evict();
        backend.remove(evictionKey);
      }

      V value = persistenceStore.read(key);
      evictionPolicy.insert(key);
      backend.add(key, value);

      return value;
    }
  }

  /**
   * Writes an item to cache by the given key. If the write operation hits the cache,
   * updates the item and refresh the key. Otherwise check if we need to evict
   * an existing item to write the new item to cache.
   * Use the writing policy to determine how to persis the item in persistence store.
   * @param key The key of the item.
   * @param value The item associated with the key.
   */
  public void write(K key, V value) {
    if (backend.contains(key)) {
      evictionPolicy.refresh(key);
      backend.update(key, value);
    } else {
      if (backend.size() == capacity) {
        K evictionKey = evictionPolicy.evict();
        backend.remove(evictionKey);
      }

      evictionPolicy.insert(key);
      backend.add(key, value);
    }

    writingPolicy.write(key, value);
  }
}
