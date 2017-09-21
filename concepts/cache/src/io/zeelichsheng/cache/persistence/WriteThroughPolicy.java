package io.zeelichsheng.cache.persistence;

/**
 * Implements the Write-Through policy.
 */
public class WriteThroughPolicy<K, V> implements WritingPolicy<K, V> {

  private PersistenceStore<K, V> persistenceStore;

  public WriteThroughPolicy(PersistenceStore persistenceStore) {
    this.persistenceStore = persistenceStore;
  }

  @Override
  public void write(K key, V value) {
    persistenceStore.write(key, value);
  }
}
