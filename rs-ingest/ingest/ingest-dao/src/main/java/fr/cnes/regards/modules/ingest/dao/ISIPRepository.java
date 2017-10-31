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
package fr.cnes.regards.modules.ingest.dao;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * {@link SIPEntity} repository
 *
 * @author Marc Sordi
 *
 */
@Repository
public interface ISIPRepository extends JpaRepository<SIPEntity, Long> {

    /**
     * Find last ingest SIP with specified SIP ID according to ingest date
     * @param sipId external SIP identifier
     * @return the latest registered SIP
     */
    SIPEntity findTopBySipIdOrderByIngestDateDesc(String sipId);

    /**
     * Find all SIP version of a sipId
     * @param sipId SIP_ID
     * @return all SIP versions of a sipId
     */
    Collection<SIPEntity> findAllBySipIdOrderByVersionAsc(String sipId);

    /**
     * Find all {@link SIPEntity}s by given {@link SIPState}
     * @param state {@link SIPState}
     * @return {@link SIPEntity}s
     */
    Collection<SIPEntity> findAllByState(SIPState state);

    /**
     * Check if SIP already ingested
     * @param checksum checksum
     * @return 0 or 1
     */
    Long countByChecksum(String checksum);

    /**
     * Get next version of the SIP identified by sipId
     * @param sipId SIP_ID
     * @return next version
     */
    default Integer getNextVersion(String sipId) {
        SIPEntity latest = findTopBySipIdOrderByIngestDateDesc(sipId);
        return latest == null ? 1 : latest.getVersion() + 1;
    }

    /**
     * Find all SIP version of a sipId
     * @param sipId SIP_ID
     * @return all SIP versions of a sipId
     */
    default Collection<SIPEntity> getAllVersions(String sipId) {
        return findAllBySipIdOrderByVersionAsc(sipId);
    }

    /**
     * Check if SIP is already ingested based on its checksum
     * @param checksum
     * @return
     */
    default Boolean isAlreadyIngested(String checksum) {
        return countByChecksum(checksum) == 1;
    }
}