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
package fr.cnes.regards.modules.filecatalog.client;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;

import java.util.Collection;
import java.util.UUID;

/**
 * POJO containing information about a group of requests.
 *
 * @author Sébastien Binda
 */
public class RequestInfo {

    private String groupId;

    private Collection<RequestResultInfoDto> successRequests = Sets.newHashSet();

    private Collection<RequestResultInfoDto> errorRequests = Sets.newHashSet();

    private String message;

    public String getGroupId() {
        return groupId;
    }

    public Collection<RequestResultInfoDto> getSuccessRequests() {
        return successRequests;
    }

    public Collection<RequestResultInfoDto> getErrorRequests() {
        return errorRequests;
    }

    public String getMessage() {
        return message;
    }

    public RequestInfo withMessage(String message) {
        this.message = message;
        return this;
    }

    public static RequestInfo build() {
        RequestInfo ri = new RequestInfo();
        ri.groupId = UUID.randomUUID().toString();
        return ri;
    }

    public static RequestInfo build(String groupId) {
        RequestInfo ri = new RequestInfo();
        ri.groupId = groupId;
        return ri;
    }

    public static RequestInfo build(String groupId,
                                    Collection<RequestResultInfoDto> successRequests,
                                    Collection<RequestResultInfoDto> errorRequests) {
        RequestInfo ri = new RequestInfo();
        ri.groupId = groupId;
        ri.successRequests = successRequests;
        ri.errorRequests = errorRequests;
        return ri;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((groupId == null) ? 0 : groupId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RequestInfo other = (RequestInfo) obj;
        if (groupId == null) {
            if (other.groupId != null) {
                return false;
            }
        } else if (!groupId.equals(other.groupId)) {
            return false;
        }
        return true;
    }

}
