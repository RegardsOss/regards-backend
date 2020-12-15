/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service.processing;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderJobService;
import fr.cnes.regards.modules.order.service.processing.correlation.BatchSuborderCorrelationIdentifier;
import fr.cnes.regards.modules.order.service.processing.correlation.ExecutionCorrelationIdentifier;
import fr.cnes.regards.modules.order.service.processing.correlation.ProcessInputCorrelationIdentifier;
import fr.cnes.regards.modules.processing.domain.dto.POutputFileDTO;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.order.Cardinality;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;
import fr.cnes.regards.modules.processing.order.OrderProcessInfoMapper;
import io.vavr.collection.HashSet;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * When rs-process has finished an execution, it sends a PExecutionResultEvent.
 * This class handles these events, setting the corresponding output OrderDataFiles as
 * available for download in case of success.
 *
 * @author Guillaume Andrieu
 */
@Service
public class ProcessingExecutionResultEventHandler implements IProcessingExecutionResultEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingExecutionResultEventHandler.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ISubscriber subscriber;

    private final IOrderDataFileRepository orderDataFileRepository;

    private final IOrderJobService orderJobService;

    private final IOrderDataFileService dataFileService;

    private final ApplicationEventPublisher applicationPublisher;

    private final Gson gson;

    @Autowired
    public ProcessingExecutionResultEventHandler(IRuntimeTenantResolver runtimeTenantResolver, ISubscriber subscriber,
            IOrderDataFileRepository orderDataFileRepository, IOrderJobService orderJobService,
            IOrderDataFileService dataFileService, ApplicationEventPublisher applicationPublisher, Gson gson) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
        this.orderDataFileRepository = orderDataFileRepository;
        this.orderJobService = orderJobService;
        this.dataFileService = dataFileService;
        this.applicationPublisher = applicationPublisher;
        this.gson = gson;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(PExecutionResultEvent.class, this);
    }

    @Override
    public void handle(String tenant, PExecutionResultEvent evt) {
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.info("Received execution result: cid={} evt={}", evt.getExecutionCorrelationId(), evt);

        OrderProcessInfo processInfo = readProcessInfo(evt);
        Cardinality cardinality = processInfo.getCardinality();

        String batchCorrelationId = evt.getBatchCorrelationId();
        BatchSuborderCorrelationIdentifier batchSuborderCorrId = readBatchSuborderIdentifier(batchCorrelationId);
        ExecutionCorrelationIdentifier execCorrId = gson.fromJson(evt.getExecutionCorrelationId(),
                                                                  ExecutionCorrelationIdentifier.class);


        ExecutionStatus finalStatus = evt.getFinalStatus();
        final List<OrderDataFile> updatedDataFiles;
        if (finalStatus != ExecutionStatus.SUCCESS) {
            LOGGER.error("{} terminated with status {}", logPrefix(evt), finalStatus);
            updatedDataFiles = setAllDataFilesInSuborderAsInError(batchSuborderCorrId);
        } else if (evt.getOutputs().isEmpty()) {
            LOGGER.error("{} no output found", logPrefix(evt));
            updatedDataFiles = setAllDataFilesInSuborderAsInError(batchSuborderCorrId);
        } else {
            /*
             * As a reminder of what the previous steps of the process did:
             * - in OrderProcessingService, we created OrderDataFile corresponding to the output files
             *   and we gave a temporary URL for these files with the following pattern as defined in
             *   ProcessInputCorrelationIdentifier:
             *   - if cardinality is ONE_PER_EXECUTION,  the url is file:/batchCorrelationId
             *   - if cardinality is ONE_PER_FEATURE,    the url is file:/batchCorrelationId/featureIpId
             *   - if cardinality is ONE_PER_INPUT_FILE, the url is file:/batchCorrelationId/featureIpId/fileName
             *
             * - in ProcessExecutionJob, we sent executions with input files for which every correlationId
             *   is defined as file:/batchCorrelationId/featureIpId/fileName
             *
             * Here we need to find back the OrderDataFile from a list of inputCorrelationIds, because
             * each execution output provides such a list.
             */
            switch (cardinality) {
                case ONE_PER_EXECUTION: {
                    updatedDataFiles = dealWithSingleExecutionOutputFile(evt, batchSuborderCorrId);
                    break;
                }
                case ONE_PER_FEATURE: {
                    updatedDataFiles = dealWithOutputFilePerFeature(evt, batchSuborderCorrId);
                    break;
                }
                case ONE_PER_INPUT_FILE: {
                    updatedDataFiles = dealWithOutputFilePerInputFile(evt, batchSuborderCorrId);
                    break;
                }
                default:
                    throw new NotImplementedException("New cardinality case not implemented: " + cardinality.name());
            }

        }

        dataFileService.save(updatedDataFiles);
        orderJobService.manageUserOrderStorageFilesJobInfos(execCorrId.getUser());
        // Used by tests
        applicationPublisher.publishEvent(ExecResultHandlerResultEvent.event(evt, updatedDataFiles));

    }

    private String logPrefix(PExecutionResultEvent evt) {
        return String.format("batchCid=%s execCid=%s", evt.getBatchCorrelationId(), evt.getExecutionCorrelationId());
    }

    /**
     * This is the case where there should be exactly as many output file as input files.
     * Each output file is supposed to reference the corresponding inputCorrelationId, and
     * thus we can find the OrderDataFile which references it.
     */
    private List<OrderDataFile> dealWithOutputFilePerInputFile(
            PExecutionResultEvent evt,
            BatchSuborderCorrelationIdentifier batchSuborderIdentifier
    ) {
        Seq<POutputFileDTO> outputs = evt.getOutputs();
        List<OrderDataFile> updatedDataFiles = new ArrayList<>();

        // TODO: detect if there are too few outputs, set the corresponding OrderDataFiles as PROCESSING_ERROR

        for (POutputFileDTO outputFile : outputs) {
            io.vavr.collection.List<String> inputCorrelationIds = outputFile.getInputCorrelationIds();
            int inputsCidCount = inputCorrelationIds.size();
            if (inputsCidCount == 0) {
                LOGGER.error("{} the output {} has no corresponding inputCorrelationIds, but it is supposed to have only one",
                             logPrefix(evt), outputFile.getName());
                return setAllDataFilesInSuborderAsInError(batchSuborderIdentifier);
            } else if (inputsCidCount != 1) {
                LOGGER.warn("{} the output {} has {} corresponding inputCorrelationIds, but it is supposed to have only one",
                            logPrefix(evt), outputFile.getName(), inputsCidCount);
            }
            // In this case, the corresponding OrderDataFile URL is supposed to be exactly the input correlation ID.
            String orderDataFileUrl = inputCorrelationIds.head();

            List<OrderDataFile> orderDataFiles = orderDataFileRepository.findAllByUrlStartingWith(orderDataFileUrl);
            int orderDataFileCount = orderDataFiles.size();
            if (orderDataFileCount != 1) {
                LOGGER.warn("{} expected to find exactly one OrderDataFile with temporary URL set to {}, but found {}",
                            logPrefix(evt), orderDataFileUrl, orderDataFileCount);
            }
            OrderDataFile odf = orderDataFiles.get(0);
            String url = outputFile.getUrl().toString();
            odf.setUrl(url);
            odf.setState(FileState.AVAILABLE);
            odf.setChecksum(outputFile.getChecksumValue());
            odf.setFilename(outputFile.getName());
            odf.setFilesize(outputFile.getSize());
            updatedDataFiles.add(odf);
        }

        return updatedDataFiles;
    }

    /**
     * This is the case where there should be exactly as many output file as features, possibly
     * several input files per feature.
     *
     * Each output file is supposed to reference one inputCorrelationId per input file, and
     * all of these input files are supposed to reference the same feature ID.
     */
    private List<OrderDataFile> dealWithOutputFilePerFeature(
            PExecutionResultEvent evt,
            BatchSuborderCorrelationIdentifier batchSuborderIdentifier
    ) {

        Seq<POutputFileDTO> outputs = evt.getOutputs();
        List<OrderDataFile> updatedDataFiles = new ArrayList<>();
        AtomicBoolean error = new AtomicBoolean();

        // TODO: detect if there are too few outputs, set the corresponding OrderDataFiles as PROCESSING_ERROR

        for (POutputFileDTO outputFile : outputs) {

            if (error.get()) {
                return setAllDataFilesInSuborderAsInError(batchSuborderIdentifier);
            }

            allInputCidsReferenceTheSameFeature(outputFile.getInputCorrelationIds()).peek(featureIpId -> {
                String orderDataFileUrl = ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier, featureIpId);
                List<OrderDataFile> orderDataFiles = orderDataFileRepository.findAllByUrlStartingWith(orderDataFileUrl);
                int orderDataFileCount = orderDataFiles.size();
                if (orderDataFileCount != 1) {
                    LOGGER.warn("{} expected to find exactly one OrderDataFile with temporary URL set to {}, but found {}",
                                logPrefix(evt), orderDataFileUrl, orderDataFileCount);
                }
                OrderDataFile odf = orderDataFiles.get(0);
                String url = outputFile.getUrl().toString();
                odf.setUrl(url);
                odf.setState(FileState.AVAILABLE);
                odf.setChecksum(outputFile.getChecksumValue());
                odf.setFilename(outputFile.getName());
                odf.setFilesize(outputFile.getSize());
                updatedDataFiles.add(odf);
            }).onEmpty(() -> {
                LOGGER.error("{} the output {} has incoherent inputCorrelationIds, referencing several features when they should reference only one",
                             logPrefix(evt), outputFile.getName());
                error.set(true);
            });
        }

        return updatedDataFiles;
    }

    /**
     * This is the case where there should be exactly one output file.
     * Finding the corresponding OrderDataFile in the database is easy, we only need to use
     * the batchSuborderIdentifier.
     */
    private List<OrderDataFile> dealWithSingleExecutionOutputFile(
            PExecutionResultEvent evt,
            BatchSuborderCorrelationIdentifier batchSuborderIdentifier
    ) {
        Seq<POutputFileDTO> outputs = evt.getOutputs();
        if (outputs.size() > 1) {
            LOGGER.warn("{} more than one output, while exactly one is expected ; ignoring all but first output",
                        logPrefix(evt));
        }
        POutputFileDTO outputFile = outputs.head();
        String orderDataFileUrl = ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier);
        List<OrderDataFile> orderDataFiles = orderDataFileRepository.findAllByUrlStartingWith(orderDataFileUrl);
        int orderDataFileCount = orderDataFiles.size();
        if (orderDataFileCount != 1) {
            LOGGER.warn("{} expected to find exactly one OrderDataFile with temporary URL set to {}, but found {}",
                        logPrefix(evt), orderDataFileUrl, orderDataFileCount);
        }
        for (OrderDataFile odf : orderDataFiles) {
            String url = outputFile.getUrl().toString();
            odf.setUrl(url);
            odf.setState(FileState.AVAILABLE);
            odf.setChecksum(outputFile.getChecksumValue());
            odf.setFilename(outputFile.getName());
            odf.setFilesize(outputFile.getSize());
        }
        return orderDataFiles;
    }

    private List<OrderDataFile> setAllDataFilesInSuborderAsInError(
            BatchSuborderCorrelationIdentifier batchSuborderIdentifier) {
        List<OrderDataFile> allFilesInSuborder = orderDataFileRepository
                .findAllByUrlStartingWith(ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier));
        List<OrderDataFile> allInError = Stream.ofAll(allFilesInSuborder).map(file -> {
            file.setState(FileState.PROCESSING_ERROR);
            return file;
        }).collect(Collectors.toList());
        return allInError;
    }

    private BatchSuborderCorrelationIdentifier readBatchSuborderIdentifier(String batchCorrelationId) {
        return BatchSuborderCorrelationIdentifier.parse(batchCorrelationId).getOrElseThrow(() -> {
            String errMsg = String.format("Could not parse batch correlation ID: %s", batchCorrelationId);
            LOGGER.error(errMsg);
            return new IllegalArgumentException(errMsg);
        });
    }

    private OrderProcessInfo readProcessInfo(PExecutionResultEvent message) {
        return new OrderProcessInfoMapper().fromMap(message.getProcessInfo()).getOrElseThrow(() -> {
            String errMsg = String.format("Could not parse process info from map in message %s", message);
            LOGGER.error(errMsg);
            return new IllegalArgumentException(errMsg);
        });
    }

    private Option<String> allInputCidsReferenceTheSameFeature(io.vavr.collection.List<String> inputCorrelationIds) {
        String missing = "";
        HashSet<String> featureIpIds = inputCorrelationIds
                .map(cid -> ProcessInputCorrelationIdentifier.parse(cid)
                        .flatMap(ProcessInputCorrelationIdentifier::getFeatureIpId).getOrElse(missing))
                .collect(HashSet.collector()).filter(s -> !missing.equals(s));
        return featureIpIds.size() == 1 ? featureIpIds.headOption() : Option.none();
    }

}
