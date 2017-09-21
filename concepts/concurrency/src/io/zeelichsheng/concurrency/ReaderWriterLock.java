package io.zeelichsheng.concurrency;

import java.util.concurrent.Semaphore;

/**
 * Implements a Readers-Writer lock.
 */
public class ReaderWriterLock {
  private Semaphore readTryLock;
  private Semaphore serviceQueueLock;
  private Semaphore resourceLock;
  private int readerCount;

  public ReaderWriterLock() {
    this.readTryLock = new Semaphore(1);
    this.serviceQueueLock = new Semaphore(1);
    this.resourceLock = new Semaphore(1);
    this.readerCount = 0;
  }

  /**
   * Acquires a lock before a read operation.
   * @throws InterruptedException
   */
  public void acquireReaderLock() throws InterruptedException {
    serviceQueueLock.wait();
    readTryLock.wait();
    if (readerCount == 0) {
      resourceLock.wait();
    }
    ++readerCount;
    serviceQueueLock.notify();
    readTryLock.notify();
  }

  /**
   * Releases a lock after a read operation.
   * @throws InterruptedException
   */
  public void releaseReaderLock() throws InterruptedException {
    readTryLock.wait();
    --readerCount;
    if (readerCount == 0) {
      resourceLock.notify();
    }
    readTryLock.notify();
  }

  /**
   * Acquires a lock before a write operation.
   * @throws InterruptedException
   */
  public void acquireWriterLock() throws InterruptedException {
    serviceQueueLock.wait();
    resourceLock.wait();
    serviceQueueLock.notify();
  }

  /**
   * Releases a lock after a write operation.
   * @throws InterruptedException
   */
  public void releaseWriterLock() throws InterruptedException {
    resourceLock.notify();
  }
}
