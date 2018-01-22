/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.accessrights.instance.domain.Account;

/**
 * Event fired when an account is refused.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnRefuseAccountEvent extends ApplicationEvent {

    /**
     * The account
     */
    private Account account;

    /**
     * @param pAccount
     */
    public OnRefuseAccountEvent(Account pAccount) {
        super(pAccount);
        account = pAccount;
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
    public void setAccount(Account pAccount) {
        account = pAccount;
    }

}