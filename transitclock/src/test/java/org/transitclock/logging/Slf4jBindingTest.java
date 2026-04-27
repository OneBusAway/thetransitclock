package org.transitclock.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Phase B bumped {@code logback-classic} 1.1.x → 1.3.14 (the SLF4J 2.0 line)
 * but left {@code slf4j-api} pinned at 1.7.36. Logback 1.3 binds via
 * {@code SLF4JServiceProvider} ServiceLoader; with 1.7.x on the classpath the
 * old {@code StaticLoggerBinder} lookup misses and SLF4J prints
 * {@code SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder"} once
 * at JVM start, then routes every subsequent log call to {@link
 * org.slf4j.helpers.NOPLogger}. That's what hid the matcher NPE for half of
 * yesterday's debug session.
 *
 * <p>The two checks below would have failed loudly against the pre-fix poms:
 * {@link #loggerFactoryIsLogback} catches the binding miss in the bare
 * factory lookup, and {@link #infoLogReachesAttachedAppender} catches the
 * NOPLogger silent-drop end-to-end.
 *
 * <p>Putting the test in {@code transitclockCore} covers it automatically;
 * {@code transitclockApi}, {@code transitclockWebapp}, and
 * {@code transitclockIntegration} each declare their own {@code slf4j-api}
 * version, so a regression in <em>those</em> poms still slips through —
 * caught only when the Tomcat WAR boots or the integration suite runs.
 */
public class Slf4jBindingTest {

    @Test
    public void loggerFactoryIsLogback() {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        // If SLF4J failed to bind, the factory is org.slf4j.helpers.NOPLoggerFactory.
        assertThat(factory)
                .as("SLF4J ILoggerFactory — expected logback LoggerContext, "
                        + "got %s. If you see NOPLoggerFactory the slf4j-api / "
                        + "logback-classic versions disagree (slf4j 2.0 + "
                        + "logback 1.3.x is the current pairing).",
                        factory.getClass().getName())
                .isInstanceOf(LoggerContext.class);
    }

    @Test
    public void infoLogReachesAttachedAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root =
                context.getLogger(Logger.ROOT_LOGGER_NAME);

        Level previousLevel = root.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        root.addAppender(appender);
        root.setLevel(Level.INFO);
        try {
            Logger logger = LoggerFactory.getLogger(Slf4jBindingTest.class);
            logger.info("slf4j-binding-canary");

            List<ILoggingEvent> events = appender.list;
            assertThat(events)
                    .as("Expected exactly one INFO event from a freshly-emitted "
                            + "log; got %d. Empty list means SLF4J is bound to "
                            + "NOPLogger and nothing reaches logback.",
                            events.size())
                    .hasSize(1);
            assertThat(events.get(0).getFormattedMessage())
                    .isEqualTo("slf4j-binding-canary");
        } finally {
            root.detachAppender(appender);
            root.setLevel(previousLevel);
        }
    }
}
