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
package fr.cnes.regards.modules.ingest.dao;

import fr.cnes.regards.modules.ingest.domain.sip.ISipIdAndVersion;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link SIPEntity} repository
 *
 * @author Marc Sordi
 */
public interface ISIPRepository extends JpaRepository<SIPEntity, Long>, JpaSpecificationExecutor<SIPEntity> {

    @Override
    Optional<SIPEntity> findById(Long id);

    List<SIPEntity> findByProviderId(String providerId);

    List<ISipIdAndVersion> findAllLightByProviderId(String providerId);

    /**
     * Find last ingest SIP with specified SIP ID according to ingest date
     *
     * @param providerId external SIP identifier
     * @return the latest registered SIP
     */
    default SIPEntity findTopByProviderIdOrderByCreationDateDesc(String providerId) {
        return findByProviderId(providerId).stream()
                                           .sorted(Comparator.comparing(SIPEntity::getCreationDate).reversed())
                                           .findFirst()
                                           .orElse(null);
    }

    /**
     * Find all SIP version of a provider id
     *
     * @param providerId provider id
     * @return all SIP versions of this provider id
     */
    default Collection<SIPEntity> findAllByProviderIdOrderByVersionAsc(String providerId) {
        return findByProviderId(providerId).stream()
                                           .sorted(Comparator.comparing(SIPEntity::getCreationDate))
                                           .collect(Collectors.toList());
    }

    long countByProviderId(String providerId);

    /**
     * Find one {@link SIPEntity} by its unique ipId
     */
    Optional<SIPEntity> findOneBySipId(String sipId);

    /**
     * Check if SIP already ingested
     *
     * @param checksum checksum
     * @return 0 or 1
     */
    Long countByChecksum(String checksum);

    long countByState(SIPState sipState);

    /**
     * Get next version of the SIP identified by provider id
     *
     * @param providerId provider id
     * @return next version
     */
    default Integer getNextVersion(String providerId) {
        List<Integer> versions = findVersionByProviderId(providerId);
        return versions == null ? 1 : (versions.stream().mapToInt(v -> v).max().orElse(0)) + 1;
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
        return new PageImpl<>(loaded,
                              PageRequest.of(sips.getNumber(), sips.getSize(), sips.getSort()),
                              sips.getTotalElements());
    }

    List<SIPEntity> findAllByIdIn(List<Long> ingestProcChainIds, Sort sort);

    /**
     * Retrieve partial SIP avoiding mutating SIP state that may be mutated on other thread.
     * <br/>
     * Note that due to deferred constraint, we can have two versions of a SIP with last flag at a moment.
     */
    default List<ISipIdAndVersion> findByProviderIdAndLast(String providerId, boolean last) {
        return findAllLightByProviderId(providerId).stream()
                                                   .filter(s -> s.getLast() == last)
                                                   .collect(Collectors.toList());
    }

    @Query(value = "select version from SIPEntity where providerId = :providerId")
    List<Integer> findVersionByProviderId(@Param("providerId") String providerId);

    @Modifying
    @Query(value = "UPDATE SIPEntity SET last = :last WHERE id = :id")
    int updateLast(@Param("id") Long id, @Param("last") boolean last);

    /**
     * Remove rawsip value of SIP where lowerDate < SIP.lastUpdate <= upperDate
     */
    @Modifying
    @Query(value = "UPDATE SIPEntity SET sip = null "
                   + "WHERE state IN (:states) "
                   + "AND (lastUpdate > :lowerDate AND lastUpdate <= :upperDate)")
    int updateRawSIPOutdatedForLastUpdateBetween(@Param("states") Collection<SIPState> states,
                                                 @Param("lowerDate") OffsetDateTime lowerDate,
                                                 @Param("upperDate") OffsetDateTime upperDate);

    default int removeSIPContent(OffsetDateTime lowerDate, OffsetDateTime upperDate) {
        return updateRawSIPOutdatedForLastUpdateBetween(List.of(SIPState.STORED, SIPState.DELETED),
                                                        lowerDate,
                                                        upperDate);
    }

    List<SIPEntity> findAllByOrderByLastUpdateAsc();
}