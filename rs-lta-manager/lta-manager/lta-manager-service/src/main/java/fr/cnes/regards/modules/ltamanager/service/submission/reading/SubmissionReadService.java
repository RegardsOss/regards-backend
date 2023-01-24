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
package fr.cnes.regards.modules.ltamanager.service.submission.reading;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.dao.submission.SubmissionRequestSpecificationBuilder;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.mapping.SubmissionRequestMapper;
import fr.cnes.regards.modules.ltamanager.domain.submission.search.SearchSubmissionRequestParameters;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionRequestInfoDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmittedSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service class to search for {@link SubmissionRequest}s
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class SubmissionReadService {

    /**
     * Repository class for submission request
     */
    private final ISubmissionRequestRepository submissionRequestRepository;

    /**
     * Mapper class for submission request
     */
    private final SubmissionRequestMapper submissionRequestMapper;

    /**
     * Constructor
     *
     * @param requestRepository
     * @param submissionRequestMapper
     */
    public SubmissionReadService(ISubmissionRequestRepository requestRepository,
                                 SubmissionRequestMapper submissionRequestMapper) {
        this.submissionRequestRepository = requestRepository;
        this.submissionRequestMapper = submissionRequestMapper;
    }

    /**
     * Retrieve a request status info, when existing, using provided correlation id
     *
     * @param correlationId the correlation id
     * @return the request status info
     */
    public Optional<SubmissionRequestInfoDto> retrieveRequestStatusInfo(String correlationId) {
        return submissionRequestRepository.findSubmissionRequestByCorrelationId(correlationId)
                                          .map(submissionRequestMapper::convertToSubmissionRequestInfoDto);
    }

    public Page<SubmittedSearchResponseDto> retrieveSubmittedRequestsByCriteria(SearchSubmissionRequestParameters searchCriterion,
                                                                                Pageable page) {
        Page<SubmissionRequest> submissionPage = submissionRequestRepository.findAll(new SubmissionRequestSpecificationBuilder().withParameters(
            searchCriterion).build(), page);
        return new PageImpl<>(submissionPage.stream()
                                            .map(submissionRequestMapper::convertToSubmittedSearchResponseDto)
                                            .toList(), submissionPage.getPageable(), submissionPage.getTotalElements());
    }

    /**
     * Retrieve the submitted product, when existing, using provided correlation id.
     *
     * @param correlationId
     * @return the submitted product
     */
    public Optional<SubmissionRequestDto> findSubmissionRequestByCorrelationId(String correlationId) {

        Optional<SubmissionRequest> submissionRequest = submissionRequestRepository.findSubmissionRequestByCorrelationId(
            correlationId);
        if (submissionRequest.isPresent()) {
            return Optional.of(submissionRequest.get().getSubmittedProduct().getProduct());
        }
        return Optional.empty();
    }

}
