package com.sibilante.oci.prometheus.service;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.Datapoint;
import com.oracle.bmc.monitoring.model.FailedMetricRecord;
import com.oracle.bmc.monitoring.model.MetricDataDetails;
import com.oracle.bmc.monitoring.model.PostMetricDataDetails;
import com.oracle.bmc.monitoring.requests.PostMetricDataRequest;
import com.oracle.bmc.monitoring.responses.PostMetricDataResponse;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prometheus.Remote;
import prometheus.Types;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * Based on https://github.com/oracle/oci-java-sdk/blob/master/bmc-examples/src/main/java/MonitoringMetricPostExample.java
 * and https://blogs.oracle.com/developers/post/publishing-and-analyzing-custom-application-metrics-with-the-oracle-cloud-monitoring-service
 */
@Service
public class MonitoringService {

    private final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    private MonitoringClient monitoringClient;
    private final String compartment;

    public MonitoringService() {
        compartment = Optional.ofNullable(System.getenv("COMPARTMENT"))
                .orElseThrow(() -> new IllegalArgumentException("Please defined COMPARTMENT environment variable"));
        try {
            var provider = new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault());

            monitoringClient = MonitoringClient.builder()
                    .endpoint("https://telemetry-ingestion." + provider.getRegion().getRegionId() + ".oraclecloud.com")
                    .build(provider);
        } catch (IOException exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

    public PostMetricDataResponse process(Remote.WriteRequest writeRequest) {
        var metricDataDetailsList = transform(writeRequest);
        if (metricDataDetailsList.isEmpty()) {
            return null;
        } else {
            logger.debug("Sending {} metrics", metricDataDetailsList.size());
            return send(metricDataDetailsList);
        }
    }

    private List<MetricDataDetails> transform(Remote.WriteRequest writeRequest) {
        List<MetricDataDetails> metricDataDetailsList = new ArrayList<>();
        for (Types.TimeSeries timeSeries: writeRequest.getTimeseriesList()) {
            var metricDataDetails = MetricDataDetails.builder()
                    .compartmentId(compartment);

            Map<String, String> dimensions = new HashMap<>();
            for (Types.Label label : timeSeries.getLabelsList()) {
                if (label.getName().equals("job")) {
                    metricDataDetails.namespace(label.getValue()); // Still not sure if is better set as "Resource group"
                } else if (label.getName().equals("__name__")) {
                    metricDataDetails.name(label.getValue());
                } else {
                    dimensions.put(label.getName(), label.getValue());
                }
            }

            List<Datapoint> dataPoints = new ArrayList<>();
            for (Types.Sample sample : timeSeries.getSamplesList()) {
                dataPoints.add(Datapoint.builder()
                        .timestamp(new Date(sample.getTimestamp()))
                        .value(sample.getValue())
                        .build());
            }

            metricDataDetailsList.add(metricDataDetails
                    .dimensions(dimensions)
                    .metadata(null)
                    .datapoints(dataPoints)
                    .build());
        }
        return metricDataDetailsList;
    }

    public PostMetricDataResponse send(List<MetricDataDetails> metricDataDetailsList) {
        var postMetricDataResponse = monitoringClient.postMetricData(PostMetricDataRequest.builder()
                        .postMetricDataDetails(PostMetricDataDetails.builder()
                                .metricData(metricDataDetailsList)
                                .build())
                        .build());
        postMetricDataResponse.getPostMetricDataResponseDetails()
                .getFailedMetrics()
                .stream()
                .map(FailedMetricRecord::toString)
                .forEach(logger::warn);
        return postMetricDataResponse;
    }
}
