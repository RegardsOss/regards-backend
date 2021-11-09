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
package fr.cnes.regards.framework.modules.tinyurl.dao;

import fr.cnes.regards.framework.modules.tinyurl.domain.TinyUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * JPA repository to manage {@link fr.cnes.regards.framework.modules.tinyurl.domain.TinyUrl}
 *
 * @author Marc SORDI
 */
@Repository
public interface TinyUrlRepository extends JpaRepository<TinyUrl, Long> {

    void deleteByUuid(String uuid);

    Optional<TinyUrl> findByUuid(String uuid);

    void deleteByExpirationDateLessThan(OffsetDateTime deadline);
}
