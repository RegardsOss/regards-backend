/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.templates.service.TemplateService;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;


@Service
@MultitenantTransactional
@RefreshScope
@EnableScheduling
public class OrderMaintenanceService implements IOrderMaintenanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderMaintenanceService.class);

    @Value("${regards.order.days.before.considering.order.as.aside:7}")
    private int daysBeforeSendingNotifEmail;

    @Autowired
    private IOrderMaintenanceService self;

    private final IOrderService orderService;
    private final IOrderRepository orderRepository;
    private final IOrderDataFileService orderDataFileService;
    private final IJobInfoService jobInfoService;
    private final ITenantResolver tenantResolver;
    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final TemplateService templateService;
    private final IEmailClient emailClient;

    public OrderMaintenanceService(IOrderService orderService, IOrderRepository orderRepository, IOrderDataFileService orderDataFileService, IJobInfoService jobInfoService,
                                   ITenantResolver tenantResolver, IRuntimeTenantResolver runtimeTenantResolver, TemplateService templateService, IEmailClient emailClient
    ) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.orderDataFileService = orderDataFileService;
        this.jobInfoService = jobInfoService;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.templateService = templateService;
        this.emailClient = emailClient;
    }


    @Override
    @Transactional(propagation = Propagation.NEVER) // Must not create a transaction, it is a multitenant method
    @Scheduled(fixedDelayString = "${regards.order.computation.update.rate.ms:1000}")
    public void updateCurrentOrdersComputations() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                self.updateTenantOrdersComputations();
            } catch (Exception e) {
                // FIXME - The Spring type of exception is not stable yet
                // So the catch can be more specific once Spring will be updated 5.3.0
                // @see https://github.com/spring-projects/spring-framework/issues/24064
                LOGGER.warn("Failed to update orders as the database returned us a serialisation anomaly", e);
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTenantOrdersComputations() {
        Set<Order> orders = orderDataFileService.updateCurrentOrdersComputedValues();
        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);
        }
        // Because previous method (updateCurrentOrdersComputedValues) takes care of CURRENT jobs, it is necessary
        // to update finished ones ie setting availableFilesCount to 0 for finished jobs not waiting for user
        List<Order> finishedOrders = orderRepository.findFinishedOrdersToUpdate();
        if (!finishedOrders.isEmpty()) {
            // For orders with DONE status and download errors, set status to DONE_WITH_WARNING
            finishedOrders.stream().filter(order -> OrderStatus.DONE.equals(order.getStatus()))
                    .forEach(order -> {
                        if (orderDataFileService.hasDownloadErrors(order.getId())) {
                            order.setStatus(OrderStatus.DONE_WITH_WARNING);
                        }
                    });
            finishedOrders.forEach(order -> order.setAvailableFilesCount(0));
            orderRepository.saveAll(finishedOrders);
        }
    }

    /**
     * 0 0 7 * * MON-FRI : every working day at 7 AM
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)// Must not create a transaction, it is a multitenant method
    @Scheduled(cron = "${regards.order.periodic.files.availability.check.cron:0 0 7 * * MON-FRI}")
    public void sendPeriodicNotifications() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            self.sendTenantPeriodicNotifications();
        }
    }

    @Override
    public void sendTenantPeriodicNotifications() {
        List<Order> asideOrders = orderRepository.findAsideOrders(daysBeforeSendingNotifEmail);

        Multimap<String, Order> orderMultimap = TreeMultimap.create(Comparator.naturalOrder(),
                                                                    Comparator.comparing(Order::getCreationDate)
        );
        asideOrders.forEach(o -> orderMultimap.put(o.getOwner(), o));

        // For each owner
        for (Map.Entry<String, Collection<Order>> entry : orderMultimap.asMap().entrySet()) {
            OffsetDateTime now = OffsetDateTime.now();
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("orders", entry.getValue());
            dataMap.put("project", runtimeTenantResolver.getTenant());
            // Create mail
            String message;
            try {
                message = templateService.render(OrderTemplateConf.ASIDE_ORDERS_NOTIFICATION_TEMPLATE_NAME, dataMap);
            } catch (TemplateException e) {
                throw new RsRuntimeException(e);
            }

            // Send it
            FeignSecurityManager.asSystem();
            emailClient.sendEmail(message, "Reminder: some orders are waiting for you", null, entry.getKey());
            FeignSecurityManager.reset();
            // Update order availableUpdateDate to avoid another microservice instance sending notification emails
            entry.getValue().forEach(order -> order.setAvailableUpdateDate(now));
            orderRepository.saveAll(entry.getValue());
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER) // Must not create a transaction, it is a multitenant method
    @Scheduled(fixedDelayString = "${regards.order.clean.expired.rate.ms:3600000}")
    public void cleanExpiredOrders() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            // In a transaction
            Optional<Order> optional = findOneOrderAndMarkAsExpired();
            while (optional.isPresent()) {
                // in another transaction
                self.cleanExpiredOrder(optional.get());
                // and again
                optional = findOneOrderAndMarkAsExpired();
            }
        }
    }

    @Override
    public Optional<Order> findOneOrderAndMarkAsExpired() {
        Optional<Order> optional = orderRepository.findOneExpiredOrder();
        optional.ifPresent(order -> {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
        });
        return optional;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    // No transaction because :
    // - loadComplete use a new one and so when delete is called, order state is at start of transaction (so with state
    // EXPIRED)
    // - loadComplete needs a new transaction each time it is called. If nothing is specified, it seems that the same
    // transaction is used each time loadComplete is called (I think it is due to Hibernate Flush mode set as NEVER
    // specified by Spring so it is cached in first level)
    public void cleanExpiredOrder(Order order) {
        // Ask for all jobInfos abortion (don't call self.pause() because of status, order must stay EXPIRED)
        order.getDatasetTasks().stream().flatMap(dsTask -> dsTask.getReliantTasks().stream()).map(FilesTask::getJobInfo)
                .forEach(jobInfo -> jobInfoService.stopJob(jobInfo.getId()));

        // Wait for its complete stop
        order = orderService.loadComplete(order.getId());
        while (!OrderHelperService.isOrderEffectivelyInPause(order)) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RsRuntimeException(e); // NOSONAR
            }
            order = orderService.loadComplete(order.getId());
        }
        // Delete all its data files
        // Don't forget no relation is hardly mapped between OrderDataFile and Order
        orderDataFileService.removeAll(order.getId());
        // Delete all filesTasks
        for (DatasetTask dsTask : order.getDatasetTasks()) {
            dsTask.getReliantTasks().clear();
        }
        // Deactivate waitingForUser tag
        order.setWaitingForUser(false);
        // Order is already at EXPIRED state so let it be
        orderRepository.save(order);
    }

}
