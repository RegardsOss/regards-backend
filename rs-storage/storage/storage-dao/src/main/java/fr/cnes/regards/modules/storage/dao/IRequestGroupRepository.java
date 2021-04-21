/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storage.domain.database.request.RequestGroup;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;

/**
 * JPA Repository to handle access to {@link RequestGroup} entities.
 *
 * @author Sébatien Binda
 *
 */
public interface IRequestGroupRepository extends JpaRepository<RequestGroup, String> {

    Page<RequestGroup> findByType(FileRequestType type, Pageable page);

    Page<RequestGroup> findAllByOrderByCreationDateAsc(Pageable page);

}
