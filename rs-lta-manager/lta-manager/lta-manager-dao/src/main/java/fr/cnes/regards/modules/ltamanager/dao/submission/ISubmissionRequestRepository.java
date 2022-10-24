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
package fr.cnes.regards.modules.ltamanager.dao.submission;

import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link SubmissionRequest}
 *
 * @author Iliana Ghazali
 **/
@Repository
public interface ISubmissionRequestRepository extends JpaRepository<SubmissionRequest, Long> {

    // ------------
    // -- SEARCH --
    // ------------
    Page<SubmissionRequest> findAll(Specification<SubmissionRequest> specifications, Pageable page);

    @Query("select req.requestId from SubmissionRequest req where req.requestId in :ids and req.submissionStatus"
           + ".status in :states")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<String> findIdsByRequestIdInAndStatesIn(@Param("ids") List<String> requestIds,
                                                 @Param("states") List<SubmissionRequestState> allowedStatesToUpdate);

    @Query("select req.requestId from SubmissionRequest req where req.requestId in :ids")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<String> findIdsByRequestIdIn(@Param("ids") List<String> requestIds);

    boolean existsBySubmissionStatusCreationDateLessThanEqual(OffsetDateTime expiredDate);

    Optional<SubmissionRequest> findSubmissionRequestByRequestId(@Param("id") String requestId);

    @Query(value = "SELECT DISTINCT status FROM {h-schema}t_submission_requests WHERE session = ?1 AND owner = ?2",
        nativeQuery = true)
    List<String> findStatesBySessionAndOwner(String session, String owner);
 
    Page<SubmissionRequest> findBySessionAndOwner(String session, String owner, Pageable page);

    // ------------
    // -- UPDATE --
    // ------------
    @Modifying
    @Query("update SubmissionRequest req set req.submissionStatus.status = :state, req.submissionStatus.statusDate = "
           + ":date, req.submissionStatus.message = :message where req.requestId = :id")
    void updateRequestState(@Param("id") String requestId,
                            @Param("state") SubmissionRequestState state,
                            @Param("message") String message,
                            @Param("date") OffsetDateTime statusDate);

    // ------------
    // -- DELETE --
    // ------------
    @Modifying
    @Query(value = "delete from SubmissionRequest req where req.submissionStatus.creationDate <= :expired")
    void deleteByExpiredDates(@Param("expired") OffsetDateTime expiredDate);
}
