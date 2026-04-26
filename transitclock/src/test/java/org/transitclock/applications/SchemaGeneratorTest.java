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
}
