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
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderJobService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Complement to OrderService when dealing with dataset selections having processing.
 *
 * @author Guillaume Andrieu
 *
 */
@Service
public class OrderProcessingService implements IOrderProcessingService {

    private static final String OCTET_STREAM = "application/octet-stream";

    protected final OrderProcessInfoMapper processInfoMapper = new OrderProcessInfoMapper();

    protected final BasketSelectionPageSearch basketSelectionPageSearch;
    protected final IProcessingRestClient processingClient;
    protected final SuborderSizeCounter suborderSizeCounter;
    protected final IOrderDataFileService orderDataFileService;
    protected final IOrderDataFileRepository orderDataFileRepository;
    protected final IOrderJobService orderJobService;
    protected final IJobInfoService jobInfoService;
    protected final int orderValidationPeriodDays;

    @Autowired
    public OrderProcessingService(
            BasketSelectionPageSearch basketSelectionPageSearch,
            IProcessingRestClient processingClient,
            SuborderSizeCounter suborderSizeCounter,
            IOrderDataFileService orderDataFileService,
            IOrderDataFileRepository orderDataFileRepository,
            IOrderJobService orderJobService,
            IJobInfoService jobInfoService,
            @Value("${regards.order.validation.period.days:3}") int orderValidationPeriodDays
    ) {
        this.basketSelectionPageSearch = basketSelectionPageSearch;
        this.processingClient = processingClient;
        this.suborderSizeCounter = suborderSizeCounter;
        this.orderDataFileService = orderDataFileService;
        this.orderDataFileRepository = orderDataFileRepository;
        this.orderJobService = orderJobService;
        this.jobInfoService = jobInfoService;
        this.orderValidationPeriodDays = orderValidationPeriodDays;
    }

    @Override
    public OrderCounts manageProcessedDatasetSelection(
            Order order,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole,
            OrderCounts orderCounts
    ) throws ModuleException {

        ProcessDatasetDescription processDatasetDesc = dsSel.getProcessDatasetDescription();
        UUID processBusinessId = processDatasetDesc.getProcessBusinessId();
        String processIdStr = processBusinessId.toString();
        try {
            ResponseEntity<PProcessDTO> response = processingClient.findByUuid(processIdStr);
            if (response.hasBody() && response.getStatusCode() == HttpStatus.OK) {
                PProcessDTO processDto = response.getBody();
                OrderProcessInfo orderProcessInfo = processInfoMapper.fromMap(processDto.getProcessInfo())
                        .getOrElseThrow(() -> new UnparsableProcessInfoException(
                                String.format("Unparsable process infos: order=%s user=%s dsSel=%s processDto=%s",
                                        order.getLabel(), user, dsSel.getId(), processDto)));
        
                List<DataType> requiredDatatypes = orderProcessInfo.getRequiredDatatypes();
                
                // Creates datasetTasks with required data types and update estimated size
                DatasetTask dsTask = DatasetTask.fromBasketSelection(dsSel, requiredDatatypes.toJavaList());
                dsTask.setFilesSize(orderProcessInfo.getSizeForecast().expectedResultSizeInBytes(dsTask.getFilesSize()));
        
                AtomicLong suborderCount = new AtomicLong(1);
        
                OrderCounts result = basketSelectionPageSearch.fluxSearchDataObjects(dsSel)
                        .groupBy(feature -> hasAtLeastOneRequiredFileInStorage(feature, requiredDatatypes))
                        .flatMap(featureGroup -> hasRequiredFilesInStorage(featureGroup)
                                ? manageFeaturesWithFilesInStorage(featureGroup, order, suborderCount, processDto, orderProcessInfo, dsSel, tenant, user, userRole)
                                : manageFeaturesWithOnlyExternalFiles(featureGroup, order, suborderCount, processDto, orderProcessInfo, dsSel, tenant, user, userRole)
                        )
                        .doOnNext(dsTask::addReliantTask)
                        .map(filesTask -> new OrderCounts(0, filesTask.getFiles().size(), 1))
                        .reduce(OrderCounts.initial(), OrderCounts::add)
                        .block();
        
                if (!dsTask.getReliantTasks().isEmpty()) {
                    order.addDatasetOrderTask(dsTask);
                }
                
                return OrderCounts.add(orderCounts, result);
            } else {
                throw new ModuleException("Error retrieving  process infor for id " + processIdStr) ;
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ModuleException(e.getMessage());
        }
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

        return windowAccordingToScopeAndSizeLimit(featureGroup, orderProcessInfo)
                .flatMap(features -> {
                    if (orderProcessInfo.getScope() == Scope.SUBORDER) {
                        return createProcessExecutionJobAndFilesTask(order, suborderCount, pProcessDTO, orderProcessInfo, dsSel, tenant, user, userRole, features);
                    } else {
                        return Flux.fromIterable(features)
                                .flatMap(feature -> createProcessExecutionJobAndFilesTask(order, suborderCount, pProcessDTO, orderProcessInfo, dsSel, tenant, user, userRole, List.of(feature)));
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
        JobInfo jobInfo = createProcessExecutionJobForFeatures(order, suborderCount, pProcessDTO, orderProcessInfo, dsSel, tenant, user, userRole, features);
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
        ProcessInputsPerFeature processInputsPerFeature = createInputFiles(features, orderProcessInfo.getRequiredDatatypes());
        Long suborderCountId = suborderCount.getAndIncrement();

        BatchSuborderCorrelationIdentifier batchSuborderIdentifier = new BatchSuborderCorrelationIdentifier(order.getId(), dsSel.getId(), suborderCountId);
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
                        new BasketDatasetSelectionJobParameter(dsSel)
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

        Cardinality cardinality = processInfo.getCardinality();
        List<DataType> requiredDataTypes = processInfo.getRequiredDatatypes();
        IResultSizeForecast sizeForecast = processInfo.getSizeForecast();
        Long orderId = batchSuborderIdentifier.getOrderId();

        if (cardinality == Cardinality.ONE_PER_EXECUTION) {
            List<DataFile> applicableDataFilesIn = features.flatMap(f -> List.ofAll(f.getFiles().values()))
                    .filter(f -> requiredDataTypes.contains(f.getDataType()));

            long expectedSize = sizeForecast.expectedResultSizeInBytes(applicableDataFilesIn.map(DataFile::getFilesize).reduce(Long::sum));

            DataFile dataFileOut = DataFile.build(
                    DataType.OTHER,
                    batchSuborderIdentifier.repr(),
                    ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier),
                    MimeType.valueOf(OCTET_STREAM),
                    true,
                    true
            );
            dataFileOut.setFilesize(expectedSize);
            return List.of(createAndSaveOrderDataFile(dataFileOut, dsSelIpId, orderId))
                    .map(OrderDataFile::getId)
                    .toJavaArray(Long[]::new);
        } else {
            return features.flatMap(feature -> {
                if (cardinality == Cardinality.ONE_PER_FEATURE) {
                    List<DataFile> applicableDataFilesIn = List.ofAll(feature.getFiles().values())
                            .filter(f -> requiredDataTypes.contains(f.getDataType()));
                    long expectedSize = sizeForecast.expectedResultSizeInBytes(applicableDataFilesIn.map(DataFile::getFilesize).reduce(Long::sum));
                    DataFile dataFileOut = DataFile.build(
                            DataType.OTHER,
                            feature.getId().toString(),
                            ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier, feature),
                            MimeType.valueOf(OCTET_STREAM),
                            true,
                            true
                    );
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
        orderDataFileService.save(orderDataFile);
        return orderDataFile;
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

    protected Flux<List<EntityFeature>> windowAccordingToScopeAndSizeLimit(GroupedFlux<Boolean, EntityFeature> featureGroup, OrderProcessInfo orderProcessInfo) {
        return featureGroup
                .publish(fg -> fg.zipWith(fg.scan(
                        new FeatureAccumulator(),
                        (acc, f) -> acc.addFeatureAndReturnInitialAccumulatorIfOverLimits(f, orderProcessInfo))
                )
                .bufferUntil(featAndAcc -> featAndAcc.getT2().isInitial(), true)
                .map(featsAndAccs -> List.ofAll(featsAndAccs).map(Tuple2::getT1)));
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
            String userRole
    ) {
        return windowAccordingToScopeAndSizeLimit(featureGroup, orderProcessInfo)
                .flatMap(features -> createProcessJobAndStorageFilesJobForFeatures(order, suborderCount, pProcessDTO, orderProcessInfo, dsSel, tenant, user, userRole, features));
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
            List<EntityFeature> features
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
                        new SubOrderAvailabilityPeriodJobParameter(orderValidationPeriodDays),
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
                )
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
        int count;
        long size;

        public FeatureAccumulator() {
            count = 0;
            size = 0L;
        }

        public FeatureAccumulator addFeatureAndReturnInitialAccumulatorIfOverLimits(EntityFeature feature, OrderProcessInfo orderProcessInfo) {
            List<DataFile> files = featureRequiredDatafiles(feature, orderProcessInfo.getRequiredDatatypes()).collect(List.collector());
            int deltaCount = files.size();
            long deltaSize = files.map(DataFile::getFilesize).sum().longValue();
            FeatureAccumulator newAcc = new FeatureAccumulator(count + deltaCount, size + deltaSize);
            return newAcc.isOverProcessAndStorageLimits(orderProcessInfo) ? new FeatureAccumulator() : newAcc;
        }

        public boolean isOverProcessAndStorageLimits(OrderProcessInfo orderProcessInfo) {
            SizeLimit sizeLimit = orderProcessInfo.getSizeLimit();
            SizeLimit.Type processLimitType = sizeLimit.getLimit() == 0L ? SizeLimit.Type.NO_LIMIT : sizeLimit.getType();
            Long processLimitValue = sizeLimit.getLimit();
            boolean countExceedsMaxExternalBucketSize = count >= suborderSizeCounter.maxExternalBucketSize();
            boolean sizeExceedsStorageBucketSize = size >= suborderSizeCounter.getStorageBucketSize();
            boolean countExceedsProcessFilesLimit = processLimitType == SizeLimit.Type.FILES && count >= processLimitValue;
            boolean sizeExceedsProcessBytesLimit = processLimitType == SizeLimit.Type.BYTES && size >= processLimitValue;
            return countExceedsMaxExternalBucketSize
                    || sizeExceedsStorageBucketSize
                    || countExceedsProcessFilesLimit
                    || sizeExceedsProcessBytesLimit;
        }

        public boolean isInitial() {
            return size == 0L && count == 0;
        }
    }
}
