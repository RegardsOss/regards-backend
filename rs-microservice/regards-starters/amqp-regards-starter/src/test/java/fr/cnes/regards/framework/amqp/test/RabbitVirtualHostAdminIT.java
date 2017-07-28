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

import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.CleaningRabbitMQVhostException;

/**
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AmqpTestsConfiguration.class })
public class RabbitVirtualHostAdminIT {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitVirtualHostAdminIT.class);

    /**
     * TEST_VHOST_1
     */
    private static final String TEST_VHOST = "TEST_VHOST_1";

    /**
     * \/
     */
    private static final String SLASH = "/";

    /**
     * bean to be tested
     */
    @Autowired
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    /**
     * used to clean rabbit
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * create and start a message listener to receive the published event
     *
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(rabbitVirtualHostAdmin.brokerRunning());
    }

    /**
     * Test adding and retrieving vhost from the broker
     */
    @Test
    public void testVhost() {
        rabbitVirtualHostAdmin.retrieveVhostList();
        try {
            rabbitVirtualHostAdmin.addVhost(TEST_VHOST);

            final List<String> secondRetrieve = rabbitVirtualHostAdmin.retrieveVhostList();
            // Assert.assertEquals(firstRetrieve.size(), secondRetrieve.size() - 1);
            Assert.assertTrue(secondRetrieve.stream()
                    .filter(v -> v.equals(RabbitVirtualHostAdmin.getVhostName(TEST_VHOST))).findAny().isPresent());
        } catch (RabbitMQVhostException e) {
            Assert.fail();
            LOGGER.error("Issue during adding the Vhost", e);
        } finally {
            try {
                cleanRabbit(RabbitVirtualHostAdmin.getVhostName(TEST_VHOST));
            } catch (CleaningRabbitMQVhostException e) {
                LOGGER.debug("Issue during cleaning the broker", e);
            }
        }
    }

    /**
     * delete the virtual host if existing
     *
     * @param pTenant1
     *            tenant to clean
     * @throws CleaningRabbitMQVhostException
     *             any issues that could occur
     */
    private void cleanRabbit(String pTenant1) throws CleaningRabbitMQVhostException {
        final List<String> existingVhost = rabbitVirtualHostAdmin.retrieveVhostList();
        final String vhostName = RabbitVirtualHostAdmin.getVhostName(pTenant1);
        if (existingVhost.stream().filter(vhost -> vhost.equals(vhostName)).findAny().isPresent()) {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostAdmin.setBasic());
            final HttpEntity<Void> request = new HttpEntity<>(headers);
            final ResponseEntity<String> response = restTemplate
                    .exchange(rabbitVirtualHostAdmin.getRabbitApiVhostEndpoint() + SLASH + vhostName, HttpMethod.DELETE,
                              request, String.class);
            final int statusValue = response.getStatusCodeValue();
            // if successful or 404 then the broker is clean
            if (!(rabbitVirtualHostAdmin.isSuccess(statusValue) || (statusValue == HttpStatus.NOT_FOUND.value()))) {
                throw new CleaningRabbitMQVhostException(response.getBody());
            }
        }
    }
}
