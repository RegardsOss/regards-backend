/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service.job;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.dto.dto.ProcessDatasetDescriptionDto;
import fr.cnes.regards.modules.order.service.job.parameters.*;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import fr.cnes.regards.modules.order.service.processing.correlation.BatchSuborderCorrelationIdentifier;
import fr.cnes.regards.modules.order.service.processing.correlation.ExecutionCorrelationIdentifier;
import fr.cnes.regards.modules.order.service.processing.correlation.ProcessInputCorrelationIdentifier;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import fr.cnes.regards.modules.processing.order.*;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * This class allows to launch the execution for a processed dataset selection.
 * <p>
 * It must be launched after all the internal input files have been set as accessible,
 * and thus is launched by the {@link StorageFilesJob} at the end of its execution.
 *
 * @author Guillaume Andrieu
 */
public class ProcessExecutionJob extends AbstractJob<Void> {

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(ProcessExecutionJob.class);

    protected final OrderInputFileMetadataMapper mapper = new OrderInputFileMetadataMapper();

    @Autowired
    protected IOrderDataFileRepository orderDataFileRepository;

    @Autowired
    protected IProcessingRestClient processingClient;

    @Autowired
    protected IProcessingEventSender eventSender;

    @Autowired
    protected Gson gson;

    @Autowired
    protected IPublisher publisher;

    protected String tenant;

    protected String user;

    protected String userRole;

    protected BasketDatasetSelectionDescriptor dsSel;

    protected BatchSuborderCorrelationIdentifier batchCorrelationId;

    protected PProcessDTO processDesc;

    protected OrderProcessInfo processInfo;

    protected ProcessInputsPerFeature processInputDataFiles;

    protected List<OrderDataFile> processResultDataFiles;

    @Override
    public void run() {

        ProcessDatasetDescriptionDto processDatasetDescription = dsSel.getProcessDatasetDescription();
        PBatchRequest request = createBatchRequest(dsSel.getDatasetIpId(), processDatasetDescription);
        try {
            PBatchResponse batchResponse = createBatch(dsSel.getDsSelId(),
                                                       processDatasetDescription,
                                                       request,
                                                       processingClient);

            Scope scope = processInfo.getScope();

            switch (scope) {
                case FEATURE: {
                    HashMap.ofAll(processInputDataFiles.getFilesPerFeature()).forEach((feature, inputs) -> {
                        sendExecRequest(createExecRequestEvent(gson.toJson(new ExecutionCorrelationIdentifier(user,
                                                                                                              Option.of(
                                                                                                                  feature.getIpId()))),
                                                               batchResponse.getBatchId(),
                                                               createInputsForFeatureWithCorrelationId(feature,
                                                                                                       inputs)));
                    });
                    break;
                }
                case SUBORDER: {
                    sendExecRequest(createExecRequestEvent(gson.toJson(new ExecutionCorrelationIdentifier(user,
                                                                                                          Option.none())),
                                                           batchResponse.getBatchId(),
                                                           createAllInputsWithCorrelationId()));
                    break;
                }
                default:
                    throw new NotImplementedException("A Scope case implementation is missing in " + this.getClass()
                                                                                                         .getName());
            }
        } catch (NotImplementedException e) {
            UUID errorCorrelationId = UUID.randomUUID();
            STATIC_LOGGER.error("errorCid={} dFailure to create executions for suborder {}",
                                errorCorrelationId,
                                batchCorrelationId.repr(),
                                e);
            // The error management is already coded in the ProcessingExecutionResultEventHandler
            // we only need to notify that a suborder execution has failed.
            PExecutionResultEvent event = new PExecutionResultEvent(errorCorrelationId,
                                                                    // unused by the handler in case of failure
                                                                    gson.toJson(new ExecutionCorrelationIdentifier(user,
                                                                                                                   Option.none())),
                                                                    errorCorrelationId,
                                                                    // unused by the handler in case of failure
                                                                    batchCorrelationId.repr(),
                                                                    processDesc.getProcessId(),
                                                                    processDesc.getProcessInfo(),
                                                                    ExecutionStatus.FAILURE,
                                                                    List.empty(),
                                                                    List.of("Executions could not be created errorCid="
                                                                            + errorCorrelationId));
            publisher.publish(event);
        } finally {
            advanceCompletion();
        }
    }

    private List<PInputFile> createInputsForFeatureWithCorrelationId(ProcessOutputFeatureDesc feature,
                                                                     java.util.List<OrderDataFile> inputs) {
        return List.ofAll(inputs)
                   .map(orderDataFile -> createInputWithCorrelationId(orderDataFile,
                                                                      feature.getIpId(),
                                                                      ProcessInputCorrelationIdentifier.repr(
                                                                          batchCorrelationId,
                                                                          feature.getIpId(),
                                                                          orderDataFile.getFilename())));
    }

    private List<PInputFile> createAllInputsWithCorrelationId() {
        List<Tuple2<ProcessOutputFeatureDesc, OrderDataFile>> featureAndFileTuples = HashMap.ofAll(processInputDataFiles.getFilesPerFeature())
                                                                                            .toList()
                                                                                            .flatMap(
                                                                                                featureDescAndDataFiles -> List.ofAll(
                                                                                                                                   featureDescAndDataFiles._2())
                                                                                                                               .map(
                                                                                                                                   dataFile -> Tuple.of(
                                                                                                                                       featureDescAndDataFiles._1,
                                                                                                                                       dataFile)));
        return featureAndFileTuples.map(featureAndFile -> createInputWithCorrelationId(featureAndFile._2,
                                                                                       featureAndFile._1.getIpId(),
                                                                                       ProcessInputCorrelationIdentifier.repr(
                                                                                           batchCorrelationId,
                                                                                           featureAndFile._1.getIpId(),
                                                                                           featureAndFile._2.getFilename())));
    }

    private void sendExecRequest(PExecutionRequestEvent event) {
        this.eventSender.sendProcessingRequest(event)
                        .onFailure(t -> STATIC_LOGGER.error("Failed to send execution request event {}, {}",
                                                            event,
                                                            t.getMessage(),
                                                            t));
    }

    protected PBatchRequest createBatchRequest(String datasetIpid,
                                               ProcessDatasetDescriptionDto processDatasetDescription) {
        FileSetStatistics stats = createBatchStats(datasetIpid);
        return new PBatchRequest(batchCorrelationId.repr(),
                                 processDesc.getProcessId(),
                                 tenant,
                                 user,
                                 userRole,
                                 HashMap.ofAll(processDatasetDescription.getParameters()),
                                 HashMap.of(datasetIpid, stats));
    }

    protected FileSetStatistics createBatchStats(String datasetIpid) {
        Long totalInputSizes = List.ofAll(processInputDataFiles.getFilesPerFeature().values())
                                   .flatMap(Function.identity())
                                   .map(OrderDataFile::getFilesize)
                                   .fold(0L, Long::sum);
        return new FileSetStatistics(datasetIpid, 1, totalInputSizes);
    }

    protected PExecutionRequestEvent createExecRequestEvent(String correlationId,
                                                            UUID batchId,
                                                            List<PInputFile> inputFiles) {
        return new PExecutionRequestEvent(correlationId, batchId, inputFiles);
    }

    protected PInputFile createInputWithCorrelationId(OrderDataFile df, String featureIpId, String inputCorrelationId) {
        URL fileUrl = Try.of(() -> new URL(df.getUrl())).getOrNull();
        UniformResourceName featureIdUrn = UniformResourceName.fromString(featureIpId);
        io.vavr.collection.Map<String, String> metadataMap = mapper.toMap(new OrderInputFileMetadata(!df.isReference(),
                                                                                                     featureIdUrn,
                                                                                                     null));
        return new PInputFile("",
                              // unused parameter name
                              featureIpId + "/" + df.getFilename(),
                              df.getMimeType().toString(),
                              fileUrl,
                              df.getFilesize(),
                              df.getChecksum(),
                              df.getFilename(),
                              metadataMap,
                              inputCorrelationId);
    }

    protected PBatchResponse createBatch(Long dsSelId,
                                         ProcessDatasetDescriptionDto processDatasetDescription,
                                         PBatchRequest request,
                                         IProcessingRestClient processingClient) {
        try {
            FeignSecurityManager.asUser(user, userRole);
            ResponseEntity<PBatchResponse> batchResponse = processingClient.createBatch(request);
            FeignSecurityManager.reset();

            if (!batchResponse.getStatusCode().is2xxSuccessful()) {
                throw new CouldNotCreateBatchException(jobInfoId,
                                                       dsSelId,
                                                       processDatasetDescription.getProcessBusinessId(),
                                                       batchResponse);
            }

            return batchResponse.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            STATIC_LOGGER.error(e.getMessage(), e);
            throw new CouldNotCreateBatchException(jobInfoId,
                                                   dsSelId,
                                                   processDatasetDescription.getProcessBusinessId());
        }
    }

    @Override
    public int getCompletionCount() {
        return 1;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        for (JobParameter param : parameters.values()) {
            if (ProcessInputsPerFeatureJobParameter.isCompatible(param)) {
                processInputDataFiles = param.getValue();
            } else if (ProcessOutputFilesJobParameter.isCompatible(param)) {
                List<Long> odfIds = List.of(param.getValue());
                processResultDataFiles = List.ofAll(orderDataFileRepository.findAllById(odfIds));
            } else if (TenantJobParameter.isCompatible(param)) {
                tenant = param.getValue();
            } else if (UserJobParameter.isCompatible(param)) {
                user = param.getValue();
            } else if (UserRoleJobParameter.isCompatible(param)) {
                userRole = param.getValue();
            } else if (ProcessDTOJobParameter.isCompatible(param)) {
                processDesc = param.getValue();
                processInfo = new OrderProcessInfoMapper().fromMap(processDesc.getProcessInfo())
                                                          .getOrElseThrow(() -> new JobParameterInvalidException(
                                                              "Cannot find processInfo from processDesc"));
            } else if (ProcessBatchCorrelationIdJobParameter.isCompatible(param)) {
                batchCorrelationId = BatchSuborderCorrelationIdentifier.parse(param.getValue())
                                                                       .getOrElseThrow(() -> new JobParameterInvalidException(
                                                                           "Cannot parse batchCorrelationId"));
            } else if (BasketDatasetSelectionJobParameter.isCompatible(param)) {
                dsSel = param.getValue();
            }
        }
        checkForMissingParameters();
    }

    private void checkForMissingParameters() throws JobParameterMissingException {
        ArrayList<String> missingParams = new ArrayList<>();
        checkMissing(missingParams, tenant, TenantJobParameter.NAME);
        checkMissing(missingParams, user, UserJobParameter.NAME);
        checkMissing(missingParams, userRole, UserRoleJobParameter.NAME);
        checkMissing(missingParams, processInputDataFiles, ProcessInputsPerFeatureJobParameter.NAME);
        checkMissing(missingParams, processDesc, ProcessDTOJobParameter.NAME);
        checkMissing(missingParams, processResultDataFiles, ProcessOutputFilesJobParameter.NAME);
        checkMissing(missingParams, batchCorrelationId, ProcessBatchCorrelationIdJobParameter.NAME);
        checkMissing(missingParams, dsSel, BasketDatasetSelectionJobParameter.NAME);
        if (!missingParams.isEmpty()) {
            throw new JobParameterMissingException("Missing parameters: " + StringUtils.join(missingParams, ", "));
        }
    }

    private void checkMissing(ArrayList<String> missingParams, Object field, String name) {
        if (field == null) {
            missingParams.add(name);
        }
    }

    public static class CouldNotCreateBatchException extends RuntimeException {

        public CouldNotCreateBatchException(UUID jobInfoId,
                                            Long dsSelId,
                                            UUID processBusinessId,
                                            ResponseEntity<PBatchResponse> batchResponse) {
            super(String.format("jobInfo:%s dsSel:%d Could not create batch, response status is %s",
                                jobInfoId,
                                dsSelId,
                                batchResponse.getStatusCode()));
        }

        public CouldNotCreateBatchException(UUID jobInfoId, Long dsSelId, UUID processBusinessId) {
            super(String.format("jobInfo:%s dsSel:%d Could not create batch, response status is %s",
                                jobInfoId,
                                dsSelId,
                                HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

}
