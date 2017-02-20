/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * @author svissier
 *
 */
public class AmqpEnumsTests {

    @Test
    public void amqpCommunicationTargetTest() {
        Assert.assertEquals(Target.ALL, Target.valueOf(Target.ALL.toString()));
        Assert.assertEquals(Target.MICROSERVICE, Target.valueOf(Target.MICROSERVICE.toString()));
    }

    @Test
    public void amqpCommunicationModeTest() {
        Assert.assertEquals(WorkerMode.ALL, WorkerMode.valueOf(WorkerMode.ALL.toString()));
        Assert.assertEquals(WorkerMode.SINGLE, WorkerMode.valueOf(WorkerMode.SINGLE.toString()));
    }

}
