package org.transitclock.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.transitclock.applications.SchemaGenerator.Dialect;

/**
 * Hibernate's schema-generation SPI is silent about its own failures: an
 * empty or missing DDL script does not raise. {@link SchemaGenerator} is the
 * only place we call into that SPI, so the post-write contract belongs here.
 *
 * <p>Each dialect gets its own test; {@code SchemaGenerator.main} runs all
 * three in sequence so a stale class-name in any one of them aborts the
 * webstructs pass and breaks the docker setup flow.
 */
public class SchemaGeneratorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private void assertDdlEmittedFor(Dialect dialect, String filenamePrefix) throws Exception {
        File outDir = tempFolder.newFolder("ddl");
        new SchemaGenerator("org.transitclock.db.webstructs", outDir.getAbsolutePath())
                .generate(dialect);

        File ddl = new File(outDir,
                "ddl_" + filenamePrefix + "_org_transitclock_db_webstructs.sql");
        assertThat(ddl).exists();
        assertThat(ddl.length()).isGreaterThan(0L);
        assertThat(Files.readString(ddl.toPath()))
                .containsIgnoringCase("create table");
    }

    @Test
    public void generateEmitsPostgresDdl() throws Exception {
        assertDdlEmittedFor(Dialect.POSTGRES, "postgres");
    }

    @Test
    public void generateEmitsOracleDdl() throws Exception {
        assertDdlEmittedFor(Dialect.ORACLE, "oracle");
    }

    @Test
    public void generateEmitsMysqlDdl() throws Exception {
        assertDdlEmittedFor(Dialect.MYSQL, "mysql");
    }

    @Test
    public void generateThrowsWhenPackageHasNoEntities() throws Exception {
        File outDir = tempFolder.newFolder("ddl");
        SchemaGenerator gen = new SchemaGenerator(
                "org.transitclock.applications.does_not_exist",
                outDir.getAbsolutePath());

        assertThatThrownBy(() -> gen.generate(Dialect.POSTGRES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("create table");
    }
}
