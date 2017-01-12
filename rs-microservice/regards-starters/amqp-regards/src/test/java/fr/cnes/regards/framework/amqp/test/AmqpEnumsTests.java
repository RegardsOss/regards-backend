/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;

/**
 * @author svissier
 *
 */
public class AmqpEnumsTests {

    @Test
    public void amqpCommunicationTargetTest() {
        Assert.assertEquals(AmqpCommunicationTarget.ALL,
                            AmqpCommunicationTarget.valueOf(AmqpCommunicationTarget.ALL.toString()));
        Assert.assertEquals(AmqpCommunicationTarget.MICROSERVICE,
                            AmqpCommunicationTarget.valueOf(AmqpCommunicationTarget.MICROSERVICE.toString()));
    }

    @Test
    public void amqpCommunicationModeTest() {
        Assert.assertEquals(AmqpCommunicationMode.ONE_TO_MANY,
                            AmqpCommunicationMode.valueOf(AmqpCommunicationMode.ONE_TO_MANY.toString()));
        Assert.assertEquals(AmqpCommunicationMode.ONE_TO_ONE,
                            AmqpCommunicationMode.valueOf(AmqpCommunicationMode.ONE_TO_ONE.toString()));
    }

}
