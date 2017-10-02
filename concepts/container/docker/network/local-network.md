This document explains how docker configures the network of the host and the container in a single host scenario.

In this example, we follow [Docker Machine on Localhost](http://docker-k8s-lab.readthedocs.io/en/latest/docker/docker-machine.html) to setup a virtual machine that runs docker locally. 

# Initial State

## Docker Network Configuration

Initially docker creates three networks by default: none, host, and bridge.

```text
ubuntu@docker-node1:~$ sudo docker network list
NETWORK ID          NAME                DRIVER              SCOPE
1a6b8fcfaace        bridge              bridge              local               
75cd689ba0d1        host                host                local               
eb3d2bb371c8        none                null                local               
```

When creating a container, the "bridge" driver initializes the container's network by default. This can be changed by giving "--network=<network_name>" parameter to the docker command.

Let's look at the initial state of the "bridge" network.

```text
ubuntu@docker-node1:~$ sudo docker network inspect bridge
[
    {
        "Name": "bridge",
        "Id": "1a6b8fcfaace504aba2b2339d315550cd87b6fbb4c2cc9f0e784df145a20ca9a",
        "Scope": "local",
        "Driver": "bridge",
        "EnableIPv6": false,
        "IPAM": {
            "Driver": "default",
            "Options": null,
            "Config": [
                {
                    "Subnet": "172.17.0.0/16"
                }
            ]
        },
        "Internal": false,
        "Containers": {},
        "Options": {
            "com.docker.network.bridge.default_bridge": "true",
            "com.docker.network.bridge.enable_icc": "true",
            "com.docker.network.bridge.enable_ip_masquerade": "true",
            "com.docker.network.bridge.host_binding_ipv4": "0.0.0.0",
            "com.docker.network.bridge.name": "docker0",
            "com.docker.network.driver.mtu": "1500"
        },
        "Labels": {}
    }
]
```

## Host Network Configuration

We can use some "ip" commands to inspect the network configuration of the host.

```text
ubuntu@docker-node1:~$ ip route
default via 10.0.2.2 dev enp0s3 
10.0.2.0/24 dev enp0s3  proto kernel  scope link  src 10.0.2.15 
172.17.0.0/16 dev docker0  proto kernel  scope link  src 172.17.0.1 linkdown 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.10
```

```text
ubuntu@docker-node1:~$ ip link
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: enp0s3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 02:d2:3e:0e:ff:c0 brd ff:ff:ff:ff:ff:ff
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 08:00:27:38:48:47 brd ff:ff:ff:ff:ff:ff
4: docker0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN mode DEFAULT group default 
    link/ether 02:42:d2:3b:16:c6 brd ff:ff:ff:ff:ff:ff
```

```text
ubuntu@docker-node1:~$ ip neighbor
10.0.2.3 dev enp0s3 lladdr 52:54:00:12:35:03 STALE
10.0.2.2 dev enp0s3 lladdr 52:54:00:12:35:02 DELAY
```

```text
ubuntu@docker-node1:~$ bridge fdb show br docker0
33:33:00:00:00:01 dev docker0 self permanent
01:00:5e:00:00:01 dev docker0 self permanent
02:42:d2:3b:16:c6 dev docker0 master docker0 permanent
02:42:d2:3b:16:c6 dev docker0 vlan 1 master docker0 permanent
```

The following diagram illustrates the configuration.

```text
|--------------------------------------------------|
|                                                  |
|      |-------------|  IP:  172.17.0.1            |
|      |   docker0   |  MAC: 02:42:d2:3b:16:c6     |        
|      |-------------|                             |
|                                                  |
|                                                  |
|      |-------------|  IP:  192.168.250.10        |
|      |   enp0se8   |  MAC: 08:00:27:38:48:47     |
|      |-------------|                             |
|                                                  |
|--------------------------------------------------|
```

# Create One Container

We create a container test1 on docker-node1. Let's examine the network configurations again.

## Docker Network Configuration

We run the docker command to inspect the "bridge" network.

```text
ubuntu@docker-node1:~$ sudo docker network inspect bridge
[
    {
        "Name": "bridge",
        "Id": "1a6b8fcfaace504aba2b2339d315550cd87b6fbb4c2cc9f0e784df145a20ca9a",
        "Scope": "local",
        "Driver": "bridge",
        "EnableIPv6": false,
        "IPAM": {
            "Driver": "default",
            "Options": null,
            "Config": [
                {
                    "Subnet": "172.17.0.0/16"
                }
            ]
        },
        "Internal": false,
        "Containers": {
            "6e29ca2667503b50b4f2d2f153b0883588ac2622ee99df07db361c441edd6306": {
                "Name": "test1",
                "EndpointID": "8873cd7f27508f4ec35828a8ee0b4ed9c3e8431c36054d1c4758cf326065fc4e",
                "MacAddress": "02:42:ac:11:00:02",
                "IPv4Address": "172.17.0.2/16",
                "IPv6Address": ""
            }
        },
        "Options": {
            "com.docker.network.bridge.default_bridge": "true",
            "com.docker.network.bridge.enable_icc": "true",
            "com.docker.network.bridge.enable_ip_masquerade": "true",
            "com.docker.network.bridge.host_binding_ipv4": "0.0.0.0",
            "com.docker.network.bridge.name": "docker0",
            "com.docker.network.driver.mtu": "1500"
        },
        "Labels": {}
    }
]
```

And we run the docker command to examine the container's network configuration.

```text
ubuntu@docker-node1:~$ sudo docker exec test1 ifconfig
eth0      Link encap:Ethernet  HWaddr 02:42:AC:11:00:02  
          inet addr:172.17.0.2  Bcast:0.0.0.0  Mask:255.255.0.0
          inet6 addr: fe80::42:acff:fe11:2/64 Scope:Link
          UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
          RX packets:10 errors:0 dropped:0 overruns:0 frame:0
          TX packets:8 errors:0 dropped:0 overruns:0 carrier:0
          collisions:0 txqueuelen:0 
          RX bytes:828 (828.0 B)  TX bytes:648 (648.0 B)

lo        Link encap:Local Loopback  
          inet addr:127.0.0.1  Mask:255.0.0.0
          inet6 addr: ::1/128 Scope:Host
          UP LOOPBACK RUNNING  MTU:65536  Metric:1
          RX packets:0 errors:0 dropped:0 overruns:0 frame:0
          TX packets:0 errors:0 dropped:0 overruns:0 carrier:0
          collisions:0 txqueuelen:1 
          RX bytes:0 (0.0 B)  TX bytes:0 (0.0 B)
```

Note that the container's IP (172.17.0.2) is from the "bridge" network's CIDR (172.17.0.0/16).

## Host Network Configuration

We run the same "ip" commands again to examine the host's network configuration.

```text
ubuntu@docker-node1:~$ ip route
default via 10.0.2.2 dev enp0s3 
10.0.2.0/24 dev enp0s3  proto kernel  scope link  src 10.0.2.15 
172.17.0.0/16 dev docker0  proto kernel  scope link  src 172.17.0.1 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.10 
```

```text
ubuntu@docker-node1:~$ ip link
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: enp0s3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 02:d2:3e:0e:ff:c0 brd ff:ff:ff:ff:ff:ff
3: enp0s8: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP mode DEFAULT group default qlen 1000
    link/ether 08:00:27:38:48:47 brd ff:ff:ff:ff:ff:ff
4: docker0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP mode DEFAULT group default 
    link/ether 02:42:d2:3b:16:c6 brd ff:ff:ff:ff:ff:ff
8: veth9a62ffa@if7: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue master docker0 state UP mode DEFAULT group default 
    link/ether de:6d:82:f2:b9:54 brd ff:ff:ff:ff:ff:ff link-netnsid 0
```

```text
ubuntu@docker-node1:~$ ip neighbor
10.0.2.3 dev enp0s3 lladdr 52:54:00:12:35:03 STALE
10.0.2.2 dev enp0s3 lladdr 52:54:00:12:35:02 DELAY
```

```text
ubuntu@docker-node1:~$ bridge fdb show bridge docker0
33:33:00:00:00:01 dev enp0s3 self permanent
01:00:5e:00:00:01 dev enp0s3 self permanent
33:33:ff:0e:ff:c0 dev enp0s3 self permanent
33:33:00:00:00:01 dev enp0s8 self permanent
01:00:5e:00:00:01 dev enp0s8 self permanent
33:33:ff:38:48:47 dev enp0s8 self permanent
33:33:00:00:00:01 dev docker0 self permanent
01:00:5e:00:00:01 dev docker0 self permanent
33:33:ff:3b:16:c6 dev docker0 self permanent
02:42:d2:3b:16:c6 dev docker0 master docker0 permanent
02:42:d2:3b:16:c6 dev docker0 vlan 1 master docker0 permanent
de:6d:82:f2:b9:54 dev veth9a62ffa master docker0 permanent
de:6d:82:f2:b9:54 dev veth9a62ffa vlan 1 master docker0 permanent
02:42:ac:11:00:02 dev veth9a62ffa master docker0 
33:33:00:00:00:01 dev veth9a62ffa self permanent
01:00:5e:00:00:01 dev veth9a62ffa self permanent
33:33:ff:f2:b9:54 dev veth9a62ffa self permanent
```

## What Happened

A veth device "veth9a62ffa" is created. One end of the veth device, veth1, is placed in the container's namespace, shown as the container's "eth0" device. The other end of the veth device, veth2, is placed in the host's namespace, and is connected to "bridge0".

The following diagram illustrates the configuration.

```text
|---------------------------------------------------|
|                                                   |
|      |-------------|  IP:  172.17.0.2             |
|      | eth0(veth1) |  MAC: 02:42:ac:11:00:02      |        
|      |-------------|                              |
|           | v |                                   |
|           | e |         container namespace       |
|           | t |      --------------------------   |
|           | h |            host namespace         |
|           | 9 |                                   |
|           | a |                                   |
|           | 6 |                                   |
|           | 2 |       veth2                       |
|           | f |       MAC: de:6d:82:f2:b9:54      |
|           | f |                                   |
|           | a |                                   |
|      |-------------|  IP:  172.17.0.1             |
|      |   docker0   |  MAC: 02:42:d2:3b:16:c6      |       
|      |-------------|                              |
|                                                   |
|                                                   |
|      |-------------|  IP:  192.168.250.10         |
|      |   enp0se8   |  MAC: 08:00:27:38:48:47      |
|      |-------------|                              |
|                                                   |
|---------------------------------------------------|
```

## How It Works

The docker0 bridge serves as the gateway for the container, as shown in the "ifconfig" output from the container. Therefore the container can send traffic out.

The host's IP table is configured to route all traffic to 172.17.0.0/16 subnet through the docker0 bridge. Therefore the host can send traffic to the container.

However, because the docker0's subnet is not advertised, no traffic from external network can reach the container.

# Reference

1. [Docker libnetwork](https://github.com/docker/libnetwork)
2. [Docker networking deep dive (Youtube)](https://www.youtube.com/watch?v=b3XDl0YsVsg&t=541s)
3. [Setup docker machine](http://docker-k8s-lab.readthedocs.io/en/latest/docker/docker-machine.html)
