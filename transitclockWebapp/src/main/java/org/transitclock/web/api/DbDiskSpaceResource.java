package org.transitclock.web.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitclock.db.webstructs.ApiKeyManager;
import org.transitclock.reports.DbDiskSpaceQuery;

/**
 * GET /api/status/db-disk-space?a=&lt;agency&gt;&amp;k=&lt;apiKey&gt;
 *
 * Returns Postgres on-disk size info for the given agency's DB. Body is
 * {"totals": &lt;Google Charts DataTable JSON&gt;, "details": &lt;ditto&gt;}; either
 * member may be null if the underlying query produced no rows. Requires a
 * valid API key — the same key namespace transitclockApi uses (managed via
 * the CreateAPIKey shaded jar).
 */
@Path("status/db-disk-space")
public class DbDiskSpaceResource {

    private static final Logger logger = LoggerFactory.getLogger(DbDiskSpaceResource.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@QueryParam("a") String agencyId,
                        @QueryParam("k") String apiKey) {
        requireValidKey(apiKey);
        if (agencyId == null || agencyId.isEmpty()) {
            throw error(Status.BAD_REQUEST, "Missing required query parameter 'a' (agency id)");
        }

        try {
            String totals = DbDiskSpaceQuery.getTotalsJson(agencyId);
            String details = DbDiskSpaceQuery.getDetailsJson(agencyId);
            // Both query results are already valid JSON (or null); concatenate
            // raw to avoid a parse/re-serialize round-trip.
            String body = "{\"totals\":" + (totals != null ? totals : "null")
                    + ",\"details\":" + (details != null ? details : "null") + "}";
            return Response.ok(body).type(MediaType.APPLICATION_JSON).build();
        } catch (SQLException | RuntimeException e) {
            // Log the full stack server-side; return a generic body so we
            // don't leak Postgres error text (schema names, role names,
            // connection URLs) to API clients.
            logger.error("db-disk-space query failed for agency={}", agencyId, e);
            throw error(Status.INTERNAL_SERVER_ERROR, "Database query failed; see server logs.");
        }
    }

    private static void requireValidKey(String key) {
        if (key == null || key.isEmpty() || !ApiKeyManager.getInstance().isKeyValid(key)) {
            throw error(Status.UNAUTHORIZED, "Missing or invalid API key (query param 'k')");
        }
    }

    private static WebApplicationException error(Status status, String message) {
        return new WebApplicationException(
                Response.status(status).entity(message).type(MediaType.TEXT_PLAIN).build());
    }
}
