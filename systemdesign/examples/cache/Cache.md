# Cache

Cache system stores used resources and when next time someone requests the same resource, the system can return immediately. It increases the system efficiency by consuming more storage space.

## Design Goals

- Latency: latency is important since the purpose of designing a cache is to reduce latency.

- Consistency: is it ok if things are eventually consistent? For example, if we are designing a cache for a social network website, it is OK to miss a few updates as long as those updates eventually show up.

- Availability: unavailability in a caching system means that the caching machine goes down, which in turn means that we have a cache miss that leads to a high latency. Unavailability could lead to latency spikes and increased load on DB. Choosing from consistency and availability, we should prioritize for availability.

## Things to Consider

Before jumping into designing details, we should ask some questions.

### What is the amount of data we need to cache?

MB? GB? TB? Can all the data fit in memory?

### What should be eviction strategy?

If the memory cannot fit all the data, we should consider an eviction strategy to accomodate new data. We need to remove one or more entries to make space.

### What should be the access pattern for the given cache?

- Write-through cache directs write I/O onto cache and through to underlying permanent storage before confirming I/O completion to the client. This ensures data updates are safely stored on, for example, a shared storage array, but has the disadvantage that I/O still experiences latency based on writing to that storage. Write-through cache is good for applications that write and then re-read data frequently as data is stored in cache and results in low read latency.

- Write-around cache is a similar technique to write-through cache, but write I/O is written directly to permanent storage, bypassing the cache. This can reduce the cache being flooded with write I/O that will not subsequently be re-read, but has the disadvantage is that a read request for recently written data will create a “cache miss” and have to be read from slower bulk storage and experience higher latency.

- Write-back cache is where write I/O is directed to cache and completion is immediately confirmed to the client. This results in low latency and high throughput for write-intensive applications, but there is data availability exposure risk because the only copy of the written data is in cache. As we will discuss later, suppliers have added resiliency with products that duplicate writes. Users need to consider whether write-back cache solutions offer enough protection as data is exposed until it is staged to external storage. Write-back cache is the best performing solution for mixed workloads as both read and write I/O have similar response time levels.

## Estimation

- Number of machines: numOfMachine = totalDataSize / perMachineMemorySize. For example, if the total data set is 10TB, and an average server has 128GB memory, then the total number of servers required is ~80.

- QPS (Query-per-second) of machine: how many queries one machine should be able to handle? qpsOfMachine = totalQps / numOfMachine. For example, if the total QPS of the service is 16M, and the total machine is 80, then QPS of each machine is 20,000.

- CPU time: for each query, the CPU will be occupied for numOfCpuCore * 1sec / qpsOfMachine. For example, an average server has a single CPU with 4 cores. Then to serve one query, the CPU will be occupied for 4 * 1 / 20,000 / 1,000 / 1,000 = 200us.

- If CPU time is not feasible, then we should increase the number of shards by use more machines. The memory requirement of each machine recudes correspondingly.

## LRU Cache

Least Recently Used cache is the most common cache system. When the client requiests resource A, it happens as follow:

- If A exists in the cache, we just return immediately.
- If not and the cache has extra storage slots, we fetch resource A from cold storage and return to the client. In addition, insert A into the cache.
- If the cache is full, we kick out the resource that is least recently used and replace it with resource A.

### Design

An LRU cache should support three operations: find, insert, and delete.

- Find: in order to achieve fast lookup, we need to use hash.
- Insert/delete: insert/delete should be fast and easy. So linked list is the best choices here.

To combine all these analyses, we can use linked list to store all the resources. Also, a hash table with resource identifier as key and the corresponding list node as the value is needed.

### Algorithm

When resource A is requested, we check the hash table to see if A exists in the cache. If exists, we can locate the corresponding list node and return the resource. If not, we will retrieve A from cold storage and insert A into the cache. If there are enough space, we insert A to the head of the list and update the hash table. Otherwise, we remove the tail of the list to make space and insert A to the head of the list.

### Eviction Policy

When the cache is full, we need to remove existing items for new resource.

The strategy we described in the above Algorithm section is the simplest - it removes the resource that is the last accessed. There are other approaches:

- Random Replacement (RR): as the term suggests, we can just randomly delete an entry.

- Least frequently used (LFU): we keep the count of how frequent each item is requested and delete the one least frequently used.

- W-TinyLFU: the problem of LFU is that sometimes an item is only used frequently in the past, but LFU will still keep this item for a long while. W-TinyLFU solves this problem by calculating frequency within a time window. It also has various optimizations of storage.

### Concurrency

When multiple clients are trying to update the cache at the same time, there can be conflicts. The common solution is using a lock. But it affects performance a lot.

Another approach is to split the cache into multiple shard, and have a lock for each of them so that clients won't wait for each other if they are updating cache from different shards. However, given that hot entries are more likely to be visited, certain shards will be more often locked than others.

## Distributed Cache

Using a hash table usually means you need to store everything in memory, which may not be possible when the data set is big. There are two common solutions:

- Compress the data.
- Storing the data in a disk (cold storage).

But ultimately you will want to scale the cache into multiple machines. The general idea here is to split the data into multiple machines by some rules and a coordinator machine can direct clients to the machine with requested resource.

### Sharding

Assume the key type is string, and only lower-case character is allowed. Then the first approach is to allocate 26 machines, where each machine can handle a request with a key starts with a corresponding character.

However, the disadvantage is that the key can be distributed unevenly - some machine will receive more traffic than the others. For example, if the key is URL, then we would expect to have more URLs starting with "a" than "z".

- A good sharding algorithm should be able to balance traffic equally to all machines.
- Hashing the key can achieve much better performance by reducing the length of the key. It also helps to balance the traffic since the hashed value is random.

### Replica

If one of the machine fails, the cache would become unavailable. The most common solution is replica. By setting machines with duplicate resources, we can significantly reduce the system downtime.

- Consistency: since the resources are replicated over multiple machines, over time the data on different machines can become inconsistent. To solve this, we can keep a local copy on the coordinator. Whenever there is an update, the coordinator saves a copy of the updated version. In case the update fails, the coordinator is able to re-do the update. The second solution is to use a commit log, where each machine keeps a log of the update operation. If the operation fail, we can recover from the log. The third solution would be to resolve the conflict on the fly. When requesting for the resource, the coordinator would request all machines for the same resource, and resolve difference if any.

- Latency: if inconsistency can be tolerated, then we can use the master-slave technique. We can have one master that takes all write traffic, while many slaves take all read traffic.

## References

1. [Cache System Wiki](https://en.wikipedia.org/wiki/Cache_(computing))
2. [Cache Access Pattern Explained](http://www.computerweekly.com/feature/Write-through-write-around-write-back-Cache-explained)
3. [Design a Cache System](http://blog.gainlo.co/index.php/2016/05/17/design-a-cache-system/)
4. [Design Key-Value Store](http://blog.gainlo.co/index.php/2016/06/14/design-a-key-value-store-part-i/)
5. [Design a Cache Interview Walkthrough](https://www.interviewbit.com/problems/design-cache)
6. [Intro to Caching](http://javalandscape.blogspot.com/2009/01/cachingcaching-algorithms-and-caching.html?m=1)