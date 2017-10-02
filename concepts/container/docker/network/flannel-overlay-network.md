This document explains how docker configures the network of the host and the container in a multi-host scenario. We will examine how flannel provides overlay network support to docker. We use ETCD as the external key-value store.

In this example, we follow [Docker Machine on Localhost](http://docker-k8s-lab.readthedocs.io/en/latest/docker/docker-machine.html) to setup two virtual machines that run docker locally. Some output are trimmed to save output space.

# Initial Network Configuration on docker-node1

```text
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ sudo docker network ls
NETWORK ID          NAME                DRIVER              SCOPE
1782f2dc755b        bridge              bridge              local               
2bc5c4ae8ee3        host                host                local               
977994a4596a        none                null                local 
```    
          
```text
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ sudo docker network inspect bridge
[
    {
        "Name": "bridge",
        "Id": "1782f2dc755b4683c9fa6b2a6ffef67d4f5a62a4559270f76639ce4add3b1a8c",
        "Scope": "local",
        "Driver": "bridge",
        "IPAM": {
            "Driver": "default",
            "Options": null,
            "Config": [
                {
                    "Subnet": "172.17.0.0/16"
                }
            ]
        },
    }
]
```

```text
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ ip route
172.17.0.0/16 dev docker0  proto kernel  scope link  src 172.17.0.1 linkdown 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.10 
```

```text
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ ip link
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 08:00:27:cb:b7:b0 brd ff:ff:ff:ff:ff:ff
4: docker0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN mode DEFAULT group default 
    link/ether 02:42:f3:a3:02:96 brd ff:ff:ff:ff:ff:ff
```

```text
ubuntu@docker-node1:~/etcd-v3.2.8-linux-amd64$ ip neighbor
192.168.205.11 dev enp0s8 lladdr 08:00:27:25:31:d8 REACHABLE
```

The docker-node2 machine has a symmetric network configuration as docker-node1.

# Setup flannel and Configure ETCD

First we write the network configuration to ETCD to reserve a 10.0.0.0/8 network. The subnets of this network starts from 10.10.0.0 to 10.99.0.0, where each subnet has a /20 prefix.

```text
ubuntu@docker-node1:~$ etcdctl set /coreos.com/network/config '{"Network": "10.0.0.0/8", "SubnetLen": 20, "SubnetMin": "10.10.0.0","SubnetMax": "10.99.0.0","Backend": {"Type": "vxlan","VNI": 100,"Port": 8472}}'
```

Then we start flannel on docker-node1. Note that we need to change the docker's default bridge driver (docker0) to use gateway IP that flannel allocates to docker-node1. In this case, it is 10.15.240.1. We also need to restart the docker daemon so that it starts using the new subnet range (10.15.240.0/20) for any new container created.

Let's examine the network configuration of docker-node1.

```text
ubuntu@docker-node1:~$ sudo docker network inspect bridge
[
    {
        "Name": "bridge",
        "Id": "3e514939271ed4e5ce4d79d2a85cc373d958226a80c6f1618e32263f7b365376",
        "Scope": "local",
        "Driver": "bridge",
        "IPAM": {
            "Driver": "default",
            "Options": null,
            "Config": [
                {
                    "Subnet": "10.15.240.1/20",
                    "Gateway": "10.15.240.1"
                }
            ]
        },
    }
]
```

```text
ubuntu@docker-node1:~$ ip link
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 08:00:27:cb:b7:b0 brd ff:ff:ff:ff:ff:ff
4: docker0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN mode DEFAULT group default 
    link/ether 02:42:f3:a3:02:96 brd ff:ff:ff:ff:ff:ff
5: flannel.100: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UNKNOWN mode DEFAULT group default 
    link/ether 5a:13:d0:c0:00:db brd ff:ff:ff:ff:ff:ff
```

```text
ubuntu@docker-node1:~$ ip route
10.15.240.0/20 dev docker0  proto kernel  scope link  src 10.15.240.1 linkdown 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.10 
```

```text
ubuntu@docker-node1:~$ ip neighbor
192.168.205.11 dev enp0s8 lladdr 08:00:27:25:31:d8 REACHABLE
```

Note that a new ethernet device (flannel.100) is created.

# Create Container on docker-node1

Next we create a container (test1) on docker-node1. After the container is created, we examine the changes of the network configuration.

```text
ubuntu@docker-node1:~$ sudo docker network inspect bridge
[
    {
        "Name": "bridge",
        "Id": "3e514939271ed4e5ce4d79d2a85cc373d958226a80c6f1618e32263f7b365376",
        "Scope": "local",
        "Driver": "bridge",
        "IPAM": {
            "Driver": "default",
            "Options": null,
            "Config": [
                {
                    "Subnet": "10.15.240.1/20",
                    "Gateway": "10.15.240.1"
                }
            ]
        },
        "Containers": {
            "93b54e4ae0ea293244fcf6608c8e393f2a4092f9a675d1604fec0783ea971b25": {
                "Name": "test1",
                "EndpointID": "6aaf9a10e534f1bd22898109b7eb862baa29d94d18a4a2edc8209fb917b77154",
                "MacAddress": "02:42:0a:0f:f0:02",
                "IPv4Address": "10.15.240.2/20",
                "IPv6Address": ""
            }
        },
    }
]
```

```text
ubuntu@docker-node1:~$ ip link
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 08:00:27:cb:b7:b0 brd ff:ff:ff:ff:ff:ff
4: docker0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP mode DEFAULT group default 
    link/ether 02:42:f3:a3:02:96 brd ff:ff:ff:ff:ff:ff
5: flannel.100: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UNKNOWN mode DEFAULT group default 
    link/ether 5a:13:d0:c0:00:db brd ff:ff:ff:ff:ff:ff
7: vetha5a04ff@if6: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue master docker0 state UP mode DEFAULT group default 
    link/ether b6:4c:92:de:88:1f brd ff:ff:ff:ff:ff:ff link-netnsid 0
```

Note that a new veth endpoint is created in the host's namespace.

```text
ubuntu@docker-node1:~$ ip route
10.15.240.0/20 dev docker0  proto kernel  scope link  src 10.15.240.1 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.10 
```

```text
ubuntu@docker-node1:~$ ip neighbor
192.168.205.11 dev enp0s8 lladdr 08:00:27:25:31:d8 REACHABLE
```

No change in the host's routing table and ARP table. Let's examine the container's network configuration.

```text
ubuntu@docker-node1:~$ sudo docker inspect 93b5
[
    {
        "NetworkSettings": {
            "SandboxID": "4965ebdf2a36833a66b2e552fe27b2eea82961f3a7491dba1691a33bee570d9c",
            "EndpointID": "6aaf9a10e534f1bd22898109b7eb862baa29d94d18a4a2edc8209fb917b77154",
            "Gateway": "10.15.240.1",
            "IPAddress": "10.15.240.2",
            "IPPrefixLen": 20,
            "MacAddress": "02:42:0a:0f:f0:02",
            "Networks": {
                "bridge": {
                    "NetworkID": "3e514939271ed4e5ce4d79d2a85cc373d958226a80c6f1618e32263f7b365376",
                    "EndpointID": "6aaf9a10e534f1bd22898109b7eb862baa29d94d18a4a2edc8209fb917b77154",
                    "Gateway": "10.15.240.1",
                    "IPAddress": "10.15.240.2",
                    "IPPrefixLen": 20,
                    "MacAddress": "02:42:0a:0f:f0:02"
                }
            }
        }
    }
]
```

```text
ubuntu@docker-node1:~$ sudo docker exec test1 ip link
6: eth0@if7: <BROADCAST,MULTICAST,UP,LOWER_UP,M-DOWN> mtu 1450 qdisc noqueue 
    link/ether 02:42:0a:0f:f0:02 brd ff:ff:ff:ff:ff:ff
```

```text
ubuntu@docker-node1:~$ sudo docker exec test1 ip route
default via 10.15.240.1 dev eth0
10.15.240.0/20 dev eth0 scope link  src 10.15.240.2 
```

# What Happened

1. flannel created a flannel.100 vxlan device in the host's namepsace.
2. flannel created a veth device. One end (vetha5a04ff) is placed in the host's namespace and connected to docker0. The other end (eth0) is placed in the container's namespace.

The following diagram illustrates the network configuration.

```text
|---------------------------------------------------|
|                                                   |
|      |-------------|  IP:  10.15.240.2            |
|      | eth0(veth1) |  MAC: 02:42:0a:0f:f0:02      |        
|      |-------------|                              |
|           | v |                                   |
|           | e |         container namespace       |
|           | t |      --------------------------   |
|           | h |            host namespace         |
|           | a |                                   |
|           | 5 |                                   |
|           | a |                                   |
|           | 0 |       veth2                       |
|           | 4 |       MAC: b6:4c:92:de:88:1f      |
|           | f |                                   |
|           | f |                                   |
|      |-------------|  IP:  10.15.240.1            |
|      |   docker0   |  MAC: 02:42:f3:a3:02:96      |       
|      |-------------|                              |
|                                                   |
|                                                   |
|      |-------------|  IP: 10.15.240.0             |
|      | flannel.100 |  MAC: 5a:13:d0:c0:00:db      |
|      |-------------|                              |
|                                                   |
|                                                   |
|      |-------------|  IP:  192.168.250.10         |
|      |   enp0se8   |  MAC: 08:00:27:cb:b7:b0      |
|      |-------------|                              |
|                                                   |
|                                                   |
|      IP table:                                    |
|      10.15.240.0/20 -> docker0                    |
|      192.168.205.0/24 -> enp0s8                   |
|                                                   |
|      ARP table:                                   |
|      192.168.205.11 <-> 08:00:27:25:31:d8         |
|                                                   |
|---------------------------------------------------|
```

# Start flannel on docker-node2

We follow the same process to start flannel and restart docker on docker-node2. Then we create the same container on docker-node2. Let's examine the network configuration of docker-node2.

## Network Configuration on docker-node2

```text
ubuntu@docker-node2:~$ sudo docker network inspect bridge
[
    {
        "Name": "bridge",
        "Id": "9a5df9748b64c260c5ed5dd512213ce77bee4338425024bac09a8b713d8e16ce",
        "Scope": "local",
        "Driver": "bridge",
        "IPAM": {
            "Driver": "default",
            "Options": null,
            "Config": [
                {
                    "Subnet": "10.10.192.1/20",
                    "Gateway": "10.10.192.1"
                }
            ]
        },
    }
]
```

```text
ubuntu@docker-node2:~$ sudo docker exec test2 ip link
8: eth0@if9: <BROADCAST,MULTICAST,UP,LOWER_UP,M-DOWN> mtu 1450 qdisc noqueue 
    link/ether 02:42:0a:0a:c0:02 brd ff:ff:ff:ff:ff:ff
```

```text
ubuntu@docker-node2:~$ sudo docker exec test2 ip route
default via 10.10.192.1 dev eth0 
10.10.192.0/20 dev eth0 scope link  src 10.10.192.2
```

```text
ubuntu@docker-node2:~$ ip link
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 08:00:27:25:31:d8 brd ff:ff:ff:ff:ff:ff
4: docker0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN mode DEFAULT group default 
    link/ether 02:42:d8:8e:06:58 brd ff:ff:ff:ff:ff:ff
7: flannel.100: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UNKNOWN mode DEFAULT group default 
    link/ether e2:07:73:97:b4:0a brd ff:ff:ff:ff:ff:ff
```

Similarly, a flannel.100 device is created on docker-node2.

```text
ubuntu@docker-node2:~$ ip route
10.10.192.0/20 dev docker0  proto kernel  scope link  src 10.10.192.1 linkdown 
10.15.240.0/20 via 10.15.240.0 dev flannel.100 onlink 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.11
```

Note that not only the local container subnet's route is added to the host's route table, but also a route to the docker-node1's container subnet is added.

```text
ubuntu@docker-node2:~$ ip neighbor
192.168.205.10 dev enp0s8 lladdr 08:00:27:cb:b7:b0 REACHABLE
10.15.240.0 dev flannel.100 lladdr 5a:13:d0:c0:00:db PERMANENT
```

Similarly, an ARP entry is added to the ARP table. The entry contains the MAC address of the device on docker-node1, which has access to the docker-node1's container subnet.

## Network Configuration on docker-node1

Let's look at docker-node1's network configuration again.

```text
ubuntu@docker-node1:~$ ip route
10.10.192.0/20 via 10.10.192.0 dev flannel.100 onlink 
10.15.240.0/20 dev docker0  proto kernel  scope link  src 10.15.240.1 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.10
```

```text
ubuntu@docker-node1:~$ ip neighbor
192.168.205.11 dev enp0s8 lladdr 08:00:27:25:31:d8 REACHABLE
10.10.192.0 dev flannel.100 lladdr e2:07:73:97:b4:0a PERMANENT
```

As expected, docker-node1's route table and ARP table are also updated with information that points to docker-node2.

# What Happened

1. When a new node joins the flannel's overlay network, flannel automatically updates the route table and ARP table of the new node as well as the other existing nodes in the overlay network, such that they can communicate with each other via their corresponding flannel.100 devices.
2. The information is stored in ETCD under /coreos.com/network

The following diagram illustrates the network configuration of both nodes.

```text
|---------------------------------------------------|              |---------------------------------------------------|
| docker-node1                                      |              | docker-node2                                      |
|                                                   |              |                                                   |
|      |-------------|  IP:  10.15.240.2            |              |      |-------------|  IP:  10.10.192.2            |
|      | eth0(veth1) |  MAC: 02:42:0a:0f:f0:02      |              |      | eth0(veth1) |  MAC: 02:42:0a:0a:c0:02      |  
|      |-------------|                              |              |      |-------------|                              |
|           | v |                                   |              |           | v |                                   |
|           | e |         container namespace       |              |           | e |         container namespace       |
|      -----| t |--------------------------------   |              |      -----| t |--------------------------------   |
|           | h |            host namespace         |              |           | h |            host namespace         |
|           | a |                                   |              |           | e |                                   |
|           | 5 |                                   |              |           | f |                                   |
|           | a |                                   |              |           | 4 |                                   |
|           | 0 |       veth2                       |              |           | 2 |       veth2                       |
|           | 4 |       MAC: b6:4c:92:de:88:1f      |              |           | a |       MAC: 2a:ba:cd:27:c5:e5      |
|           | f |                                   |              |           | 2 |                                   |
|           | f |                                   |              |           | 5 |                                   |
|      |-------------|  IP:  10.15.240.1            |              |      |-------------|  IP:  10.10.192.1            |
|      |   docker0   |  MAC: 02:42:f3:a3:02:96      |              |      |   docker0   |  MAC: 02:42:d8:8e:06:58      |       
|      |-------------|                              |              |      |-------------|                              |
|                                                   |              |                                                   |
|                                                   |              |                                                   |
|      |-------------|  IP: 10.15.240.0           __|______________|__    |-------------|  IP: 10.10.192.0             |
|      | flannel.100 |  MAC: 5a:13:d0:c0:00:db   (____vxlan tunnel___()   | flannel.100 |  MAC: e2:07:73:97:b4:0a      |
|      |-------------|                              |              |      |-------------|                              |
|                                                   |              |                                                   |
|                                                   |              |                                                   |
|      |-------------|  IP:  192.168.250.10         |              |      |-------------|  IP:  192.168.250.11         |
|      |   enp0se8   |  MAC: 08:00:27:cb:b7:b0      |              |      |   enp0se8   |  MAC: 08:00:27:25:31:d8      |
|      |-------------|                              |              |      |-------------|                              |
|                                                   |              |                                                   |
|                                                   |              |                                                   |
|      IP table:                                    |              |      IP table:                                    |
|      10.15.240.0/20 -> docker0                    |              |      10.10.192.0/20 -> docker0                    |
|      10.10.192.0/20 -> flannel.100                |              |      10.15.240.0/20 -> flannel.100                |
|      192.168.205.0/24 -> enp0s8                   |              |      192.168.205.0/24 -> enp0s8                   |
|                                                   |              |                                                   |
|      ARP table:                                   |              |      ARP table:                                   |
|      10.10.192.0 <-> e2:07:73:97:b4:0a            |              |      10.15.240.0 <-> 5a:13:d0:c0:00:db            |
|      192.168.205.11 <-> 08:00:27:25:31:d8         |              |      192.168.205.10 <-> 08:00:27:cb:b7:b0         |
|                                                   |              |                                                   |
|---------------------------------------------------|              |---------------------------------------------------|
```

# Flannel vs Docker Native Overlay Network

1. Both flannel and docker native overlay network rely on external key-value store. In this case we use ETCD.
2. Docker native overlay network requires the creation of a new overlay network. The new overlay network creates a new network namespace, which creates a bridge device. The container's ethernet is directly connected to the overlay's secret bridge.
3. In order for the host to reach the container, a second ethernet device is created in the container, and connected to the default docker bridge.
4. In contrary, flannel piggybacks an existing docker bridge (or we can create a new docker bridge network). It creates a flannel device in the host's namespace.
5. The new flannel device in the host's namespace does not connect to the container directly. The container's ethernet device still connects to the docker bridge.
6. Therefore, host has direct access to the container using the container's subnet IP, which is allocated by flannel.
7. Flannel needs to manually update the route table and ARP table of the host such that the host knows how to reach the peer's flannel device and container subnet.