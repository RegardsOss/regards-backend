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
package fr.cnes.regards.modules.ltamanager.service.submission.creation;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ltamanager.amqp.output.LtaWorkerRequestDtoEvent;
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.settings.DatatypeParameter;
import fr.cnes.regards.modules.ltamanager.domain.settings.LtaSettingsException;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionStatus;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmittedProduct;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.settings.LtaSettingService;
import fr.cnes.regards.modules.workermanager.dto.events.in.RequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service to handle {@link SubmissionRequest}s
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class SubmissionCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionCreateService.class);

    public static final String LTA_CONTENT_TYPE = "lta-request";

    private final ISubmissionRequestRepository requestRepository;

    private final LtaSettingService settingService;

    private final CreateDatatypeService createDatatypeService;

    private final IPublisher publisher;

    private final IRuntimeTenantResolver tenantResolver;

    public SubmissionCreateService(ISubmissionRequestRepository requestRepository,
                                   LtaSettingService settingService,
                                   CreateDatatypeService createDatatypeService,
                                   IPublisher publisher,
                                   IRuntimeTenantResolver tenantResolver) {
        this.requestRepository = requestRepository;
        this.settingService = settingService;
        this.createDatatypeService = createDatatypeService;
        this.publisher = publisher;
        this.tenantResolver = tenantResolver;
    }

    /**
     * See {@link this#handleSubmissionRequestsCreation(List)}
     */
    public SubmissionResponseDtoEvent handleSubmissionRequestCreation(SubmissionRequestDto requestDto) {
        return handleSubmissionRequestsCreation(List.of(requestDto)).get(0);
    }

    /**
     * Create and save {@link SubmissionRequest}s from {@link SubmissionRequestDto}s.
     * A submission request is saved in the database only if all checks are passed, i.e.,
     * <ul>
     * <li>the owner of the request is present</li>
     * <li>the datatype associated to the submission request dto exists in the lta datatypes configuration</li>
     * <li>the storePath can be generated successfully either from the request dto or the configuration</li>
     * </ul>
     * In case of error, the response indicates the reason why it could not be saved. <br/>
     * In case of success, {@link SubmissionResponseDtoEvent}s are built and {@link LtaWorkerRequestDtoEvent} events are
     * sent to the worker manager microservice.
     *
     * @return {@link SubmissionResponseDtoEvent}s containing the ids and the status of the submission requests created or
     * rejected.
     */
    public List<SubmissionResponseDtoEvent> handleSubmissionRequestsCreation(List<? extends SubmissionRequestDto> requestDtos) {
        List<SubmissionRequest> submissionRequestsToSave = new ArrayList<>();
        List<SubmissionResponseDtoEvent> responses = new ArrayList<>();
        // Get lta setting configuration from database
        Set<DynamicTenantSetting> settings = settingService.retrieve();

        // 1) Prepare requests : create submission requests from requests dtos
        OffsetDateTime currentDateTime = OffsetDateTime.now();
        for (SubmissionRequestDto requestDto : requestDtos) {
            LOGGER.debug("---> Processing SubmissionRequestDto with correlationId \"{}\"", requestDto.getCorrelationId());
            // Create a submission request only from a valid submission request dto
            try {
                DatatypeParameter datatypeConfig = createDatatypeService.createValidConfiguration(requestDto,
                                                                                                  settingService.getDatypesConfig(
                                                                                                      settings),
                                                                                                  currentDateTime);
                SubmissionRequest submissionRequest = buildSubmissionRequest(requestDto,
                                                                             datatypeConfig,
                                                                             currentDateTime);
                submissionRequestsToSave.add(submissionRequest);
            } catch (LtaSettingsException e) {
                responses.add(buildErrorResponse(requestDto.getCorrelationId(), requestDto.getProductId(), e));
            }
        }
        // 2) Handle submission requests in success
        if (!submissionRequestsToSave.isEmpty()) {
            handleSuccess(submissionRequestsToSave, responses, settings);
        }
        // 3) Return responses from requests in success or in error
        return responses;
    }

    private SubmissionRequest buildSubmissionRequest(SubmissionRequestDto requestDto,
                                                     DatatypeParameter datatypeConfig,
                                                     OffsetDateTime currentDateTime) {
        String session = requestDto.getSession();
        String owner = requestDto.getOwner();

        if (session == null) {
            // if session is not provided, replace it with <owner.name>-<YYYYMMdd>
            session = String.format("%s-%s",
                                    owner.split("@")[0],
                                    currentDateTime.format(DateTimeFormatter.BASIC_ISO_DATE));
        }

        return new SubmissionRequest(requestDto.getCorrelationId(),
                                     owner,
                                     session,
                                     requestDto.isReplaceMode(),
                                     new SubmissionStatus(currentDateTime,
                                                          currentDateTime,
                                                          SubmissionRequestState.VALIDATED,
                                                          null),
                                     new SubmittedProduct(requestDto.getDatatype(),
                                                          datatypeConfig.getModel(),
                                                          Paths.get(datatypeConfig.getStorePath()),
                                                          requestDto),
                                     requestDto.getOriginUrn());
    }

    // -------------------------------
    // ------- BUILD RESPONSES -------
    // -------------------------------

    private void handleSuccess(List<SubmissionRequest> submissionRequestsToSave,
                               List<SubmissionResponseDtoEvent> responses,
                               Set<DynamicTenantSetting> settings) {
        // save requests in success in database
        List<SubmissionRequest> savedRequests = requestRepository.saveAll(submissionRequestsToSave);
        // handle responses
        List<LtaWorkerRequestDtoEvent> workerRequests = new ArrayList<>();
        for (SubmissionRequest successRequest : savedRequests) {
            responses.add(buildSuccessResponse(successRequest, settings));
            workerRequests.add(buildWorkerRequest(successRequest, settings));
        }
        this.publisher.publish(workerRequests, "regards.broadcast." + RequestEvent.class.getName(), Optional.empty());
    }

    private SubmissionResponseDtoEvent buildSuccessResponse(SubmissionRequest requestSaved,
                                                            Set<DynamicTenantSetting> settings) {
        LOGGER.debug("SubmissionRequest was successfully created from SubmissionRequestDto with correlationId \"{}\"",
                     requestSaved.getCorrelationId());

        return new SubmissionResponseDtoEvent(requestSaved.getCorrelationId(),
                                              SubmissionResponseStatus.GRANTED,
                                              requestSaved.getProduct().getProductId(),
                                              requestSaved.getCreationDate()
                                                          .plusSeconds(settingService.getRequestExpiresInHoursConfig(
                                                              settings)),
                                              requestSaved.getSession(),
                                              requestSaved.getMessage());
    }

    private LtaWorkerRequestDtoEvent buildWorkerRequest(SubmissionRequest requestSaved,
                                                        Set<DynamicTenantSetting> settings) {
        Path datatypeStorePath = requestSaved.getStorePath();
        // note: datatypeStorePath is set with raw config storePath if the submissionRequestDto already contains a
        // storePath. No placeholders replacement is done.
        if (datatypeStorePath.toString().equals(requestSaved.getProduct().getStorePath())) {
            try {
                // note : path has already been verified, no reason to throw an exception
                datatypeStorePath = Paths.get(settingService.getDatypesConfig(settings)
                                                            .get(requestSaved.getProduct().getDatatype())
                                                            .getStorePath());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Store path could not be retrieved from configuration.", e);
            }
        }

        LtaWorkerRequestDtoEvent workerRequest = new LtaWorkerRequestDtoEvent(settingService.getStorageConfig(settings),
                                                                              datatypeStorePath,
                                                                              requestSaved.getModel(),
                                                                              requestSaved.getProduct(),
                                                                              requestSaved.isReplaceMode());
        workerRequest.setWorkerHeaders(LTA_CONTENT_TYPE,
                                       this.tenantResolver.getTenant(),
                                       requestSaved.getCorrelationId(),
                                       requestSaved.getOwner(),
                                       requestSaved.getSession());

        return workerRequest;
    }

    private SubmissionResponseDtoEvent buildErrorResponse(String correlationId,
                                                          String productId,
                                                          LtaSettingsException exception) {
        LOGGER.error("SubmissionRequestDto with correlationId \"{}\" and productId \"{}\" was rejected.",
                     correlationId,
                     productId,
                     exception);
        return new SubmissionResponseDtoEvent(correlationId, SubmissionResponseStatus.DENIED, productId, exception.getMessage());
    }

}
