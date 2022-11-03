package com.sibilante.oci.prometheus;

import com.sibilante.oci.prometheus.service.MonitoringService;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI;
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        // Bridging Grizzly logs from JUL to SLF4J.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        var host = Optional.ofNullable(System.getenv("HOSTNAME"));
        var port = Optional.ofNullable(System.getenv("PORT"));
        BASE_URI = "http://" + host.orElse("localhost") + ":" + port.orElse("9201") + "/";
    }

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // Creating a resource config that scans for JAX-RS resources and providers
        // in com.oracle.oci.prometheus.endpoint package
        final var rc = new ResourceConfig().packages("com.oracle.oci.prometheus.endpoint");
        rc.register(new AbstractBinder(){
            @Override
            protected void configure() {
                bindAsContract(MonitoringService.class).in(Singleton.class);
            }
        });
        rc.property("jersey.config.server.wadl.disableWadl", "true");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws IOException {
        final var server = startServer();
        logger.info("Hit Ctrl-C to stop it...");
        System.in.read();
        server.shutdown();
    }

}

