package org.transitclock.reports;

import java.sql.SQLException;

/**
 * Postgres-catalog queries for per-table on-disk size, returned as
 * Google Charts DataTable JSON. Used by status/dbDiskSpace.jsp (server-side
 * render) and by the JAX-RS endpoint at /api/status/db-disk-space (external
 * authorized clients). Centralized here so the SQL doesn't drift between
 * the two callers.
 */
public final class DbDiskSpaceQuery {

    // Two queries unioned so the "Total:" row stays at the bottom regardless
    // of how the table is later sorted client-side. The outer SELECT drops
    // the ordering column and renames the rest to human-readable headers.
    private static final String TOTALS_SQL =
            "SELECT relname AS \"Table Name\", "
                    + "     total_size AS \"Total Size\", "
                    + "     total_bytes AS \"Total Bytes\" "
                    + " FROM "
                    + "((SELECT relname , "
                    + "        pg_size_pretty(pg_total_relation_size(C.oid)) AS total_size, "
                    + "        pg_total_relation_size(C.oid) AS total_bytes, "
                    + "        1 AS ordering"
                    + "   FROM pg_class C "
                    + "   LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace) "
                    + "  WHERE nspname NOT IN ('pg_catalog', 'information_schema') "
                    + "    AND C.relkind <> 'i' "
                    + "    AND nspname !~ '^pg_toast' "
                    + ") "
                    + "UNION "
                    + "SELECT 'Total:', "
                    + "       pg_size_pretty(SUM(pg_relation_size(C.oid))), "
                    + "       SUM(pg_relation_size(C.oid)), "
                    + "       2 as ordering "
                    + "  FROM pg_class C "
                    + "  LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace) "
                    + " WHERE nspname NOT IN ('pg_catalog', 'information_schema') "
                    + ") AS needed_alias_name "
                    + "ORDER BY ordering, \"Total Bytes\" DESC";

    // Detail view: one row per user relation (tables and indexes). Toast
    // tables are filtered out as standalone rows; their bytes still roll
    // into pg_total_relation_size for the owning table.
    private static final String DETAILS_SQL =
            "SELECT relname AS \"Table Name\", "
                    + "pg_size_pretty(pg_total_relation_size(C.oid)) AS \"Total Size\", "
                    + "pg_total_relation_size(C.oid) AS \"Total Bytes\" "
                    + "FROM pg_class C "
                    + "LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace) "
                    + "WHERE nspname NOT IN ('pg_catalog', 'information_schema') "
                    + "    AND nspname !~ '^pg_toast' "
                    + "ORDER BY pg_total_relation_size(C.oid) DESC";

    private DbDiskSpaceQuery() {}

    public static String getTotalsJson(String agencyId) throws SQLException {
        return ChartGenericJsonQuery.getJsonString(agencyId, TOTALS_SQL, null, null);
    }

    public static String getDetailsJson(String agencyId) throws SQLException {
        return ChartGenericJsonQuery.getJsonString(agencyId, DETAILS_SQL);
    }
}
