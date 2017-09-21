package io.zeelichsheng.cache.persistence;

/**
 * Implements the Write-Back policy.
 */
public class WriteBackPolicy<K, V> implements WritingPolicy<K, V> {

  private PersistenceStore<K, V> persistenceStore;

  public WriteBackPolicy(PersistenceStore persistenceStore) {
    this.persistenceStore = persistenceStore;
  }

  @Override
  public void write(K key, V value) {
    persistenceStore.writeAsync(key, value);
  }
}
