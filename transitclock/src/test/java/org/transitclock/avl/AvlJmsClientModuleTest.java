package org.transitclock.avl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import org.transitclock.ipc.jms.JMSWrapper;

/**
 * The legacy implementation logged an error when the JMS broker was
 * unreachable and then left {@code msgConsumer} null, which caused the
 * {@code run()} loop to NPE-storm forever. The expected behavior is loud
 * failure — a typed exception whose message tells operators which broker
 * URL and topic name to fix — and an upstream {@code run()} that exits
 * once that exception is thrown rather than retrying with a null consumer.
 */
public class AvlJmsClientModuleTest {

    @Test
    public void createMessageConsumerThrowsWhenBrokerUnreachable() throws Exception {
        AvlJmsClientModule module = new AvlJmsClientModule("test-agency");

        assertThatThrownBy(module::createMessageConsumer)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("test-agency-AVLTopic")
                .hasMessageContaining(JMSWrapper.getJmsServerUrl());
    }

    @Test
    public void jmsServerUrlConfigKeyUsesCurrentName() {
        // Config key was renamed from the stale HornetQ-era name.
        // The value lookup itself going through StringConfigValue is what
        // the test exercises — a missing key would surface as null.
        assertThat(JMSWrapper.getJmsServerUrl()).isNotNull();
    }
}
