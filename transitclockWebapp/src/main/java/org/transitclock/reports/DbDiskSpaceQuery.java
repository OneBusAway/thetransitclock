package org.transitclock.reports;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.transitclock.db.GenericQuery;

/**
 * Single source of truth for per-table on-disk-size SQL. Centralized so the
 * chart, dashboard, and API callers can't drift out of sync on filters.
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
                    // Total row must use the same size function (pg_total_relation_size:
                    // main + indexes + TOAST) and the same WHERE filters as the per-row
                    // inner SELECT, otherwise it under- or double-counts.
                    + "SELECT 'Total:', "
                    + "       pg_size_pretty(SUM(pg_total_relation_size(C.oid))), "
                    + "       SUM(pg_total_relation_size(C.oid)), "
                    + "       2 as ordering "
                    + "  FROM pg_class C "
                    + "  LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace) "
                    + " WHERE nspname NOT IN ('pg_catalog', 'information_schema') "
                    + "   AND C.relkind <> 'i' "
                    + "   AND nspname !~ '^pg_toast' "
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

    // Filters must mirror TOTALS_SQL's inner SELECT so the dashboard bar
    // chart and the dbDiskSpace page report the same per-table sizes.
    // LIMIT is appended at runtime from a clamped int.
    private static final String TOP_TABLES_SQL =
            "SELECT relname, "
                    + "pg_size_pretty(pg_total_relation_size(C.oid)) AS total_size, "
                    + "pg_total_relation_size(C.oid) AS total_bytes "
                    + "FROM pg_class C "
                    + "LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace) "
                    + "WHERE nspname NOT IN ('pg_catalog', 'information_schema') "
                    + "    AND C.relkind <> 'i' "
                    + "    AND nspname !~ '^pg_toast' "
                    + "ORDER BY pg_total_relation_size(C.oid) DESC";

    public record TableSize(String tableName, String prettySize, long bytes) {}

    private DbDiskSpaceQuery() {}

    public static String getTotalsJson(String agencyId) throws SQLException {
        return ChartGenericJsonQuery.getJsonString(agencyId, TOTALS_SQL);
    }

    public static String getDetailsJson(String agencyId) throws SQLException {
        return ChartGenericJsonQuery.getJsonString(agencyId, DETAILS_SQL);
    }

    /**
     * Top-N largest user tables by on-disk size, biggest first. Limit is
     * clamped to [1, 1000] before being inlined into the SQL — the value is
     * caller-supplied but never user-controlled; the clamp guards against
     * accidental zero/negative or absurd page sizes.
     */
    public static List<TableSize> getTopTables(String agencyId, int limit) throws SQLException {
        int clamped = Math.max(1, Math.min(limit, 1000));
        return TopTablesQuery.run(agencyId, TOP_TABLES_SQL + " LIMIT " + clamped);
    }

    private static final class TopTablesQuery extends GenericQuery {
        private final List<TableSize> rows = new ArrayList<>();

        private TopTablesQuery(String agencyId) throws SQLException {
            super(agencyId);
        }

        static List<TableSize> run(String agencyId, String sql) throws SQLException {
            TopTablesQuery q = new TopTablesQuery(agencyId);
            q.doQuery(sql);
            return q.rows;
        }

        @Override
        protected void addRow(List<Object> values) {
            String name = String.valueOf(values.get(0));
            String pretty = String.valueOf(values.get(1));
            long bytes = ((Number) values.get(2)).longValue();
            rows.add(new TableSize(name, pretty, bytes));
        }
    }
}
