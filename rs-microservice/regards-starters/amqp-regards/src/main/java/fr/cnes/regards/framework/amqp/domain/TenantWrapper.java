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
package fr.cnes.regards.framework.amqp.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 *
 * @param <T> type of event that is wrapped
 * @author svissier
 */
public class TenantWrapper<T> {

    /**
     * the event originally sent to the message broker
     */
    private T content;

    /**
     * Tenant from which the message comes from
     */
    private String tenant;

    /**
     * Event publishing date
     */
    private Date date;

    public TenantWrapper() {
        // constructor for serialization
    }

    public TenantWrapper(T pContent, String pTenant) {
        content = pContent;
        tenant = pTenant;
        date = new Date();
    }

    @JsonTypeInfo(use = Id.CLASS)
    public T getContent() {
        return content;
    }

    public void setContent(T pContent) {
        content = pContent;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String pTenant) {
        tenant = pTenant;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "{\"content\" : " + content + " , \"tenant\" : \"" + tenant + "\"}";
    }
}
