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
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionRequestInfoDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmittedSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class SubmissionReadService {

    private final ISubmissionRequestRepository requestRepository;

    private final SubmissionRequestMapper submissionRequestMapper;

    public SubmissionReadService(ISubmissionRequestRepository requestRepository,
                                 SubmissionRequestMapper submissionRequestMapper) {
        this.requestRepository = requestRepository;
        this.submissionRequestMapper = submissionRequestMapper;
    }

    public SubmissionRequestInfoDto retrieveRequestStatusInfo(String correlationId) {
        return requestRepository.findSubmissionRequestByCorrelationId(correlationId)
                                .map(submissionRequestMapper::convertToSubmissionRequestInfoDto)
                                .orElse(null);
    }

    public Page<SubmittedSearchResponseDto> retrieveSubmittedRequestsByCriteria(SearchSubmissionRequestParameters searchCriterion,
                                                                                Pageable page) {
        Page<SubmissionRequest> submissionPage = requestRepository.findAll(new SubmissionRequestSpecificationBuilder().withParameters(
            searchCriterion).build(), page);
        return new PageImpl<>(submissionPage.stream()
                                            .map(submissionRequestMapper::convertToSubmittedSearchResponseDto)
                                            .toList(), submissionPage.getPageable(), submissionPage.getTotalElements());
    }

}
