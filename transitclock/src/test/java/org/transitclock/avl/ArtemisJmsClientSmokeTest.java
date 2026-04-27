package org.transitclock.avl;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.jms.ConnectionFactory;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.junit.Test;

/**
 * Phase B replaced HornetQ with ActiveMQ Artemis. The migrating code does
 * not yet exercise an in-process broker (that requires a follow-up rewrite
 * of {@code JMSWrapper.initiateConnection()} away from JBoss Naming, which
 * is bigger than the namespace bump). This smoke test fails the build if
 * the Artemis jakarta-namespace client classes are not on the classpath
 * or cannot wire a ConnectionFactory — the minimum we can prove without
 * spinning up a broker.
 */
public class ArtemisJmsClientSmokeTest {

    @Test
    public void artemisClientCanBuildConnectionFactory() {
        TransportConfiguration transport = new TransportConfiguration(
                NettyConnectorFactory.class.getName());

        ConnectionFactory factory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(
                JMSFactoryType.CF, transport);

        assertThat(factory).isNotNull();
    }
}
