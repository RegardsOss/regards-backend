/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Multi virtual host tests
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@TestPropertySource(properties = { "regards.amqp.management.mode=MULTI", "regards.tenants=PROJECT, PROJECT1",
        "regards.tenant=PROJECT", "regards.amqp.internal.transaction=true", "spring.jmx.enabled=false" },
        locations = "classpath:amqp.properties")
public class MultiVhostSubscriberIT extends AbstractSubscriberIT {

}
