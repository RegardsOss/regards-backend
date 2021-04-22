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
package fr.cnes.regards.modules.ingest.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.ingest.domain.sip.ISipIdAndVersion;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;

/**
 * {@link SIPEntity} repository
 *
 * @author Marc Sordi
 *
 */
public interface ISIPRepository extends JpaRepository<SIPEntity, Long>, JpaSpecificationExecutor<SIPEntity> {

    @Override
    Optional<SIPEntity> findById(Long id);

    /**
     * Find last ingest SIP with specified SIP ID according to ingest date
     * @param providerId external SIP identifier
     * @return the latest registered SIP
     */
    SIPEntity findTopByProviderIdOrderByCreationDateDesc(String providerId);

    /**
     * Find all SIP version of a provider id
     * @param providerId provider id
     * @return all SIP versions of this provider id
     */
    Collection<SIPEntity> findAllByProviderIdOrderByVersionAsc(String providerId);

    /**
     * Count SIPEntity with given providerId that have one the given states
     */
    long countByProviderIdAndStateIn(String providerId, Collection<SIPState> states);

    long countByProviderId(String providerId);

    default long countByProviderIdAndStateIn(String providerId, SIPState... states) {
        return countByProviderIdAndStateIn(providerId, Arrays.asList(states));
    }

    /**
     * Find one {@link SIPEntity} by its unique ipId
     */
    Optional<SIPEntity> findOneBySipId(String sipId);

    /**
     * Check if SIP already ingested
     * @param checksum checksum
     * @return 0 or 1
     */
    Long countByChecksum(String checksum);

    long countByState(SIPState sipState);

    /**
     * Get next version of the SIP identified by provider id
     * @param providerId provider id
     * @return next version
     */
    default Integer getNextVersion(String providerId) {
        SIPEntity latest = findTopByProviderIdOrderByCreationDateDesc(providerId);
        return latest == null ? 1 : latest.getVersion() + 1;
    }

    /**
     * Check if SIP is already ingested based on its checksum
     */
    default boolean isAlreadyIngested(String checksum) {
        return countByChecksum(checksum) != 0;
    }

    default Page<SIPEntity> loadAll(Specification<SIPEntity> search, Pageable pageable) {
        // as a Specification is used to constrain the page, we cannot simply ask for ids with a query
        // to mimic that, we are querying without any entity graph to extract ids
        Page<SIPEntity> sips = findAll(search, pageable);
        List<Long> sipIds = sips.stream().map(p -> p.getId()).collect(Collectors.toList());
        // now that we have the ids, lets load the products and keep the same sort
        List<SIPEntity> loaded = findAllByIdIn(sipIds, pageable.getSort());
        return new PageImpl<>(loaded, PageRequest.of(sips.getNumber(), sips.getSize(), sips.getSort()),
                sips.getTotalElements());
    }

    List<SIPEntity> findAllByIdIn(List<Long> ingestProcChainIds, Sort sort);

    /**
     * Retrieve partial SIP avoiding mutating SIP state that may be mutated on other thread.
     * <br/>
     * Note that due to deferred constraint, we can have two versions of a SIP with last flag at a moment.
     */
    List<ISipIdAndVersion> findByProviderIdAndLast(String providerId, boolean last);

    @Modifying
    @Query(value = "UPDATE SIPEntity SET last = :last WHERE id = :id")
    int updateLast(@Param("id") Long id, @Param("last") boolean last);
}