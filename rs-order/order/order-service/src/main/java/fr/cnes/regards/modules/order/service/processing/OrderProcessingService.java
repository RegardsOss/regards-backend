package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
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
import fr.cnes.regards.modules.processing.order.*;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.apache.commons.lang3.NotImplementedException;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderProcessingService implements IOrderProcessingService {

    protected final OrderProcessInfoMapper processInfoMapper = new OrderProcessInfoMapper();

    protected final BasketSelectionPageSearch basketSelectionPageSearch;
    protected final IProcessingRestClient processingClient;
    protected final SuborderSizeCounter suborderSizeCounter;
    protected final IOrderDataFileService orderDataFileService;
    protected final IOrderJobService orderJobService;
    protected final IJobInfoService jobInfoService;
    protected final int orderValidationPeriodDays;

    @Autowired
    public OrderProcessingService(
            BasketSelectionPageSearch basketSelectionPageSearch,
            IProcessingRestClient processingClient,
            SuborderSizeCounter suborderSizeCounter,
            IOrderDataFileService orderDataFileService,
            IOrderJobService orderJobService,
            IJobInfoService jobInfoService,
            @Value("${regards.order.validation.period.days:3}") int orderValidationPeriodDays
    ) {
        this.basketSelectionPageSearch = basketSelectionPageSearch;
        this.processingClient = processingClient;
        this.suborderSizeCounter = suborderSizeCounter;
        this.orderDataFileService = orderDataFileService;
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
    ) {

        DatasetTask dsTask = DatasetTask.fromBasketSelection(dsSel);

        ProcessDatasetDescription processDatasetDesc = dsSel.getProcessDatasetDescription();
        UUID processBusinessId = processDatasetDesc.getProcessBusinessId();
        String processIdStr = processBusinessId.toString();
        PProcessDTO processDto = processingClient.findByUuid(processIdStr).getBody();

        OrderProcessInfo orderProcessInfo = processInfoMapper.fromMap(processDto.getProcessInfo())
                .getOrElseThrow(() -> new UnparsableProcessInfoException(
                        String.format("Unparsable process infos: order=%s user=%s dsSel=%s processDto=%s",
                                order.getId(), user, dsSel.getId(), processDto)));

        List<DataType> requiredDatatypes = orderProcessInfo.getRequiredDatatypes();

        AtomicLong suborderCount = new AtomicLong(1);

        OrderCounts result = basketSelectionPageSearch.fluxSearchDataObjects(dsSel)
                .groupBy(feature -> hasAtLeastOneInternalRequiredFile(feature, requiredDatatypes))
                .flatMap(featureGroup -> hasInternalRequiredFiles(featureGroup)
                        ? manageFeaturesWithInternalFiles(featureGroup, order, suborderCount, orderProcessInfo, dsSel, tenant, user, userRole)
                        : manageFeaturesWithOnlyExternalFiles(featureGroup, order, suborderCount, orderProcessInfo, dsSel, tenant, user, userRole)
                )
                .doOnNext(dsTask::addReliantTask)
                .map(filesTask -> new OrderCounts(0, filesTask.getFiles().size(), 1))
                .reduce(OrderCounts.initial(), OrderCounts::add)
                .block();

        if (!dsTask.getReliantTasks().isEmpty()) {
            order.addDatasetOrderTask(dsTask);
        }

        return OrderCounts.add(orderCounts, result);
    }

    protected Publisher<FilesTask> manageFeaturesWithOnlyExternalFiles(
            GroupedFlux<Boolean, EntityFeature> featureGroup,
            Order order,
            AtomicLong suborderCount,
            OrderProcessInfo orderProcessInfo,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole
    ) {

        return windowAccordingToScopeAndSizeLimit(featureGroup, orderProcessInfo)
                .flatMap(features -> {
                    if (orderProcessInfo.getScope() == Scope.SUBORDER) {
                        return createProcessExecutionJobAndFilesTask(order, suborderCount, orderProcessInfo, dsSel, tenant, user, userRole, features);
                    } else {
                        return Flux.fromIterable(features)
                                .flatMap(feature -> createProcessExecutionJobAndFilesTask(order, suborderCount, orderProcessInfo, dsSel, tenant, user, userRole, List.of(feature)));
                    }
                });
    }

    protected Publisher<FilesTask> createProcessExecutionJobAndFilesTask(
            Order order,
            AtomicLong suborderCount,
            OrderProcessInfo orderProcessInfo,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole,
            List<EntityFeature> features
    ) {
        JobInfo jobInfo = createProcessExecutionJobForFeatures(order, suborderCount, orderProcessInfo, dsSel, tenant, user, userRole, features);
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
        OrderDataFile[] outputFiles = createOutputFiles(
                batchSuborderIdentifier,
                UniformResourceName.fromString(dsSel.getDatasetIpid()),
                features,
                orderProcessInfo.getCardinality(),
                orderProcessInfo.getRequiredDatatypes()
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
                        new ProcessDTOJobParameter(orderProcessInfo),
                        new ProcessBatchCorrelationIdJobParameter(batchSuborderIdentifier.repr())
                ).toJavaSet(),
                user,
                ProcessExecutionJob.class.getName()
        );
        result.setExpirationDate(order.getExpirationDate());
        return result;
    }

    protected OrderDataFile[] createOutputFiles(
            BatchSuborderCorrelationIdentifier batchSuborderIdentifier,
            UniformResourceName dsSelIpId,
            List<EntityFeature> features,
            Cardinality cardinality,
            List<DataType> requiredDataTypes
    ) {
        Long orderId = batchSuborderIdentifier.getOrderId();

        if (cardinality == Cardinality.ONE_PER_EXECUTION) {
            DataFile dataFile = DataFile.build(
                    DataType.OTHER,
                    batchSuborderIdentifier.repr(),
                    ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier),
                    MimeType.valueOf("application/octet-stream"),
                    true,
                    true
            );
            return new OrderDataFile[]{createAndSaveOrderDataFile(dataFile, dsSelIpId, orderId)};
        } else {
            return features.flatMap(feature -> {
                if (cardinality == Cardinality.ONE_PER_FEATURE) {
                    DataFile dataFile = DataFile.build(
                            DataType.OTHER,
                            feature.getId().toString(),
                            ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier, feature),
                            MimeType.valueOf("application/octet-stream"),
                            true,
                            true
                    );
                    return List.of(createAndSaveOrderDataFile(dataFile, feature.getId(), orderId));
                } else if (cardinality == Cardinality.ONE_PER_INPUT_FILE) {
                    return featureRequiredDatafiles(feature, requiredDataTypes)
                            .map(dataFile -> {
                                DataFile dataFileOut = DataFile.build(
                                        DataType.OTHER,
                                        dataFile.getFilename(),
                                        ProcessInputCorrelationIdentifier.repr(batchSuborderIdentifier, feature, dataFile),
                                        MimeType.valueOf("application/octet-stream"),
                                        true,
                                        true
                                );
                                return createAndSaveOrderDataFile(dataFileOut, feature.getId(), orderId);
                            })
                            .collect(Collectors.toList());
                } else {
                    // Happens only if some new cases appear for Cardinality
                    throw new NotImplementedException("New Cardinality case missing");
                }
            }).toJavaArray(OrderDataFile[]::new);
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

    protected Flux<FilesTask> manageFeaturesWithInternalFiles(
            GroupedFlux<Boolean, EntityFeature> featureGroup,
            Order order,
            AtomicLong suborderCount,
            OrderProcessInfo orderProcessInfo,
            BasketDatasetSelection dsSel,
            String tenant,
            String user,
            String userRole
    ) {
        return windowAccordingToScopeAndSizeLimit(featureGroup, orderProcessInfo)
                .flatMap(features -> createProcessJobAndStorageFilesJobForFeatures(order, suborderCount, orderProcessInfo, dsSel, tenant, user, userRole, features));
    }

    private Mono<FilesTask> createProcessJobAndStorageFilesJobForFeatures(Order order, AtomicLong suborderCount, OrderProcessInfo orderProcessInfo, BasketDatasetSelection dsSel, String tenant, String user, String userRole, List<EntityFeature> features) {
        JobInfo processExecJobUnsaved = createProcessExecutionJobForFeatures(order, suborderCount, orderProcessInfo, dsSel, tenant, user, userRole, features);
        processExecJobUnsaved.setExpirationDate(order.getExpirationDate());
        JobInfo processExecJob = jobInfoService.createAsPending(processExecJobUnsaved);

        OrderDataFile[] outputFiles = extractOutputFilesFromProcessExecJob(processExecJob);
        OrderDataFile[] internalInputFiles = findInternalInputFiles(order.getId(), features);
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

        FilesTask filesTask = new FilesTask();
        filesTask.setOrderId(order.getId());
        filesTask.setOwner(user);
        filesTask.addAllFiles(List.of(outputFiles).toJavaList());
        filesTask.setJobInfo(storageFilesJob);
        return Mono.just(filesTask);
    }

    private OrderDataFile[] extractOutputFilesFromProcessExecJob(JobInfo processExecJob) {
        return processExecJob.getParametersAsMap().get(ProcessOutputFilesJobParameter.NAME).getValue();
    }

    private OrderDataFile[] findInternalInputFiles(Long orderId, List<EntityFeature> features) {
        return features
                .flatMap(f -> List.ofAll(f.getFiles().values())
                        .filter(file -> !file.isReference())
                        .map(file -> new OrderDataFile(file, f.getId(), orderId))
                )
                .toJavaArray(OrderDataFile[]::new);
    }

    protected boolean hasAtLeastOneInternalRequiredFile(EntityFeature entityFeature, List<DataType> requiredDatatypes) {
        return featureRequiredDatafiles(entityFeature, requiredDatatypes)
                .anyMatch(DataFile::isReference);
    }

    protected boolean hasInternalRequiredFiles(GroupedFlux<Boolean, EntityFeature> featureGroup) {
        return Option.of(featureGroup.key()).getOrElse(false);
    }

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
            SizeLimit.Type processLimitType = sizeLimit.getType();
            Long processLimitValue = sizeLimit.getLimit();
            return count > suborderSizeCounter.maxExternalBucketSize()
                    || size > suborderSizeCounter.getStorageBucketSize()
                    || (processLimitType == SizeLimit.Type.FILES && count > processLimitValue)
                    || (processLimitType == SizeLimit.Type.BYTES && size > processLimitValue);
        }

        public boolean isInitial() {
            return size == 0L && count == 0;
        }
    }
}
