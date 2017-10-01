This document explains how docker configures the network of the host and the container in a multi-host scenario.

In this example, we follow [Docker Machine on Localhost](http://docker-k8s-lab.readthedocs.io/en/latest/docker/docker-machine.html) to setup two virtual machines that run docker locally. 

# Docker Overlay Network with ETCD

In this example, we use docker's built-in support of overlay network. We setup ETCD to store the overlay network information.

Note that following the instruction, we created a new "overlay-demo" network that is used to create containers instead of using the default "bridge" network.

## Initial State

### Initial Network Configuration - docker-node1

```text
ubuntu@docker-node1:~$ sudo docker network ls
NETWORK ID          NAME                DRIVER              SCOPE
afe9167478ca        bridge              bridge              local               
333e4c95c7e3        host                host                local               
abf421788228        none                null                local               
c60a3769ec74        overlay-demo        overlay             global
```

```text
ubuntu@docker-node1:~$ sudo docker network inspect overlay-demo
[
    {
        "Name": "overlay-demo",
        "Id": "c60a3769ec74301345bae3a633ebec0482c1b633bb6b00d1e0c48df3e2427aac",
        "Scope": "global",
        "Driver": "overlay",
        "EnableIPv6": false,
        "IPAM": {
            "Driver": "default",
            "Options": {},
            "Config": [
                {
                    "Subnet": "10.0.0.0/24",
                    "Gateway": "10.0.0.1/24"
                }
            ]
        },
        "Internal": false,
        "Containers": {},
        "Options": {},
        "Labels": {}
    }
]
```

```text
ubuntu@docker-node1:~$ ip route
default via 10.0.2.2 dev enp0s3 
10.0.2.0/24 dev enp0s3  proto kernel  scope link  src 10.0.2.15 
172.17.0.0/16 dev docker0  proto kernel  scope link  src 172.17.0.1 linkdown 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.10 
```

```text
ubuntu@docker-node1:~$ ip link
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: enp0s3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 02:a4:a0:f1:db:6e brd ff:ff:ff:ff:ff:ff
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 08:00:27:76:c9:85 brd ff:ff:ff:ff:ff:ff
4: docker0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN mode DEFAULT group default 
    link/ether 02:42:1c:b7:39:99 brd ff:ff:ff:ff:ff:ff
```

```text
ubuntu@docker-node1:~$ ip neighbor
192.168.205.11 dev enp0s8 lladdr 08:00:27:7c:fe:ad REACHABLE
10.0.2.3 dev enp0s3 lladdr 52:54:00:12:35:03 STALE
10.0.2.2 dev enp0s3 lladdr 52:54:00:12:35:02 REACHABLE
```

### Initial Network Configuration - docker-node2

```text
ubuntu@docker-node2:~$ sudo docker network ls
NETWORK ID          NAME                DRIVER              SCOPE
4a4836658592        bridge              bridge              local               
9df19371a9d9        host                host                local               
df978b974708        none                null                local               
c60a3769ec74        overlay-demo        overlay             global 
```

```text
ubuntu@docker-node2:~$ sudo docker network inspect overlay-demo
[
    {
        "Name": "overlay-demo",
        "Id": "c60a3769ec74301345bae3a633ebec0482c1b633bb6b00d1e0c48df3e2427aac",
        "Scope": "global",
        "Driver": "overlay",
        "EnableIPv6": false,
        "IPAM": {
            "Driver": "default",
            "Options": {},
            "Config": [
                {
                    "Subnet": "10.0.0.0/24",
                    "Gateway": "10.0.0.1/24"
                }
            ]
        },
        "Internal": false,
        "Containers": {},
        "Options": {},
        "Labels": {}
    }
]
```

```text
ubuntu@docker-node2:~$ ip route
default via 10.0.2.2 dev enp0s3 
10.0.2.0/24 dev enp0s3  proto kernel  scope link  src 10.0.2.15 
172.17.0.0/16 dev docker0  proto kernel  scope link  src 172.17.0.1 linkdown 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.11 
```

```text
ubuntu@docker-node2:~$ ip link
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: enp0s3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 02:a4:a0:f1:db:6e brd ff:ff:ff:ff:ff:ff
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 08:00:27:7c:fe:ad brd ff:ff:ff:ff:ff:ff
4: docker0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN mode DEFAULT group default 
    link/ether 02:42:92:bc:dd:3b brd ff:ff:ff:ff:ff:ff
```

```text
ubuntu@docker-node2:~$ ip neighbor
10.0.2.3 dev enp0s3 lladdr 52:54:00:12:35:03 STALE
192.168.205.10 dev enp0s8 lladdr 08:00:27:76:c9:85 REACHABLE
10.0.2.2 dev enp0s3 lladdr 52:54:00:12:35:02 REACHABLE
```

### ETCD Configuration

Docker writes configurations to ETCD.

```text
ubuntu@docker-node1:~$ etcdctl get /docker/network/v1.0/network/c60a3769ec74301345bae3a633ebec0482c1b633bb6b00d1e0c48df3e2427aac
{"addrSpace":"GlobalDefault","enableIPv6":false,"generic":{"com.docker.network.enable_ipv6":false,"com.docker.network.generic":{}},"id":"c60a3769ec74301345bae3a633ebec0482c1b633bb6b00d1e0c48df3e2427aac","inDelete":false,"ingress":false,"internal":false,"ipamOptions":{},"ipamType":"default","ipamV4Config":"[{\"PreferredPool\":\"\",\"SubPool\":\"\",\"Gateway\":\"\",\"AuxAddresses\":null}]","ipamV4Info":"[{\"IPAMData\":\"{\\\"AddressSpace\\\":\\\"GlobalDefault\\\",\\\"Gateway\\\":\\\"10.0.0.1/24\\\",\\\"Pool\\\":\\\"10.0.0.0/24\\\"}\",\"PoolID\":\"GlobalDefault/10.0.0.0/24\"}]","labels":{},"name":"overlay-demo","networkType":"overlay","persist":true,"postIPv6":false,"scope":"global"}
```

```text
ubuntu@docker-node1:~$ etcdctl get /docker/network/v1.0/overlay/network/c60a3769ec74301345bae3a633ebec0482c1b633bb6b00d1e0c48df3e2427aac
{"mtu":0,"secure":false,"subnets":[{"SubnetIP":"10.0.0.0/24","GwIP":"10.0.0.1/24","Vni":256}]}
```

## Create Container test1 on docker-node1

We create a container on docker-node1. Let's examine the network configuration changes.

### Host Network Configuration on docker-node1

```text
ubuntu@docker-node1:~$ ip route
default via 10.0.2.2 dev enp0s3 
10.0.2.0/24 dev enp0s3  proto kernel  scope link  src 10.0.2.15 
172.17.0.0/16 dev docker0  proto kernel  scope link  src 172.17.0.1 linkdown 
172.18.0.0/16 dev docker_gwbridge  proto kernel  scope link  src 172.18.0.1 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.10 
```

A new bridge (docker_gwbridge) is created, and subnet 172.18.0.0/16 is assigned to this bridge.

```text
ubuntu@docker-node1:~$ ip link
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: enp0s3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 02:a4:a0:f1:db:6e brd ff:ff:ff:ff:ff:ff
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 08:00:27:76:c9:85 brd ff:ff:ff:ff:ff:ff
4: docker0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN mode DEFAULT group default 
    link/ether 02:42:1c:b7:39:99 brd ff:ff:ff:ff:ff:ff
9: docker_gwbridge: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP mode DEFAULT group default 
    link/ether 02:42:44:98:d0:30 brd ff:ff:ff:ff:ff:ff
11: veth5f5c9f5@if10: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue master docker_gwbridge state UP mode DEFAULT group default 
    link/ether a6:7e:5b:e5:c7:b8 brd ff:ff:ff:ff:ff:ff link-netnsid 1
```

A veth end (veth5f5c9f5) is created in the host's namespace, which is connected to the docker_gwbridge bridge. This means that we can communicate from host to container via this device (172.18.0.0 subnet). 

```text
ubuntu@docker-node1:~$ ip neighbor
10.0.2.2 dev enp0s3 lladdr 52:54:00:12:35:02 DELAY
10.0.2.3 dev enp0s3 lladdr 52:54:00:12:35:03 STALE
172.18.0.2 dev docker_gwbridge lladdr 02:42:ac:12:00:02 STALE
192.168.205.11 dev enp0s8 lladdr 08:00:27:7c:fe:ad REACHABLE
```

### Container Network Configuration on docker-node1

```text
ubuntu@docker-node1:~$ sudo docker network inspect overlay-demo
[
    {
        "Name": "overlay-demo",
        "Id": "c60a3769ec74301345bae3a633ebec0482c1b633bb6b00d1e0c48df3e2427aac",
        "Scope": "global",
        "Driver": "overlay",
        "EnableIPv6": false,
        "IPAM": {
            "Driver": "default",
            "Options": {},
            "Config": [
                {
                    "Subnet": "10.0.0.0/24",
                    "Gateway": "10.0.0.1/24"
                }
            ]
        },
        "Internal": false,
        "Containers": {
            "72194b2de037fc3454ebb1dccb21c91ec0469c504bee263d2a8326bf94a08571": {
                "Name": "test1",
                "EndpointID": "0b97fc2894ea49bb8f655a0598dfdd12454933f156ade2016a601fa8243d9df3",
                "MacAddress": "02:42:0a:00:00:02",
                "IPv4Address": "10.0.0.2/24",
                "IPv6Address": ""
            }
        },
        "Options": {},
        "Labels": {}
    }
]
```

Note that the container's network is actually in 10.0.0.0/24 subnet (container's IP is 10.0.0.2).

This is contradictory to what we just observed in the host's namespace. The veth device is assigned an IP in the 172.18.0.0/16 subnet. 

```text
ubuntu@docker-node1:~$ sudo docker exec test1 ifconfig
eth0      Link encap:Ethernet  HWaddr 02:42:0A:00:00:02  
          inet addr:10.0.0.2  Bcast:0.0.0.0  Mask:255.255.255.0
          inet6 addr: fe80::42:aff:fe00:2/64 Scope:Link
          UP BROADCAST RUNNING MULTICAST  MTU:1450  Metric:1
          RX packets:14 errors:0 dropped:0 overruns:0 frame:0
          TX packets:8 errors:0 dropped:0 overruns:0 carrier:0
          collisions:0 txqueuelen:0 
          RX bytes:1116 (1.0 KiB)  TX bytes:648 (648.0 B)

eth1      Link encap:Ethernet  HWaddr 02:42:AC:12:00:02  
          inet addr:172.18.0.2  Bcast:0.0.0.0  Mask:255.255.0.0
          inet6 addr: fe80::42:acff:fe12:2/64 Scope:Link
          UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
          RX packets:15 errors:0 dropped:0 overruns:0 frame:0
          TX packets:8 errors:0 dropped:0 overruns:0 carrier:0
          collisions:0 txqueuelen:0 
          RX bytes:1218 (1.1 KiB)  TX bytes:648 (648.0 B)

lo        Link encap:Local Loopback  
          inet addr:127.0.0.1  Mask:255.0.0.0
          inet6 addr: ::1/128 Scope:Host
          UP LOOPBACK RUNNING  MTU:65536  Metric:1
          RX packets:0 errors:0 dropped:0 overruns:0 frame:0
          TX packets:0 errors:0 dropped:0 overruns:0 carrier:0
          collisions:0 txqueuelen:1 
          RX bytes:0 (0.0 B)  TX bytes:0 (0.0 B)
```

We find that there are two ethernet devices created in the container. eth0 is in the container's overlay subnet (10.0.0.2), while eth1 is in the local subnet (172.18.0.2).

```text
ubuntu@docker-node1:~$ sudo docker exec test1 ip route
default via 172.18.0.1 dev eth1 
10.0.0.0/24 dev eth0 scope link  src 10.0.0.2 
172.18.0.0/16 dev eth1 scope link  src 172.18.0.2
```

```text
ubuntu@docker-node1:~$ sudo docker exec test1 ip neigh show
172.18.0.1 dev eth1 lladdr 02:42:44:98:d0:30 used 0/0/0 probes 1 STALE
10.0.0.1 dev eth0 lladdr 76:20:6b:fc:a5:19 used 0/0/0 probes 4 STALE
```

```text
ubuntu@docker-node1:~$ sudo docker exec test1 arp -i eth0
? (10.0.0.1) at 76:20:6b:fc:a5:19 [ether]  on eth0
ubuntu@docker-node1:~$ sudo docker exec test1 arp -i eth1
? (172.18.0.1) at 02:42:44:98:d0:30 [ether]  on eth1
```

The ARP table shows that the gateway of the overlay subnet (10.0.0.0/16) is at 10.0.0.1. However, the device is not visible in the host's namespace.

Docker creates another namespace for the overlay network itself. The other end of the container's eth0 is in that namespace.

Because docker does not create the network namespace under /var/run/netns, the usual "ip netns" command cannot show docker's network namespaces.

The following commands links the docker's network namespaces to /var/run/netns, and hence we can use "ip netns" to examine those namespaces.

```text
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ sudo ls /var/run/docker/netns
1-c60a3769ec  bb266b85d325
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ sudo ln -s /var/run/docker/netns/bb266b85d325 /var/run/netns/bb26
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ sudo ln -s /var/run/docker/netns/1-c60a3769ec /var/run/netns/c60a
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ ip netns ls
c60a bb26
```

What is the first namespace (1-c60a3769ec)? Remember the network overlay-demo we created?

```text
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ sudo docker network ls          
c60a3769ec74        overlay-demo        overlay             global
```

This is the namespace of our overlay network.

```text
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ sudo ip netns exec c60a ip link
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: br0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP mode DEFAULT group default 
    link/ether 76:20:6b:fc:a5:19 brd ff:ff:ff:ff:ff:ff
6: vxlan1: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue master br0 state UNKNOWN mode DEFAULT group default 
    link/ether 76:20:6b:fc:a5:19 brd ff:ff:ff:ff:ff:ff link-netnsid 0
8: veth2@if7: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue master br0 state UP mode DEFAULT group default 
    link/ether aa:e4:94:a7:7a:56 brd ff:ff:ff:ff:ff:ff link-netnsid 1
```

Here we find the other end of the container's eth1 device. It is connected to a bridge (br0), which is also in this namespace.

The MAC address of br0 and vxlan1 are same. This indicates that br0 is just a vtep in the vxlan network.

Let's examine the other namespace (bb266b85d325).

```text
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ sudo ip netns exec bb26 ip link
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
7: eth0@if8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP mode DEFAULT group default 
    link/ether 02:42:0a:00:00:02 brd ff:ff:ff:ff:ff:ff link-netnsid 0
10: eth1@if11: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP mode DEFAULT group default 
    link/ether 02:42:ac:12:00:02 brd ff:ff:ff:ff:ff:ff link-netnsid 1
```

This is exactly the same output as we have in the container. It means that bb266b85d325 is the container test1's network namespace.

### Diagram

The following diagram illustrates the configuration.

```text
|-------------------------------------------------------------------------------------------------------------------------------------|
|                                                                                                                                     |
|      |-------------|  IP:  172.18.0.2                                           |-------------|  IP:  10.0.0.2                      |
|      | eth1(veth1) |  MAC: 02:42:ac:12:00:02                                    | eth0(veth1) |  MAC: 02:42:0a:00:00:02             |        
|      |-------------|                                                            |-------------|                                     |
|           | v |                                                                      |   |                                          |
|           | e |                                        container namespace           |   |                                          |
|  ---------| t |----------------------------------------------------------------------|   |----------------------------------------  |
|           | h |       host namespace                            |                    | v |       overlay namespace                  |
|           | 5 |                                                 |                    | e |                                          |
|           | f |                                                 |                    | t |                                          |
|           | 5 |                                                 |                    | h |                                          |
|           | c |       veth2                                     |                    |   |       veth2                              |
|           | 9 |       MAC: a6:7e:5b:e5:c7:b8                    |                    |   |       MAC: aa:e4:94:a7:7a:56             |
|           | f |                                                 |                    |   |                                          |
|           | 5 |                                                 |                    |   |                                          |
|      |---------------------|  IP:  172.18.0.1                   |               |---------------------|  IP:  10.0.0.1           ___|________________ 
|      |   docker_gwbridge   |  MAC: 02:42:44:98:d0:30            |               |         br0         |  MAC: 76:20:6b:fc:a5:19 (___vxlan__tunnel___() 
|      |---------------------|                                    |               |---------------------|                             |
|                                                                 |                                                                   |
|                                                                 |                                                                   |
|      |---------------------|  IP:  192.168.250.10               |                                                                   |
|      |       enp0se8       |  MAC: 08:00:27:38:48:47            |                                                                   |
|      |---------------------|                                    |                                                                   |
|                                                                 |                                                                   |
|                                                                 |                                                                   |
|      IP table:                                                  |               IP table:                                           |
|      172.18.0.0/16 -> docker_gwbridge                           |               10.0.0.0/24 -> br0                                  |
|      192.168.205.0/24 -> enp0s8                                 |                                                                   |
|                                                                 |                                                                   |
|      ARP table:                                                 |               ARP table:                                          |
|      172.18.0.2 <-> 02:42:ac:12:00:02                           |               10.0.0.2 <-> 02:42:0a:00:00:02                      |
|      192.168.205.11 <-> 08:00:27:7c:fe:ad                       |                                                                   |
|                                                                                                                                     |
|-------------------------------------------------------------------------------------------------------------------------------------|
```

## Create Container test2 on docker-node2

After creating a container test2 on the second node, the same configuration is applied to the host and the container.

Note that the IP route to/from container test1 on docker-node1 is not automatically populated in test2. Once we establish the first connection (e.g. ping), the IP table and ARP cache is refreshed. This indicates that docker's libnetwork is not responsible for adding static routes to the container.

