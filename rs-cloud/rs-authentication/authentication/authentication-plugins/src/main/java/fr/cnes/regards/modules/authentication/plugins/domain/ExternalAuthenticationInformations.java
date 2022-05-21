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
package fr.cnes.regards.modules.authentication.plugins.domain;

/**
 * Class ExternalAuthenticationInformations
 * <p>
 * POJO for needed informations to authenticate from an external service provider.
 *
 * @author Sébastien Binda
 */
public class ExternalAuthenticationInformations {

    /**
     * User name
     */
    private String userName;

    /**
     * Regards project to authenticate to
     */
    private String project;

    /**
     * External authentication ticket from the service provider
     */
    private byte[] ticket;

    /**
     * API Key to authenticate REGARDS to the service provider
     */
    private String providerKey;

    public ExternalAuthenticationInformations() {
        super();
    }

    public ExternalAuthenticationInformations(final String userName,
                                              final String project,
                                              final byte[] ticket,
                                              final String pProviderKey) {
        super();
        this.userName = userName;
        this.project = project;
        this.ticket = ticket;
        providerKey = pProviderKey;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public String getProject() {
        return project;
    }

    public void setProject(final String project) {
        this.project = project;
    }

    public byte[] getTicket() {
        return ticket;
    }

    public void setTicket(final byte[] ticket) {
        this.ticket = ticket;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(final String providerKey) {
        this.providerKey = providerKey;
    }

}
