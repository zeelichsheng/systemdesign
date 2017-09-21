package io.zeelichsheng.concurrency;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/**
 * Implements a thread safe queue that can be shared by producer and consumer.
 */
public class ProducerConsumerQueue<V> {
  private java.util.Queue<V> queue;
  private Semaphore pollLock;
  private Semaphore offerLock;
  private Object queueLock;

  public ProducerConsumerQueue(int capacity) {
    this.queue = new LinkedList<>();
    this.pollLock = new Semaphore(0);
    this.offerLock = new Semaphore(capacity);
    this.queueLock = new Object();
  }

  /**
   * Consumes one item from the queue.
   * @return First item that is inserted in the queue.
   * @throws InterruptedException
   */
  public V consume() throws InterruptedException {
    V value;
    pollLock.wait();
    synchronized (queueLock) {
      value = queue.poll();
    }
    offerLock.notify();
    return value;
  }

  /**
   * Produces one item to the queue.
   * @param value Last item that is inserted in the queue.
   * @throws InterruptedException
   */
  public void produce(V value) throws InterruptedException {
    offerLock.wait();
    synchronized (queueLock) {
      queue.offer(value);
    }
    pollLock.notify();
  }
}
