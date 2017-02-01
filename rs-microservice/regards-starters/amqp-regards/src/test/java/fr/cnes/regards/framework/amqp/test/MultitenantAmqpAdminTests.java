/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * @author svissier
 *
 */
public class MultitenantAmqpAdminTests {

    /**
     * Prefix for naming
     */
    private static final String UNDERSCORE = "_";

    /**
     * sample type id
     */
    private static final String TYPE_IDENTIFIER = "TypeIdentifier";

    /**
     * sample instance id
     */
    private static final String INSTANCE_IDENTIFIER = "InstanceIdentifier";

    /**
     * Regard AMQP admin
     */
    private static RegardsAmqpAdmin regardsAmqpAdmin;

    @BeforeClass
    public static void init() {
        regardsAmqpAdmin = new RegardsAmqpAdmin(TYPE_IDENTIFIER, INSTANCE_IDENTIFIER);
    }

    /**
     * test getExchangeName method
     */
    @Test
    public void testGetExchangeName() {
        final String name = "name";

        String expected = name;
        Assert.assertEquals(expected, regardsAmqpAdmin.getExchangeName(name, Target.ALL));

        expected = TYPE_IDENTIFIER + UNDERSCORE + name;

        Assert.assertEquals(expected, regardsAmqpAdmin.getExchangeName(name, Target.MICROSERVICE));
    }

    /**
     * test getQueueName method
     */
    @Test
    public void testGetQueueName() {
        final Class<String> stringClass = String.class;

        String expectedOneToOne = stringClass.getName();
        Assert.assertEquals(expectedOneToOne,
                            regardsAmqpAdmin.getQueueName(stringClass, WorkerMode.SINGLE, Target.ALL));

        expectedOneToOne = TYPE_IDENTIFIER + UNDERSCORE + stringClass.getName();
        Assert.assertEquals(expectedOneToOne,
                            regardsAmqpAdmin.getQueueName(stringClass, WorkerMode.SINGLE, Target.MICROSERVICE));

        String expectedOneToMany = stringClass.getName() + UNDERSCORE + INSTANCE_IDENTIFIER;
        Assert.assertEquals(expectedOneToMany, regardsAmqpAdmin.getQueueName(stringClass, WorkerMode.ALL, Target.ALL));

        expectedOneToMany = TYPE_IDENTIFIER + UNDERSCORE + stringClass.getName() + UNDERSCORE + INSTANCE_IDENTIFIER;
        Assert.assertEquals(expectedOneToMany,
                            regardsAmqpAdmin.getQueueName(stringClass, WorkerMode.ALL, Target.MICROSERVICE));

    }

    /**
     * test getRoutingKey method
     */
    @Test
    public void testGetRoutingKey() {
        final String expected = "TOTO";
        Assert.assertEquals(expected, regardsAmqpAdmin.getRoutingKey(expected, WorkerMode.SINGLE));
        Assert.assertEquals("", regardsAmqpAdmin.getRoutingKey(expected, WorkerMode.ALL));
    }
}
