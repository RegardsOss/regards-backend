/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.validation.Valid;
import java.util.Set;

/**
 * Data Transfer Object wrapping the {@link Email} and a list of recipients received.The point is to match the
 * two-parameters "sendEmail" endpoint requirement.
 *
 * @author CS SI
 *
 */
public class EmailWithRecipientsDTO {

    /**
     * Set of recipients' email addresses
     */
    @Valid
    private Set<Recipient> recipients;

    /**
     * The mail
     */
    private Email email;

    /**
     * Get recipients
     *
     * @return the set of recipients' email addresses
     */
    public Set<Recipient> getRecipients() {
        return recipients;
    }

    /**
     * Set recipients
     *
     * @param pRecipients
     *            The set of recipients' email addresses
     */
    public void setRecipients(final Set<Recipient> pRecipients) {
        recipients = pRecipients;
    }

    /**
     * Get email
     *
     * @return the email
     */
    public Email getEmail() {
        return email;
    }

    /**
     * Set email
     *
     * @param pEmail
     *            The email
     */
    public void setEmail(final Email pEmail) {
        email = pEmail;
    }

}
