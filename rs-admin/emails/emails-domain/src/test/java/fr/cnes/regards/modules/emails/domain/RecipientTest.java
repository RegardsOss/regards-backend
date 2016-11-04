/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit testing of {@link Recipient}
 */
public class RecipientTest {

    /**
     * Test recipient
     */
    Recipient recipient = new Recipient();

    /**
     * Test address
     */
    String address = "adress";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        recipient.setAddress(address);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Recipient#getAddress()}.
     */
    @Test
    public void testGetAddress() {
        Assert.assertEquals(address, recipient.getAddress());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.emails.domain.Recipient#setAddress(java.lang.String)}.
     */
    @Test
    public void testSetAddress() {
        String newAddress = "newAddress";
        recipient.setAddress(newAddress);
        Assert.assertEquals(newAddress, recipient.getAddress());
    }

}
