package org.transitclock.db.structs;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.junit.Test;

/**
 * Phase B's Hibernate 5.5 → 6.5 bump changed when the entity-loader formats
 * an entity for debug logging. Hibernate 6's
 * {@code AbstractEntityInitializer.resolveKey} calls {@code debugf("...{}...",
 * entity)} <em>during</em> entity initialization — before the lazy-collection
 * proxies are attached — which routes through {@code Block.toString()} →
 * {@code getTrips()}. The pre-fix {@code getTrips()} guard was
 * {@code if (Hibernate.isInitialized(trips)) return Collections.unmodifiableList(trips)};
 * but {@code Hibernate.isInitialized(null)} returns {@code true}, so the call
 * fell straight through to {@code unmodifiableList(null)} and NPE'd every
 * Block load — silently killing the matcher worker thread under live AVL.
 * Hibernate 5 didn't trigger {@code toString} that early.
 *
 * <p>Block has a {@code private Block()} no-arg constructor (line 189) that
 * Hibernate uses as its instance factory and that explicitly sets
 * {@code trips = null}. Reflecting that constructor reproduces the exact
 * pre-init state without needing a CoreHarness; the assertions below would
 * have NPE'd against the pre-fix {@code getTrips}/{@code toString} pair.
 */
public class BlockHibernateInitTest {

    private static Block newUninitializedBlock() throws Exception {
        Constructor<Block> ctor = Block.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    @Test
    public void getTripsReturnsEmptyWhenTripsFieldIsNull() throws Exception {
        Block block = newUninitializedBlock();

        assertThat(block.getTrips()).isEmpty();
    }

    @Test
    public void toStringDoesNotThrowWhenTripsFieldIsNull() throws Exception {
        Block block = newUninitializedBlock();

        // Pre-fix: would NPE inside getTrips() (called from toString).
        // Post-fix: prints "trips=[]" alongside the rest of the fields.
        String str = block.toString();
        assertThat(str).contains("Block [").contains("trips=[]");
    }
}
