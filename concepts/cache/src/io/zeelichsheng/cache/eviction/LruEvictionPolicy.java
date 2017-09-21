package io.zeelichsheng.cache.eviction;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements the Least-Recently-Used eviction policy.
 */
public class LruEvictionPolicy<K> implements EvictionPolicy<K> {

  private LinkedListNode<K> dummy;
  private Map<K, LinkedListNode<K>> hashTable;

  public LruEvictionPolicy() {
    this.dummy = new LinkedListNode<>();
    this.hashTable = new HashMap<>();
  }

  @Override
  public void refresh(K key) {
    LinkedListNode<K> node = hashTable.get(key);
    LinkedListNode<K> nodePrev = node.prev;
    LinkedListNode<K> nodeNext = node.next;

    LinkedListNode<K> oldHead = dummy.next;
    LinkedListNode<K> oldTail = dummy.prev;

    if (node == oldHead) {
      return;
    }

    if (node == oldTail) {
      dummy.prev = nodePrev;
    }

    if (nodePrev != null) {
      nodePrev.next = nodeNext;
    }

    if (nodeNext != null) {
      nodeNext.prev = nodePrev;
    }

    dummy.next = node;
    node.prev = null;
    node.next = oldHead;
    oldHead.prev = node;
  }

  @Override
  public K evict() {
    LinkedListNode<K> oldHead = dummy.next;
    LinkedListNode<K> oldTail = dummy.prev;

    if (oldTail == oldHead) {
      dummy.next = null;
      dummy.prev = null;
      hashTable.remove(oldHead.key);
      return oldHead.key;
    } else {
      dummy.prev = oldTail.prev;
      hashTable.remove(oldTail.key);
      return oldTail.key;
    }
  }

  @Override
  public void insert(K key) {
    LinkedListNode<K> oldHead = dummy.next;

    LinkedListNode<K> node = new LinkedListNode<>();
    node.key = key;

    dummy.next = node;
    hashTable.put(key, node);

    if (oldHead == null) {
      dummy.prev = node;
    } else {
      node.next = oldHead;
      oldHead.prev = node;
    }
  }

  class LinkedListNode<K> {
    K key;
    LinkedListNode<K> prev;
    LinkedListNode<K> next;
  }
}
