# Concurrency Learning Notes

## Overview

### Properties of Concurrent Systems
    - Multiple actors
    - Shared resources
    - Rules for access

### Use of Concurrency
    - Producer/consumer
    - Async IO
    - Parallel programs

### Managing Conurreny
    - Synchronization
    - Atomicity
    - Conditional synchronization

## Race Condition

[Wiki](https://en.wikipedia.org/wiki/Race_condition)

    - A race condition occurs when output is dependent on the sequence or timing of uncontrolled events.
    - Indeterministic
    - Synchronization is necessary for any shared data (critical sections)
    - Mutual exclusion is needed for critical sections

### Properties of Critical Section

    - Mutual exclusion - only one thread at a time
    - Progress - a thread outside the critical section cannot stop another from entering
    - Bounded waiting - a thread waiting to enter will eventually enter
    - Performance - overhead of entering/existing should be small
    - Fair - don't make some threads wait much longer than others

### Synchronization Solutions

    - Atomicity - guarantee to finish actions in one step
    - Conditional Synchronization - sequencing actions

## Locks

[Wiki](https://en.wikipedia.org/wiki/Lock_%28computer_science%29)

A lock is a operating system structure that have two states:

    - Held: someone is in the critical section
    - Not held: nobody is in the critical section

Two operations:

    - Acquire at start of critical section
    - Release at end of critical section

Lock requires hardware support of atomic operation (test_and_set)

### Example: spinlock

[Wiki](https://en.wikipedia.org/wiki/Spinlock)

```java
class Lock {
   boolean held;
   Thread owner

   void acquire(Thread t) {
       while (test_and_set(held)) {
           owner = t;
       }
   }

   void release(Thread t) {
       if (owner != t) {
           return;
       }
       held = false;
   }
}
```

### Lock Granularity

    - Lock overhead - the extra resources for using locks
    - Lock contention - processes/threads competing for the lock
    - Lock granularity - a measure of the amount of data the lock is protecting
    - There is a trade off between decreasing lock overhead and decreasing lock contention.
    - Coarse granularity (small number of locks, each protecting a large segment of data) results in less lock overhead but more lock contention.
    - Fine granularity (large number of locks, each protecting a small segment of data) results in less lock contention but more lock overhead

## Semaphores

[Wiki](https://en.wikipedia.org/wiki/Semaphore_%28programming%29)

A semaphore is a synchronization primitive:

    - Higher-level than locks
    - wait() - decrement counter, if counter is zero then block until signaled
    - signal() - increment counter, wake up one waiter if any
    - init() - initialize counter value

### Example:

```java
class Semaphore {
    int counter;
    Queue threads;
    Lock lock;

    void wait(Thread t) {
        lock.acquire();
        if (counter > 0) {
            --counter;
            lock.release();
        } else {
            threads.add(t);
            lock.release();
            t.sleep();
        }
    }
  
    void signal(Thread t) {
        lock.acquire();
        if (!threads.isEmpty()) {
            threads.remove(t);
            lock.release();
            t.wake();
        } else {
            ++counter;
            lock.release();
        }
    }
}
```

### Binary semaphore

    - Initial counter = 1
    - Useful for lock
    - Difference - binary semaphore is not spinning, hence not wasting CPU time

### Counting semaphore

    - Initial counter > 1
    - Useful for conditional synchronization

### Example: Producer/consumer problem

[Wiki](https://en.wikipedia.org/wiki/Producer%E2%80%93consumer_problem)

```java
class Buffer {
    Semaphore empty;
    Semaphore full;
    Semaphore mutex;
    List resources;

    Buffer(int capacity) {
        empty = new Semaphore(capacity);
        full = new Semaphore(0);
        mutex = new Semaphore(1);
        resources = new List();
    }

    void produce(Resource r) {
        empty.wait();

        mutex.wait();
        resources.add(r);
        mutex.signal();

        full.signal();
    }

    void consume() {
        full.wait();

        mutex.wait();
        Resource r = resources.remove();
        // do something with r
        mutex.signal();

        empty.signal();
    }
}
```

## Deadlock

[Wiki](https://en.wikipedia.org/wiki/Deadlock)

### Starvation, Livelock, Deadlock

    - Starvation is a policy that leaves some thread not executing in some situation, where one or more threads wait indefinitely. For example, OS always scheduling high-priority threads that makes low-priority threads waiting forever.
    - Livelock is a policy that makes all thread do something endlessly do something without making progress. For example, when multiple threads keep trying to allocate memory when there is no memory available.
    - Deadlock is a policy that leaves all thread stuck and wait forever. For example, thread A waits for resource held by thread B, while thread B waits for resource held by thread A.

### Four Conditions for Deadlock

    - Mutual exclusion
    - Hold and wait
    - No preemption
    - Circular wait

### How to Prevent Deadlock

    - Don't allow mutual exclusion (read-only data, no locks necessary, but now you can't do writes)
    - No hold and wait (don't ask for a resource if you're already holding one)
    - Preemption (forcing a process to give up its resource, requires some external force)
    - No circular wait (assign a rank to all resources, every thread must take highest ranked one first, but this might not work for all threads)﻿

## Reader-Writer Lock

[Wiki](https://en.wikipedia.org/wiki/Readers%E2%80%93writers_problem)

    - Multiple reads, one write
    - Fairness to both readers and writers

### Example:

```java
class ResourcePool {
    Semaphore readerLock;
    Semaphore resourceLock;
    Semaphore queueLock;
    int readerCount;

    void reader() {
        queueLock.wait();
        readerLock.wait();

        if (readerCount == 0) {
            resourceLock.wait();
        }

        ++readerCount;

        queueLock.signal();
        readerLock.signal();

        // read resource

        readerLock.wait();
        —readerCount;

        if (readerCount == 0) {
            resourceLock.signal();
        }

        readerLock.signal();
    }

    void writer() {
        queueLock.wait();
        resourceLock.wait();
        queueLock.signal();

        // write resource
        resourceLock.signal();
    }
}
```

### Distributed Locking

[Use Redis to implement distributed lock](https://redis.io/topics/distlock)

[Is Redis distributed lock safe?](http://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html)

[Answer to Redis distrubuted lock analysis](http://antirez.com/news/101)

[Use Consul to implement distributed lock in go](https://distributedbydefault.com/distributed-locks-with-consul-and-golang-c4eccc217dd5)

[Use Zookeeper to implement distributed lock](https://zookeeper.apache.org/doc/r3.3.6/recipes.html#sc_recipes_Locks)
