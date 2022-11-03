package com.sibilante.oci.prometheus.endpoint;

import com.sibilante.oci.prometheus.service.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import prometheus.Remote;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Root resource for the OCI Prometheus adapter.
 */
@Path("/")
public class AdapterResource {

    @Inject
    public MonitoringService monitoringService;
    Logger logger = LoggerFactory.getLogger(AdapterResource.class);

    /**
     * Method handling Prometheus remote write requests.
     */
    @POST
    @Path("write")
    public Response write(byte[] input) {
        try {
            monitoringService.process(Remote.WriteRequest
                    .parseFrom(Snappy.uncompress(input)));
            return Response.ok().build();
        } catch (final Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

}
