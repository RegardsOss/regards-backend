package fr.cnes.regards.modules.order.service.job;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.order.dao.IBasketDatasetSelectionRepository;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.order.service.job.parameters.*;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import fr.cnes.regards.modules.order.service.processing.correlation.BatchSuborderCorrelationIdentifier;
import fr.cnes.regards.modules.order.service.processing.correlation.ProcessInputCorrelationIdentifier;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;
import fr.cnes.regards.modules.processing.order.OrderProcessInfoMapper;
import fr.cnes.regards.modules.processing.order.Scope;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * This class allows to launch the execution for a processed dataset selection.
 * <p>
 * It must be launched after all the internal input files have been set as accessible.
 */
public class ProcessExecutionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecutionJob.class);

    @Autowired
    protected IProcessingRestClient processingClient;

    @Autowired
    protected IProcessingEventSender eventSender;

    @Autowired
    protected IBasketDatasetSelectionRepository dsSelRepository;

    protected String tenant;

    protected String user;

    protected String userRole;

    protected BatchSuborderCorrelationIdentifier batchCorrelationId;

    protected PProcessDTO processDesc;

    protected OrderProcessInfo processInfo;

    protected ProcessInputsPerFeature processInputDataFiles;

    protected List<OrderDataFile> processResultDataFiles;

    @Override
    public void run() {
        try {
            BasketDatasetSelection dsSel = findDatasetSelectionById(dsSelRepository, batchCorrelationId.getDsSelId());

            PBatchResponse batchResponse = createBatch(dsSel, processingClient);

            Scope scope = processInfo.getScope();

            switch (scope) {
                case ITEM: {
                    HashMap.ofAll(processInputDataFiles.getFilesPerFeature())
                            .forEach((feature, inputs) -> {
                                sendExecRequest(createExecRequestEvent(
                                        feature.getIpId(),
                                        batchResponse.getBatchId(),
                                        createInputsForFeatureWithCorrelationId(feature, inputs)
                                ));
                            });
                    break;
                }
                case SUBORDER: {
                    sendExecRequest(createExecRequestEvent(
                            batchCorrelationId.repr(),
                            batchResponse.getBatchId(),
                            createAllInputsWithCorrelationId()
                    ));
                    break;
                }
                default:
                    throw new NotImplementedException("A Scope case implementation is missing in " + this.getClass().getName());
            }
        }
        finally {
            advanceCompletion();
        }
    }

    private List<PInputFile> createInputsForFeatureWithCorrelationId(ProcessOutputFeatureDesc feature, java.util.List<OrderDataFile> inputs) {
        return List.ofAll(inputs)
            .map(orderDataFile -> createInputWithCorrelationId(
                orderDataFile,
                ProcessInputCorrelationIdentifier.repr(batchCorrelationId, feature.getIpId(), orderDataFile.getFilename())
            ));
    }

    private List<PInputFile> createAllInputsWithCorrelationId() {
        List<Tuple2<ProcessOutputFeatureDesc, OrderDataFile>> featureAndFileTuples =
                HashMap.ofAll(processInputDataFiles.getFilesPerFeature())
                .toList()
                .flatMap(featureDescAndDataFiles -> List.ofAll(featureDescAndDataFiles._2())
                        .map(dataFile -> Tuple.of(featureDescAndDataFiles._1, dataFile)));
        return featureAndFileTuples
            .map(featureAndFile -> createInputWithCorrelationId(
                    featureAndFile._2,
                    ProcessInputCorrelationIdentifier.repr(batchCorrelationId, featureAndFile._1.getIpId(), featureAndFile._2.getFilename())
            ));
    }

    private void sendExecRequest(PExecutionRequestEvent event) {
        this.eventSender.sendProcessingRequest(event)
            .onFailure(t -> LOGGER.error("Failed to send execution request event {}, {}", event, t.getMessage(), t));
    }

    private List<OrderDataFile> allInputFilesIn(ProcessInputsPerFeature processInputDataFiles) {
        return HashMap.ofAll(processInputDataFiles.getFilesPerFeature())
                .values()
                .flatMap(Function.identity())
                .collect(List.collector());
    }

    protected BasketDatasetSelection findDatasetSelectionById(IBasketDatasetSelectionRepository dsSelRepository, Long dsSelId) {
        return dsSelRepository.findById(dsSelId)
                .orElseThrow(() -> new DatasetSelectionNotFoundException(jobInfoId, dsSelId));
    }

    protected PBatchRequest createBatchRequest(
            BasketDatasetSelection dsSel,
            ProcessDatasetDescription processDatasetDescription
    ) {
        FileSetStatistics stats = createBatchStats(dsSel);
        return new PBatchRequest(
                batchCorrelationId.repr(),
                processDesc.getProcessId(),
                tenant, user, userRole,
                HashMap.ofAll(processDatasetDescription.getParameters()),
                HashMap.of(dsSel.getDatasetIpid(), stats)
        );
    }

    protected FileSetStatistics createBatchStats(BasketDatasetSelection dsSel) {
        Long totalInputSizes = List.ofAll(processInputDataFiles.getFilesPerFeature().values())
                .flatMap(Function.identity())
                .map(OrderDataFile::getFilesize)
                .fold(0L, Long::sum);
        return new FileSetStatistics(dsSel.getDatasetIpid(), 1, totalInputSizes);
    }

    protected PExecutionRequestEvent createExecRequestEvent(String correlationId, UUID batchId, List<PInputFile> inputFiles) {
        return new PExecutionRequestEvent(correlationId, batchId, inputFiles);
    }

    protected PInputFile createInputWithCorrelationId(OrderDataFile df, String inputCorrelationId) {
        URL fileUrl = Try.of(() -> new URL(df.getUrl())).getOrNull();
        return new PInputFile(
                "", // unused parameter name
                df.getFilename(),
                df.getMimeType().toString(),
                fileUrl,
                df.getFilesize(),
                df.getChecksum(),
                !df.isReference(),
                inputCorrelationId
        );
    }

    protected PBatchResponse createBatch(
            BasketDatasetSelection dsSel,
            IProcessingRestClient processingClient
    ) {
        ProcessDatasetDescription processDatasetDescription = dsSel.getProcessDatasetDescription();
        PBatchRequest request = createBatchRequest(dsSel, processDatasetDescription);
        ResponseEntity<PBatchResponse> batchResponse = processingClient.createBatch(request);

        if (!batchResponse.getStatusCode().is2xxSuccessful()) {
            throw new CouldNotCreateBatchException(jobInfoId, dsSel.getId(), processDatasetDescription.getProcessBusinessId(), batchResponse);
        }

        return batchResponse.getBody();
    }

    @Override
    public int getCompletionCount() {
        return 1;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters) throws JobParameterMissingException, JobParameterInvalidException {
        for (JobParameter param : parameters.values()) {
            if (ProcessInputsPerFeatureJobParameter.isCompatible(param)) {
                processInputDataFiles = param.getValue();
            } else if (ProcessOutputFilesJobParameter.isCompatible(param)) {
                processResultDataFiles = List.of(param.getValue());
            } else if (TenantJobParameter.isCompatible(param)) {
                tenant = param.getValue();
            } else if (UserJobParameter.isCompatible(param)) {
                user = param.getValue();
            } else if (UserRoleJobParameter.isCompatible(param)) {
                userRole = param.getValue();
            } else if (ProcessDTOJobParameter.isCompatible(param)) {
                processDesc = param.getValue();
                processInfo = new OrderProcessInfoMapper().fromMap(processDesc.getProcessInfo())
                        .getOrElseThrow(() -> new JobParameterInvalidException("Cannot find processInfo from processDesc"));
            } else if (ProcessBatchCorrelationIdJobParameter.isCompatible(param)) {
                batchCorrelationId = BatchSuborderCorrelationIdentifier.parse(param.getValue())
                        .getOrElseThrow(() -> new JobParameterInvalidException("Cannot parse batchCorrelationId"));
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
        if (!missingParams.isEmpty()) {
            throw new JobParameterMissingException("Missing parameters: " + StringUtils.join(missingParams, ", "));
        }
    }

    private void checkMissing(ArrayList<String> missingParams, Object field, String name) {
        if (field == null) { missingParams.add(name); }
    }


    public static class DatasetSelectionNotFoundException extends RuntimeException {
        public DatasetSelectionNotFoundException(UUID jobInfoId, Long dsSelId) {
            super(String.format("jobInfo:%s dsSel:%d The dataset selection could not be found.",
                    jobInfoId, dsSelId)
            );
        }
    }

    public static class CouldNotCreateBatchException extends RuntimeException {
        public CouldNotCreateBatchException(UUID jobInfoId, Long dsSelId, UUID processBusinessId, ResponseEntity<PBatchResponse> batchResponse) {
            super(String.format("jobInfo:%s dsSel:%d Could not create batch, response status is %s",
                    jobInfoId, dsSelId, batchResponse.getStatusCode())
            );
        }
    }
}
