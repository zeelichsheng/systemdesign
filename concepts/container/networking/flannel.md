This document explains how flannel works and how it integrates with container runtime.

# Overview

[Flannel](https://github.com/coreos/flannel) is a daemon running on each node to provide overlay networking to the container cluster. It is responsible to:

1. Adding a vxlan device and bond it with the physical device (vtep).
2. Allocates and manages a subnet CIDR for the containers running on this node.
3. Configuring routing information whenever other nodes join or leave the overlay network.

# Overlay Network: An Example with docker + etcd + flannel

Flannel depends on external data store to store overlay network metadata. Currently it supports ETCD as a key-value store, or using Kubernetes' node information. In this example, we will show how Flannel works with ETCD.

We follow [Docker Machine on Localhost](http://docker-k8s-lab.readthedocs.io/en/latest/docker/docker-machine.html) to setup two virtual machines that run docker locally. Some output are trimmed to save output space.

## Initial Network Configuration

Here we show the initial network configuration of docker-node1. The docker-node2 has a symmetric network configuration as docker-node1.

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

The following diagram illustrates the system. Note that without flannel, containers created on each node can only communicate with other local containers. No inter-node communication is possible. Note the docker0's subnet CIDR is same between the two nodes.

```text
|---------------------------------------------------|              |---------------------------------------------------|
| docker-node1                                      |              | docker-node2                                      |
|                                                   |              |                                                   |
|      |-------------|  IP:  172.17.0.0/16          |              |      |-------------|  IP:  172.17.0.0/16          |
|      |   docker0   |  MAC: 02:42:f3:a3:02:96      |              |      |   docker0   |  MAC: 02:42:d8:8e:06:58      |       
|      |-------------|                              |              |      |-------------|                              |
|                                                   |              |                                                   |
|                                                   |              |                                                   |
|      |-------------|  IP:  192.168.250.10        _|______________|_     |-------------|  IP:  192.168.250.11         |
|      |   enp0se8   |  MAC: 08:00:27:cb:b7:b0    (____phy__link____()    |   enp0se8   |  MAC: 08:00:27:25:31:d8      |
|      |-------------|                              |              |      |-------------|                              |
|                                                   |              |                                                   |
|                                                   |              |                                                   |
|      IP table:                                    |              |      IP table:                                    |
|      172.17.0.0/16 -> docker0                     |              |      172.17.0.0/16 -> docker0                     |
|      192.168.205.0/24 -> enp0s8                   |              |      192.168.205.0/24 -> enp0s8                   |
|                                                   |              |                                                   |
|      ARP table:                                   |              |      ARP table:                                   |
|      192.168.205.11 <-> 08:00:27:25:31:d8         |              |      192.168.205.10 <-> 08:00:27:cb:b7:b0         |
|                                                   |              |                                                   |
|---------------------------------------------------|              |---------------------------------------------------|
```

## Write Overlay Network Metadata to ETCD

We write the following network configuration to ETCD to reserve a 10.0.0.0/8 network. The subnets of this network starts from 10.10.0.0 to 10.99.0.0, where each subnet has a /20 prefix.

```text
ubuntu@docker-node1:~$ etcdctl set /coreos.com/network/config '{"Network": "10.0.0.0/8", "SubnetLen": 20, "SubnetMin": "10.10.0.0","SubnetMax": "10.99.0.0","Backend": {"Type": "vxlan","VNI": 100,"Port": 8472}}'
```

## Start Flannel on docker-node1

We start flannel on docker-node1. Note that we change the docker's default bridge driver (docker0) to use the gateway IP that flannel allocates to docker-node1. In this case, it is 10.15.240.1. We also restart the docker daemon so that it starts using the new subnet range (10.15.240.0/20) for any new container created.

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

Note that several changes are made:

1. Flannel creates a new vxlan device called flannel.100 (100 is the VNI number we configured in the metadata).
2. Flannel binds the vxlan device with the physical device enp0s8. The enp0s8 serves as the vtep of the vxlan.

The following diagram illustrates the system.

```text
|---------------------------------------------------|
| docker-node1                                      |
|                                                   |
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

## Start Flannel on docker-node2

We follow the same step to start flannel on docker-node2. Let's examine the network configuration changes on docker-node2.

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

```text
ubuntu@docker-node2:~$ ip route
10.10.192.0/20 dev docker0  proto kernel  scope link  src 10.10.192.1 linkdown 
10.15.240.0/20 via 10.15.240.0 dev flannel.100 onlink 
192.168.205.0/24 dev enp0s8  proto kernel  scope link  src 192.168.205.11
```

```text
ubuntu@docker-node2:~$ ip neighbor
192.168.205.10 dev enp0s8 lladdr 08:00:27:cb:b7:b0 REACHABLE
10.15.240.0 dev flannel.100 lladdr 5a:13:d0:c0:00:db PERMANENT
```

And let's examine docker-node1's network configuration again.

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

Flannel makes the following changes:

1. A new vxlan device is created on docker-node2.
2. The physical device enp0s8 of docker-node2 is also bound with the new flannel device, and serves as the vtep.
3. Flannel updates the route table and ARP table on each node.

The following diagram illustrates the system.

```text
|---------------------------------------------------|              |---------------------------------------------------|
| docker-node1                                      |              | docker-node2                                      |
|                                                   |              |                                                   |
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

## Start Containers

We start docker containers on both nodes. Note that at this stage, flannel does not involve in configuring the container's network. It is docker that creates the veth device, put one end in the container's network namepsace, and put the other end in the host's network namespace and connect to bridge docker0.

The following diagram illustrates the system.

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

# Source Code Walkthrough

This walkthrough is based on v0.9 release (git commit 317b7d1).

## Overview

Flannel consists of two major parts: subnet and backend.

Subnet module is responsible for IPAM as well as watching for events like node joining and leaving the cluster.

Backend module is responsible for managing the overlay infrastructure (routing information), as well as for container ethernet device, etc.

## Subnet Management

[Subnet Management](https://github.com/coreos/flannel/tree/master/subnet) currently supports two stores for recording subnet information: ETCD and Kubernetes.

The subnet module (subnet.go) defines the Manager interface that includes IPAM operations:


flannel/subnet/subnet.go
---

```go
type Manager interface {
	GetNetworkConfig(ctx context.Context) (*Config, error)
	AcquireLease(ctx context.Context, attrs *LeaseAttrs) (*Lease, error)
	RenewLease(ctx context.Context, lease *Lease) error
	WatchLease(ctx context.Context, sn ip.IP4Net, cursor interface{}) (LeaseWatchResult, error)
	WatchLeases(ctx context.Context, cursor interface{}) (LeaseWatchResult, error)

	Name() string
}
```

And the config.go defines the data structure that contains subnet information:

flannel/subnet/config.go
---

```go
type Config struct {
	Network     ip.IP4Net
	SubnetMin   ip.IP4
	SubnetMax   ip.IP4
	SubnetLen   uint
	BackendType string          `json:"-"`
	Backend     json.RawMessage `json:",omitempty"`
}
```

### ETCD Submodule

The ETCD submodule implements a simple manager (local_manager.go). The manager manages subnet CIDR leases by using information stored in ETCD. The ETCD client is wrapped in the registry.go module, which implements several operations related to flannel's specific requirement.

When the node is up, the manager acquires the subnet CIDR by reading the global network configuration stored in ETCD (needs to be done separately during cluster initialization). Then the manager tries to allocate a subset CIDR from the global network's CIDR. Note that flannel writes the subnet configuration to local file (default /var/run/flannel/subnet.env). So if the node restarts, flannel will try to restore the previous configuration first.

flannel/subnet/etcdv2/local_manager.go
---

```go
func (m *LocalManager) tryAcquireLease(ctx context.Context, config *Config, extIaddr ip.IP4, attrs *LeaseAttrs) (*Lease, error) {
	leases, _, err := m.registry.getSubnets(ctx)
	if err != nil {
		return nil, err
	}

	// Try to reuse a subnet if there's one that matches our IP
	if l := findLeaseByIP(leases, extIaddr); l != nil {
		// Make sure the existing subnet is still within the configured network
		if isSubnetConfigCompat(config, l.Subnet) {
			log.Infof("Found lease (%v) for current IP (%v), reusing", l.Subnet, extIaddr)

			ttl := time.Duration(0)
			if !l.Expiration.IsZero() {
				// Not a reservation
				ttl = subnetTTL
			}
			exp, err := m.registry.updateSubnet(ctx, l.Subnet, attrs, ttl, 0)
			if err != nil {
				return nil, err
			}

			l.Attrs = *attrs
			l.Expiration = exp
			return l, nil
		} else {
			log.Infof("Found lease (%v) for current IP (%v) but not compatible with current config, deleting", l.Subnet, extIaddr)
			if err := m.registry.deleteSubnet(ctx, l.Subnet); err != nil {
				return nil, err
			}
		}
	}

	// no existing match, check if there was a previous subnet to use
	var sn ip.IP4Net
	if !m.previousSubnet.Empty() {
		// use previous subnet
		if l := findLeaseBySubnet(leases, m.previousSubnet); l != nil {
			// Make sure the existing subnet is still within the configured network
			if isSubnetConfigCompat(config, l.Subnet) {
				log.Infof("Found lease (%v) matching previously leased subnet, reusing", l.Subnet)

				ttl := time.Duration(0)
				if !l.Expiration.IsZero() {
					// Not a reservation
					ttl = subnetTTL
				}
				exp, err := m.registry.updateSubnet(ctx, l.Subnet, attrs, ttl, 0)
				if err != nil {
					return nil, err
				}

				l.Attrs = *attrs
				l.Expiration = exp
				return l, nil
			} else {
				log.Infof("Found lease (%v) matching previously leased subnet but not compatible with current config, deleting", l.Subnet)
				if err := m.registry.deleteSubnet(ctx, l.Subnet); err != nil {
					return nil, err
				}
			}
		} else {
			// Check if the previous subnet is a part of the network and of the right subnet length
			if isSubnetConfigCompat(config, m.previousSubnet) {
				log.Infof("Found previously leased subnet (%v), reusing", m.previousSubnet)
				sn = m.previousSubnet
			} else {
				log.Errorf("Found previously leased subnet (%v) that is not compatible with the Etcd network config, ignoring", m.previousSubnet)
			}
		}
	}

	if sn.Empty() {
		// no existing match, grab a new one
		sn, err = m.allocateSubnet(config, leases)
		if err != nil {
			return nil, err
		}
	}

	exp, err := m.registry.createSubnet(ctx, sn, attrs, subnetTTL)
	switch {
	case err == nil:
		log.Infof("Allocated lease (%v) to current node (%v) ", sn, extIaddr)
		return &Lease{
			Subnet:     sn,
			Attrs:      *attrs,
			Expiration: exp,
		}, nil
	case isErrEtcdNodeExist(err):
		return nil, errTryAgain
	default:
		return nil, err
	}
}
```


### Kubernetes Submodule

The Kubernetes submodule's manager does not actively assign CIDR to the node. Instead, it relies on the Kubernetes to properly annotate the node when the node is created. The subnet CIDR is then passed to the backend for further configuration.

When the node is up, the manager acquires the subnet CIDR lease by reading the CIDR (n.Spec.PodCIDR) from Kubernetes and parse it. The returned subnet config is derived from this information.

flannel/subnet/kube/kube.go
---

```go
func (ksm *kubeSubnetManager) AcquireLease(ctx context.Context, attrs *subnet.LeaseAttrs) (*subnet.Lease, error) {
	cachedNode, err := ksm.nodeStore.Get(ksm.nodeName)
	if err != nil {
		return nil, err
	}
	nobj, err := api.Scheme.DeepCopy(cachedNode)
	if err != nil {
		return nil, err
	}
	n := nobj.(*v1.Node)

	if n.Spec.PodCIDR == "" {
		return nil, fmt.Errorf("node %q pod cidr not assigned", ksm.nodeName)
	}
	bd, err := attrs.BackendData.MarshalJSON()
	if err != nil {
		return nil, err
	}
	_, cidr, err := net.ParseCIDR(n.Spec.PodCIDR)
	if err != nil {
		return nil, err
	}
	if n.Annotations[backendDataAnnotation] != string(bd) ||
		n.Annotations[backendTypeAnnotation] != attrs.BackendType ||
		n.Annotations[backendPublicIPAnnotation] != attrs.PublicIP.String() ||
		n.Annotations[subnetKubeManagedAnnotation] != "true" {
		n.Annotations[backendTypeAnnotation] = attrs.BackendType
		n.Annotations[backendDataAnnotation] = string(bd)
		n.Annotations[backendPublicIPAnnotation] = attrs.PublicIP.String()
		n.Annotations[subnetKubeManagedAnnotation] = "true"

		oldData, err := json.Marshal(cachedNode)
		if err != nil {
			return nil, err
		}

		newData, err := json.Marshal(n)
		if err != nil {
			return nil, err
		}

		patchBytes, err := strategicpatch.CreateTwoWayMergePatch(oldData, newData, v1.Node{})
		if err != nil {
			return nil, fmt.Errorf("failed to create patch for node %q: %v", ksm.nodeName, err)
		}

		_, err = ksm.client.CoreV1().Nodes().Patch(ksm.nodeName, types.StrategicMergePatchType, patchBytes, "status")
		if err != nil {
			return nil, err
		}
	}
	return &subnet.Lease{
		Subnet:     ip.FromIPNet(cidr),
		Attrs:      *attrs,
		Expiration: time.Now().Add(24 * time.Hour),
	}, nil
}
```

### Subnet Watch Daemon

The subnet watch daemon (watch.go) waits for subnet lease changes (e.g. node join, node leave, etc.) ETCD class implements the watch function by monitoring the ETCD registry. The Kubernetes class does this by listening to Kubernetes node events (add, update, and delete). 

flannel/subnet/watch.go
---

```go
func WatchLeases(ctx context.Context, sm Manager, ownLease *Lease, receiver chan []Event) {
	lw := &leaseWatcher{
		ownLease: ownLease,
	}
	var cursor interface{}

	for {
		res, err := sm.WatchLeases(ctx, cursor)
		if err != nil {
			if err == context.Canceled || err == context.DeadlineExceeded {
				return
			}

			log.Errorf("Watch subnets: %v", err)
			time.Sleep(time.Second)
			continue
		}

		cursor = res.Cursor

		var batch []Event

		if len(res.Events) > 0 {
			batch = lw.update(res.Events)
		} else {
			batch = lw.reset(res.Snapshot)
		}

		if len(batch) > 0 {
			receiver <- batch
		}
	}
}
```

## Backend Management

The backend module is responsible for configuring the node's routing information by using the subnet configuration acquired from the subnet submodule. It is also responsible for configuring the container's ethernet device. 

Note that the background manager is a background daemon that listens to subnet change from subnet manager. The daemon terminates when the main flannel process completes.

### vxlan Backend

The vxlan backend consists three modules:

1. device.go defines functions to create vxlan link and veth pairs and configure the vxlan device (IP address, MAC address, routing table, and ARP table).
2. vxlan_network.go constantly handles subnet changes and calls the device class to configure the vxlan device.
3. vxlan.go initializes the vxlan module, defined as following:

flannel/backend/vxlan/vxlan.go
---

```go
func (be *VXLANBackend) RegisterNetwork(ctx context.Context, config *subnet.Config) (backend.Network, error) {
	// Parse our configuration
	cfg := struct {
		VNI           int
		Port          int
		GBP           bool
		DirectRouting bool
	}{
		VNI: defaultVNI,
	}

	if len(config.Backend) > 0 {
		if err := json.Unmarshal(config.Backend, &cfg); err != nil {
			return nil, fmt.Errorf("error decoding VXLAN backend config: %v", err)
		}
	}
	log.Infof("VXLAN config: VNI=%d Port=%d GBP=%v DirectRouting=%v", cfg.VNI, cfg.Port, cfg.GBP, cfg.DirectRouting)

	devAttrs := vxlanDeviceAttrs{
		vni:       uint32(cfg.VNI),
		name:      fmt.Sprintf("flannel.%v", cfg.VNI),
		vtepIndex: be.extIface.Iface.Index,
		vtepAddr:  be.extIface.IfaceAddr,
		vtepPort:  cfg.Port,
		gbp:       cfg.GBP,
	}

	dev, err := newVXLANDevice(&devAttrs)
	if err != nil {
		return nil, err
	}
	dev.directRouting = cfg.DirectRouting

	subnetAttrs, err := newSubnetAttrs(be.extIface.ExtAddr, dev.MACAddr())
	if err != nil {
		return nil, err
	}

	lease, err := be.subnetMgr.AcquireLease(ctx, subnetAttrs)
	switch err {
	case nil:
	case context.Canceled, context.DeadlineExceeded:
		return nil, err
	default:
		return nil, fmt.Errorf("failed to acquire lease: %v", err)
	}

	// Ensure that the device has a /32 address so that no broadcast routes are created.
	// This IP is just used as a source address for host to workload traffic (so
	// the return path for the traffic has an address on the flannel network to use as the destination)
	if err := dev.Configure(ip.IP4Net{IP: lease.Subnet.IP, PrefixLen: 32}); err != nil {
		return nil, fmt.Errorf("failed to configure interface %s: %s", dev.link.Attrs().Name, err)
	}

	return newNetwork(be.subnetMgr, be.extIface, dev, ip.IP4Net{}, lease)
}
```

First, newVXLANDevice() is implemented in device.go that creates a new vxlan link. Next, be.subnetMgr.AcquireLease() retrieves the subnet information for this node. Then, dev.Configure() configures the vxlan device with the subnet information. Finally, newNetwork() returns a new network object defined in vxlan_network.

### AWS Backend

The AWS backend configures the route table that is attached to the subnet that the node is in. The backend inserts a route that consists the node's container subnet CIDR. By doing so, other nodes and other pods can send traffic to the pods on the current node.

Note that the AWS backend does not manage the lifecycle and configuration of the ethernet interface of the node. This is done by separate infrastructure orchestration.

flannel/backend/awsvpc/awsvpc.go
---

```go
func (be *AwsVpcBackend) RegisterNetwork(ctx context.Context, config *subnet.Config) (backend.Network, error) {
	// Parse our configuration
	var cfg backendConfig

	if len(config.Backend) > 0 {
		log.Info("Backend configured as: %s", string(config.Backend))
		if err := json.Unmarshal(config.Backend, &cfg); err != nil {
			return nil, fmt.Errorf("error decoding VPC backend config: %v", err)
		}
	}

	// Acquire the lease form subnet manager
	attrs := subnet.LeaseAttrs{
		PublicIP: ip.FromIP(be.extIface.ExtAddr),
	}

	l, err := be.sm.AcquireLease(ctx, &attrs)
	switch err {
	case nil:

	case context.Canceled, context.DeadlineExceeded:
		return nil, err

	default:
		return nil, fmt.Errorf("failed to acquire lease: %v", err)
	}

	sess, _ := session.NewSession(aws.NewConfig().WithMaxRetries(5))

	// Figure out this machine's EC2 instance ID and region
	metadataClient := ec2metadata.New(sess)
	region, err := metadataClient.Region()
	if err != nil {
		return nil, fmt.Errorf("error getting EC2 region name: %v", err)
	}
	sess.Config.Region = aws.String(region)
	instanceID, err := metadataClient.GetMetadata("instance-id")
	if err != nil {
		return nil, fmt.Errorf("error getting EC2 instance ID: %v", err)
	}

	ec2c := ec2.New(sess)

	// Find ENI which contains the external network interface IP address
	eni, err := be.findENI(instanceID, ec2c)
	if err != nil || eni == nil {
		return nil, fmt.Errorf("unable to find ENI that matches the %s IP address. %s\n", be.extIface.IfaceAddr, err)
	}

	// Try to disable SourceDestCheck on the main network interface
	if err := be.disableSrcDestCheck(eni.NetworkInterfaceId, ec2c); err != nil {
		log.Warningf("failed to disable SourceDestCheck on %s: %s.\n", *eni.NetworkInterfaceId, err)
	}

	if !cfg.routeTableConfigured() {
		if cfg.RouteTableID, err = be.detectRouteTableID(eni, ec2c); err != nil {
			return nil, err
		}
		log.Infof("Found route table %s.\n", cfg.RouteTableID)
	}

	networkConfig, err := be.sm.GetNetworkConfig(ctx)
	if err != nil {
		log.Errorf("Error fetching network config: %v", err)
	}

	tables, err := cfg.routeTables()
	if err != nil {
		return nil, err
	}

	for _, routeTableID := range tables {
		err = be.cleanupBlackholeRoutes(routeTableID, networkConfig.Network, ec2c)
		if err != nil {
			log.Errorf("Error cleaning up blackhole routes: %v", err)
		}

		matchingRouteFound, err := be.checkMatchingRoutes(routeTableID, l.Subnet.String(), eni.NetworkInterfaceId, ec2c)
		if err != nil {
			log.Errorf("Error describing route tables: %v", err)
		}

		if !matchingRouteFound {
			cidrBlock := l.Subnet.String()
			deleteRouteInput := &ec2.DeleteRouteInput{RouteTableId: &routeTableID, DestinationCidrBlock: &cidrBlock}
			if _, err := ec2c.DeleteRoute(deleteRouteInput); err != nil {
				if ec2err, ok := err.(awserr.Error); !ok || ec2err.Code() != "InvalidRoute.NotFound" {
					// an error other than the route not already existing occurred
					return nil, fmt.Errorf("error deleting existing route for %s: %v", l.Subnet.String(), err)
				}
			}

			// Add the route for this machine's subnet
			if err := be.createRoute(routeTableID, l.Subnet.String(), eni.NetworkInterfaceId, ec2c); err != nil {
				return nil, fmt.Errorf("unable to add route %s: %v", l.Subnet.String(), err)
			}
		}
	}

	return &backend.SimpleNetwork{
		SubnetLease: l,
		ExtIface:    be.extIface,
	}, nil
}
```
