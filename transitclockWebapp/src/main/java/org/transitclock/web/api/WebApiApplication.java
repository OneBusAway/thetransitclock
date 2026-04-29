package org.transitclock.web.api;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Webapp-internal JSON layer mounted at /api/*. Distinct from
 * transitclockApi's /v1/* surface: this layer is for endpoints that talk
 * to the DB directly (e.g. db-disk-space) rather than to a Core JVM via
 * RMI. JAX-RS resource classes live in this package and are discovered
 * by Jersey's package scan.
 */
@ApplicationPath("api")
public class WebApiApplication extends ResourceConfig {
    public WebApiApplication() {
        packages("org.transitclock.web.api");
    }
}
