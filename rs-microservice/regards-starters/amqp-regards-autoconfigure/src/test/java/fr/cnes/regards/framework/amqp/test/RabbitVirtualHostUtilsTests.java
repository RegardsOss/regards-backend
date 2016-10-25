/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.crypto.codec.Base64;

import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;

/**
 * @author svissier
 *
 */
public class RabbitVirtualHostUtilsTests {

    /**
     * :
     */
    private static final String COLON = ":";

    /**
     * 200
     */
    private static final int TWO_HUNDRED = 200;

    /**
     * 300
     */
    private static final int THREE_HUNDRED = 300;

    private static final String RABBITMQ_USERNAME = "username";

    private static final String RABBITMQ_PASSWORD = "password";

    private static final String AMQP_MANAGMENT_HOST = "127.0.0.1";

    private static final Integer AMQP_MANAGMENT_PORT = 15672;

    private static IRabbitVirtualHostUtils rabbitVirtualHostUtils;

    @BeforeClass
    public static void init() {
        rabbitVirtualHostUtils = new RabbitVirtualHostUtils(RABBITMQ_USERNAME, RABBITMQ_PASSWORD, AMQP_MANAGMENT_HOST,
                AMQP_MANAGMENT_PORT, null, null);
    }

    /**
     * Test setBasic method
     */
    @Test
    public void testSetBasic() {

        final String fullCredential = RABBITMQ_USERNAME + COLON + RABBITMQ_PASSWORD;
        final byte[] plainCredsBytes = fullCredential.getBytes();
        final byte[] base64CredsBytes = Base64.encode(plainCredsBytes);
        final String encoded = new String(base64CredsBytes);
        final String expected = "Basic " + encoded;

        Assert.assertEquals(expected, rabbitVirtualHostUtils.setBasic());
    }

    /**
     * test isSuccess method
     */
    @Test
    public void testIsSuccess() {
        final List<Integer> success = new ArrayList<>(100);
        for (int i = 0; i < success.size(); i++) {
            success.set(i, TWO_HUNDRED + i);
        }
        success.parallelStream().forEach(i -> Assert.assertEquals(true, rabbitVirtualHostUtils.isSuccess(i)));

        final List<Integer> inferiorTwoHundred = new ArrayList<>(200);
        for (int i = 0; i < inferiorTwoHundred.size(); i++) {
            inferiorTwoHundred.set(i, i);
        }
        inferiorTwoHundred.parallelStream()
                .forEach(i -> Assert.assertEquals(false, rabbitVirtualHostUtils.isSuccess(i)));

        final List<Integer> superiorTwoNintyNine = new ArrayList<>(300);
        for (int i = 0; i < superiorTwoNintyNine.size(); i++) {
            superiorTwoNintyNine.set(i, THREE_HUNDRED + i);
        }
        superiorTwoNintyNine.parallelStream()
                .forEach(i -> Assert.assertEquals(false, rabbitVirtualHostUtils.isSuccess(i)));
    }
}
