/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.domain;

import org.hibernate.validator.constraints.Email;

/**
 * Simple class wrapping an email address. Its only purpose it to enable the use of Hibernate Validator's {@link @Email}
 * annotation.
 *
 * @author xbrochard
 *
 */
public class Recipient {

    /**
     * Recipients' email address
     */
    @Email
    private String address;

    /**
     * Get <code>address</code>
     *
     * @return the recipient's email address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Set <code>address</code>
     *
     * @param pAddress
     *            The address
     */
    public void setAddress(final String pAddress) {
        address = pAddress;
    }

}
