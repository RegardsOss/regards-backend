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
package fr.cnes.regards.modules.accessrights.instance.service.workflow.events;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when an account was accepted.
 *
 * @author Xavier-Alexandre Brochard
 */
@SuppressWarnings("serial")
public class OnAcceptAccountEvent extends ApplicationEvent {

    /**
     * The email of the account
     */
    private String email;

    /**
     * @param pEmail the email of the account
     */
    public OnAcceptAccountEvent(final String pEmail) {
        super(pEmail);
        email = pEmail;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param pEmail the email to set
     */
    public void setEmail(String pEmail) {
        email = pEmail;
    }

}