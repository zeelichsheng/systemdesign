# Web Crawler

Design a web crawler that collects information from all websites.

The input of the web crawler usually is a URL that points to a web page. The web page may contain many more URLs that point to other web pages. The web crawler would repeat itself to pull information from those URLs.

## Components

- URL Pool: the crawler starts with a pool of URLs. There are different ways to choose the initial URLs in the pool: they can be most popular websites on the Internet (e.g. to get latest trend), or a list of websites that focus in a certain area (e.g. music, art, etc.). New URLs parsed from the web page will be added to the pool, as the next crawling target.

- Parser: it parses the content (usually HTML) and extract potential URLs that we want to crawl.

- Data Store: stores the URLs that haven been crawled. Other metadata can be appended to the URL, e.g. number of reference, security level, etc.

## Problems

### Size of the URL Pool

First we need to consider the size of the URL pool. Can we host the entire pool in memory? For example, if the average length of a URL is 200 characters, then 1 million URLs would require 200 * 1M = 200MB memory usage. If the pool becomes too large, we have to persist already-crawled URLs to disk.

However, we need the crawling history since we don't want to crawl the same URL again and again. If the URL is pesisted to disk, then looking up that URL is much slower.

- Hash the URL to reduce its length.
- Use bit map instead of string.
- LRU (if the URL hasn't appeared in the last 1,000 crawls, move it to disk)

### Repeating URLs

Next we need to think about how to eliminate crawling same or similar URLs. It is possible that different URLs point to the same web content.

For example, shortened URL v.s. regular URL. How do we know they are the same?

Another issue is how to detect a circle in the crawling path, e.g. A->B->C->A?

### Web Content

Think about the web content located by the URL. The content could be static HTML, or can be dynamically generated content that the crawler has no ability to download. Or the URLs embedded in the HTML has strange markups/encoding that is hard to parse.

### Data Store

What information do we need other than the URL and its content? For example, we can record the popularity of the URL such that we can show the user the top 1000 most popular websites. Or can we tweak the algorithm to start the next crawl from the most popular URLs?

Over time, some URLs get stale, and other URLs' content changes. How do we maintain the data store's freshness? What about URL content versioning?

### Crawling Frequency

We don't want to overwhelm the bandwidth of our and other's servers by crawling too frequenly. But we also want fresh web content.

Can we record which URL's content changes most frequently? Can we design an algorithm to crawl popular URLs more frequently than long-tail URLs?

### Scalability

We can start the crawler with a single server setup. We can do a quick math on how much crawling request a single server can handle:

- Server's resources (CPU, memory, disk, network bandwidth)
- Start with crawling the web sequentially. Then we only need to worry about the size of a single web page to be downloaded into memory. Once the new URLs are extracted from the web page, the web page can be persisted to data store to free up memory.
- Then crawl the web concurrently. How to coordinate between multiple crawlers, e.g. avoid crawling the same URL? Memory requirement is multiplied by the number of crawlers as well, since the crawlers all need to download the web content into memory at the same time.
- At certain point scaling a single server vertically is not possible, say limited by the network bandwidth. Can we design a crawler that runs in a distributed system?

## References

1. [Design a web crawler](http://flexaired.blogspot.com/2011/09/design-web-crawler.html)

2. [Build a web crawler](http://blog.gainlo.co/index.php/2016/06/29/build-web-crawler/)

3. [Design and Implement of a High-Performance Distributed Web Crawler](http://engineering.nyu.edu/~suel/papers/crawl.pdf)

