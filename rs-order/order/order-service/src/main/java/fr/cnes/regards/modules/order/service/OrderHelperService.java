package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.job.StorageFilesJob;
import fr.cnes.regards.modules.order.service.job.parameters.FilesJobParameter;
import fr.cnes.regards.modules.order.service.job.parameters.SubOrderAvailabilityPeriodJobParameter;
import fr.cnes.regards.modules.order.service.job.parameters.UserJobParameter;
import fr.cnes.regards.modules.order.service.job.parameters.UserRoleJobParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Service
@RefreshScope
public class OrderHelperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderHelperService.class);

    @Value("${regards.order.secret}")
    private String secret;
    @Value("${zuul.prefix}")
    private String urlPrefix;
    @Value("${spring.application.name}")
    private String microserviceName;

    private final JWTService jwtService;
    private final IJobInfoService jobInfoService;
    private final IAuthenticationResolver authResolver;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    public OrderHelperService(JWTService jwtService, IJobInfoService jobInfoService, IAuthenticationResolver authResolver, IRuntimeTenantResolver runtimeTenantResolver) {
        this.jwtService = jwtService;
        this.jobInfoService = jobInfoService;
        this.authResolver = authResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }


    @MultitenantTransactional
    public UUID createStorageSubOrder(DatasetTask datasetTask, Set<OrderDataFile> orderDataFiles, long orderId, String owner, int subOrderDuration, String role, int priority) {

        LOGGER.info("Creating storage sub-order of {} files (Order: {} - Owner: {})", orderDataFiles.size(), orderId, owner);

        FilesTask currentFilesTask = new FilesTask();
        currentFilesTask.setOrderId(orderId);
        currentFilesTask.setOwner(owner);
        currentFilesTask.addAllFiles(orderDataFiles);

        // storageJobInfo is pointed by currentFilesTask so it must be locked to avoid being cleaned before FilesTask
        JobInfo storageJobInfo = new JobInfo(true);
        storageJobInfo.setParameters(
                new FilesJobParameter(orderDataFiles.stream().map(OrderDataFile::getId).toArray(Long[]::new)),
                new SubOrderAvailabilityPeriodJobParameter(subOrderDuration),
                new UserJobParameter(owner), new UserRoleJobParameter(role)
        );
        storageJobInfo.setOwner(owner);
        storageJobInfo.setClassName(StorageFilesJob.class.getName());
        storageJobInfo.setPriority(priority);

        JobInfo jobInfo = jobInfoService.createAsPending(storageJobInfo);
        currentFilesTask.setJobInfo(jobInfo);
        datasetTask.addReliantTask(currentFilesTask);
        return jobInfo.getId();
    }

    @MultitenantTransactional
    public void createExternalSubOrder(DatasetTask datasetTask, Set<OrderDataFile> orderDataFiles, long orderId, String owner) {

        LOGGER.info("Creating external sub-order of {} files (Order: {} - Owner: {})", orderDataFiles.size(), orderId, owner);

        FilesTask currentFilesTask = new FilesTask();
        currentFilesTask.setOrderId(orderId);
        currentFilesTask.setOwner(owner);
        currentFilesTask.addAllFiles(orderDataFiles);
        datasetTask.addReliantTask(currentFilesTask);
    }

    @MultitenantTransactional
    public void updateJobInfosExpirationDate(OffsetDateTime expirationDate, Set<UUID> jobInfosId) {
        jobInfoService.updateExpirationDate(expirationDate, jobInfosId);
    }

    public OffsetDateTime computeOrderExpirationDate(OffsetDateTime currentDate, int subOrderCount, int subOrderDuration) {
        OffsetDateTime fromDate = OffsetDateTime.now();
        if (currentDate != null && currentDate.isAfter(fromDate)) {
            fromDate = currentDate;
        }
        long hoursCount = subOrderCount == 0 ? subOrderDuration : (long) (subOrderCount + 2) * subOrderDuration;
        return fromDate.plusHours(hoursCount);
    }

    public static boolean isOrderEffectivelyInPause(Order order) {
        // No associated jobInfo or all associated jobs finished
        return order.getDatasetTasks()
                .stream()
                .flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .noneMatch(ft -> ft.getJobInfo() != null)
                || order.getDatasetTasks()
                .stream()
                .flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .filter(ft -> ft.getJobInfo() != null)
                .map(ft -> ft.getJobInfo().getStatus().getStatus())
                .allMatch(JobStatus::isFinished);
    }

    public String buildUrl() {
        return urlPrefix + "/" + new String(UriUtils.encode(microserviceName, Charset.defaultCharset().name()).getBytes(), StandardCharsets.US_ASCII);
    }

    public String generateToken4PublicEndpoint(Order order) {
        return jwtService.generateToken(
                runtimeTenantResolver.getTenant(),
                authResolver.getUser(),
                authResolver.getUser(),
                authResolver.getRole(),
                order.getExpirationDate(),
                Collections.singletonMap(IOrderService.ORDER_ID_KEY, order.getId().toString()),
                secret,
                true
        );
    }

}
