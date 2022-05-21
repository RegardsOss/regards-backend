/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.emails.domain;

import javax.validation.constraints.Email;

/**
 * Simple class wrapping an email address. Its only purpose it to enable the use of Hibernate Validator's email
 * annotation.
 *
 * @author xbrochard
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
     * @param pAddress The address
     */
    public void setAddress(final String pAddress) {
        address = pAddress;
    }

}
