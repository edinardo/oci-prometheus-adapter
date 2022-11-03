# OCI Prometheus Adapter

This [remote storage](https://prometheus.io/docs/operating/integrations/#remote-endpoints-and-storage) adapter allows
Prometheus to use [OCI Monitoring](https://docs.oracle.com/en-us/iaas/Content/Monitoring/home.htm) as a long-term store
for time-series metrics. This code is inspired by the [Remote storage adapter](https://github.com/prometheus/prometheus/tree/main/documentation/examples/remote_storage/remote_storage_adapter).

This project is in an initial phase and is used only as a proof of concept. Use it at your own risk.

This adapter currently accepts only [remote write](https://prometheus.io/docs/prometheus/latest/configuration/configuration/#remote_write)
requests, limited to a maximum of [50 samples](https://docs.oracle.com/en-us/iaas/api/#/en/monitoring/20180401/MetricData/PostMetricData) per send.

## Prerequisites

* An OCI user with permission to [Publishing Custom Metrics](https://docs.oracle.com/en-us/iaas/Content/Monitoring/Tasks/publishingcustommetrics.htm).
* The [CLI Configuration File](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdkconfig.htm) set with basic
configuration information, like user credentials and tenancy where the metrics will be sent.
* [JDK](https://jdk.java.net/) version 11 or higher.
* [Protocol Buffer Compiler](https://grpc.io/docs/protoc-installation/). This is necessary to generate the Java source
from the [Prometheus protobuf spec](https://github.com/prometheus/prometheus/tree/main/prompb).
* [Apache Maven](https://maven.apache.org/).

## Building

### Compile

```shell
mvn clean compile
```

### Generate the Java source from protobuf spec (optional in case the source code is needed at IDE level before compilation).

```shell
mvn protobuf:compile
```

## Running

### Running adapter

```shell
export COMPARTMENT=ocid1.compartment...<full OCID of compartment that will receive the metrics>
mvn exec:java
```

#### Optional server parameters

HOSTNAME environment variable defines the address where the adapter will be listening. Default `localhost`

PORT environment variable defines the port where the adapter will be listening.

```shell
export HOSTNAME=0.0.0.0
export PORT=9201
```

## Prometheus Configuration

Add the following to your prometheus.yml:

```yaml
remote_write:
  - url: "http://<ip address>:9201/write"
    queue_config:
      max_samples_per_send: 50
 ```
**Note:** `max_samples_per_send` must be between 1 and 50.