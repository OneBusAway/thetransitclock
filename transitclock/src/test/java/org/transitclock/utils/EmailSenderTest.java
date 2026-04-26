package org.transitclock.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.Test;

/**
 * Phase B migrated EmailSender from javax.mail to jakarta.mail. Without
 * a typecheck-level guarantee, a future revert could re-introduce the
 * legacy namespace and only fail at runtime when the Jakarta-only
 * implementation is missing the javax classes.
 */
public class EmailSenderTest {

    @Test
    public void sessionFieldUsesJakartaMailNamespace() throws Exception {
        Field session = EmailSender.class.getDeclaredField("session");
        assertThat(session.getType().getName()).isEqualTo("jakarta.mail.Session");
    }
}
