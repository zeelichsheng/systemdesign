# Design a Push Notification System

If you are using mobile application, you are probably familiar with push notification. If there is update happening on the server side, a notification will be pushed to your application. For example, if you have an new email, your device will pop up a notification to notify you about it.

## Features

- User can register to event source. Here "user" and "event source" are generic terms. For example, in a social network website, "user" means people registered to the website, and "event source" means other people that is linked to the user. In an email application, "user" means the email account owner, and "event source" means the email server.

- When an event happens at event source, a notification is pushed by the source to the user. The notification can include a title, a summary, a link to the original event, a thumbnail picture, etc.

- When the user clicks/touches on the notification, the user should be redirected to the original event (e.g. social status update, news, etc.)

- The user can also dismiss the notification.

## Model

- User: userid (long), username (128 char), password (128 char), email (256 char), phone number (16 char), connection (avg 50 connections, 50 * userid) ~= 0.6KB

- Notification: notificationid (long), userid (long), title (128 char), summary (1024 char), link (2048 char), thumbnail image (32 KB), userid of notified user (long * 50) ~= 35KB

## Estimation

Q: what is the total number of users?
A: 1 billion (which means 1b * 0.6KB ~= 600GB user data)

Q: what is DAU?
A: 100 million

Q: how many of them creates a status update everyday?
A: 20% (which equals to 20 million updates, 20m * 35KB = 700GB new update data per day)

Q: how many connection each user has?
A: 50 connections (which means 20m * 50 = 1000m notifications)

	Notification traffic per day: 1000m * 35KB ~= 35TB/d
	Notification traffic per second: 35TB / 86400 ~= 400MB/s
	
## Procedure

Consider the notification service is one module of the entire system. Other modules of the system handles status update, rendering, etc. These modules will interact with notification service.

There are four states for each notification object:

- CREATE - when a status update is generated, a notification is created.
- STORE - the notification needs to be stored on some persistent storage.
- PUSH - the notification gets pushed to subscribed users.
- UPDATE - the notification gets updated when a subscribed user reads it, so that it will not be pushed to the same user again.

### Create

When a status update is generated, say by the frontend module, the notification service gets notified, and it creates a notification object. The notification service sends the object to storage so that the object can be persisted. The object also needs to be cached so that the service can reads it fast.

### Store

Due to the large volumn of the notifications generated each day, we need to distribute the notification objects to multiple storage servers. As discussed above, the object should also be stored in a cache.

### Push

The notification service needs to find out what users the notification should be pushed to.

For example, user A connects to user[1, 50]. We need to query the database 50 times to find out the information of those 50 users, specifically how to reach them (email, phone number, or an active connection handle). Because we are receiving 20 million updates each day, that equals to 20m * 50 / 86400 ~= 116,000 QPS.

### Update

Once the subscibted user reads the notification, we need to update the notification object with that userid.

## References
1. [How UrbanAirship scaled to 2.5 billion notifications](https://www.urbanairship.com/blog/how-we-scaled-to-2.5-billion-mobile-notifications-during-us-election)
2. [How to design a scalable notification system](http://softwareengineering.stackexchange.com/questions/177973/how-to-design-a-scalable-notification-system)
3. [Best practices for implementing a Quora-like notification system](https://www.quora.com/What-are-best-practices-for-implementing-a-Quora-Facebook-like-notification-system)
4. [How are Facebook notifications architected?](https://www.quora.com/How-are-Facebook-notifications-architected)
5. [Architecture of notification system similar to Facebook](https://hashnode.com/post/architecture-how-to-build-a-notification-system-similar-to-facebook-cioms9pud0094mz532hcjzuqd)
6. [Facebook architecture collection](http://stackoverflow.com/questions/3533948/facebook-architecture)