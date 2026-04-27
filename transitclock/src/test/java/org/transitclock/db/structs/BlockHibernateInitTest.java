package org.transitclock.db.structs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.transitclock.testutil.TransitFixtures;

/**
 * Hibernate 6's {@code AbstractEntityInitializer.resolveKey} formats the
 * entity (via {@code toString()}) for debug logging during entity load,
 * before lazy collections are wired up. Block has a {@code private Block()}
 * factory constructor that leaves {@code trips} null in that window;
 * {@code Hibernate.isInitialized(null)} returns true, so a naive guard on
 * {@code isInitialized} alone falls through to {@code unmodifiableList(null)}.
 */
public class BlockHibernateInitTest {

    @Test
    public void getTripsReturnsEmptyWhenTripsFieldIsNull() throws Exception {
        Block block = TransitFixtures.newViaNoArgConstructor(Block.class);

        assertThat(block.getTrips()).isEmpty();
    }

    @Test
    public void toStringDoesNotThrowWhenTripsFieldIsNull() throws Exception {
        Block block = TransitFixtures.newViaNoArgConstructor(Block.class);

        assertThat(block.toString()).contains("Block [").contains("trips=[]");
    }
}
