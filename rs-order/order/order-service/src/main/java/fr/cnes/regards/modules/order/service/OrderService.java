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

import com.google.common.base.Strings;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.dao.OrderSpecifications;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.exception.*;
import fr.cnes.regards.modules.order.service.settings.IOrderSettingsService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@MultitenantTransactional
public class OrderService implements IOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    /**
     * Format for generated order label
     */
    private static final String ORDER_GENERATED_LABEL_FORMAT = "Order of %s";
    /**
     * Date formatter for order generated label
     */
    private static final DateTimeFormatter ORDER_GENERATED_LABEL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd 'at' HH:mm:ss");

    @Autowired
    private IOrderService self;

    private final IOrderRepository orderRepository;
    private final IBasketService basketService;
    private final IOrderCreationService orderCreationService;
    private final IOrderRetryService orderRetryService;
    private final IOrderDataFileService dataFileService;
    private final IJobInfoService jobInfoService;
    private final IOrderJobService orderJobService;
    private final ITenantResolver tenantResolver;
    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final IOrderSettingsService orderSettingsService;
    private final OrderHelperService orderHelperService;

    public OrderService(IOrderRepository orderRepository, IBasketService basketService, IOrderCreationService orderCreationService, IOrderRetryService orderRetryService,
                        IOrderDataFileService dataFileService, IJobInfoService jobInfoService, IOrderJobService orderJobService, ITenantResolver tenantResolver,
                        IRuntimeTenantResolver runtimeTenantResolver, IOrderSettingsService orderSettingsService, OrderHelperService orderHelperService
    ) {
        this.basketService = basketService;
        this.orderRepository = orderRepository;
        this.orderCreationService = orderCreationService;
        this.orderRetryService = orderRetryService;
        this.dataFileService = dataFileService;
        this.jobInfoService = jobInfoService;
        this.orderJobService = orderJobService;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.orderSettingsService = orderSettingsService;
        this.orderHelperService = orderHelperService;
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void init(ApplicationReadyEvent event) {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                LOGGER.info("OrderService created with : userOrderParameters: {}, appSubOrderDuration: {}",
                            orderSettingsService.getUserOrderParameters(),
                            orderSettingsService.getAppSubOrderDuration());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Override
    public Order getOrder(Long orderId) {
        return orderRepository.findCompleteById(orderId);
    }

    @Override
    public Order loadSimple(Long id) {
        return orderRepository.findSimpleById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order loadComplete(Long id) {
        return orderRepository.findCompleteById(id);
    }

    @Override
    public Page<Order> findAll(Pageable pageRequest) {
        return orderRepository.findAllByOrderByCreationDateDesc(pageRequest);
    }

    @Override
    public Page<Order> findAll(String user, Pageable pageRequest, OrderStatus... excludeStatuses) {
        if (excludeStatuses.length == 0) {
            return orderRepository.findAllByOwnerOrderByCreationDateDesc(user, pageRequest);
        } else {
            return orderRepository.findAllByOwnerAndStatusNotInOrderByCreationDateDesc(user, excludeStatuses, pageRequest);
        }
    }

    @Override
    public boolean isPaused(Long orderId) {
        Order order = self.loadComplete(orderId);
        if (order.getStatus() != OrderStatus.PAUSED) {
            return false;
        }
        return OrderHelperService.isOrderEffectivelyInPause(order);
    }

    @Override
    public boolean hasProcessing(Order order) {
        return order.getDatasetTasks().stream().anyMatch(DatasetTask::hasProcessing);
    }

    @Override
    public Order createOrder(Basket basket, String label, String url, int subOrderDuration) throws EntityInvalidException {
        return createOrder(basket, label, url, subOrderDuration, basket.getOwner());
    }

    private Order createOrder(Basket basket, String label, String url, int subOrderDuration, String user) throws EntityInvalidException {

        LOGGER.info("Generate and / or check label is unique for owner before creating back");
        // generate label when none is provided
        String orderLabel = label;
        if (Strings.isNullOrEmpty(orderLabel)) {
            orderLabel = String.format(ORDER_GENERATED_LABEL_FORMAT, ORDER_GENERATED_LABEL_DATE_FORMAT.format(OffsetDateTime.now()));
        }
        // check length (>0 is already checked above)
        if (orderLabel.length() > Order.LABEL_FIELD_LENGTH) {
            throw new EntityInvalidException(OrderLabelErrorEnum.TOO_MANY_CHARACTERS_IN_LABEL.toString());
        } else { // check unique for current owner
            Optional<Order> sameOrderLabelOpt = orderRepository.findByLabelAndOwner(orderLabel, user);
            if (sameOrderLabelOpt.isPresent()) {
                throw new EntityInvalidException(OrderLabelErrorEnum.LABEL_NOT_UNIQUE_FOR_OWNER.toString());
            }
        }

        // Create order and ensure it's properly persisted
        Order order = new Order();
        order.setOwner(user);
        order.setLabel(orderLabel);
        order.setFrontendUrl(url);
        order = self.create(order);

        // Asynchronous operation
        orderCreationService.asyncCompleteOrderCreation(basket, order.getId(), subOrderDuration, orderHelperService.getRole(user), runtimeTenantResolver.getTenant());
        return order;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order create(Order order) {
        LOGGER.info("Creating order with owner {}", order.getOwner());
        order.setCreationDate(OffsetDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        return orderRepository.save(order);
    }

    @Override
    public void pause(Long id) throws ModuleException {

        checkAction(id, Action.PAUSE);
        Order order = orderRepository.findCompleteById(id);

        // Ask for all jobInfos abortion
        order.getDatasetTasks().stream()
                .flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(FilesTask::getJobInfo)
                .forEach(jobInfo -> {
                    if (jobInfo != null) {
                        jobInfoService.stopJob(jobInfo.getId());
                    }
                });
        order.setStatus(OrderStatus.PAUSED);
        orderRepository.save(order);
    }

    @Override
    public void resume(Long id) throws ModuleException {

        checkAction(id, Action.RESUME);
        Order order = orderRepository.findCompleteById(id);

        // Passes all ABORTED jobInfo to PENDING
        order.getDatasetTasks().stream()
                .flatMap(dsTask -> dsTask.getReliantTasks().stream())
                .map(FilesTask::getJobInfo)
                .filter(jobInfo -> jobInfo.getStatus().getStatus() == JobStatus.ABORTED)
                .forEach(jobInfo -> {
                    jobInfo.updateStatus(JobStatus.PENDING);
                    jobInfoService.save(jobInfo);
                });
        order.setStatus(OrderStatus.RUNNING);
        orderRepository.save(order);
        // Don't forget to manage user order jobs again (PENDING -> QUEUED)
        orderJobService.manageUserOrderStorageFilesJobInfos(order.getOwner());
    }

    @Override
    public void delete(Long id) throws ModuleException {

        checkAction(id, Action.DELETE);
        Order order = orderRepository.findCompleteById(id);

        // Delete all order data files
        dataFileService.removeAll(order.getId());
        // Delete all filesTasks
        for (DatasetTask dsTask : order.getDatasetTasks()) {
            dsTask.getReliantTasks().clear();
        }
        // Deactivate waitingForUser tag
        order.setWaitingForUser(false);
        order.setStatus(OrderStatus.DELETED);
        orderRepository.save(order);
    }

    @Override
    public Order restart(long oldOrderId, String label, String successUrl) throws ModuleException {

        checkAction(oldOrderId, Action.RESTART);
        String oldOrderOwner = orderRepository.findSimpleById(oldOrderId).getOwner();
        Basket oldBasket;

        try {
            oldBasket = basketService.find(BASKET_OWNER_PREFIX + oldOrderId);
        } catch (EmptyBasketException e) {
            // This should not happen, since basket is not deleted anymore - check is added for transition period
            throw new CannotRestartOrderException("BASKET_NOT_FOUND");
        }

        Basket newBasket = basketService.duplicate(oldBasket.getId(), BASKET_RESTART_OWNER_PREFIX + oldOrderId);

        return createOrder(newBasket, label, successUrl, orderSettingsService.getUserOrderParameters().getSubOrderDuration(), oldOrderOwner);
    }

    @Override
    public void retryErrors(long orderId) throws ModuleException {

        checkAction(orderId, Action.RETRY);
        Order order = orderRepository.findSimpleById(orderId);

        String orderOwner = order.getOwner();
        String orderOwnerRole = orderHelperService.getRole(orderOwner);

        orderRetryService.asyncCompleteRetry(order.getId(), orderOwnerRole, orderSettingsService.getUserOrderParameters().getSubOrderDuration(), runtimeTenantResolver.getTenant());
    }

    @Override
    public void remove(Long id) throws ModuleException {
        checkAction(id, Action.REMOVE);
        Order order = orderRepository.findCompleteById(id);
        // Data files have already been deleted so there's only the basket and the order to remove
        basketService.deleteIfExists(BASKET_OWNER_PREFIX + order.getId());
        orderRepository.deleteById(order.getId());
    }

    @Override
    public void writeAllOrdersInCsv(BufferedWriter writer, OrderStatus status, OffsetDateTime from, OffsetDateTime to)
            throws IOException {
        List<Order> orders = orderRepository.findAll(OrderSpecifications.search(status, from, to), Sort.by(Sort.Direction.ASC, "id"));
        writer.append("ORDER_ID;CREATION_DATE;EXPIRATION_DATE;OWNER;STATUS;STATUS_DATE;PERCENT_COMPLETE;FILES_IN_ERROR");
        writer.newLine();
        for (Order order : orders) {
            writer.append(order.getId().toString()).append(';');
            writer.append(OffsetDateTimeAdapter.format(order.getCreationDate())).append(';');
            writer.append(OffsetDateTimeAdapter.format(order.getExpirationDate())).append(';');
            writer.append(order.getOwner()).append(';');
            writer.append(order.getStatus().toString()).append(';');
            writer.append(OffsetDateTimeAdapter.format(order.getStatusDate())).append(';');
            writer.append(Integer.toString(order.getPercentCompleted())).append(';');
            writer.append(Integer.toString(order.getFilesInErrorCount()));
            writer.newLine();
        }
        writer.close();
    }

    @Override
    public boolean isActionAvailable(long orderId, Action action) {
        return StringUtils.isEmpty(getErrorMessageOnAction(orderRepository.findSimpleById(orderId), action));
    }

    private void checkAction(long orderId, Action action) throws ModuleException {
        Order order = orderRepository.findSimpleById(orderId);
        if (order == null) {
            throw new EntityNotFoundException(orderId, Order.class);
        }
        String message = getErrorMessageOnAction(order, action);
        if (!StringUtils.isEmpty(message)) {
            throw action.getException(message);
        }
    }

    private String getErrorMessageOnAction(Order order, Action action) {
        String message = null;
        if (!orderHelperService.isCurrentUserOwnerOrAdmin(order.getOwner())) {
            message = "USER_NOT_ALLOWED_TO_MANAGE_ORDER";
        } else {
            switch (action) {
                case PAUSE:
                    if (!Arrays.asList(OrderStatus.PENDING, OrderStatus.RUNNING).contains(order.getStatus())) {
                        message = "ORDER_MUST_BE_PENDING_OR_RUNNING";
                    }
                    break;
                case RESUME:
                    if (!isPaused(order.getId())) {
                        message = "ORDER_MUST_BE_PAUSED";
                    }
                    break;
                case DELETE:
                    if (!Arrays.asList(OrderStatus.DONE, OrderStatus.DONE_WITH_WARNING, OrderStatus.PAUSED, OrderStatus.FAILED).contains(order.getStatus())) {
                        message = "ORDER_MUST_BE_DONE_OR_DONE_WITH_WARNING_OR_PAUSED_OR_FAILED";
                    }
                    break;
                case REMOVE:
                    if (!orderHelperService.isAdmin()) {
                        message = "USER_NOT_ALLOWED_TO_MANAGE_ORDER";
                    } else if (!OrderStatus.DELETED.equals(order.getStatus())) {
                        message = "ORDER_MUST_BE_DELETED";
                    }
                    break;
                case RESTART:
                    if (!Arrays.asList(OrderStatus.DONE, OrderStatus.DONE_WITH_WARNING, OrderStatus.FAILED).contains(order.getStatus())) {
                        message = "ORDER_MUST_BE_DONE_OR_DONE_WITH_WARNING_OR_FAILED";
                    }
                    break;
                case RETRY:
                    if (!Arrays.asList(OrderStatus.DONE_WITH_WARNING, OrderStatus.FAILED).contains(order.getStatus())) {
                        message = "ORDER_MUST_BE_DONE_WITH_WARNING_OR_FAILED";
                    } else if (hasProcessing(order)) {
                        message = "ORDER_HAS_PROCESSING";
                    }
                    break;
                case DOWNLOAD:
                    if (OrderStatus.DELETED.equals(order.getStatus()) || order.getAvailableFilesCount() == 0) {
                        message = "ORDER_MUST_HAVE_AVAILABLE_FILES";
                    }
                    break;
            }
        }
        return message;
    }

    @AllArgsConstructor
    public enum Action {

        PAUSE(CannotPauseOrderException.class),
        RESUME(CannotResumeOrderException.class),
        DELETE(CannotDeleteOrderException.class),
        REMOVE(CannotRemoveOrderException.class),
        RESTART(CannotRestartOrderException.class),
        RETRY(CannotRetryOrderException.class),
        DOWNLOAD(NotYetAvailableException.class);

        private final Class<? extends ModuleException> exceptionClass;

        public ModuleException getException(String message) {
            try {
                return exceptionClass.getConstructor(String.class).newInstance(message);
            } catch (ReflectiveOperationException e) {
                throw new RsRuntimeException(e);
            }
        }
    }

}
