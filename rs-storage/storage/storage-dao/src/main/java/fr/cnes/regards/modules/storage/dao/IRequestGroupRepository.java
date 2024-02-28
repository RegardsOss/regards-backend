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
package fr.cnes.regards.modules.storage.dao;

import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.storage.domain.database.request.RequestGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * JPA Repository to handle access to {@link RequestGroup} entities.
 *
 * @author Sébatien Binda
 */

public interface IRequestGroupRepository extends JpaRepository<RequestGroup, String> {

    Page<RequestGroup> findByType(FileRequestType type, Pageable page);

    Page<RequestGroup> findAllByOrderByCreationDateAsc(Pageable page);

    /**
     * Retrieve all terminated {@link RequestGroup}. <br/>
     * A {@link RequestGroup} is terminated if there is no more running request associated to.
     *
     * @param limit Maximum number of terminated groups to return
     * @return List of terminated {@link RequestGroup}s
     */
    @Query(value = "SELECT * from t_request_group groups "
                   + " WHERE NOT EXISTS (SELECT * "
                   + "        FROM (select g.id as groupId, a.file_storage_request_id as requestId FROM"
                   + "              t_request_group g LEFT OUTER JOIN ta_storage_request_group_ids a ON g.id = a.group_id) AS result "
                   + "        LEFT OUTER JOIN t_file_storage_request r ON result.requestId = r.id "
                   + "        LEFT OUTER JOIN t_file_deletion_request d ON result.groupId = d.group_id "
                   + "        LEFT OUTER JOIN t_file_cache_request cache ON result.groupId in (select group_id from ta_file_cache_request_group_id) "
                   + "        LEFT OUTER JOIN t_file_copy_request copy ON result.groupId = copy.group_id "
                   + "        WHERE (r.status != 'ERROR' OR"
                   + "               d.status != 'ERROR' OR "
                   + "               copy.status != 'ERROR' OR "
                   + "               cache.status != 'ERROR') "
                   + "               AND groups.id = groupId)"
                   + " LIMIT :limit", nativeQuery = true)
    List<RequestGroup> findGroupDones(@Param("limit") Integer limit);

    Page<RequestGroup> findByExpirationDateLessThanEqual(OffsetDateTime expirationDate, Pageable page);
}
