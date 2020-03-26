/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.dao;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storage.domain.database.request.RequestResultInfo;

/**
 * JPA Repository to handle access to {@link RequestResultInfo} entities.
 *
 * @author Sébatien Binda
 *
 */
public interface IGroupRequestInfoRepository extends JpaRepository<RequestResultInfo, Long> {

    /**
     * Retrieve all {@RequestResultInfo}s matching the given group id.
     * @param groupId
     * @return {@RequestResultInfo}s
     */
    Set<RequestResultInfo> findByGroupId(String groupId);

    /**
     * Delete all {@RequestResultInfo}s by file id.
     * @param fileId
     */
    void deleteByResultFileId(Long fileId);

    /**
     * Delete all {@RequestResultInfo}s by group id.
     * @param groupId
     */
    void deleteByGroupId(String groupId);

    /**
     * Retrieve all {@RequestResultInfo}s matching the given group id and error status.
     * @param groupId
     * @param isError
     * @return {@RequestResultInfo}s
     */
    Set<RequestResultInfo> findByGroupIdAndError(String groupId, boolean isError);

    Set<RequestResultInfo> findByGroupIdIn(Set<String> groupIds);

}
