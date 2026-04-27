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
 * Catches the slf4j-api / logback-classic version mismatch that silently
 * routes every log call to {@link org.slf4j.helpers.NOPLogger}. logback 1.3.x
 * binds via SLF4J 2.0's {@code SLF4JServiceProvider} ServiceLoader; with
 * slf4j-api 1.7.x on the classpath the binding misses and TransitClock's
 * matcher diagnostics disappear.
 */
public class Slf4jBindingTest {

    @Test
    public void loggerFactoryIsLogback() {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        assertThat(factory)
                .as("Expected logback LoggerContext, got %s — slf4j-api / "
                        + "logback-classic versions disagree", factory.getClass().getName())
                .isInstanceOf(LoggerContext.class);
    }

    @Test
    public void infoLogReachesAttachedAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

        Level previousLevel = root.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        root.addAppender(appender);
        root.setLevel(Level.INFO);
        try {
            LoggerFactory.getLogger(Slf4jBindingTest.class).info("slf4j-binding-canary");

            List<ILoggingEvent> events = appender.list;
            assertThat(events)
                    .as("Empty list means SLF4J is bound to NOPLogger and "
                            + "nothing reaches logback")
                    .hasSize(1);
            assertThat(events.get(0).getFormattedMessage()).isEqualTo("slf4j-binding-canary");
        } finally {
            root.detachAppender(appender);
            root.setLevel(previousLevel);
        }
    }
}
