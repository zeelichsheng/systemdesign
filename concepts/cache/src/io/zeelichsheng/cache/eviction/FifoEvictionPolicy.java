package io.zeelichsheng.cache.eviction;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Implements the First-In-First-Out eviction policy.
 */
public class FifoEvictionPolicy<K> implements EvictionPolicy<K> {
  private Queue<K> queue;

  public FifoEvictionPolicy() {
    this.queue = new LinkedList<>();
  }

  @Override
  public void refresh(K key) {
  }

  @Override
  public K evict() {
    return queue.poll();
  }

  @Override
  public void insert(K key) {
    queue.offer(key);
  }
}
