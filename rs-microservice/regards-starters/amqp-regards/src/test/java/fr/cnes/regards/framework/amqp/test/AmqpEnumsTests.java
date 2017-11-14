/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
        Assert.assertEquals(WorkerMode.BROADCAST, WorkerMode.valueOf(WorkerMode.BROADCAST.toString()));
        Assert.assertEquals(WorkerMode.UNICAST, WorkerMode.valueOf(WorkerMode.UNICAST.toString()));
    }

}
