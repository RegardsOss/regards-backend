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
package fr.cnes.regards.modules.featureprovider.service;

import java.util.List;

import javax.validation.Valid;

import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.amqp.event.IRequestDeniedService;
import fr.cnes.regards.framework.amqp.event.IRequestValidation;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureReferenceCollection;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;

/**
 * Service for deleting Features
 * @author Kevin Marchois
 *
 */
public interface IFeatureExtractionService extends IRequestDeniedService, IRequestValidation {

    /**
     * Register {@link FeatureExtractionRequest}in database for further processing from incoming request events
     */
    RequestInfo<String> registerRequests(List<FeatureExtractionRequestEvent> events);

    /**
     * Schedule a job to process a batch of requests<br/>
     * @return number of scheduled {@link FeatureExtractionRequest}
     */
    int scheduleRequests();

    /**
     * Process batch of {@link FeatureExtractionRequest} during job
     * We will call referenced plugin to obtain a {@link Feature} and publish it to create it
     */
    void processRequests(List<FeatureExtractionRequest> requests);

    /**
     * Register {@link FeatureExtractionRequest} from a {@link FeatureReferenceCollection}
     * @param collection
     * @return {@link RequestInfo} contain request ids of granted/denied features
     */
    RequestInfo<String> registerRequests(@Valid FeatureReferenceCollection collection);

    /**
     * Request are not feature module property so we have to keep them in feature provider module in error
     */
    void handleDenied(List<FeatureRequestEvent> denied);

    /**
     * Request are now feature module property so we just delete them from feature provider module
     */
    void handleGranted(List<FeatureRequestEvent> granted);

    void handleProcessingUnexpectedException(List<FeatureExtractionRequest> featureExtractionRequests);

    /**
     * Find all {@link FeatureExtractionRequest}s
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @param page
     * @return {@link FeatureCreationRequest}s
     */
    RequestsPage<FeatureRequestDTO> findRequests(FeatureRequestsSelectionDTO selection, Pageable page);

    /**
     * Delete requests by selection
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @return {@link RequestHandledResponse}
     */
    RequestHandledResponse deleteRequests(FeatureRequestsSelectionDTO selection);

    /**
     * Retry requests by selection
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @return {@link RequestHandledResponse}
     */
    RequestHandledResponse retryRequests(FeatureRequestsSelectionDTO selection);
}
