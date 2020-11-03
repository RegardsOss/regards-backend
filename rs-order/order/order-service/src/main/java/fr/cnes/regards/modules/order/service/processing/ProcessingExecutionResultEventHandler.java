package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.order.dao.IBasketDatasetSelectionRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.service.processing.correlation.BatchSuborderCorrelationIdentifier;
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
import org.springframework.context.ApplicationEvent;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ProcessingExecutionResultEventHandler implements IProcessingExecutionResultEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingExecutionResultEventHandler.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final ISubscriber subscriber;
    private final IBasketDatasetSelectionRepository dsSelRepository;
    private final IOrderDataFileRepository orderDataFileRepository;

    @Autowired
    public ProcessingExecutionResultEventHandler(
            IRuntimeTenantResolver runtimeTenantResolver,
            ISubscriber subscriber,
            IBasketDatasetSelectionRepository dsSelRepository,
            IOrderDataFileRepository orderDataFileRepository
    ) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
        this.dsSelRepository = dsSelRepository;
        this.orderDataFileRepository = orderDataFileRepository;
    }

    @Override public void onApplicationEvent(ApplicationEvent event) {
        subscriber.subscribeTo(PExecutionResultEvent.class, this);
    }

    @Override public void handle(String tenant, PExecutionResultEvent evt) {
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.info("Received execution result: {}", evt);

        OrderProcessInfo processInfo = readProcessInfo(evt);
        Cardinality cardinality = processInfo.getCardinality();

        String batchCorrelationId = evt.getBatchCorrelationId();
        BatchSuborderCorrelationIdentifier batchSuborderIdentifier = readBatchSuborderIdentifier(batchCorrelationId);

        BasketDatasetSelection dsSel = getDatasetSelection(batchCorrelationId, batchSuborderIdentifier);

        ExecutionStatus finalStatus = evt.getFinalStatus();
        if (finalStatus != ExecutionStatus.SUCCESS) {
            LOGGER.error("{} terminated with status {}", logPrefix(evt), finalStatus);
            setAllDataFilesInSuborderAsInError(batchSuborderIdentifier);
        }
        else if (evt.getOutputs().isEmpty()) {
            LOGGER.error("{} no output found", logPrefix(evt));
            setAllDataFilesInSuborderAsInError(batchSuborderIdentifier);
        }
        else {
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
                case ONE_PER_EXECUTION: { dealWithSingleExecutionOutputFile(evt, processInfo, batchSuborderIdentifier, dsSel); }
                case ONE_PER_FEATURE: { dealWithOutputFilePerFeature(evt, processInfo, batchSuborderIdentifier, dsSel); }
                case ONE_PER_INPUT_FILE: { dealWithOutputFilePerInputFile(evt, processInfo, batchSuborderIdentifier, dsSel); }
                default: throw new NotImplementedException("New cadrinality case not implemented: " + cardinality.name());
            }
        }
    }

    private String logPrefix(PExecutionResultEvent evt) {
        return String.format("batchCid=%s execCid=%s", evt.getBatchCorrelationId(), evt.getExecutionCorrelationId());
    }

    /**
     * This is the case where there should be exactly as many output file as input files.
     * Each output file is supposed to reference the corresponding inputCorrelationId, and
     * thus we can find the OrderDataFile which references it.
     */
    private void dealWithOutputFilePerInputFile(
            PExecutionResultEvent evt,
            OrderProcessInfo processInfo,
            BatchSuborderCorrelationIdentifier batchSuborderIdentifier,
            BasketDatasetSelection dsSel
    ) {
        Seq<POutputFileDTO> outputs = evt.getOutputs();
        List<OrderDataFile> updatedDataFiles = new ArrayList<>();

        for (POutputFileDTO outputFile: outputs) {
            io.vavr.collection.List<String> inputCorrelationIds = outputFile.getInputCorrelationIds();
            int inputsCidCount = inputCorrelationIds.size();
            if (inputsCidCount == 0) {
                LOGGER.error("{} the output {} has no corresponding inputCorrelationIds, but it is supposed to have only one",
                        logPrefix(evt), outputFile.getName());
                setAllDataFilesInSuborderAsInError(batchSuborderIdentifier);
                return;
            }
            else if (inputsCidCount != 1) {
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
            try {
                OrderDataFile odf = orderDataFiles.get(0);
                String url = outputFile.getUrl().toString();
                odf.setUrl(url);
                odf.setState(FileState.AVAILABLE);
                odf.setChecksum(outputFile.getChecksumValue());
                odf.setFilename(outputFile.getName());
                odf.setFilesize(outputFile.getSize());
                updatedDataFiles.add(odf);
            } catch (URISyntaxException e) {
                LOGGER.error("{} could not create URI from execution output URL: {}",
                        logPrefix(evt), e.getInput(), e);
                setAllDataFilesInSuborderAsInError(batchSuborderIdentifier);
                return;
            }
        }

        orderDataFileRepository.saveAll(updatedDataFiles);
    }

    /**
     * This is the case where there should be exactly as many output file as features, possibly
     * several input files per feature.
     *
     * Each output file is supposed to reference one inputCorrelationId per input file, and
     * all of these input files are supposed to reference the same feature ID.
     */
    private void dealWithOutputFilePerFeature(
            PExecutionResultEvent evt,
            OrderProcessInfo processInfo,
            BatchSuborderCorrelationIdentifier batchSuborderIdentifier,
            BasketDatasetSelection dsSel
    ) {

        Seq<POutputFileDTO> outputs = evt.getOutputs();
        List<OrderDataFile> updatedDataFiles = new ArrayList<>();
        AtomicBoolean error = new AtomicBoolean();

        for (POutputFileDTO outputFile: outputs) {

            if (error.get()) {
                setAllDataFilesInSuborderAsInError(batchSuborderIdentifier);
                return;
            }

            allInputCidsReferenceTheSameFeature(outputFile.getInputCorrelationIds())
                .peek(featureIpId -> {
                    String orderDataFileUrl = ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier, featureIpId);
                    List<OrderDataFile> orderDataFiles = orderDataFileRepository.findAllByUrlStartingWith(orderDataFileUrl);
                    int orderDataFileCount = orderDataFiles.size();
                    if (orderDataFileCount != 1) {
                        LOGGER.warn("{} expected to find exactly one OrderDataFile with temporary URL set to {}, but found {}",
                                logPrefix(evt), orderDataFileUrl, orderDataFileCount);
                    }
                    try {
                        OrderDataFile odf = orderDataFiles.get(0);
                        String url = outputFile.getUrl().toString();
                        odf.setUrl(url);
                        odf.setState(FileState.AVAILABLE);
                        odf.setChecksum(outputFile.getChecksumValue());
                        odf.setFilename(outputFile.getName());
                        odf.setFilesize(outputFile.getSize());

                        updatedDataFiles.add(odf);
                    } catch (URISyntaxException e) {
                        LOGGER.error("{} could not create URI from execution output URL: {}",
                                logPrefix(evt), e.getInput(), e);
                        error.set(true);
                    }
                })
                .onEmpty(() -> {
                    LOGGER.error("{} the output {} has incoherent inputCorrelationIds, referencing several features when they should reference only one",
                            logPrefix(evt), outputFile.getName());
                    error.set(true);
                });
        }

        orderDataFileRepository.saveAll(updatedDataFiles);
    }


    /**
     * This is the case where there should be exactly one output file.
     * Finding the corresponding OrderDataFile in the database is easy, we only need to use
     * the batchSuborderIdentifier.
     */
    private void dealWithSingleExecutionOutputFile(
            PExecutionResultEvent evt,
            OrderProcessInfo processInfo,
            BatchSuborderCorrelationIdentifier batchSuborderIdentifier,
            BasketDatasetSelection dsSel
    ) {
        Seq<POutputFileDTO> outputs = evt.getOutputs();
        if (outputs.size() > 1) {
            LOGGER.warn("{} more than one output, while exactly one is expected ; ignoring all but first output", logPrefix(evt));
        }
        POutputFileDTO outputFile = outputs.head();
        String orderDataFileUrl = ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier);
        Collection<OrderDataFile> orderDataFiles = orderDataFileRepository.findAllByUrlStartingWith(orderDataFileUrl);
        int orderDataFileCount = orderDataFiles.size();
        if (orderDataFileCount != 1) {
            LOGGER.warn("{} expected to find exactly one OrderDataFile with temporary URL set to {}, but found {}",
                logPrefix(evt), orderDataFileUrl, orderDataFileCount);
        }
        try {
            for (OrderDataFile odf: orderDataFiles) {
                String url = outputFile.getUrl().toString();
                odf.setUrl(url);
                odf.setState(FileState.AVAILABLE);
                odf.setChecksum(outputFile.getChecksumValue());
                odf.setFilename(outputFile.getName());
                odf.setFilesize(outputFile.getSize());
            }
            orderDataFileRepository.saveAll(orderDataFiles);


        } catch (URISyntaxException e) {
            LOGGER.error("{} could not create URI from execution output URL: {}",
                    logPrefix(evt), e.getInput(), e);
            setAllDataFilesInSuborderAsInError(batchSuborderIdentifier);
        }
    }

    private void setAllDataFilesInSuborderAsInError(BatchSuborderCorrelationIdentifier batchSuborderIdentifier) {
        List<OrderDataFile> allFilesInSuborder = orderDataFileRepository.findAllByUrlStartingWith(ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier));
        List<OrderDataFile> allOnError = Stream.ofAll(allFilesInSuborder)
                .map(file -> {
                    file.setState(FileState.PROCESSING_ERROR);
                    return file;
                })
                .collect(Collectors.toList());
        orderDataFileRepository.saveAll(allOnError);
    }

    private BasketDatasetSelection getDatasetSelection(String batchCorrelationId, BatchSuborderCorrelationIdentifier batchSuborderIdentifier) {
        return dsSelRepository.findById(batchSuborderIdentifier.getDsSelId())
                .orElseThrow(() -> {
                    String msgErr = String.format("Could not find BasketDatasetSelection with ID in batch correlation %s", batchCorrelationId);
                    LOGGER.error(msgErr);
                    return new IllegalArgumentException(msgErr);
                });
    }

    private BatchSuborderCorrelationIdentifier readBatchSuborderIdentifier(String batchCorrelationId) {
        return BatchSuborderCorrelationIdentifier.parse(batchCorrelationId)
                .getOrElseThrow(() -> {
                    String errMsg = String.format("Could not parse batch correlation ID: %s", batchCorrelationId);
                    LOGGER.error(errMsg);
                    return new IllegalArgumentException(errMsg);
                });
    }

    private OrderProcessInfo readProcessInfo(PExecutionResultEvent message) {
        return new OrderProcessInfoMapper().fromMap(message.getProcessInfo())
                .getOrElseThrow(() -> {
                    String errMsg = String.format("Could not parse process info from map in message %s", message);
                    LOGGER.error(errMsg);
                    return new IllegalArgumentException(errMsg);
                });
    }
    
    private Option<String> allInputCidsReferenceTheSameFeature(io.vavr.collection.List<String> inputCorrelationIds) {
        String missing = "";
        HashSet<String> featureIpIds = inputCorrelationIds
                .map(cid ->
                        ProcessInputCorrelationIdentifier.parse(cid)
                                .flatMap(ProcessInputCorrelationIdentifier::getFeatureIpId)
                                .getOrElse(missing)
                )
                .collect(HashSet.collector())
                .filter(s -> !missing.equals(s));
        return featureIpIds.size() == 1 ? featureIpIds.headOption() : Option.none();
    }

}
