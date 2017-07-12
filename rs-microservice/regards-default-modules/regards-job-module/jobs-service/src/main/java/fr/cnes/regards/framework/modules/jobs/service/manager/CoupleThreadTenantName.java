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
package fr.cnes.regards.framework.modules.jobs.service.manager;

/**
 * @author Léo Mieulet
 */
public class CoupleThreadTenantName {

    /**
     * Store the running job
     */
    private final Thread thread;

    /**
     * Store the tenant name for this job
     */
    private final String tenantName;

    /**
     *
     * @param pTenantName
     *            the tenant name
     * @param pThread
     *            the running job
     */
    public CoupleThreadTenantName(final String pTenantName, final Thread pThread) {
        thread = pThread;
        tenantName = pTenantName;
    }

    /**
     * @return the thread
     */
    public Thread getThread() {
        return thread;
    }

    /**
     * @return the tenantName
     */
    public String getTenantName() {
        return tenantName;
    }

}