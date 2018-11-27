/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.test.event.Info;
import fr.cnes.regards.framework.amqp.test.handler.AbstractInfoReceiver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Single virtual host tests
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@TestPropertySource(properties = { "regards.amqp.management.mode=SINGLE", "regards.tenants=PROJECT, PROJECT1",
        "regards.tenant=PROJECT", "regards.amqp.internal.transaction=true" }, locations = "classpath:amqp.properties")
public class SingleVhostSubscriberIT extends AbstractSubscriberIT {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Purpose("Publish one event by tenant and receive them on same handler using same queue.")
    @Test
    public void fromMultipleTenants() {

        MultipleReceiver receiver = new MultipleReceiver();
        subscriber.subscribeTo(Info.class, receiver, true);
        String message = "Default tenant message!";
        publisher.publish(Info.create(message));
        receiver.assertCount(1);

        TenantWrapper<Info> wrapper = receiver.getLastWrapper();
        Assert.assertNotNull(wrapper);
        Assert.assertEquals("PROJECT", wrapper.getTenant());
        Assert.assertEquals(message, wrapper.getContent().getMessage());

        // Change tenant
        String tenant = "PROJECT1";
        runtimeTenantResolver.forceTenant(tenant);

        message = "Forced tenant message!";
        publisher.publish(Info.create(message));
        // Same receiver so count is incremented
        receiver.assertCount(2);

        wrapper = receiver.getLastWrapper();
        Assert.assertNotNull(wrapper);
        Assert.assertEquals(tenant, wrapper.getTenant());
        Assert.assertEquals(message, wrapper.getContent().getMessage());
    }

    private class MultipleReceiver extends AbstractInfoReceiver {

    }

    @Override
    public void publishMicroserviceInfo() {
        super.publishMicroserviceInfo();
    }

    @Override
    public void onePerMicroserviceTypeTest() {
        super.onePerMicroserviceTypeTest();
    }

}
