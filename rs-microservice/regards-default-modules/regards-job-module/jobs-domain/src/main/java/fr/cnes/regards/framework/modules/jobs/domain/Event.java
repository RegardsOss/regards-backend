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
package fr.cnes.regards.framework.modules.jobs.domain;

/**
 * 
 * @author Léo Mieulet
 *
 */
public class Event implements IEvent {

    /**
     * Store the event type
     */
    private final EventType eventType;

    /**
     * Store any data related to the event (nullable)
     */
    private final Object data;

    /**
     * Store the jobId
     */
    private final Long jobId;

    /**
     * Store the tenantName
     */
    private final String tenantName;

    /**
     * @param pEventType
     *            the event type
     * @param pData
     *            to store some data
     * @param pJobInfoId
     *            the jobInfo id
     * @param pTenantName
     *            the tenant name
     */
    public Event(final EventType pEventType, final Object pData, final Long pJobInfoId, final String pTenantName) {
        super();
        eventType = pEventType;
        data = pData;
        jobId = pJobInfoId;
        tenantName = pTenantName;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public EventType getType() {
        return eventType;
    }

    @Override
    public Long getJobInfoId() {
        return jobId;
    }

    @Override
    public String getTenantName() {
        return tenantName;
    }
}
