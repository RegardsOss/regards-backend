/*
 * LICENSE_PLACEHOLDER
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.domain.CleaningRabbitMQVhostException;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;

/**
 * @author svissier
 *
 */
@ActiveProfiles("rabbit")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class RabbitVirtualHostUtilsIT {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitVirtualHostUtilsIT.class);

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
    private IRabbitVirtualHostUtils rabbitVirtualHostUtils;

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
        Assume.assumeTrue(rabbitVirtualHostUtils.brokerRunning());
    }

    /**
     * Test adding and retrieving vhost from the broker
     */
    @Test
    public void testVhost() {
        final List<String> firstRetrieve = rabbitVirtualHostUtils.retrieveVhostList();
        try {
            rabbitVirtualHostUtils.addVhost(TEST_VHOST);

            final List<String> secondRetrieve = rabbitVirtualHostUtils.retrieveVhostList();
            Assert.assertEquals(firstRetrieve.size(), secondRetrieve.size() - 1);
            Assert.assertTrue(secondRetrieve.stream().filter(v -> v.equals(TEST_VHOST)).findAny().isPresent());
        } catch (RabbitMQVhostException e) {
            Assert.fail();
            LOGGER.error("Issue during adding the Vhost", e);
        }
        try {
            cleanRabbit(TEST_VHOST);
        } catch (CleaningRabbitMQVhostException e) {
            LOGGER.debug("Issue during cleaning the broker", e);
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
        final List<String> existingVhost = rabbitVirtualHostUtils.retrieveVhostList();
        if (existingVhost.stream().filter(vhost -> vhost.equals(pTenant1)).findAny().isPresent()) {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, rabbitVirtualHostUtils.setBasic());
            final HttpEntity<Void> request = new HttpEntity<>(headers);
            final ResponseEntity<String> response = restTemplate
                    .exchange(rabbitVirtualHostUtils.getRabbitApiVhostEndpoint() + SLASH + pTenant1, HttpMethod.DELETE,
                              request, String.class);
            final int statusValue = response.getStatusCodeValue();
            // if successful or 404 then the broker is clean
            if (!(rabbitVirtualHostUtils.isSuccess(statusValue) || (statusValue == HttpStatus.NOT_FOUND.value()))) {
                throw new CleaningRabbitMQVhostException(response.getBody());
            }
        }
    }
}
