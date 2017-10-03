# Overview

[Kube-proxy](https://kubernetes.io/docs/concepts/services-networking/service/#virtual-ips-and-service-proxies) runs on every node in a kubernetes cluster. It is responsible for implementing a form of virtual IP for Services of type other than ExternalName. It watches the Kubernetes master for the addition and removal of Service and Endpoints objects. For each Service it installs iptables rules which capture traffic to the Service’s clusterIP (which is virtual) and Port and redirects that traffic to one of the Service’s backend sets. For each Endpoints object it installs iptables rules which select a backend Pod.

In this example, we are looking at a two node kubernetes cluster that uses canal for pod overlay networking. We will see iptable changes made by kube-proxy to expose a nginx application as different types of kubernetes service.

Note that the iptables look very similar between master node and worker nodes. Therefore we only display the iptables output of the first worker.

Due to the length of the output, we trimmed the consequential iptables when the application and the services are created. We compare the difference between the iptables with previous state.

# Before Exposing Application as Service

This output contains the initial iptables configuration on k8s-worker1.

```text
ubuntu@k8s-worker1:~$ sudo iptables -nL -t nat
Chain PREROUTING (policy ACCEPT)
target     prot opt source               destination         
KUBE-SERVICES  all  --  0.0.0.0/0            0.0.0.0/0            /* kubernetes service portals */
DOCKER     all  --  0.0.0.0/0            0.0.0.0/0            ADDRTYPE match dst-type LOCAL

Chain INPUT (policy ACCEPT)
target     prot opt source               destination         

Chain OUTPUT (policy ACCEPT)
target     prot opt source               destination         
KUBE-SERVICES  all  --  0.0.0.0/0            0.0.0.0/0            /* kubernetes service portals */
DOCKER     all  --  0.0.0.0/0           !127.0.0.0/8          ADDRTYPE match dst-type LOCAL

Chain POSTROUTING (policy ACCEPT)
target     prot opt source               destination         
KUBE-POSTROUTING  all  --  0.0.0.0/0            0.0.0.0/0            /* kubernetes postrouting rules */
MASQUERADE  all  --  172.17.0.0/16        0.0.0.0/0           
RETURN     all  --  10.244.0.0/16        10.244.0.0/16       
MASQUERADE  all  --  10.244.0.0/16       !224.0.0.0/4         
RETURN     all  -- !10.244.0.0/16        10.244.1.0/24       
MASQUERADE  all  -- !10.244.0.0/16        10.244.0.0/16       

Chain DOCKER (2 references)
target     prot opt source               destination         
RETURN     all  --  0.0.0.0/0            0.0.0.0/0           

Chain KUBE-MARK-DROP (0 references)
target     prot opt source               destination         
MARK       all  --  0.0.0.0/0            0.0.0.0/0            MARK or 0x8000

Chain KUBE-MARK-MASQ (6 references)
target     prot opt source               destination         
MARK       all  --  0.0.0.0/0            0.0.0.0/0            MARK or 0x4000

Chain KUBE-NODEPORTS (1 references)
target     prot opt source               destination         

Chain KUBE-POSTROUTING (1 references)
target     prot opt source               destination         
MASQUERADE  all  --  0.0.0.0/0            0.0.0.0/0            /* kubernetes service traffic requiring SNAT */ mark match 0x4000/0x4000

Chain KUBE-SEP-AJDQ5GXJFICQGLFW (2 references)
target     prot opt source               destination         
KUBE-MARK-MASQ  all  --  192.168.205.10       0.0.0.0/0            /* default/kubernetes:https */
DNAT       tcp  --  0.0.0.0/0            0.0.0.0/0            /* default/kubernetes:https */ recent: SET name: KUBE-SEP-AJDQ5GXJFICQGLFW side: source mask: 255.255.255.255 tcp to:192.168.205.10:6443

Chain KUBE-SEP-H7FN6LU3RSH6CC2T (1 references)
target     prot opt source               destination         
KUBE-MARK-MASQ  all  --  10.244.2.2           0.0.0.0/0            /* kube-system/kube-dns:dns-tcp */
DNAT       tcp  --  0.0.0.0/0            0.0.0.0/0            /* kube-system/kube-dns:dns-tcp */ tcp to:10.244.2.2:53

Chain KUBE-SEP-TCIZBYBD3WWXNWF5 (1 references)
target     prot opt source               destination         
KUBE-MARK-MASQ  all  --  10.244.2.2           0.0.0.0/0            /* kube-system/kube-dns:dns */
DNAT       udp  --  0.0.0.0/0            0.0.0.0/0            /* kube-system/kube-dns:dns */ udp to:10.244.2.2:53

Chain KUBE-SERVICES (2 references)
target     prot opt source               destination         
KUBE-MARK-MASQ  tcp  -- !10.244.0.0/16        10.96.0.1            /* default/kubernetes:https cluster IP */ tcp dpt:443
KUBE-SVC-NPX46M4PTMTKRN6Y  tcp  --  0.0.0.0/0            10.96.0.1            /* default/kubernetes:https cluster IP */ tcp dpt:443
KUBE-MARK-MASQ  udp  -- !10.244.0.0/16        10.96.0.10           /* kube-system/kube-dns:dns cluster IP */ udp dpt:53
KUBE-SVC-TCOU7JCQXEZGVUNU  udp  --  0.0.0.0/0            10.96.0.10           /* kube-system/kube-dns:dns cluster IP */ udp dpt:53
KUBE-MARK-MASQ  tcp  -- !10.244.0.0/16        10.96.0.10           /* kube-system/kube-dns:dns-tcp cluster IP */ tcp dpt:53
KUBE-SVC-ERIFXISQEP7F7OF4  tcp  --  0.0.0.0/0            10.96.0.10           /* kube-system/kube-dns:dns-tcp cluster IP */ tcp dpt:53
KUBE-NODEPORTS  all  --  0.0.0.0/0            0.0.0.0/0            /* kubernetes service nodeports; NOTE: this must be the last rule in this chain */ ADDRTYPE match dst-type LOCAL

Chain KUBE-SVC-ERIFXISQEP7F7OF4 (1 references)
target     prot opt source               destination         
KUBE-SEP-H7FN6LU3RSH6CC2T  all  --  0.0.0.0/0            0.0.0.0/0            /* kube-system/kube-dns:dns-tcp */

Chain KUBE-SVC-NPX46M4PTMTKRN6Y (1 references)
target     prot opt source               destination         
KUBE-SEP-AJDQ5GXJFICQGLFW  all  --  0.0.0.0/0            0.0.0.0/0            /* default/kubernetes:https */ recent: CHECK seconds: 10800 reap name: KUBE-SEP-AJDQ5GXJFICQGLFW side: source mask: 255.255.255.255
KUBE-SEP-AJDQ5GXJFICQGLFW  all  --  0.0.0.0/0            0.0.0.0/0            /* default/kubernetes:https */

Chain KUBE-SVC-TCOU7JCQXEZGVUNU (1 references)
target     prot opt source               destination         
KUBE-SEP-TCIZBYBD3WWXNWF5  all  --  0.0.0.0/0            0.0.0.0/0            /* kube-system/kube-dns:dns */
```

# Create Application (Replication = 2)

After creating an application (with replica = 2), kube-proxy does not make any change to the iptables other than relocating the texts.

```text
ubuntu@k8s-worker1:~$ diff k8s-worker1-init.txt k8s-worker1-pod.txt 
59,60d58
< KUBE-MARK-MASQ  tcp  -- !10.244.0.0/16        10.96.0.10           /* kube-system/kube-dns:dns-tcp cluster IP */ tcp dpt:53
< KUBE-SVC-ERIFXISQEP7F7OF4  tcp  --  0.0.0.0/0            10.96.0.10           /* kube-system/kube-dns:dns-tcp cluster IP */ tcp dpt:53
64a63,64
> KUBE-MARK-MASQ  tcp  -- !10.244.0.0/16        10.96.0.10           /* kube-system/kube-dns:dns-tcp cluster IP */ tcp dpt:53
> KUBE-SVC-ERIFXISQEP7F7OF4  tcp  --  0.0.0.0/0            10.96.0.10           /* kube-system/kube-dns:dns-tcp cluster IP */ tcp dpt:53
```

# Create Service - ClusterIP Type

We first create a ClusterIP type service. The nginx pods are exposed only internally. Kubernetes assigns an internal cluster IP 10.102.69.101 to the service. Note that the port is also 80 for the service because we specified so in the yaml file.

```text
ubuntu@k8s-worker1:~$ diff k8s-worker1-pod.txt k8s-worker1-svc.txt 
31c31
< Chain KUBE-MARK-MASQ (6 references)
---
> Chain KUBE-MARK-MASQ (9 references)
46a47,51
> Chain KUBE-SEP-FUXUN3DWXBWDLOEM (1 references)
> target     prot opt source               destination         
> KUBE-MARK-MASQ  all  --  10.244.2.3           0.0.0.0/0            /* default/nginx-svc:http */
> DNAT       tcp  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */ tcp to:10.244.2.3:80
> 
51a57,61
> Chain KUBE-SEP-KGZD7PWTNPWPM76Z (1 references)
> target     prot opt source               destination         
> KUBE-MARK-MASQ  all  --  10.244.1.2           0.0.0.0/0            /* default/nginx-svc:http */
> DNAT       tcp  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */ tcp to:10.244.1.2:80
> 
64a75,76
> KUBE-MARK-MASQ  tcp  -- !10.244.0.0/16        10.102.69.101        /* default/nginx-svc:http cluster IP */ tcp dpt:80
> KUBE-SVC-ELCM5PCEQWBTUJ2I  tcp  --  0.0.0.0/0            10.102.69.101        /* default/nginx-svc:http cluster IP */ tcp dpt:80
65a78,82
> 
> Chain KUBE-SVC-ELCM5PCEQWBTUJ2I (1 references)
> target     prot opt source               destination         
> KUBE-SEP-KGZD7PWTNPWPM76Z  all  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */ statistic mode random probability 0.50000000000
> KUBE-SEP-FUXUN3DWXBWDLOEM  all  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */
```

The following diagram illustrates the routes:

```text
                                     |---------------------------|
                                     | KUBE-SEP-KGZD7PWTNPWPM76Z |
                                     | DNAT: 10.244.1.2:80       |
|---------------------------|        | Protocol: TCP             |     
| KUBE-SVC-ELCM5PCEQWBTUJ2I | -----> | Probability: 0.5          |
| IP: 10.102.69.101         |        |---------------------------|  
| Protocol: TCP             |        
| Port: 80                  | -----> |---------------------------|
|---------------------------|        | KUBE-SEP-FUXUN3DWXBWDLOEM |
                                     | DNAT: 10.244.2.3:80       |
                                     | Protocol: TCP             |
                                     | Probability: 0.5          |
                                     |---------------------------|                                     
```
                                     
What it means is that when a client accesses 10.102.69.101:80 (internal service endpoint), the traffic is DNATted to one of the two pods, either 10.244.1.2:80 or 10.244.2.3:80. The probability of hitting the first pod is 50%, and hence the second pod 50% as well.

# Create Service - NodePort Type

Then we change the service type to NodePort. Otherthan the internal cluster IP, Kubernetes also exposes the service on each node through a random port. In this example, the port is 31245.

This means that we can:
1. Access the service inside the cluster with http://10.102.69.101:80, where 10.102.69.101 is an internal IP.
2. Access the service outside the cluster with http://<node_ip>:31245, where node_ip is the IP address of any node (either master or worker) in the cluster.

```text
ubuntu@k8s-master:~$ kubectl get service nginx-svc
NAME        TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
nginx-svc   NodePort   10.102.69.101   <none>        80:31245/TCP   28m
```

```text
ubuntu@k8s-worker1:~$ diff k8s-worker1-pod.txt k8s-worker1-svc-nodeport.txt 
31c31
< Chain KUBE-MARK-MASQ (6 references)
---
> Chain KUBE-MARK-MASQ (10 references)
36a37,38
> KUBE-MARK-MASQ  tcp  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */ tcp dpt:31245
> KUBE-SVC-ELCM5PCEQWBTUJ2I  tcp  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */ tcp dpt:31245
46a49,53
> Chain KUBE-SEP-FUXUN3DWXBWDLOEM (1 references)
> target     prot opt source               destination         
> KUBE-MARK-MASQ  all  --  10.244.2.3           0.0.0.0/0            /* default/nginx-svc:http */
> DNAT       tcp  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */ tcp to:10.244.2.3:80
> 
51a59,63
> Chain KUBE-SEP-KGZD7PWTNPWPM76Z (1 references)
> target     prot opt source               destination         
> KUBE-MARK-MASQ  all  --  10.244.1.2           0.0.0.0/0            /* default/nginx-svc:http */
> DNAT       tcp  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */ tcp to:10.244.1.2:80
> 
64a77,78
> KUBE-MARK-MASQ  tcp  -- !10.244.0.0/16        10.102.69.101        /* default/nginx-svc:http cluster IP */ tcp dpt:80
> KUBE-SVC-ELCM5PCEQWBTUJ2I  tcp  --  0.0.0.0/0            10.102.69.101        /* default/nginx-svc:http cluster IP */ tcp dpt:80
65a80,84
> 
> Chain KUBE-SVC-ELCM5PCEQWBTUJ2I (2 references)
> target     prot opt source               destination         
> KUBE-SEP-KGZD7PWTNPWPM76Z  all  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */ statistic mode random probability 0.50000000000
> KUBE-SEP-FUXUN3DWXBWDLOEM  all  --  0.0.0.0/0            0.0.0.0/0            /* default/nginx-svc:http */
```

The following diagram illustrates the routes:

```text
                                                                             |---------------------------|
                                                                             | KUBE-SEP-KGZD7PWTNPWPM76Z |
                                                                             | DNAT: 10.244.1.2:80       |
   |---------------------------|        |---------------------------|        | Protocol: TCP             |     
   | KUBE-NODEPORTS            |        | KUBE-SVC-ELCM5PCEQWBTUJ2I | -----> | Probability: 0.5          |
   | IP: 0.0.0.0/0             |        | IP 10.102.69.101          |        |---------------------------|  
   | Protocol: TCP             | -----> | Protocol: TCP             |        
   | Port: 31245               |        | Port: 80                  | -----> |---------------------------|
   |---------------------------|        |---------------------------|        | KUBE-SEP-FUXUN3DWXBWDLOEM |
                                                                             | DNAT: 10.244.2.3:80       |
                                                                             | Protocol: TCP             |
                                                                             | Probability: 0.5          |
                                                                             |---------------------------|                                     
```