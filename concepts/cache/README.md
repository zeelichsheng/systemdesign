# Cache

## Definition

A cache is a hardware or software component that stores data so future requests for that data can be served faster. The data stored in a cache might be the result of an earlier computation, or the duplicate of data stored elsewhere.

A cache is made up of a pool of entries. Each entry has associated data, which is a copy of the same data in some backing store. Each entry also has a tag, which specifies the identity of the data in the backing store of which the entry is a copy.

When the cache client (a CPU, web browser, operating system) needs to access data presumed to exist in the backing store, it first checks the cache. If an entry can be found with a tag matching that of the desired data, the data in the entry is used instead. This situation is known as a cache hit. The alternative situation, when the cache is consulted and found not to contain data with the desired tag, has become known as a cache miss. The previously uncached data fetched from the backing store during miss handling is usually copied into the cache, ready for the next access.

```java
// The cache interface defines the read and write operations
public interface Cache {
  Data read(int key);
  void write(int key, Data data);
}
```

```java
// The eviction policy interface defines the operations needed to evict a cache
public interface EvictionPolicy {
  void refresh(int key);
  int evict();
  void insert(int key);
  
}
```

```java
// The writing policy interface defines the operations needed to write data to a cache
public interface WritingPolicy {
  void write(Data data);
}
```

```java
// The cache base class defines common functions that supports the cache operations
public class CacheBase implements Cache {
  private int capacity;
  private Map<Integer, Data> hashTable;
  
  private Backend backend;
  
  private EvicitionPolicy ep;
  private WritingPolicy wp;
  
  public Data read(int key) {
    if (hashTable.containsKey(key)) {
      ep.refresh(key);
      return hashTable.get(key);
    } else {      
      if (hashTable.size() == capacity) {
        int evictedKey = ep.evict();
        hashTable.remove(evictedKey);
      }
      
      Data data = backend.read(key);
      ep.insert(key);
      hashTable.put(key, data);
      
      return data;
    }
  }
  
  public void write(int key, Data data) {
    if (hashTable.containsKey(key)) {
      ep.refresh(key);
    } else {
      if (hashTable.size() == capacity) {
        int evictedKey = ep.evict();
        hashTable.remove(evictedKey);
      }
      
      ep.insert(key);
    }
    hashTable.put(key, data);
    
    wp.write(data);
  }
}
```

## Replacement (Eviction) Policy

Cache replacement policy are optimizing instructions‍—‌or algorithms‍—‌that a computer program or a hardware-maintained structure can follow in order to manage a cache of information stored on the computer.

### First In First Out (FIFO)

The cache evicts the first block accessed first without any regard to how often or how many times it was accessed before.

```java
public class FifoEvictionPolicy implements EvictionPolicy {
  private Queue<Integer> queue;
    
  public void refresh(int key) {
    return;
  }
  
  public int evict() {
    return queue.poll();
  }
  
  public void insert(int key) {
    queue.offer(key);    
  }
}
```

### Last In First Out (LIFO)

The cache evicts the block accessed most recently first without any regard to how often or how many times it was accessed before.

```java
public class LifoEvictionPolicy implements EvictionPolicy {
  private Stack<Integer> stack;
    
  public void refresh(int key) {
    return;
  }
  
  public int evict(Cache cache) {
    return stack.pop();
  }
  
  public void insert(int key) {
    stack.push(key);
  }
}
```

### Least Recently Used (LRU)

The cache discards the least recently used items first.

```java
public class LruEvictionPolicy implements EvictionPolicy {
  private LinkedList<Integer> linkedList;
    
  public void refresh(int key) {
    int index = linkedList.indexOf(key);
    linkedList.remove(index);
    linkedList.offerFirst(key);
  }
  
  public int evict() {
    return linkedList.pollLast();
  }
  
  public void insert(int key) {
    linkedList.offerFirst(key);  
  }
}
```

### Least Frequently Used (LFU)

The cache evicts an item that is least often used.

```java
public class LfuEvictionPolicy implements EvictionPolicy {
  class KeyWrapper {
    int key;
    int counter;
    
    KeyWrapper(int key) {
      this.key = key;
      this.counter = 1;
    }
  }
  
  class KeyWrapperComparator implements Comparator {
    public int compare(KeyWrapper a, KeyWrapper b) {
      return a.counter - b.counter;
    }
  }
  
  private static KeyWrapperComparator comparator = new KeyWrapperComparator();  
  private LinkedList<KeyWrapper> linkedList;
  private Map<Integer, KeyWrapper> hashTable;
  
  public void refresh(int key) {    
    KeyWrapper keyWrapper = hashTable.get(key);
    keyWrapper.counter++;
    Collections.sort(linkedList, comparator);
  }
  
  public int evict() {
    int key = linkedList.pollLast().key;
    hashTable.remove(key);
    return key;
  }
  
  public void insert(int key) {
    // Insert the new data to the end of the list
    KeyWrapper keyWrapper = new KeyWrapper(key);
    linkedList.offerLast(keyWrapper);
    hashTable.put(key, keyWrapper);
  }
}
```

### Random Replacement (RR)

The cache randomly selects and evicts an item.

```java
public class RandomEvictionPolicy implements EvictionPolicy {
  private Stack<Integer> availableIndexes;
  private ArrayList<Integer> arrayList;
  private Random random;
  
  public void refresh(int key) {  
    return;
  }
  
  public int evict() {
    int index = random.nextInt(arrayList.size());
    int key = arrayList.get(index);
    arrayList.put(index, null);
    availableIndexes.push(index);
    
    return key;
  }
  
  public void insert(int key) {
    int index = availableIndexes.pop();
    arrayList.put(index, key);  
  }
}
```

## Writing Policy

Write-through cache directs write I/O onto cache and through to underlying permanent storage before confirming I/O completion to the host. This ensures data updates are safely stored on, for example, a shared storage array, but has the disadvantage that I/O still experiences latency based on writing to that storage. Write-through cache is good for applications that write and then re-read data frequently as data is stored in cache and results in low read latency.

Write-around cache is a similar technique to write-through cache, but write I/O is written directly to permanent storage, bypassing the cache. This can reduce the cache being flooded with write I/O that will not subsequently be re-read, but has the disadvantage is that a read request for recently written data will create a “cache miss” and have to be read from slower bulk storage and experience higher latency.

Write-back cache is where write I/O is directed to cache and completion is immediately confirmed to the host. This results in low latency and high throughput for write-intensive applications, but there is data availability exposure risk because the only copy of the written data is in cache. As we will discuss later, suppliers have added resiliency with products that duplicate writes. Users need to consider whether write-back cache offers enough protection as data is exposed until it is staged to external storage. Write-back cache is the best performer for mixed workloads as both read and write I/O have similar response time levels.


## References

[Wikipedia - Cache https://en.wikipedia.org/wiki/Cache_(computing)](https://en.wikipedia.org/wiki/Cache_(computing))

[Wikipedia - Cache Replacement Policies https://en.wikipedia.org/wiki/Cache_replacement_policies](https://en.wikipedia.org/wiki/Cache_replacement_policies)

[Wikipedia - Cache Coherence https://en.wikipedia.org/wiki/Cache_coherence](https://en.wikipedia.org/wiki/Cache_coherence)

[Cache Write Policies http://www.computerweekly.com/feature/Write-through-write-around-write-back-Cache-explained](http://www.computerweekly.com/feature/Write-through-write-around-write-back-Cache-explained)

[Cache Write Policies http://rivoire.cs.sonoma.edu/cs351/other/cache_write.html](http://rivoire.cs.sonoma.edu/cs351/other/cache_write.html)