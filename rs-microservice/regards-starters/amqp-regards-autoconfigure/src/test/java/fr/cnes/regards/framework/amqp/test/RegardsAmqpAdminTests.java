/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;

/**
 * @author svissier
 *
 */
public class RegardsAmqpAdminTests {

    /**
     * _
     */
    private static final String UNDERSCORE = "_";

    /**
     * :
     */
    private static final String COLON = ":";

    private static final String TYPE_IDENTIFIER = "TypeIdentifier";

    private static final String INSTANCE_IDENTIFIER = "InstanceIdentifier";

    private static final String ADDRESSES = "127.0.0.1:5762";

    private static RegardsAmqpAdmin regardsAmqpAdmin;

    @BeforeClass
    public static void init() {
        regardsAmqpAdmin = new RegardsAmqpAdmin(TYPE_IDENTIFIER, INSTANCE_IDENTIFIER, ADDRESSES);
    }

    /**
     * test createConnectionFactory method
     */
    @Test
    public void testCreateConnectionFactory() {
        final String vhost = "vhost";
        final String[] rabbitHostAndPort = parseRabbitAddresses(ADDRESSES);
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHostAndPort[0],
                Integer.parseInt(rabbitHostAndPort[1]));
        connectionFactory.setVirtualHost(vhost);
        final CachingConnectionFactory result = regardsAmqpAdmin.createConnectionFactory(vhost);
        // same host
        Assert.assertEquals(connectionFactory.getHost(), result.getHost());
        Assert.assertEquals(connectionFactory.getVirtualHost(), result.getVirtualHost());
        Assert.assertEquals(connectionFactory.getPort(), result.getPort());
    }

    /**
     * @param pRabbitAddresses
     *            addresses from configuration file
     * @return {host, port}
     */
    protected String[] parseRabbitAddresses(String pRabbitAddresses) {
        return pRabbitAddresses.split(COLON);
    }

    /**
     * test getExchangeName method
     */
    @Test
    public void testGetExchangeName() {
        final String name = "name";

        String expected = name;
        Assert.assertEquals(expected, regardsAmqpAdmin.getExchangeName(name, AmqpCommunicationTarget.EXTERNAL));

        expected = TYPE_IDENTIFIER + UNDERSCORE + name;

        Assert.assertEquals(expected, regardsAmqpAdmin.getExchangeName(name, AmqpCommunicationTarget.INTERNAL));
    }

    /**
     * test getQueueName method
     */
    @Test
    public void testGetQueueName() {
        final Class<String> stringClass = String.class;

        String expectedOneToOne = stringClass.getName();
        Assert.assertEquals(expectedOneToOne, regardsAmqpAdmin
                .getQueueName(stringClass, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.EXTERNAL));

        expectedOneToOne = TYPE_IDENTIFIER + UNDERSCORE + stringClass.getName();
        Assert.assertEquals(expectedOneToOne, regardsAmqpAdmin
                .getQueueName(stringClass, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.INTERNAL));

        String expectedOneToMany = stringClass.getName() + UNDERSCORE + INSTANCE_IDENTIFIER;
        Assert.assertEquals(expectedOneToMany, regardsAmqpAdmin
                .getQueueName(stringClass, AmqpCommunicationMode.ONE_TO_MANY, AmqpCommunicationTarget.EXTERNAL));

        expectedOneToMany = TYPE_IDENTIFIER + UNDERSCORE + stringClass.getName() + UNDERSCORE + INSTANCE_IDENTIFIER;
        Assert.assertEquals(expectedOneToMany, regardsAmqpAdmin
                .getQueueName(stringClass, AmqpCommunicationMode.ONE_TO_MANY, AmqpCommunicationTarget.INTERNAL));

    }

    /**
     * test getRoutingKey method
     */
    @Test
    public void testGetRoutingKey() {
        final String expected = "TOTO";
        Assert.assertEquals(expected, regardsAmqpAdmin.getRoutingKey(expected, AmqpCommunicationMode.ONE_TO_ONE));
        Assert.assertEquals("", regardsAmqpAdmin.getRoutingKey(expected, AmqpCommunicationMode.ONE_TO_MANY));
    }

    /**
     * test getUniqueName method
     */
    @Test
    public void testGetUniqueName() {
        Assert.assertEquals(INSTANCE_IDENTIFIER, regardsAmqpAdmin.getUniqueName());
    }
}
