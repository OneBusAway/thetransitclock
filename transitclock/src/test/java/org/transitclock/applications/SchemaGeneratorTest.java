package org.transitclock.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Hibernate's schema-generation SPI is silent about its own failures: an
 * empty or missing DDL script does not raise. {@link SchemaGenerator} is the
 * only place we call into that SPI, so the post-write contract belongs here.
 */
public class SchemaGeneratorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void generateEmitsNonEmptyDdlWithCreateTable() throws Exception {
        File outDir = tempFolder.newFolder("ddl");
        SchemaGenerator gen = new SchemaGenerator(
                "org.transitclock.db.webstructs", outDir.getAbsolutePath());

        gen.generate(SchemaGenerator.Dialect.POSTGRES);

        File ddl = new File(outDir,
                "ddl_postgres_org_transitclock_db_webstructs.sql");
        assertThat(ddl).exists();
        assertThat(ddl.length()).isGreaterThan(0L);
        assertThat(Files.readString(ddl.toPath()))
                .containsIgnoringCase("create table");
    }

    @Test
    public void generateThrowsWhenPackageHasNoEntities() throws Exception {
        File outDir = tempFolder.newFolder("ddl");
        SchemaGenerator gen = new SchemaGenerator(
                "org.transitclock.applications.does_not_exist",
                outDir.getAbsolutePath());

        assertThatThrownBy(() -> gen.generate(SchemaGenerator.Dialect.POSTGRES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("create table");
    }

    /**
     * Phase B's Hibernate 5.5 → 6.5 bump dropped {@code Oracle10gDialect}, but
     * {@code Dialect.ORACLE}'s class-name string wasn't updated to the new
     * {@code OracleDialect}. The pre-existing test only exercised
     * {@code Dialect.POSTGRES}, so the broken Oracle pass shipped — and
     * {@code SchemaGenerator.main} runs the dialects in sequence inside one
     * Maven invocation, so an Oracle failure aborts the second DDL run for
     * webstructs and the docker setup flow's psql apply step has nothing to
     * import. The two tests below pin every dialect string to a class that
     * actually loads under the current Hibernate version.
     */
    @Test
    public void generateEmitsOracleDdl() throws Exception {
        File outDir = tempFolder.newFolder("ddl");
        SchemaGenerator gen = new SchemaGenerator(
                "org.transitclock.db.webstructs", outDir.getAbsolutePath());

        gen.generate(SchemaGenerator.Dialect.ORACLE);

        File ddl = new File(outDir,
                "ddl_oracle_org_transitclock_db_webstructs.sql");
        assertThat(ddl).exists();
        assertThat(ddl.length()).isGreaterThan(0L);
        assertThat(Files.readString(ddl.toPath()))
                .containsIgnoringCase("create table");
    }

    @Test
    public void generateEmitsMysqlDdl() throws Exception {
        File outDir = tempFolder.newFolder("ddl");
        SchemaGenerator gen = new SchemaGenerator(
                "org.transitclock.db.webstructs", outDir.getAbsolutePath());

        gen.generate(SchemaGenerator.Dialect.MYSQL);

        File ddl = new File(outDir,
                "ddl_mysql_org_transitclock_db_webstructs.sql");
        assertThat(ddl).exists();
        assertThat(ddl.length()).isGreaterThan(0L);
        assertThat(Files.readString(ddl.toPath()))
                .containsIgnoringCase("create table");
    }
}
