package org.transitclock.web.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.function.Predicate;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.transitclock.web.api.DbDiskSpaceResource.AgencyQuery;

/**
 * In-process JerseyTest spinning DbDiskSpaceResource on a Grizzly2 HTTP
 * container. Each test rebuilds the resource via the package-private DI
 * constructor with stubbed query / validator fakes — the real DB and the
 * ApiKeyManager singleton never get touched.
 */
public class DbDiskSpaceResourceTest extends JerseyTest {

    private static final String VALID_KEY = "test-key";
    private static final String AGENCY = "test-agency";

    private AgencyQuery totalsFn = a -> "{\"totals_default\":true}";
    private AgencyQuery detailsFn = a -> "{\"details_default\":true}";
    private Predicate<String> apiKeyValidator = VALID_KEY::equals;

    @Override
    protected Application configure() {
        return new ResourceConfig().register(new DbDiskSpaceResource(
                a -> totalsFn.run(a),
                a -> detailsFn.run(a),
                k -> apiKeyValidator.test(k)));
    }

    private Response get(String agencyId, String apiKey) {
        var t = target("/status/db-disk-space");
        if (agencyId != null) t = t.queryParam("a", agencyId);
        if (apiKey != null) t = t.queryParam("k", apiKey);
        return t.request(MediaType.APPLICATION_JSON).get();
    }

    @Test
    public void missingApiKey_returns401() {
        Response r = get(AGENCY, null);
        assertThat(r.getStatus()).isEqualTo(401);
        assertThat(r.readEntity(String.class)).contains("Missing or invalid API key");
    }

    @Test
    public void invalidApiKey_returns401() {
        Response r = get(AGENCY, "bogus");
        assertThat(r.getStatus()).isEqualTo(401);
    }

    @Test
    public void missingAgency_returns400() {
        Response r = get(null, VALID_KEY);
        assertThat(r.getStatus()).isEqualTo(400);
        assertThat(r.readEntity(String.class)).contains("query parameter 'a'");
    }

    @Test
    public void emptyAgency_returns400() {
        Response r = get("", VALID_KEY);
        assertThat(r.getStatus()).isEqualTo(400);
    }

    @Test
    public void happyPath_wrapsBothJsonsInEnvelope() {
        totalsFn = a -> "{\"cols\":[],\"rows\":[1]}";
        detailsFn = a -> "{\"cols\":[],\"rows\":[2,3]}";

        Response r = get(AGENCY, VALID_KEY);

        assertThat(r.getStatus()).isEqualTo(200);
        assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)).isTrue();
        assertThat(r.readEntity(String.class))
                .isEqualTo("{\"totals\":{\"cols\":[],\"rows\":[1]},"
                        + "\"details\":{\"cols\":[],\"rows\":[2,3]}}");
    }

    @Test
    public void agencyIdIsForwardedToBothQueries() {
        var capturedTotals = new String[1];
        var capturedDetails = new String[1];
        totalsFn = a -> { capturedTotals[0] = a; return "1"; };
        detailsFn = a -> { capturedDetails[0] = a; return "2"; };

        get(AGENCY, VALID_KEY);

        assertThat(capturedTotals[0]).isEqualTo(AGENCY);
        assertThat(capturedDetails[0]).isEqualTo(AGENCY);
    }

    @Test
    public void nullQueryResults_emitJsonNull() {
        totalsFn = a -> null;
        detailsFn = a -> null;

        Response r = get(AGENCY, VALID_KEY);

        assertThat(r.getStatus()).isEqualTo(200);
        assertThat(r.readEntity(String.class))
                .isEqualTo("{\"totals\":null,\"details\":null}");
    }

    @Test
    public void sqlException_returnsSanitized500() {
        totalsFn = a -> { throw new SQLException("relation \"secrets\" does not exist"); };

        Response r = get(AGENCY, VALID_KEY);

        assertThat(r.getStatus()).isEqualTo(500);
        String body = r.readEntity(String.class);
        assertThat(body).isEqualTo("Database query failed; see server logs.");
        // Critical: don't leak the raw Postgres error text to clients.
        assertThat(body).doesNotContain("secrets");
        assertThat(body).doesNotContain("relation");
    }

    @Test
    public void runtimeException_returnsSanitized500() {
        totalsFn = a -> { throw new IllegalStateException("connection pool exhausted: 30/30"); };

        Response r = get(AGENCY, VALID_KEY);

        assertThat(r.getStatus()).isEqualTo(500);
        String body = r.readEntity(String.class);
        assertThat(body).isEqualTo("Database query failed; see server logs.");
        assertThat(body).doesNotContain("connection pool");
    }
}
