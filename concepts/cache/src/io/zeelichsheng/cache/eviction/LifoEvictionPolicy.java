package io.zeelichsheng.cache.eviction;

import java.util.Stack;

/**
 * Implements the Last-In-First-Out eviction policy.
 */
public class LifoEvictionPolicy<K> implements EvictionPolicy<K> {
  private Stack<K> stack;

  public LifoEvictionPolicy() {
    this.stack = new Stack<>();
  }

  @Override
  public void refresh(K key) {
  }

  @Override
  public K evict() {
    return stack.pop();
  }

  @Override
  public void insert(K key) {
    stack.push(key);
  }
}
