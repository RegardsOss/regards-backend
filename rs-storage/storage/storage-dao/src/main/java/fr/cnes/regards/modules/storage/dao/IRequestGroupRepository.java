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
    @Query(value = """
        select * from t_request_group groups 
        WHERE NOT EXISTS (
            SELECT * FROM ta_storage_request_group_ids ta_storage_request
            INNER JOIN t_file_storage_request storage_request
                ON storage_request.id = ta_storage_request.file_storage_request_id
            WHERE groups.id = ta_storage_request.group_id
            AND storage_request.status != 'ERROR'
        )
        AND NOT EXISTS (
            SELECT * FROM ta_file_cache_request_group_id ta_file_cache_request
            INNER JOIN t_file_cache_request cache_requests
                ON cache_requests.id = ta_file_cache_request.file_cache_request_id
            WHERE groups.id = ta_file_cache_request.group_id
            AND cache_requests.status != 'ERROR'
        )
        AND NOT EXISTS (
            SELECT * FROM t_file_deletion_request del_requests
            WHERE groups.id = del_requests.group_id 
            AND del_requests.status != 'ERROR'
        )
        AND NOT EXISTS (
            SELECT * FROM t_file_copy_request copy_requests
            WHERE groups.id = copy_requests.group_id 
            AND copy_requests.status != 'ERROR'
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<RequestGroup> findGroupDones(@Param("limit") Integer limit);

    Page<RequestGroup> findByExpirationDateLessThanEqual(OffsetDateTime expirationDate, Pageable page);
}
