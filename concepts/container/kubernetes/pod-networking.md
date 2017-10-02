# Overview

Kubernetes defines a [network plug-in](https://kubernetes.io/docs/concepts/cluster-administration/network-plugins/) interface that wraps the CNI interface. The kubelet command uses the plug-in.

# Network Plug-in Interface

Kubernetes defines the following interface that is consumed by kubelet:

kubernetes/pkg/kubelet/network/plugin.go
---

```go
// Plugin is an interface to network plugins for the kubelet
type NetworkPlugin interface {
	// Init initializes the plugin.  This will be called exactly once
	// before any other methods are called.
	Init(host Host, hairpinMode kubeletconfig.HairpinMode, nonMasqueradeCIDR string, mtu int) error

	// Called on various events like:
	// NET_PLUGIN_EVENT_POD_CIDR_CHANGE
	Event(name string, details map[string]interface{})

	// Name returns the plugin's name. This will be used when searching
	// for a plugin by name, e.g.
	Name() string

	// Returns a set of NET_PLUGIN_CAPABILITY_*
	Capabilities() utilsets.Int

	// SetUpPod is the method called after the infra container of
	// the pod has been created but before the other containers of the
	// pod are launched.
	SetUpPod(namespace string, name string, podSandboxID kubecontainer.ContainerID, annotations map[string]string) error

	// TearDownPod is the method called before a pod's infra container will be deleted
	TearDownPod(namespace string, name string, podSandboxID kubecontainer.ContainerID) error

	// GetPodNetworkStatus is the method called to obtain the ipv4 or ipv6 addresses of the container
	GetPodNetworkStatus(namespace string, name string, podSandboxID kubecontainer.ContainerID) (*PodNetworkStatus, error)

	// Status returns error if the network plugin is in error state
	Status() error
}
```

When kubelet starts, several parameters control the selection and configuration of the plugin:

kubernetes/cmd/kubelet/app/options/container_runtime.go
---

```go
func (s *ContainerRuntimeOptions) AddFlags(fs *pflag.FlagSet) {
    ...

	// Network plugin settings. Shared by both docker and rkt.
	fs.StringVar(&s.NetworkPluginName, "network-plugin", s.NetworkPluginName, "<Warning: Alpha feature> The name of the network plugin to be invoked for various events in kubelet/pod lifecycle")
	//TODO(#46410): Remove the network-plugin-dir flag.
	fs.StringVar(&s.NetworkPluginDir, "network-plugin-dir", s.NetworkPluginDir, "<Warning: Alpha feature> The full path of the directory in which to search for network plugins or CNI config")
	fs.MarkDeprecated("network-plugin-dir", "Use --cni-bin-dir instead. This flag will be removed in a future version.")
	fs.StringVar(&s.CNIConfDir, "cni-conf-dir", s.CNIConfDir, "<Warning: Alpha feature> The full path of the directory in which to search for CNI config files. Default: /etc/cni/net.d")
	fs.StringVar(&s.CNIBinDir, "cni-bin-dir", s.CNIBinDir, "<Warning: Alpha feature> The full path of the directory in which to search for CNI plugin binaries. Default: /opt/cni/bin")
	fs.Int32Var(&s.NetworkPluginMTU, "network-plugin-mtu", s.NetworkPluginMTU, "<Warning: Alpha feature> The MTU to be passed to the network plugin, to override the default. Set to 0 to use the default 1460 MTU.")
	...
}
```

## Kubenet Plug-in

Kubenet is a very basic, simple network plugin, on Linux only. It does not, of itself, implement more advanced features. Instead, it depends on three other CNI plug-ins: bridge, host-local, and loopback.

Kubenet creates a Linux bridge named cbr0 and creates a veth pair for each pod with the host end of each pair connected to cbr0. The pod end of the pair is assigned an IP address allocated from a range assigned to the node either through configuration or by the controller-manager. cbr0 is assigned an MTU matching the smallest MTU of an enabled normal interface on the host. The plugin requires a few things:

    - The standard CNI bridge, lo and host-local plugins are required, at minimum version 0.2.0. Kubenet will first search for them in /opt/cni/bin. Specify network-plugin-dir to supply additional search path. The first found match will take effect.
    - Kubelet must be run with the --network-plugin=kubenet argument to enable the plugin
    - Kubelet should also be run with the --non-masquerade-cidr=<clusterCidr> argument to ensure traffic to IPs outside this range will use IP masquerade.
    - The node must be assigned an IP subnet through either the --pod-cidr kubelet command-line option or the --allocate-node-cidrs=true --cluster-cidr=<cidr> controller-manager command-line options.

kubernetes/pkg/kubelet/network/kubnet/kubenet_linux.go
---

When invoking the CNI plug-ins to setup container network, kubenet uses the a pre-defined static CNI configuration template:

```go
const NET_CONFIG_TEMPLATE = `{
  "cniVersion": "0.1.0",
  "name": "kubenet",
  "type": "bridge",
  "bridge": "%s",
  "mtu": %d,
  "addIf": "%s",
  "isGateway": true,
  "ipMasq": false,
  "hairpinMode": %t,
  "ipam": {
    "type": "host-local",
    "subnet": "%s",
    "gateway": "%s",
    "routes": [
      { "dst": "0.0.0.0/0" }
    ]
  }
}`
```

This JSON template is filled when an pod CIDR is allocated or updated (e.g. when docker starts, it can either be docker's default CIDR, or through --bip argument):

```go
func (plugin *kubenetNetworkPlugin) Event(name string, details map[string]interface{}) {
	...

	podCIDR, ok := details[network.NET_PLUGIN_EVENT_POD_CIDR_CHANGE_DETAIL_CIDR].(string)
	...

	_, cidr, err := net.ParseCIDR(podCIDR)
	if err == nil {
		...
		// Set bridge address to first address in IPNet
		cidr.IP[len(cidr.IP)-1] += 1

		json := fmt.Sprintf(NET_CONFIG_TEMPLATE, BridgeName, plugin.mtu, network.DefaultInterfaceName, setHairpin, podCIDR, cidr.IP.String())
		...
		plugin.netConfig, err = libcni.ConfFromBytes([]byte(json))
		...
		plugin.podCidr = podCIDR
		plugin.gateway = cidr.IP
	}

	if err != nil {
		glog.Warningf("Failed to generate CNI network config: %v", err)
	}
}
```

When a pod is created, kubenet.SetUpPod() is invoked to setup the container network.

```go
func (plugin *kubenetNetworkPlugin) SetUpPod(namespace string, name string, id kubecontainer.ContainerID, annotations map[string]string) error {
    ...
    
	// TODO: Entire pod object only required for bw shaping and hostport.
	pod, ok := plugin.host.GetPodByName(namespace, name)
	...

	if err := plugin.setup(namespace, name, id, pod, annotations); err != nil {
		// Make sure everything gets cleaned up on errors
		podIP, _ := plugin.podIPs[id]
		if err := plugin.teardown(namespace, name, id, podIP); err != nil {
			// Not a hard error or warning
			glog.V(4).Infof("Failed to clean up %s/%s after SetUpPod failure: %v", namespace, name, err)
		}
		...
	}
    ...
}
```

```go
func (plugin *kubenetNetworkPlugin) setup(namespace string, name string, id kubecontainer.ContainerID, pod *v1.Pod, annotations map[string]string) error {
	// Bring up container loopback interface
	if _, err := plugin.addContainerToNetwork(plugin.loConfig, "lo", namespace, name, id); err != nil {
		return err
	}

	// Hook container up with our bridge
	resT, err := plugin.addContainerToNetwork(plugin.netConfig, network.DefaultInterfaceName, namespace, name, id)
	...
}
```

```go
func (plugin *kubenetNetworkPlugin) addContainerToNetwork(config *libcni.NetworkConfig, ifName, namespace, name string, id kubecontainer.ContainerID) (cnitypes.Result, error) {
	rt, err := plugin.buildCNIRuntimeConf(ifName, id, true)
	...

	glog.V(3).Infof("Adding %s/%s to '%s' with CNI '%s' plugin and runtime: %+v", namespace, name, config.Network.Name, config.Network.Type, rt)
	res, err := plugin.cniConfig.AddNetwork(config, rt)
	...
}
```

Note that plugin.cniConfig is libcni, which is discussed in detail [here](https://github.com/zeelichsheng/systemdesign/blob/master/concepts/container/networking/cni.md)

## CNI Plug-in

The CNI plug-in is selected by passing Kubelet the --network-plugin=cni command-line option. Kubelet reads a file from --cni-conf-dir (default /etc/cni/net.d) and uses the CNI configuration from that file to set up each pod’s network. The CNI configuration file must match the CNI specification, and any required CNI plugins referenced by the configuration must be present in --cni-bin-dir (default /opt/cni/bin).

The CNI plug-in is a thin adapter layer that abstracts the networking operations for kubelet.

kubernetes/pkg/kubelet/network/cni/cni.go
---

```go
func (plugin *cniNetworkPlugin) SetUpPod(namespace string, name string, id kubecontainer.ContainerID, annotations map[string]string) error {
	...
	netnsPath, err := plugin.host.GetNetNS(id.ID)
	...

	_, err = plugin.addToNetwork(plugin.loNetwork, name, namespace, id, netnsPath)
	...

	_, err = plugin.addToNetwork(plugin.getDefaultNetwork(), name, namespace, id, netnsPath)
	...
}
```

```go
func (plugin *cniNetworkPlugin) addToNetwork(network *cniNetwork, podName string, podNamespace string, podSandboxID kubecontainer.ContainerID, podNetnsPath string) (cnitypes.Result, error) {
	rt, err := plugin.buildCNIRuntimeConf(podName, podNamespace, podSandboxID, podNetnsPath)
	...

	netConf, cniNet := network.NetworkConfig, network.CNIConfig
	...
	res, err := cniNet.AddNetworkList(netConf, rt)
	...
}
```

Where cniNet is also libcni.