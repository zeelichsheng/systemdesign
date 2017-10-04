# Overview

[Kube-dns](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/) is an add-on to the cluster that provides service discovery via DNS resolution. The DNS server watches the Kubernetes API for new Services and creates a set of DNS records for each. If DNS has been enabled throughout the cluster then all Pods should be able to do name resolution of Services automatically.

In our [kubeproxy example](https://github.com/zeelichsheng/systemdesign/blob/master/concepts/container/kubernetes/kubeproxy.md), the service can be accessed internally through the cluster IP and port. Because we deployed the kube-dns in the cluster, the service can also be accessed via the DNS name that kube-dns automatically generates.
 
# Kube-dns Configuration

This is the output of the configuration that we used to deploy kube-dns.

```text
I1003 20:04:32.903949       1 dns.go:48] version: 1.14.4-2-g5584e04
I1003 20:04:32.905235       1 server.go:70] Using configuration read from directory: /kube-dns-config with period 10s
I1003 20:04:32.905309       1 server.go:113] FLAG: --alsologtostderr="false"
I1003 20:04:32.905336       1 server.go:113] FLAG: --config-dir="/kube-dns-config"
I1003 20:04:32.905353       1 server.go:113] FLAG: --config-map=""
I1003 20:04:32.905365       1 server.go:113] FLAG: --config-map-namespace="kube-system"
I1003 20:04:32.905380       1 server.go:113] FLAG: --config-period="10s"
I1003 20:04:32.905396       1 server.go:113] FLAG: --dns-bind-address="0.0.0.0"
I1003 20:04:32.905408       1 server.go:113] FLAG: --dns-port="10053"
I1003 20:04:32.905423       1 server.go:113] FLAG: --domain="cluster.local."
I1003 20:04:32.905438       1 server.go:113] FLAG: --federations=""
I1003 20:04:32.905453       1 server.go:113] FLAG: --healthz-port="8081"
I1003 20:04:32.905466       1 server.go:113] FLAG: --initial-sync-timeout="1m0s"
I1003 20:04:32.905482       1 server.go:113] FLAG: --kube-master-url=""
I1003 20:04:32.905496       1 server.go:113] FLAG: --kubecfg-file=""
I1003 20:04:32.905508       1 server.go:113] FLAG: --log-backtrace-at=":0"
I1003 20:04:32.905524       1 server.go:113] FLAG: --log-dir=""
I1003 20:04:32.905537       1 server.go:113] FLAG: --log-flush-frequency="5s"
I1003 20:04:32.905549       1 server.go:113] FLAG: --logtostderr="true"
I1003 20:04:32.905561       1 server.go:113] FLAG: --nameservers=""
I1003 20:04:32.905573       1 server.go:113] FLAG: --stderrthreshold="2"
I1003 20:04:32.905586       1 server.go:113] FLAG: --v="2"
I1003 20:04:32.905598       1 server.go:113] FLAG: --version="false"
I1003 20:04:32.905613       1 server.go:113] FLAG: --vmodule=""
I1003 20:04:32.907319       1 server.go:176] Starting SkyDNS server (0.0.0.0:10053)
I1003 20:04:32.907547       1 server.go:198] Skydns metrics enabled (/metrics:10055)
I1003 20:04:32.907566       1 dns.go:147] Starting endpointsController
I1003 20:04:32.907585       1 dns.go:150] Starting serviceController
I1003 20:04:32.908709       1 logs.go:41] skydns: ready for queries on cluster.local. for tcp://0.0.0.0:10053 [rcache 0]
I1003 20:04:32.908733       1 logs.go:41] skydns: ready for queries on cluster.local. for udp://0.0.0.0:10053 [rcache 0]
I1003 20:04:33.407989       1 dns.go:171] Initialized services and endpoints from apiserver
I1003 20:04:33.408020       1 server.go:129] Setting up Healthz Handler (/readiness)
I1003 20:04:33.408030       1 server.go:134] Setting up cache handler (/cache)
I1003 20:04:33.408036       1 server.go:120] Status HTTP port 8081
```

Note that the base domain name is "cluster.local". The service that we created has a name in this domain.

```bash
ubuntu@k8s-master:~$ curl http://nginx-svc.default.svc.cluster.local
<!DOCTYPE html>
<html>
...
```

By default, kube-dns creates the FQDN of the service in the following format:

```text
<service>.<namespace>.svc.<cluster_domain>
```
