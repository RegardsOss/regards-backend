/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.service.passwordreset;

import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import org.springframework.context.ApplicationEvent;

/**
 * Event transporting the data needed for the password reset process.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnPasswordResetEvent extends ApplicationEvent {

    /**
     * Generated serial
     */
    private static final long serialVersionUID = -7099682370525387294L;

    /**
     * The initiator's account
     */
    private Account account;

    /**
     * The url of the app from where was issued the query
     */
    private final String originUrl;

    /**
     * The url to redirect the user to the password reset interface
     */
    private final String requestLink;

    /**
     * Class constructor
     *
     * @param pAccount   the account
     * @param pOriginUrl the origin url
     * @param pResetUrl  the reset url
     */
    public OnPasswordResetEvent(final Account pAccount, final String pOriginUrl, final String pResetUrl) {
        super(pAccount);
        this.account = pAccount;
        this.originUrl = pOriginUrl;
        this.requestLink = pResetUrl;
    }

    /**
     * @return the account
     */
    public Account getAccount() {
        return account;
    }

    /**
     * @param pAccount the account to set
     */
    public void setAccount(final Account pAccount) {
        account = pAccount;
    }

    /**
     * @return the originUrl
     */
    public String getOriginUrl() {
        return originUrl;
    }

    /**
     * @return the resetUrl
     */
    public String getRequestLink() {
        return requestLink;
    }

}