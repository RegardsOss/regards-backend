/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@ContextConfiguration(classes = { DefaultTestConfiguration.class })
public class LogbackAppenderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogbackAppenderTest.class);

    /**
     * Static default tenant
     */
    @Value("${regards.tenant}")
    private String tenant;

    /**
     * The microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    /**
     * bean allowing us to know if the broker is running
     */
    @Autowired
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    /**
     * bean to test
     */
    @Autowired
    private IPoller poller;

    @Before
    public void init() {
        tenantResolver.forceTenant(tenant);

        rabbitVirtualHostAdmin.addVhost(tenant);
    }

    @After
    public void clean() {
        // rabbitVirtualHostAdmin.removeVhost(tenant);
    }

    @Test
    public void getOneLogInfo() {
        final String message = "Hello info message : ";
        LOGGER.info(message + "{}", tenant);
        LOGGER.debug("Hello");

        final TenantWrapper<LogEvent> wrapperReceived;
        try {
            wrapperReceived = poller.poll(LogEvent.class);
            Assert.assertNotNull(wrapperReceived);
            final LogEvent received = wrapperReceived.getContent();
            Assert.assertNotNull(received);

            Assert.assertEquals(this.getClass().getCanonicalName(), received.getCaller());
            Assert.assertEquals(Thread.currentThread().getStackTrace()[1].getMethodName(), received.getMethod());
            Assert.assertEquals(microserviceName, received.getMicroService());
            Assert.assertEquals(message + tenant, received.getMsg());

        } catch (RabbitMQVhostException e) {
            final String msg = "Polling one to many Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }
    }

    @Test
    public void getManyLogInfo() {
        LOGGER.info("Hello info message : {}", tenant);
        LOGGER.info("Hello another info message : {}", tenant);
        LOGGER.info("Hello third info message : {}", tenant);

        LOGGER.debug("Hello");

        TenantWrapper<LogEvent> wrapperReceived;
        try {
            // first
            wrapperReceived = poller.poll(LogEvent.class);
            Assert.assertNotNull(wrapperReceived);
            final LogEvent received = wrapperReceived.getContent();
            Assert.assertNotNull(received);

            Assert.assertEquals(this.getClass().getCanonicalName(), received.getCaller());
            Assert.assertEquals(Thread.currentThread().getStackTrace()[1].getMethodName(), received.getMethod());
            Assert.assertEquals(microserviceName, received.getMicroService());

            // second
            wrapperReceived = poller.poll(LogEvent.class);
            Assert.assertNotNull(wrapperReceived);

            // third
            wrapperReceived = poller.poll(LogEvent.class);
            Assert.assertNotNull(wrapperReceived);

        } catch (RabbitMQVhostException e) {
            final String msg = "Polling one to many Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }
    }

    @Test
    public void getNoLogInfo() {
        LOGGER.debug("Hello");

        TenantWrapper<LogEvent> wrapperReceived;
        try {
            // first
            wrapperReceived = poller.poll(LogEvent.class);
            Assert.assertNull(wrapperReceived);
        } catch (RabbitMQVhostException e) {
            final String msg = "Polling one to many Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }
    }

}
