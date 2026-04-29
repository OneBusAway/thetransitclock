package org.transitclock.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.sql.SQLException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Catches drift between the Postgres SQL we promise (in DbDiskSpaceQuery
 * Javadocs) and what we actually send to ChartGenericJsonQuery — anything
 * a future edit might silently break (toast filter, totals UNION shape,
 * size-pretty column).
 */
public class DbDiskSpaceQueryTest {

    private static final String AGENCY = "test-agency";

    @Test
    public void totalsJson_passesUnionedSqlWithToastFilter() throws SQLException {
        try (MockedStatic<ChartGenericJsonQuery> chart =
                     Mockito.mockStatic(ChartGenericJsonQuery.class)) {
            chart.when(() -> ChartGenericJsonQuery.getJsonString(any(), any()))
                    .thenReturn("{}");

            DbDiskSpaceQuery.getTotalsJson(AGENCY);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            chart.verify(() -> ChartGenericJsonQuery.getJsonString(eq(AGENCY), sql.capture()));
            String captured = sql.getValue();
            assertThat(captured).contains(
                    "UNION",
                    "'Total:'",
                    "nspname !~ '^pg_toast'",
                    "C.relkind <> 'i'",
                    "ORDER BY ordering",
                    // Security-relevant: dropping this filter would expose
                    // pg_catalog and information_schema tables to API clients.
                    "nspname NOT IN ('pg_catalog', 'information_schema')",
                    // Per-row uses pg_total_relation_size; the totals row uses
                    // pg_relation_size — different functions, swapping is a
                    // plausible silent regression.
                    "pg_total_relation_size(C.oid)",
                    "SUM(pg_relation_size(C.oid))");
        }
    }

    @Test
    public void detailsJson_passesNonUnionedSqlIncludingIndexes() throws SQLException {
        try (MockedStatic<ChartGenericJsonQuery> chart =
                     Mockito.mockStatic(ChartGenericJsonQuery.class)) {
            chart.when(() -> ChartGenericJsonQuery.getJsonString(any(), any()))
                    .thenReturn("{}");

            DbDiskSpaceQuery.getDetailsJson(AGENCY);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            chart.verify(() -> ChartGenericJsonQuery.getJsonString(eq(AGENCY), sql.capture()));
            String captured = sql.getValue();
            assertThat(captured).doesNotContain("UNION");
            assertThat(captured).contains(
                    "nspname !~ '^pg_toast'",
                    "nspname NOT IN ('pg_catalog', 'information_schema')",
                    "pg_total_relation_size(C.oid)");
            // No relkind filter — details view keeps indexes as their own rows.
            assertThat(captured).doesNotContain("relkind");
        }
    }

    @Test
    public void totalsJson_propagatesNullFromQuery() throws SQLException {
        try (MockedStatic<ChartGenericJsonQuery> chart =
                     Mockito.mockStatic(ChartGenericJsonQuery.class)) {
            chart.when(() -> ChartGenericJsonQuery.getJsonString(any(), any()))
                    .thenReturn(null);

            assertThat(DbDiskSpaceQuery.getTotalsJson(AGENCY)).isNull();
        }
    }
}
