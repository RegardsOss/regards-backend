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
package fr.cnes.regards.modules.authentication.plugins.domain;

/**
 *
 * Class ExternalAuthenticationInformations
 *
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

    public ExternalAuthenticationInformations(final String pUserName, final String pProject, final byte[] pTicket,
            final String pProviderKey) {
        super();
        userName = pUserName;
        project = pProject;
        ticket = pTicket;
        providerKey = pProviderKey;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String pUserName) {
        userName = pUserName;
    }

    public String getProject() {
        return project;
    }

    public void setProject(final String pProject) {
        project = pProject;
    }

    public byte[] getTicket() {
        return ticket;
    }

    public void setTicket(final byte[] pTicket) {
        ticket = pTicket;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(final String pProviderKey) {
        providerKey = pProviderKey;
    }

}
