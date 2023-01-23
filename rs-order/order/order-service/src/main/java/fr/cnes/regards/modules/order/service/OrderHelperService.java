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
package fr.cnes.regards.modules.order.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.log.CorrelationIdUtils;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.job.StorageFilesJob;
import fr.cnes.regards.modules.order.service.job.parameters.FilesJobParameter;
import fr.cnes.regards.modules.order.service.job.parameters.SubOrderAvailabilityPeriodJobParameter;
import fr.cnes.regards.modules.order.service.job.parameters.UserJobParameter;
import fr.cnes.regards.modules.order.service.job.parameters.UserRoleJobParameter;
import fr.cnes.regards.modules.order.service.settings.IOrderSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static fr.cnes.regards.modules.order.domain.log.LogUtils.ORDER_ID_LOG_KEY;

@Service
@RefreshScope
public class OrderHelperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderHelperService.class);

    private final JWTService jwtService;

    private final IJobInfoService jobInfoService;

    private final IAuthenticationResolver authenticationResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IProjectUsersClient projectUsersClient;

    private final IOrderSettingsService orderSettingsService;

    private final OrderRequestResponseService orderRequestResponseService;

    @Value("${regards.order.secret}")
    private String secret;

    @Value("${prefix.path}")
    private String urlPrefix;

    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.order.cache.isadmin.size:1000}")
    private long maxCacheSize;

    @Value("${regards.order.cache.isadmin.ttl:10}")
    private long cacheTtl;

    private LoadingCache<String, Boolean> isAdminCache;

    public OrderHelperService(JWTService jwtService,
                              IJobInfoService jobInfoService,
                              IAuthenticationResolver authenticationResolver,
                              IRuntimeTenantResolver runtimeTenantResolver,
                              IProjectUsersClient projectUsersClient,
                              IOrderSettingsService orderSettingsService,
                              OrderRequestResponseService orderRequestResponseService) {
        this.jwtService = jwtService;
        this.jobInfoService = jobInfoService;
        this.authenticationResolver = authenticationResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.projectUsersClient = projectUsersClient;
        this.orderSettingsService = orderSettingsService;
        this.orderRequestResponseService = orderRequestResponseService;
    }

    public static boolean isOrderEffectivelyInPause(Order order) {
        // No associated jobInfo or all associated jobs finished
        return order.getDatasetTasks()
                    .stream()
                    .flatMap(dsTask -> dsTask.getReliantTasks().stream())
                    .noneMatch(ft -> ft.getJobInfo() != null) || order.getDatasetTasks()
                                                                      .stream()
                                                                      .flatMap(dsTask -> dsTask.getReliantTasks()
                                                                                               .stream())
                                                                      .filter(ft -> ft.getJobInfo() != null)
                                                                      .map(ft -> ft.getJobInfo()
                                                                                   .getStatus()
                                                                                   .getStatus())
                                                                      .allMatch(JobStatus::isFinished);
    }

    @EventListener
    public void onApplicationReadyEvent(ApplicationReadyEvent event) {
        initIsAdminCache();
    }

    @MultitenantTransactional
    public UUID createStorageSubOrder(DatasetTask datasetTask,
                                      Set<OrderDataFile> orderDataFiles,
                                      long orderId,
                                      String owner,
                                      int subOrderDuration,
                                      String role,
                                      int priority) {
        LOGGER.info("Creating storage sub-order of {} files (Order: {} - Owner: {})",
                    orderDataFiles.size(),
                    orderId,
                    owner);

        try {
            // Set log correlation id
            CorrelationIdUtils.setCorrelationId(ORDER_ID_LOG_KEY + orderId);

            FilesTask currentFilesTask = new FilesTask();
            currentFilesTask.setOrderId(orderId);
            currentFilesTask.setOwner(owner);
            currentFilesTask.addAllFiles(orderDataFiles);

            // storageJobInfo is pointed by currentFilesTask so it must be locked to avoid being cleaned before FilesTask
            JobInfo storageJobInfo = new JobInfo(true);
            storageJobInfo.setParameters(new FilesJobParameter(orderDataFiles.stream()
                                                                             .map(OrderDataFile::getId)
                                                                             .toArray(Long[]::new)),
                                         new SubOrderAvailabilityPeriodJobParameter(subOrderDuration),
                                         new UserJobParameter(owner),
                                         new UserRoleJobParameter(role));
            storageJobInfo.setOwner(owner);
            storageJobInfo.setClassName(StorageFilesJob.class.getName());
            storageJobInfo.setPriority(priority);

            JobInfo jobInfo = jobInfoService.createAsPending(storageJobInfo);
            currentFilesTask.setJobInfo(jobInfo);
            datasetTask.addReliantTask(currentFilesTask);
            LOGGER.info("Storage sub-order of {} files created (Order: {} - Owner: {})",
                        orderDataFiles.size(),
                        orderId,
                        owner);
            return jobInfo.getId();
        } finally {
            CorrelationIdUtils.clearCorrelationId();
        }
    }

    @MultitenantTransactional
    public void createExternalSubOrder(DatasetTask datasetTask,
                                       Set<OrderDataFile> orderDataFiles,
                                       long orderId,
                                       String owner,
                                       String correlationId) {

        LOGGER.info("Creating external sub-order of {} files (Order: {} - Owner: {})",
                    orderDataFiles.size(),
                    orderId,
                    owner);

        FilesTask currentFilesTask = new FilesTask();
        currentFilesTask.setOrderId(orderId);
        currentFilesTask.setOwner(owner);
        currentFilesTask.addAllFiles(orderDataFiles);
        datasetTask.addReliantTask(currentFilesTask);
        orderRequestResponseService.notifySuborderDone(correlationId, owner);
    }

    @MultitenantTransactional
    public void updateJobInfosExpirationDate(OffsetDateTime expirationDate, Set<UUID> jobInfosId) {
        jobInfoService.updateExpirationDate(expirationDate, jobInfosId);
    }

    public OffsetDateTime computeOrderExpirationDate(int subOrderCount, int subOrderDuration) {
        // calculate expiration depending on number of suborders
        long hoursCount = subOrderCount == 0 ? subOrderDuration : (long) (subOrderCount + 2) * subOrderDuration;
        if (hoursCount > orderSettingsService.getExpirationMaxDurationInHours()) {
            // avoid expiration date too far
            hoursCount = orderSettingsService.getExpirationMaxDurationInHours();
        }
        return OffsetDateTime.now().plusHours(hoursCount);
    }

    public String buildUrl() {
        return urlPrefix + "/" + new String(UriUtils.encode(microserviceName, Charset.defaultCharset().name())
                                                    .getBytes(), StandardCharsets.US_ASCII);
    }

    public String generateToken4PublicEndpoint(Order order) {
        return jwtService.generateToken(runtimeTenantResolver.getTenant(),
                                        authenticationResolver.getUser(),
                                        authenticationResolver.getUser(),
                                        authenticationResolver.getRole(),
                                        order.getExpirationDate(),
                                        Collections.singletonMap(IOrderService.ORDER_ID_KEY, order.getId().toString()),
                                        secret,
                                        true);
    }

    public String getCurrentUserRole() {
        return authenticationResolver.getRole();
    }

    public String getCurrentUser() {
        return authenticationResolver.getUser();
    }

    public String getRole(String user) {
        String role;
        try {
            FeignSecurityManager.asSystem();
            role = HateoasUtils.unwrap(projectUsersClient.retrieveProjectUserByEmail(user).getBody())
                               .getRole()
                               .getName();
        } finally {
            FeignSecurityManager.reset();
        }
        return role;
    }

    public boolean isCurrentUserOwnerOrAdmin(String orderOwner) {
        boolean isOwnerOrAdmin;
        if (Objects.equals(authenticationResolver.getUser(), orderOwner)) {
            isOwnerOrAdmin = true;
        } else {
            isOwnerOrAdmin = isAdmin(authenticationResolver.getUser());
        }
        return isOwnerOrAdmin;
    }

    public boolean isAdmin() {
        return isAdminCache.getUnchecked(authenticationResolver.getUser());
    }

    public boolean isAdmin(String user) {
        return isAdminCache.getUnchecked(user);
    }

    private void initIsAdminCache() {
        isAdminCache = CacheBuilder.newBuilder()
                                   .maximumSize(maxCacheSize)
                                   .expireAfterWrite(cacheTtl, TimeUnit.MINUTES)
                                   .build(new CacheLoader<String, Boolean>() {

                                       @Override
                                       public Boolean load(String user) {
                                           boolean isAdmin;
                                           try {
                                               FeignSecurityManager.asSystem();
                                               isAdmin = projectUsersClient.isAdmin(user).getBody();
                                           } finally {
                                               FeignSecurityManager.reset();
                                           }
                                           return isAdmin;
                                       }
                                   });
    }

}
