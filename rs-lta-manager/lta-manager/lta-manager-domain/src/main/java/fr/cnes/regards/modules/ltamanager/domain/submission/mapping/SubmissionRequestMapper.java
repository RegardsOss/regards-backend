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
package fr.cnes.regards.modules.ltamanager.domain.submission.mapping;

import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionRequestInfoDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmittedSearchResponseDto;
import org.mapstruct.Mapper;

/**
 * All mappers from {@link SubmissionRequest}
 *
 * @author Iliana Ghazali
 **/
@Mapper(componentModel = "spring")
public interface SubmissionRequestMapper {

    /**
     * Map between {@link SubmissionRequest} and {@link SubmittedSearchResponseDto}
     */
    SubmittedSearchResponseDto convertToSubmittedSearchResponseDto(SubmissionRequest submissionRequest);

    /**
     * Map between {@link SubmissionRequest} and {@link SubmissionRequestInfoDto}
     */
    default SubmissionRequestInfoDto convertToSubmissionRequestInfoDto(SubmissionRequest submissionRequest) {
        return new SubmissionRequestInfoDto(submissionRequest.getCorrelationId(),
                                            submissionRequest.getProduct().getProductId(),
                                            submissionRequest.getStatus(),
                                            submissionRequest.getStatusDate(),
                                            submissionRequest.getSession(),
                                            submissionRequest.getMessage());
    }
}
