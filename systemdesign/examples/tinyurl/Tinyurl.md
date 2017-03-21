# Tinyurl

Tinyurl is a service that shortens a given full length URL to a shorter version. The user can click on the shortened URL, then the tinyurl service will redirect the user to the website represented by the full length URL.

## Hashing

The basic idea of shortening a full length URL to a shorter version is to use hashing. A common approach is to hash the original URL to a fixed length shorter URL that is composed by only [a-zA-Z0-9] charachters.

That is a total of 62 possible characters. If we assume the short URL is 8 characters long, then the total number of unique short URLs is 62^8 = 218340105584896 ~= 218 trillion.

Now let's design the hash algorithm SU = H(LU), where H() is the hashing function, LU stands for long URL, and SU stands for shortened URL.

- Random fixed-length string generator: we can randomly generate a fixed-length string for each full length URL. One problem is that if the generated string is truely random, we cannot detect if a given LU has already been shortened or not. To solve this, we can use some established hashing algorithm that generates deterministic result. The second problem is that if we store the pair of LU/SU in database, the random string is hard to index because they are not continuous.

- Incremental string: consider that for each OU, we maintain an index in the database. Each time a new LU is shortened, we increment the index by one. Then we convert the index to a string as the shortened URL. Since there are only 62 characters allowed, the problem becomes: how to encode/decode a 62-based integer. This approach trades off performance with flexibility, i.e. the user cannot specify a customized shortened URL.

## Estimation

### Data Size

From this post [What is the maximum length of a URL](https://boutell.com/newfaq/misc/urllength.html), let's assume that the maximum URL length allowed is 2048 characters. And the shortened URL is 8 characters. The index is a long value. Then the total size for storing one URL pair is:

	2048 + 8 + 8 = 2064 bytes
	
If we have 100 million URL translations, the total size of the database is roughly 200GB.

### QPS

Assume the DAU (daily active user) is 1 million.

There are two major functionality of the tinyurl service: SU-to-LU, and LU-to-SU. SU-to-LU represents a lookup operation to the database, while LU-to-SU represents an insert operation. We should assume that SU-to-LU takes the majority of the service's traffice, since it is the whole purpose of the service. Let's assume that 100% of the DAU will use the SU-to-LU functionality, while only 1% of the DAU will use LU-to-SU functionality:

	SU-to-LU QPS = 1m * 100% / 86400 = 35
	LU-to-SU QPS = 1m * 1% / 86400 = 1.2

### Growth

Each year the new URLs added will take roughly:

	8KB * (1m * 1%) * 365 = 30TB
	
## Distributed Hash

Since the data can grow very fast, we have to distribute the data to multiple machines. The hash algorithm need to direct the request to the correct machine to fetch the LU.

There are several ways to distribute keys to multiple machines. One idea is to use H(OU) % n to distribute the keys evenly onto each machine, where n is the number of the machines.

However, this approach has a limitation: if you add more machine to the pool, the keys will no longer be directed to the correct machine. And migrating data to the correct machine is very expensive.

Another approach is to use max(H(OU)) / r to create t key slots:

	t = max(H(OU)) / r
	
The idea being that key [0, t] will be directed to machine 1, key [t + 1, 2t] will be directed to machine 2, and so on. If a new machine is added, it will only affect the other two machine neighbouring it. The new machine can off load some keys from its left/right neighbour.

The drawback of this approach is that in order to locate a machine with a given key, we have to loop the ring to compare the key with the range of each machine until we find the machine.

## Security

If we use incremental string to generate the shortened URL, the shortened URLs can be calculated. We can generate the SU from a range of random strings. But we have to detect conflict each time.

What if the service is abused? DDoS for example? The service could go down and the shortened URLs will be unavailable. Add cache to reduce database access. Add hit count for each URL. If a URL is accessed abnormally high frequently, temporary block the URL.

What if a malicious user try to register the same LU again and again? Maintain another map that maps LU to SU. The key can be a truely hashed value of the LU (e.g. MD5) to reduce size and search time.

## Useful Metadata

- Hit count: we can store the hit count of each URL. This helps companies to analyze what is the trend on the Internet.
- Timestamp: invalidate URLs that hasn't been accessed for a long time.

## References

1. [Create a Tinyurl System](http://blog.gainlo.co/index.php/2016/03/08/system-design-interview-question-create-tinyurl-system/?utm_source=quora&utm_medium=What+are+the+http%3A%2F%2Fbit.ly+and+t.co+shortening+algorithms%3F&utm_campaign=quora)
2. [How to code a URL shortener](http://stackoverflow.com/questions/742013/how-to-code-a-url-shortener)
3. [URL shortening hashes in practice](https://blog.codinghorror.com/url-shortening-hashes-in-practice/)
4. [Tiny URL shortening app](http://systemdesigns.blogspot.jp/2015/12/tinyurl-url-shorten-app.html)
5. [How URL shortening algorithm is implemented](https://www.zhihu.com/question/29270034)
6. [Tinyurl design](http://senarukana.github.io/2015/02/21/tinyurl/)