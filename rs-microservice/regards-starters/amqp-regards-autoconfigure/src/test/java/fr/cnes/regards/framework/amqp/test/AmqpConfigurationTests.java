/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;

/**
 * @author svissier
 *
 */
public class AmqpConfigurationTests {

    /**
     * 200
     */
    private static final int TWO_HUNDRED = 200;

    /**
     * 300
     */
    private static final int THREE_HUNDRED = 300;

    /**
     * _
     */
    private static final String UNDERSCORE = "_";

    /**
     * :
     */
    private static final String COLON = ":";

    /**
     * bean to Test
     */
    @Autowired
    private RegardsAmqpAdmin amqpConfiguration;

    /**
     * type identifier
     */
    @Value("${regards.amqp.microservice.type.identifier}")
    private String typeIdentifier;

    /**
     * instance identifier
     */
    @Value("${regards.amqp.microservice.instance.identifier}")
    private String instanceIdentifier;

    /**
     * addresses configured to
     */
    @Value("${spring.rabbitmq.addresses}")
    private String rabbitAddresses;

    /**
     * username used to connect to the broker and it's manager
     */
    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUserName;

    /**
     * password used to connect to the broker and it's manager
     */
    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    /**
     * value from the configuration file representing the host of the manager of the broker
     */
    @Value("${regards.amqp.management.host}")
    private String amqpManagementHost;

    /**
     * value from the configuration file representing the port on which the manager of the broker is listening
     */
    @Value("${regards.amqp.management.port}")
    private Integer amqpManagementPort;

    @Autowired
    private RabbitVirtualHostUtils rabbitVirtualHostUtils;

    /**
     * Test setBasic method
     */
    @Test
    public void testSetBasic() {

        // final String fullCredential = rabbitmqUserName + COLON + rabbitmqPassword;
        // final byte[] plainCredsBytes = fullCredential.getBytes();
        // final byte[] base64CredsBytes = Base64.encode(plainCredsBytes);
        // final String encoded = new String(base64CredsBytes);
        // final String expected = "Basic " + encoded;
        //
        // Assert.assertEquals(expected, rabbitVirtualHostUtils.setBasic());
    }

    /**
     * test isSuccess method
     */
    @Test
    public void testIsSuccess() {
        // final List<Integer> success = new ArrayList<>(100);
        // for (int i = 0; i < success.size(); i++) {
        // success.set(i, TWO_HUNDRED + i);
        // }
        // success.parallelStream().forEach(i -> Assert.assertEquals(true, rabbitVirtualHostUtils.isSuccess(i)));
        //
        // final List<Integer> inferiorTwoHundred = new ArrayList<>(200);
        // for (int i = 0; i < inferiorTwoHundred.size(); i++) {
        // inferiorTwoHundred.set(i, i);
        // }
        // inferiorTwoHundred.parallelStream()
        // .forEach(i -> Assert.assertEquals(false, rabbitVirtualHostUtils.isSuccess(i)));
        //
        // final List<Integer> superiorTwoNintyNine = new ArrayList<>(300);
        // for (int i = 0; i < superiorTwoNintyNine.size(); i++) {
        // superiorTwoNintyNine.set(i, THREE_HUNDRED + i);
        // }
        // superiorTwoNintyNine.parallelStream()
        // .forEach(i -> Assert.assertEquals(false, rabbitVirtualHostUtils.isSuccess(i)));
    }

    /**
     * test createConnectionFactory method
     */
    @Test
    public void testCreateConnectionFactory() {
        // final String vhost = "vhost";
        // final String[] rabbitHostAndPort = parseRabbitAddresses(rabbitAddresses);
        // final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHostAndPort[0],
        // Integer.parseInt(rabbitHostAndPort[1]));
        // connectionFactory.setVirtualHost(vhost);
        // Assert.assertEquals(connectionFactory, amqpConfiguration.createConnectionFactory(vhost));
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
        // final String name = "name";
        //
        // String expected = name;
        // Assert.assertEquals(expected, amqpConfiguration.getExchangeName(name, AmqpCommunicationTarget.EXTERNAL));
        //
        // expected = typeIdentifier + UNDERSCORE + name;
        //
        // Assert.assertEquals(expected, amqpConfiguration.getExchangeName(name, AmqpCommunicationTarget.INTERNAL));
    }

    /**
     * test getQueueName method
     */
    @Test
    public void testGetQueueName() {
        // final Class<String> stringClass = String.class;
        //
        // String expectedOneToOne = stringClass.getName();
        // Assert.assertEquals(expectedOneToOne, amqpConfiguration
        // .getQueueName(stringClass, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.EXTERNAL));
        //
        // expectedOneToOne = typeIdentifier + UNDERSCORE + stringClass.getName();
        // Assert.assertEquals(expectedOneToOne, amqpConfiguration
        // .getQueueName(stringClass, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.INTERNAL));
        //
        // String expectedOneToMany = stringClass.getName() + UNDERSCORE + instanceIdentifier;
        // Assert.assertEquals(expectedOneToMany, amqpConfiguration
        // .getQueueName(stringClass, AmqpCommunicationMode.ONE_TO_MANY, AmqpCommunicationTarget.EXTERNAL));
        //
        // expectedOneToMany = typeIdentifier + UNDERSCORE + stringClass.getName() + UNDERSCORE + instanceIdentifier;
        // Assert.assertEquals(expectedOneToMany, amqpConfiguration
        // .getQueueName(stringClass, AmqpCommunicationMode.ONE_TO_MANY, AmqpCommunicationTarget.INTERNAL));

    }

    /**
     * test getRoutingKey method
     */
    @Test
    public void testGetRoutingKey() {
        // final String expected = "TOTO";
        // Assert.assertEquals(expected, amqpConfiguration.getRoutingKey(expected, AmqpCommunicationMode.ONE_TO_ONE));
        // Assert.assertEquals("", amqpConfiguration.getRoutingKey(expected, AmqpCommunicationMode.ONE_TO_MANY));
    }

    /**
     * test getUniqueName method
     */
    @Test
    public void testGetUniqueName() {
        // Assert.assertEquals(instanceIdentifier, amqpConfiguration.getUniqueName());
    }
}
