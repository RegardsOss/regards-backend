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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.VirtualHostMode;

/**
 * @author svissier
 */
public class RabbitVirtualHostAdminTests {

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

    /**
     * username
     */
    private static final String RABBITMQ_USERNAME = "username";

    /**
     * password
     */
    private static final String RABBITMQ_PASSWORD = "password";

    /**
     * management host
     */
    private static final String AMQP_MANAGEMENT_HOST = "127.0.0.1";

    /**
     * management port
     */
    private static final Integer AMQP_MANAGEMENT_PORT = 15672;

    /**
     * Local adress
     */
    private static final String ADDRESSES = "127.0.0.1:5762";

    /**
     * bean to be tested
     */
    private static IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    @BeforeClass
    public static void init() {
        rabbitVirtualHostAdmin = new RabbitVirtualHostAdmin(VirtualHostMode.MULTI, null, RABBITMQ_USERNAME,
                                                            RABBITMQ_PASSWORD, AMQP_MANAGEMENT_HOST,
                                                            AMQP_MANAGEMENT_PORT, null, null, ADDRESSES, null);
    }

    /**
     * Test setBasic method
     */
    @Test
    public void testSetBasic() {

        final String fullCredential = RABBITMQ_USERNAME + COLON + RABBITMQ_PASSWORD;
        final byte[] plainCredsBytes = fullCredential.getBytes();
        String encoded = Base64.getEncoder().encodeToString(plainCredsBytes);
        final String expected = "Basic " + encoded;

        Assert.assertEquals(expected, rabbitVirtualHostAdmin.setBasic());
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
        success.parallelStream().forEach(i -> Assert.assertTrue(rabbitVirtualHostAdmin.isSuccess(i)));

        final List<Integer> inferiorTwoHundred = new ArrayList<>(200);
        for (int i = 0; i < inferiorTwoHundred.size(); i++) {
            inferiorTwoHundred.set(i, i);
        }
        inferiorTwoHundred.parallelStream()
                .forEach(i -> Assert.assertFalse(rabbitVirtualHostAdmin.isSuccess(i)));

        final List<Integer> superiorTwoNintyNine = new ArrayList<>(300);
        for (int i = 0; i < superiorTwoNintyNine.size(); i++) {
            superiorTwoNintyNine.set(i, THREE_HUNDRED + i);
        }
        superiorTwoNintyNine.parallelStream()
                .forEach(i -> Assert.assertFalse(rabbitVirtualHostAdmin.isSuccess(i)));
    }
}
