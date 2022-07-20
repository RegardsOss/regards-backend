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
package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.order.exception.CatalogSearchException;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderJobService;
import fr.cnes.regards.modules.order.service.job.BasketDatasetSelectionDescriptor;
import fr.cnes.regards.modules.order.service.job.ProcessExecutionJob;
import fr.cnes.regards.modules.order.service.job.StorageFilesJob;
import fr.cnes.regards.modules.order.service.job.parameters.*;
import fr.cnes.regards.modules.order.service.processing.correlation.BatchSuborderCorrelationIdentifier;
import fr.cnes.regards.modules.order.service.processing.correlation.ProcessInputCorrelationIdentifier;
import fr.cnes.regards.modules.order.service.utils.BasketSelectionPageSearch;
import fr.cnes.regards.modules.order.service.utils.OrderCounts;
import fr.cnes.regards.modules.order.service.utils.SuborderSizeCounter;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.order.*;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.apache.commons.lang3.NotImplementedException;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Complement to OrderService when dealing with dataset selections having processing.
 *
 * @author Guillaume Andrieu
 */
@Service
@MultitenantTransactional
public class OrderProcessingService implements IOrderProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessingService.class);

    private static final String OCTET_STREAM = "application/octet-stream";

    protected final OrderProcessInfoMapper processInfoMapper = new OrderProcessInfoMapper();

    protected final BasketSelectionPageSearch basketSelectionPageSearch;

    protected final IProcessingRestClient processingClient;

    protected final SuborderSizeCounter suborderSizeCounter;

    protected final IOrderDataFileService orderDataFileService;

    protected final IOrderDataFileRepository orderDataFileRepository;

    protected final IOrderJobService orderJobService;

    protected final IJobInfoService jobInfoService;

    // @formatter:off

    @Autowired
    public OrderProcessingService(
            BasketSelectionPageSearch basketSelectionPageSearch,
            IProcessingRestClient processingClient,
            SuborderSizeCounter suborderSizeCounter,
            IOrderDataFileService orderDataFileService,
            IOrderDataFileRepository orderDataFileRepository,
            IOrderJobService orderJobService,
            IJobInfoService jobInfoService
    ) {
        this.basketSelectionPageSearch = basketSelectionPageSearch;
        this.processingClient = processingClient;
        this.suborderSizeCounter = suborderSizeCounter;
        this.orderDataFileService = orderDataFileService;
        this.orderDataFileRepository = orderDataFileRepository;
        this.orderJobService = orderJobService;
        this.jobInfoService = jobInfoService;
    }

    @Override
    public OrderCounts manageProcessedDatasetSelection(
            Order order,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole,
            OrderCounts orderCounts,
            int subOrderDuration
    ) throws ModuleException {

        ProcessDatasetDescription processDatasetDesc = dsSel.getProcessDatasetDescription();
        UUID processBusinessId = processDatasetDesc.getProcessBusinessId();
        String processIdStr = processBusinessId.toString();
        try {
            ResponseEntity<PProcessDTO> response = processingClient.findByUuid(processIdStr);
            if (response.hasBody() && response.getStatusCode() == HttpStatus.OK) {

                PProcessDTO processDto = response.getBody();
                OrderProcessInfo orderProcessInfo = parseOrderProcessInfo(order, dsSel, user, processDto);
                List<DataType> requiredDatatypes = orderProcessInfo.getRequiredDatatypes();
                
                // Creates datasetTasks with required data types and update estimated size
                DatasetTask dsTask = DatasetTask.fromBasketSelection(dsSel, requiredDatatypes.toJavaList());
                dsTask.setFilesSize(orderProcessInfo.getSizeForecast().expectedResultSizeInBytes(dsTask.getFilesSize()));

                AtomicLong suborderCount = new AtomicLong(1);
                OrderCounts result = basketSelectionPageSearch.fluxSearchDataObjects(dsSel)
                        .groupBy(feature -> hasAtLeastOneRequiredFileInStorage(feature, requiredDatatypes))
                        .doOnError(CatalogSearchException.class, error -> Mono.error(new ModuleException(error)))
                        .flatMap(featureGroup -> discriminateBetweenSomeInStorageOrOnlyExternal(
                                        order, dsSel,
                                        tenant, user, userRole,
                                        processDto, orderProcessInfo,
                                        suborderCount, featureGroup,
                                        subOrderDuration
                                ))
                        .doOnNext(dsTask::addReliantTask)
                        .map(filesTask -> new OrderCounts(0, filesTask.getFiles().size(), 1, Collections.singleton(filesTask.getJobInfo().getId())))
                        .reduce(OrderCounts.initial(), OrderCounts::add)
                        .block();

                if (!dsTask.getReliantTasks().isEmpty()) {
                    order.addDatasetOrderTask(dsTask);
                }
                return OrderCounts.add(orderCounts, result);
            } else {
                throw new ModuleException("Error retrieving process id " + processIdStr);
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error(e.getMessage(),e);
            throw new ModuleException(e.getMessage());
        } catch (RuntimeException e) {
            // catch a runtime exception if fluxSearchDataObjects throws a module exception wrapped in a runtime
            // reactor exception
            if(e.getCause() instanceof TooManyItemsSelectedInBasketException) {
                throw (TooManyItemsSelectedInBasketException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    private OrderProcessInfo parseOrderProcessInfo(Order order, BasketDatasetSelection dsSel, String user, PProcessDTO processDto) {
        return processInfoMapper.fromMap(processDto.getProcessInfo())
                .getOrElseThrow(() -> new UnparsableProcessInfoException(
                        String.format("Unparsable process infos: order=%s user=%s dsSel=%s processDto=%s",
                                order.getLabel(), user, dsSel.getId(), processDto)));
    }

    private Publisher<? extends FilesTask> discriminateBetweenSomeInStorageOrOnlyExternal(
            Order order, BasketDatasetSelection dsSel,
            String tenant, String user, String userRole,
            PProcessDTO processDto, OrderProcessInfo orderProcessInfo,
            AtomicLong suborderCount, GroupedFlux<Boolean, EntityFeature> featureGroup,
            int subOrderDuration
    ) {
        if (hasRequiredFilesInStorage(featureGroup)) {
            return manageFeaturesWithFilesInStorage(
                    featureGroup, order, suborderCount,
                    processDto, orderProcessInfo, dsSel,
                    tenant, user, userRole, subOrderDuration
            );
        }
        else {
            return manageFeaturesWithOnlyExternalFiles(
                featureGroup, order, suborderCount,
                processDto, orderProcessInfo, dsSel,
                tenant, user, userRole
            );
        }
    }

    @Override
    public void enqueuedProcessingJob(UUID processJobId, Collection<OrderDataFile> dataFiles, String user) {
        // Delete the OrderDataFiles which were only temporary input files
        orderDataFileRepository.deleteAll(dataFiles);
        // Enqueue the processing job because all of its dependencies are ready (if there is a process to launch)
        jobInfoService.enqueueJobForId(processJobId);
        // Nudge the order job service to enqueue next storage files jobs.
        orderJobService.manageUserOrderStorageFilesJobInfos(user);
    }

    protected Publisher<FilesTask> manageFeaturesWithOnlyExternalFiles(
            GroupedFlux<Boolean, EntityFeature> featureGroup,
            Order order,
            AtomicLong suborderCount,
            PProcessDTO pProcessDTO,
            OrderProcessInfo orderProcessInfo,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole
    ) {
        return windowAccordingToScopeAndSizeLimit(order.getId(), pProcessDTO.getProcessId(), featureGroup, orderProcessInfo)
            .flatMap(features -> {
                if (orderProcessInfo.getScope() == Scope.SUBORDER) {
                    return createProcessExecutionJobAndFilesTask(
                        order, suborderCount, pProcessDTO, orderProcessInfo, dsSel,
                        tenant, user, userRole, features
                    );
                } else {
                    return Flux.fromIterable(features)
                        .flatMap(feature -> createProcessExecutionJobAndFilesTask(
                            order, suborderCount, pProcessDTO, orderProcessInfo, dsSel,
                            tenant, user, userRole, List.of(feature))
                        );
                }
            });
    }

    protected Publisher<FilesTask> createProcessExecutionJobAndFilesTask(
            Order order,
            AtomicLong suborderCount,
            PProcessDTO pProcessDTO,
            OrderProcessInfo orderProcessInfo,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole,
            List<EntityFeature> features
    ) {
        JobInfo jobInfo = createProcessExecutionJobForFeatures(
            order, suborderCount, pProcessDTO, orderProcessInfo, dsSel,
            tenant, user, userRole, features
        );
        jobInfoService.createAsQueued(jobInfo);
        OrderDataFile[] outputFiles = extractOutputFilesFromProcessExecJob(jobInfo);

        FilesTask filesTask = new FilesTask();
        filesTask.setOrderId(order.getId());
        filesTask.setOwner(user);
        filesTask.addAllFiles(List.of(outputFiles).toJavaList());
        filesTask.setJobInfo(jobInfo);
        return Mono.just(filesTask);
    }

    protected JobInfo createProcessExecutionJobForFeatures(
            Order order,
            AtomicLong suborderCount,
            PProcessDTO pProcessDTO,
            OrderProcessInfo orderProcessInfo,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole,
            List<EntityFeature> features
    ) {
        ProcessInputsPerFeature processInputsPerFeature = createInputFiles(
                features,
                orderProcessInfo.getRequiredDatatypes()
        );
        Long suborderCountId = suborderCount.getAndIncrement();

        BatchSuborderCorrelationIdentifier batchSuborderIdentifier = new BatchSuborderCorrelationIdentifier(
                order.getId(), dsSel.getId(), suborderCountId
        );
        Long[] outputFiles = createOutputFilesAndReturnIds(
                batchSuborderIdentifier,
                UniformResourceName.fromString(dsSel.getDatasetIpid()),
                features,
                orderProcessInfo
        );
        JobInfo result = new JobInfo(
                false,
                orderJobService.computePriority(user, userRole),
                HashSet.of(
                    new TenantJobParameter(tenant),
                    new UserJobParameter(user),
                    new UserRoleJobParameter(userRole),
                    new ProcessInputsPerFeatureJobParameter(processInputsPerFeature),
                    new ProcessOutputFilesJobParameter(outputFiles),
                    new ProcessDTOJobParameter(pProcessDTO),
                    new ProcessBatchCorrelationIdJobParameter(batchSuborderIdentifier.repr()),
                    new BasketDatasetSelectionJobParameter(new BasketDatasetSelectionDescriptor(dsSel))
                ).toJavaSet(),
                user,
                ProcessExecutionJob.class.getName()
        );
        result.setExpirationDate(order.getExpirationDate());
        return result;
    }

    protected Long[] createOutputFilesAndReturnIds(
            BatchSuborderCorrelationIdentifier batchSuborderIdentifier,
            UniformResourceName dsSelIpId,
            List<EntityFeature> features,
            OrderProcessInfo processInfo
    ) {
        Scope scope = processInfo.getScope();
        Cardinality cardinality = processInfo.getCardinality();
        List<DataType> requiredDataTypes = processInfo.getRequiredDatatypes();
        IResultSizeForecast sizeForecast = processInfo.getSizeForecast();
        Long orderId = batchSuborderIdentifier.getOrderId();

        if (scope == Scope.SUBORDER && cardinality == Cardinality.ONE_PER_EXECUTION) {
            List<DataFile> applicableDataFilesIn = features.flatMap(f -> List.ofAll(f.getFiles().values()))
                    .filter(f -> requiredDataTypes.contains(f.getDataType()));

            long expectedSize = sizeForecast.expectedResultSizeInBytes(
                applicableDataFilesIn.map(DataFile::getFilesize)
                    .reduceOption(Long::sum)
                    .getOrElse(0L)
            );

            DataFile dataFileOut = DataFile.build(
                    DataType.OTHER,
                    batchSuborderIdentifier.repr(),
                    ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier),
                    MimeType.valueOf(OCTET_STREAM),
                    true,
                    true
            );
            dataFileOut.setChecksum(UUID.randomUUID().toString());
            // ^- This is ugly but needed to prevent FilesTask to ignore files because they would have the same checksum
            dataFileOut.setFilesize(expectedSize);
            return List.of(createAndSaveOrderDataFile(dataFileOut, dsSelIpId, orderId))
                    .map(OrderDataFile::getId)
                    .toJavaArray(Long[]::new);
        } else {
            return features.flatMap(feature -> {
                if (cardinality == Cardinality.ONE_PER_EXECUTION || cardinality == Cardinality.ONE_PER_FEATURE) {
                    List<DataFile> applicableDataFilesIn = List.ofAll(feature.getFiles().values())
                            .filter(f -> requiredDataTypes.contains(f.getDataType()));
                    long expectedSize = sizeForecast.expectedResultSizeInBytes(
                        applicableDataFilesIn
                            .map(DataFile::getFilesize)
                            .reduceOption(Long::sum)
                            .getOrElse(0L)
                    );
                    DataFile dataFileOut = DataFile.build(
                            DataType.OTHER,
                            feature.getId().toString(),
                            ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier, feature),
                            MimeType.valueOf(OCTET_STREAM),
                            true,
                            true
                    );
                    dataFileOut.setChecksum(UUID.randomUUID().toString());
                    // ^- This is ugly but needed to prevent FilesTask to ignore files because they would have the same checksum
                    dataFileOut.setFilesize(expectedSize);
                    return List.of(createAndSaveOrderDataFile(dataFileOut, feature.getId(), orderId));
                } else if (cardinality == Cardinality.ONE_PER_INPUT_FILE) {
                    return featureRequiredDatafiles(feature, requiredDataTypes)
                        .map(dataFile -> {
                            long expectedSize = sizeForecast.expectedResultSizeInBytes(dataFile.getFilesize());

                            DataFile dataFileOut = DataFile.build(
                                DataType.OTHER,
                                dataFile.getFilename(),
                                ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier, feature, dataFile),
                                MimeType.valueOf(OCTET_STREAM),
                                true,
                                true
                            );
                            dataFileOut.setChecksum(UUID.randomUUID().toString());
                            // ^- This is ugly but needed to prevent FilesTask to ignore files because they would have the same checksum
                            dataFileOut.setFilesize(expectedSize);
                            return createAndSaveOrderDataFile(dataFileOut, feature.getId(), orderId);
                        })
                        .collect(Collectors.toList());
                } else {
                    // Happens only if some new cases appear for Cardinality
                    throw new NotImplementedException("New Cardinality case missing");
                }
            })
            .map(OrderDataFile::getId)
            .toJavaArray(Long[]::new);
        }
    }

    protected OrderDataFile createAndSaveOrderDataFile(DataFile dataFile, UniformResourceName name, Long orderId) {
        OrderDataFile orderDataFile = new OrderDataFile(dataFile, name, orderId);
        return orderDataFileService.save(orderDataFile);
    }

    /**
     * Create datafiles which are not meant to be saved into database, just serving the purpose of being converted
     * into input data files for the process executions.
     *
     * @param features          the features for which to extract the datafiles
     * @param requiredDataTypes the required data types for the process
     * @return the ProcessInputsPerFeature listing input files per feature
     */
    protected ProcessInputsPerFeature createInputFiles(List<EntityFeature> features, List<DataType> requiredDataTypes) {
        return new ProcessInputsPerFeature(features
            .toMap(ProcessOutputFeatureDesc::from, feature ->
                featureRequiredDatafiles(feature, requiredDataTypes)
                    .map(dataFile -> new OrderDataFile(dataFile, feature.getId(), -1L))
                    .collect(Collectors.toList()))
            .toJavaMap());
    }

    protected Stream<DataFile> featureRequiredDatafiles(EntityFeature feature, List<DataType> requiredTypes) {
        return feature.getFiles().values().stream()
                .filter(file -> requiredTypes.contains(file.getDataType()));
    }

    protected Flux<List<EntityFeature>> windowAccordingToScopeAndSizeLimit(
            Long orderId,
            UUID processBusinessId,
            GroupedFlux<Boolean, EntityFeature> featureGroup,
            OrderProcessInfo orderProcessInfo
    ) {
        Flux<List<Tuple2<EntityFeature, FeatureAccumulator>>> publish = featureGroup
                .publish(fg -> fg.zipWith(fg.scan(
                                new FeatureAccumulator(orderId, processBusinessId),
                                (acc, f) -> acc.addFeatureAndReturnInitialAccumulatorIfOverLimits(f, orderProcessInfo)))
                        .bufferUntil(featAndAcc -> featAndAcc.getT2().isInitial(), true)
                        .map(List::ofAll));

        // Check if the order can be split in multiple suborders
        if(!orderProcessInfo.getForbidSplitInSuborders()) {
            return publish.map(featAndAcc -> featAndAcc.map(Tuple2::getT1));
        } else {
            // if not, check the size of the buffer. If it contains more than one featureGroup, it means one of the
            // SizeLimit was reached, an exception is thrown because of the active parameter forbitSplitInSuborders
            return publish
                    .filter(featAndAcc -> !featAndAcc.isEmpty())
                    .buffer(2)
                    .flatMap(featuresBuffer -> {
                        if (featuresBuffer.size() > 1) {
                            return Mono.error(new TooManyItemsSelectedInBasketException(String.format(
                                    "%s. The order cannot be processed because the parameter " +
                                            "forbitSplitInSuborders is active. Please decrease the number of items " +
                                            "ordered or contact the administrator for more information.",
                                    featuresBuffer.get(1).last().getT2().newSubOrderMsg)));
                        } else {
                            return Mono.just(featuresBuffer.get(0).map(Tuple2::getT1));
                        }
                    });
        }
    }

    protected Flux<FilesTask> manageFeaturesWithFilesInStorage(
            GroupedFlux<Boolean, EntityFeature> featureGroup,
            Order order,
            AtomicLong suborderCount,
            PProcessDTO pProcessDTO,
            OrderProcessInfo orderProcessInfo,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole,
            int subOrderDuration
    ) {
        return windowAccordingToScopeAndSizeLimit(order.getId(), pProcessDTO.getProcessId(), featureGroup, orderProcessInfo)
            .flatMap(features -> createProcessJobAndStorageFilesJobForFeatures(
                    order, suborderCount, pProcessDTO, orderProcessInfo, dsSel,
                    tenant, user, userRole, features, subOrderDuration
                     )
            );
    }

    private Mono<FilesTask> createProcessJobAndStorageFilesJobForFeatures(
            Order order,
            AtomicLong suborderCount,
            PProcessDTO pProcessDTO,
            OrderProcessInfo orderProcessInfo,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole,
            List<EntityFeature> features,
            int subOrderDuration
    ) {
        JobInfo processExecJobUnsaved = createProcessExecutionJobForFeatures(
            order,
            suborderCount,
            pProcessDTO,
            orderProcessInfo,
            dsSel,
            tenant,
            user,
            userRole,
            features
        );
        processExecJobUnsaved.setExpirationDate(order.getExpirationDate());
        JobInfo processExecJob = jobInfoService.createAsPending(processExecJobUnsaved);

        Long[] internalInputFiles = findInputFilesInStorage(order.getId(), features, orderProcessInfo.getRequiredDatatypes());
        JobInfo storageFilesJobUnsaved = new JobInfo(false,
            orderJobService.computePriority(user, userRole),
            HashSet.of(
                    new FilesJobParameter(internalInputFiles),
                    new SubOrderAvailabilityPeriodJobParameter(subOrderDuration),
                    new UserJobParameter(user),
                    new UserRoleJobParameter(userRole),
                    new ProcessJobInfoJobParameter(processExecJob.getId())
            ).toJavaSet(),
            user,
            StorageFilesJob.class.getName()
        );
        storageFilesJobUnsaved.setExpirationDate(order.getExpirationDate());
        JobInfo storageFilesJob = jobInfoService.createAsPending(storageFilesJobUnsaved);

        OrderDataFile[] outputFiles = extractOutputFilesFromProcessExecJob(processExecJob);

        FilesTask filesTask = new FilesTask();
        filesTask.setOrderId(order.getId());
        filesTask.setOwner(user);
        filesTask.addAllFiles(List.of(outputFiles).toJavaList());
        filesTask.setJobInfo(storageFilesJob);
        return Mono.just(filesTask);
    }

    private OrderDataFile[] extractOutputFilesFromProcessExecJob(JobInfo processExecJob) {
        List<Long> ids = List.of(processExecJob.getParametersAsMap().get(ProcessOutputFilesJobParameter.NAME).getValue());
        return List.ofAll(orderDataFileRepository.findAllById(ids)).toJavaArray(OrderDataFile[]::new);
    }

    private Long[] findInputFilesInStorage(Long orderId, List<EntityFeature> features, List<DataType> requiredDatatypes) {
        return features
            .flatMap(f -> List.ofAll(f.getFiles().values())
                .filter(file -> !file.isReference() && requiredDatatypes.contains(file.getDataType()))
                .map(file -> new OrderDataFile(file, f.getId(), orderId))
            ).distinct()
            .map(orderDataFileRepository::save)
            .map(OrderDataFile::getId)
            .toJavaArray(Long[]::new);
    }

    protected boolean hasAtLeastOneRequiredFileInStorage(EntityFeature entityFeature, List<DataType> requiredDatatypes) {
        return featureRequiredDatafiles(entityFeature, requiredDatatypes)
            .anyMatch(df -> !df.isReference());
    }

    protected boolean hasRequiredFilesInStorage(GroupedFlux<Boolean, EntityFeature> featureGroup) {
        return Option.of(featureGroup.key()).getOrElse(false);
    }

    @SuppressWarnings("serial")
    public static class UnparsableProcessInfoException extends RuntimeException {
        public UnparsableProcessInfoException(String message) {
            super(message);
        }
    }

    @lombok.Value
    @lombok.AllArgsConstructor
    class FeatureAccumulator {
        Long orderId;
        UUID processBusinessId;
        int fileCount;
        int featureCount;
        long size;
        String newSubOrderMsg;

        public FeatureAccumulator(Long orderId, UUID processBusinessId) {
            this.orderId = orderId;
            this.processBusinessId = processBusinessId;
            this.fileCount = 0;
            this.featureCount = 0;
            this.size = 0L;
            this.newSubOrderMsg = "";
        }

        public FeatureAccumulator(Long orderId, UUID processBusinessId, String newSubOrderMsg) {
            this.orderId = orderId;
            this.processBusinessId = processBusinessId;
            this.fileCount = 0;
            this.featureCount = 0;
            this.size = 0L;
            this.newSubOrderMsg = newSubOrderMsg;
        }

        public FeatureAccumulator addFeatureAndReturnInitialAccumulatorIfOverLimits(EntityFeature feature,
                                                                                    OrderProcessInfo orderProcessInfo) {
            List<DataFile> files = featureRequiredDatafiles(feature, orderProcessInfo.getRequiredDatatypes()).collect(List.collector());
            int deltaCount = files.size();
            long deltaSize = files.map(DataFile::getFilesize).sum().longValue();
            FeatureAccumulator newAcc = new FeatureAccumulator(orderId, processBusinessId,
                    fileCount + deltaCount,
                    featureCount + 1,
                    size + deltaSize, newSubOrderMsg
            );
            Tuple2<Boolean, String> resultTuple = newAcc.isOverProcessAndStorageLimits(orderProcessInfo);

             if(resultTuple.getT1()) {
                 return new FeatureAccumulator(orderId, processBusinessId, resultTuple.getT2());
             } else {
                return newAcc;
             }
        }

        public Tuple2<Boolean, String> isOverProcessAndStorageLimits(OrderProcessInfo orderProcessInfo) {
            SizeLimit sizeLimit = orderProcessInfo.getSizeLimit();
            SizeLimit.Type processLimitType = sizeLimit.getType();
            boolean countExceedsMaxExternalBucketSize = fileCount >= suborderSizeCounter.maxExternalBucketSize();
            boolean sizeExceedsStorageBucketSize = size >= suborderSizeCounter.getStorageBucketSize();
            boolean countExceedsProcessFilesLimit = processLimitType == SizeLimit.Type.FILES && sizeLimit.isExceededBy(fileCount);
            boolean countExceedsProcessFeatureLimit = processLimitType == SizeLimit.Type.FEATURES && sizeLimit.isExceededBy(featureCount);
            boolean sizeExceedsProcessBytesLimit = processLimitType == SizeLimit.Type.BYTES && sizeLimit.isExceededBy(size);

            boolean result = countExceedsMaxExternalBucketSize || sizeExceedsStorageBucketSize;
            result |= countExceedsProcessFilesLimit || countExceedsProcessFeatureLimit || sizeExceedsProcessBytesLimit;

            String subOrderMsg = "";
            if (result) {
                subOrderMsg = String.format("order=%s processUuid=%s Suborder interrupted because one of the " +
                                "following limit was reached : " +
                                " \"%s\" = %s," +
                                " \"%s\" = %s," +
                                " process info \"%s\" = %s," +
                                " process info \"%s\" = %s," +
                                " process info \"%s\" (size limit) = %s",
                        orderId, processBusinessId,
                        "MAX_EXTERNAL_BUCKET_FILE_COUNT", countExceedsMaxExternalBucketSize,
                        "MAX_STORAGE_BUCKET_SIZE", sizeExceedsStorageBucketSize,
                        SizeLimit.Type.FILES, countExceedsProcessFilesLimit,
                        SizeLimit.Type.FEATURES, countExceedsProcessFeatureLimit,
                        SizeLimit.Type.BYTES, sizeExceedsProcessBytesLimit
                );
                LOGGER.info(subOrderMsg);
            }
            return Tuples.of(result, subOrderMsg);
        }

        public boolean isInitial() {
            return size == 0L && fileCount == 0;
        }
    }

    // @formatter:on
}
