# Design an eCommerce Website

## Features

- Customer management: registration, sign-in, sign-out, list transactions
- Product browsing: list product, sort product by name/category/price, search product, add product to wishlist
- Order management: add product to cart, checkout cart, order tracking, order cancelling
- Product management: add product, delete product, modify product name/description/price/inventory, promotion

## Model

Customer: userid (long), username (128 char), password (128 char), name (128 char), address (1024 char), payment info (credit card number + address  1024 char, 5 cards maximum), email (256 char), phone (10 char) ~= total 7KB

Product category: categoryid (long), category (256 char) ~= total 0.3KB

Product: productid (long), product name (1024 char), description (2048 char), price (decimal), categoryid (long), merchant name (1024 char), pictureid (long, maximum 10 pictures) ~= total 4KB

Product picture: pictureid(long), picture filepath(2048 char) ~= total 2KB

Order: orderid(long), userid(long), productid(long), order time(128 char), order state(int) ~= total 0.2KB

## Estimation

Q: how many users would we serve?
A: assume 1 million

	Total storage for user metadata is 7KB * 1m = 7TB

Q: how many products would we sell?
A: assume 10 million

	Total storage for product metadata is 4KB * 10m = 40TB

Q: what is the average size of the product image?
A: assume 256KB, 4 pics per product

	Total storage for product images is 256KB * 4 * 10m = 10PB

Q: how many concurrent customers should we support?
A: assume 10000 users per second

Q: what is the percentage of the customer that places order?
A: assume 1%

	Read:
	Product browsing per second: 10000/s
	Product browsing throughput: 10000/s * (4KB + 256KB * 4) = 10GB/s
	
	Write:
	Order per second: 10000/s * 1% = 100/s
	Order throughput: 100/s * 0.2KB = 20KB/s

## Distribute Product Data

Since we have 10 million products, we cannot host all product metadata and images on the same machine. We can distribute the product data to multiple machines.

- Distribute the product based on productid: distribute datastore concept, where product data for productid[0, n] is stored on machine 1, and product data for productid[n+1, 2n] is stored on machine 2, and so on.
- Load balancer is used to direct traffic to the correct machine.

## Cache

We can add a cache between the load balancer and the datastores, which stores most popular products. Therefore we save resources for querying the datastore for these products.

- LRU: each time a product is searched and/or ordered, insert this product to the cache.
- Analysis: use the cache to analyze which products are more popular than others. The data provides insight to the merchants.

## LCA (latency, consistency, availability)

### Latency

Customer can tolerate relatively high latency of browsing the product. But the customer cannot tolerate high latency when placing the order, because it makes the ordering look unreliable - the user may refresh page or go back, which could result in double order or other issues.

- Use different servers to handle product browsing and ordering.
- Product browsing (10000 product queries/s, 10GB/s throughput) requires much higher bandwidth and processing power than order handling (100 orders/s, 20KB/s throughput)

### Availability

For each second downtime, you are losing 100 potential orders. Therefore you want the highest availability. That means you want replicas of your product browsing system and ordering system.

However, availability and consistency are aspects on the two ends of the scale: improving one often means sacrificing the other. To achieve high availability, we would require replicating the same data across multiple servers. But replication results in data inconsistency.

### Consistency

One example of consistency issue is that an update of the product would result in inconsistent product data between the replicas. For example, the merchant can update the price of the product. If there are two replicas of the same product stored on server S1 and S2, it is possible that S1's copy gets updated and S2's lagging behind. If a customer browses the product and is directed to S1, then he will see the latest price. If he is directed to S2, then he will see the old price.

One solution is to apply a lock whenever we update a product. However, locking is a very expensive process. For example, if we lock the entire product object for updating its price, then no order can be placed for this product. We can fine tune the granularity of the lock such that it only locks on a particular field (price, inventory) instead of the entire object. However, locking is still an expensive operation.

Another consistency roblem is related to concurrency. We can tolerate some inconsistency in product browsing process. For example, N customers should be able to browse the same product of quantity M, where N >= M. The customers will see inconsistent numbers of the product. All N customers should be able to add the product to their carts.

However, we cannot tolerate inconsistency in ordering process. If N customers added the same product in their carts, and click checkout at the same time, only M orders should succeed. The rest (N - M) orders should fail.

- Add an atomic counter for each product. Whenever a product is added to the cart, lock the counter. When the order is placed, decrease the counter and unlock. This is very expensive procedure and does not support placing multiple orders in parallel.

- Use async queue between the ordering server and the product server. Each time an order is placed, the ordering server inserts a task into the queue. The product server gets notified with the task and checks if there is still available product. If so, notify the ordering server with success status, and update the product count. Otherwise notify the ordering server with failure status.

- If the task queue serves all products, the latency of the ordering process can become intolerable because the product server can lag behind processing the tasks. We can use the same load balancing technique, where the ordering task is processed on different product servers based on productid.

