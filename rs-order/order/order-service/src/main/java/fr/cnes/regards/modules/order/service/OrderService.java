/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.log.CorrelationIdUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.dao.RequestSpecificationsBuilder;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.SearchRequestParameters;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.exception.*;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.dto.dto.OrderStatusDto;
import fr.cnes.regards.modules.order.service.settings.IOrderSettingsService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.cnes.regards.modules.order.service.utils.LogUtils.ORDER_ID_LOG_KEY;

@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class OrderService implements IOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    /**
     * Format for generated order label
     */
    private static final String ORDER_GENERATED_LABEL_FORMAT = "Order of %s";

    /**
     * Date formatter for order generated label
     */
    private static final DateTimeFormatter ORDER_GENERATED_LABEL_DATE_FORMAT = DateTimeFormatter.ofPattern(
        "yyyy/MM/dd 'at' HH:mm:ss");

    /**
     * Default correlation id formatter in case it is not provided by the client (auto-correlation-id-'randomUUID')
     */
    public static final String DEFAULT_CORRELATION_ID_FORMAT = "auto-correlation-id-%s";

    private final IOrderService self;

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

    private final IAuthenticationResolver authResolver;

    public OrderService(IOrderRepository orderRepository,
                        IBasketService basketService,
                        IOrderCreationService orderCreationService,
                        IOrderRetryService orderRetryService,
                        IOrderDataFileService dataFileService,
                        IJobInfoService jobInfoService,
                        IOrderJobService orderJobService,
                        ITenantResolver tenantResolver,
                        IRuntimeTenantResolver runtimeTenantResolver,
                        IOrderSettingsService orderSettingsService,
                        OrderHelperService orderHelperService,
                        IOrderService orderService,
                        IAuthenticationResolver authResolver) {
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
        this.self = orderService;
        this.authResolver = authResolver;
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
    public Optional<String> getOrderOwner(Long orderId) {
        return orderRepository.findOwnerById(orderId);
    }

    @Override
    public Order loadSimple(Long orderId) {
        return orderRepository.findSimpleById(orderId);
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
            return orderRepository.findAllByOwnerAndStatusNotInOrderByCreationDateDesc(user,
                                                                                       excludeStatuses,
                                                                                       pageRequest);
        }
    }

    @Override
    public Page<Order> searchOrders(SearchRequestParameters filters, Pageable pageRequest) {
        return orderRepository.findAll(new RequestSpecificationsBuilder().withParameters(filters).build(), pageRequest);
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
    public Order createOrder(Basket basket, String label, String url, int subOrderDuration)
        throws EntityInvalidException {
        return createOrder(basket, label, url, subOrderDuration, basket.getOwner(), null);
    }

    @Override
    public Order createOrder(Basket basket,
                             String label,
                             String url,
                             int subOrderDuration,
                             String user,
                             String correlationId) throws EntityInvalidException {

        LOGGER.info("Generate and / or check label is unique for owner before creating back");
        // generate label when none is provided
        String orderLabel = label;
        if (Strings.isNullOrEmpty(orderLabel)) {
            orderLabel = String.format(ORDER_GENERATED_LABEL_FORMAT,
                                       ORDER_GENERATED_LABEL_DATE_FORMAT.format(OffsetDateTime.now()));
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
        order.setCorrelationId(correlationId);
        order = self.create(order);

        try {
            // Set log correlation id
            CorrelationIdUtils.setCorrelationId(ORDER_ID_LOG_KEY + order.getId());

            // Set basket owner to order (PM54)
            String newBasketOwner = IOrderService.BASKET_OWNER_PREFIX + order.getId();
            Basket newBasket = basketService.transferOwnerShip(basket.getOwner(), newBasketOwner);
            LOGGER.info("Basket saved with owner : {}", newBasket.getOwner());

            // Asynchronous operation
            orderCreationService.asyncCompleteOrderCreation(newBasket,
                                                            order.getOwner(),
                                                            order.getId(),
                                                            subOrderDuration,
                                                            orderHelperService.getRole(user),
                                                            runtimeTenantResolver.getTenant());
            return order;
        } finally {
            CorrelationIdUtils.clearCorrelationId();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order create(Order order) {
        LOGGER.info("Creating order with owner {}", order.getOwner());
        order.setCreationDate(OffsetDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        if (order.getCorrelationId() == null) {
            order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        }
        return orderRepository.save(order);
    }

    @Override
    public void pause(Long orderId, boolean checkConnectedUser) throws ModuleException {
        checkOrder(orderId, Action.PAUSE, checkConnectedUser);
        Order order = orderRepository.findCompleteById(orderId);

        try {
            // Set log correlation id
            CorrelationIdUtils.setCorrelationId(ORDER_ID_LOG_KEY + order.getId());

            // Ask for all jobInfos abortion
            order.getDatasetTasks()
                 .stream()
                 .flatMap(dsTask -> dsTask.getReliantTasks().stream())
                 .map(FilesTask::getJobInfo)
                 .forEach(jobInfo -> {
                     if (jobInfo != null) {
                         // Set log correlation id
                         CorrelationIdUtils.setCorrelationId(ORDER_ID_LOG_KEY + order.getId());
                         jobInfoService.stopJob(jobInfo.getId());
                     }
                 });
            LOGGER.info("Pausing order {}", order.getId());
            order.setStatus(OrderStatus.PAUSED);
            orderRepository.save(order);
            LOGGER.info("Order paused {}", order.getId());
        } finally {
            CorrelationIdUtils.clearCorrelationId();
        }
    }

    @Override
    public void resume(Long orderId) throws ModuleException {
        checkOrder(orderId, Action.RESUME, true);
        Order order = orderRepository.findCompleteById(orderId);

        try {
            // Set log correlation id
            CorrelationIdUtils.setCorrelationId(ORDER_ID_LOG_KEY + order.getId());

            order.setStatus(OrderStatus.RUNNING);
            orderRepository.save(order);

            // Passes all ABORTED jobInfo to PENDING
            order.getDatasetTasks()
                 .stream()
                 .flatMap(dsTask -> dsTask.getReliantTasks().stream())
                 .map(FilesTask::getJobInfo)
                 .filter(jobInfo -> jobInfo != null && jobInfo.getStatus().getStatus() == JobStatus.ABORTED)
                 .forEach(jobInfo -> {
                     // Set log correlation id
                     jobInfo.updateStatus(JobStatus.PENDING);
                     jobInfoService.save(jobInfo);
                 });
            // Don't forget to manage user order jobs again (PENDING -> QUEUED)
            orderJobService.manageUserOrderStorageFilesJobInfos(order.getOwner());
        } finally {
            CorrelationIdUtils.clearCorrelationId();
        }
    }

    @Override
    public void delete(Long orderId, boolean checkConnectedUser) throws ModuleException {
        checkOrder(orderId, Action.DELETE, checkConnectedUser);
        Order order = orderRepository.findCompleteById(orderId);

        try {
            // Set log correlation id
            CorrelationIdUtils.setCorrelationId(ORDER_ID_LOG_KEY + order.getId());

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
        } finally {
            CorrelationIdUtils.clearCorrelationId();
        }
        // Deactivate waitingForUser tag
        order.setWaitingForUser(false);
        order.setStatus(OrderStatus.DELETED);
        orderRepository.save(order);
        // Don't forget to manage user order jobs again (PENDING -> QUEUED)
        orderJobService.manageUserOrderStorageFilesJobInfos(order.getOwner());
    }

    @Override
    public Order restart(long oldOrderId, String label, String successUrl) throws ModuleException {
        checkOrder(oldOrderId, Action.RESTART, true);
        String oldOrderOwner = orderRepository.findSimpleById(oldOrderId).getOwner();

        Basket oldBasket;
        try {
            oldBasket = basketService.find(BASKET_OWNER_PREFIX + oldOrderId);
        } catch (EmptyBasketException e) {
            // This should not happen, since basket is not deleted anymore - check is added for transition period
            throw new CannotRestartOrderException("BASKET_NOT_FOUND");
        }

        Basket newBasket = basketService.duplicate(oldBasket.getId(), BASKET_RESTART_OWNER_PREFIX + oldOrderId);

        return createOrder(newBasket,
                           label,
                           successUrl,
                           orderSettingsService.getUserOrderParameters().getSubOrderDuration(),
                           oldOrderOwner,
                           null);
    }

    @Override
    public void retryErrors(Long orderId) throws ModuleException {
        checkOrder(orderId, Action.RETRY, true);
        Order order = orderRepository.findSimpleById(orderId);

        String orderOwnerRole = orderHelperService.getRole(order.getOwner());

        try {
            // Set log correlation id
            CorrelationIdUtils.setCorrelationId(ORDER_ID_LOG_KEY + order.getId());

            orderRetryService.asyncCompleteRetry(order.getId(),
                                                 orderOwnerRole,
                                                 orderSettingsService.getUserOrderParameters().getSubOrderDuration(),
                                                 runtimeTenantResolver.getTenant());
        } finally {
            CorrelationIdUtils.clearCorrelationId();
        }
    }

    @Override
    public void remove(Long orderId) throws ModuleException {
        checkOrder(orderId, Action.REMOVE, true);

        try {
            // Set log correlation id
            CorrelationIdUtils.setCorrelationId(ORDER_ID_LOG_KEY + orderId);

            // Data files have already been deleted so there's only the basket and the order to remove
            basketService.deleteIfExists(BASKET_OWNER_PREFIX + orderId);
            orderRepository.deleteById(orderId);
        } finally {
            CorrelationIdUtils.clearCorrelationId();
        }
    }

    @Override
    public void writeAllOrdersInCsv(BufferedWriter writer, SearchRequestParameters filters) throws IOException {
        List<Order> orders = orderRepository.findAll(new RequestSpecificationsBuilder().withParameters(filters).build(),
                                                     Sort.by(Sort.Direction.ASC, "id"));

        writer.append(
            "ORDER_ID;CREATION_DATE;EXPIRATION_DATE;OWNER;STATUS;STATUS_DATE;PERCENT_COMPLETE;FILES_IN_ERROR;FILES_SIZE;FILES_COUNT");
        writer.newLine();
        for (Order order : orders) {
            writer.append(order.getId().toString()).append(';');
            writer.append(OffsetDateTimeAdapter.format(order.getCreationDate())).append(';');
            if (order.getExpirationDate() != null) {
                writer.append(OffsetDateTimeAdapter.format(order.getExpirationDate()));
            }
            writer.append(';');
            writer.append(order.getOwner()).append(';');
            writer.append(order.getStatus().toString()).append(';');
            writer.append(OffsetDateTimeAdapter.format(order.getStatusDate())).append(';');
            writer.append(Integer.toString(order.getPercentCompleted())).append(';');
            writer.append(Integer.toString(order.getFilesInErrorCount())).append(';');
            writer.append(String.valueOf(order.getDatasetTasks().stream().mapToLong(dt -> dt.getFilesSize()).sum()))
                  .append(';');
            writer.append(String.valueOf(order.getDatasetTasks().stream().mapToLong(dt -> dt.getFilesCount()).sum()));
            writer.newLine();
        }
        writer.close();
    }

    @Override
    public boolean isActionAvailable(Long orderId, Action action) {
        return StringUtils.isBlank(getErrorMessageOnAction(orderRepository.findSimpleById(orderId), action));
    }

    @Override
    public boolean hasCurrentUserAccessTo(String owner) {
        String role = authResolver.getRole();
        String user = authResolver.getUser();
        return DefaultRole.INSTANCE_ADMIN.name().equals(role)
               || DefaultRole.PROJECT_ADMIN.name().equals(role)
               || owner.equals(user);
    }

    /**
     * Check for the order:
     * <ul>
     *     <li>if the order exists in the database</li>
     *     <li>the connected user of the order</li>
     *     <li>if the action is available for the order</li>
     * </ul>
     */
    private void checkOrder(Long orderId, Action action, boolean checkConnectedUser) throws ModuleException {
        Order order = orderRepository.findSimpleById(orderId);
        if (order == null) {
            throw new EntityNotFoundException(orderId, Order.class);
        }
        if (checkConnectedUser) {
            checkConnectedUser(order, action);
        }
        checkAction(order, action);
    }

    /**
     * Check if the action is available concerning the order.
     * Check following actions :
     * <ul>
     *     <li>PAUSE</li>
     *     <li>RESUME</li>
     *     <li>DELETE</li>
     *     <li>REMOVE</li>
     *     <li>RESTART</li>
     *     <li>RETRY</li>
     *     <li>DOWNLOAD</li>
     * </ul>
     */
    private void checkAction(Order order, Action action) throws ModuleException {
        String message = getErrorMessageOnAction(order, action);
        if (!StringUtils.isBlank(message)) {
            throw action.getException(message);
        }
    }

    private void checkConnectedUser(Order order, Action action) throws ModuleException {
        if (!orderHelperService.isCurrentUserOwnerOrAdmin(order.getOwner())) {
            throw action.getException("USER_NOT_ALLOWED_TO_MANAGE_ORDER");
        }
    }

    /**
     * Return a error message if the action is not available for the given action; otherwise the message is null.
     * Check following actions :
     * <ul>
     *     <li>PAUSE</li>
     *     <li>RESUME</li>
     *     <li>DELETE</li>
     *     <li>REMOVE</li>
     *     <li>RESTART</li>
     *     <li>RETRY</li>
     *     <li>DOWNLOAD</li>
     * </ul>
     */
    private String getErrorMessageOnAction(Order order, Action action) {
        String message = null;

        switch (action) {
            case PAUSE -> {
                if (!order.getStatus().isOneOfStatuses(OrderStatus.PENDING, OrderStatus.RUNNING)) {
                    message = "ORDER_MUST_BE_PENDING_OR_RUNNING";
                }
            }
            case RESUME -> {
                if (!isPaused(order.getId())) {
                    message = "ORDER_MUST_BE_PAUSED";
                }
            }
            case DELETE -> {
                if (!order.getStatus()
                          .isOneOfStatuses(OrderStatus.DONE,
                                           OrderStatus.DONE_WITH_WARNING,
                                           OrderStatus.PAUSED,
                                           OrderStatus.EXPIRED,
                                           OrderStatus.FAILED)) {
                    message = "ORDER_MUST_BE_DONE_OR_DONE_WITH_WARNING_OR_PAUSED_OR_FAILED_OR_EXPIRED";
                }
            }
            case REMOVE -> {
                if (!orderHelperService.isAdmin()) {
                    message = "USER_NOT_ALLOWED_TO_MANAGE_ORDER";
                } else if (!OrderStatus.DELETED.equals(order.getStatus())) {
                    message = "ORDER_MUST_BE_DELETED";
                }
            }
            case RESTART -> {
                if (!order.getStatus()
                          .isOneOfStatuses(OrderStatus.DONE, OrderStatus.DONE_WITH_WARNING, OrderStatus.FAILED)) {
                    message = "ORDER_MUST_BE_DONE_OR_DONE_WITH_WARNING_OR_FAILED";
                }
            }
            case RETRY -> {
                if (!order.getStatus().isOneOfStatuses(OrderStatus.DONE_WITH_WARNING, OrderStatus.FAILED)) {
                    message = "ORDER_MUST_BE_DONE_WITH_WARNING_OR_FAILED";
                } else if (hasProcessing(order)) {
                    message = "ORDER_HAS_PROCESSING";
                }
            }
            case DOWNLOAD -> {
                if (OrderStatus.DELETED.equals(order.getStatus()) || order.getAvailableFilesCount() == 0) {
                    message = "ORDER_MUST_HAVE_AVAILABLE_FILES";
                }
            }
        }
        return message;
    }

    @MultitenantTransactional(readOnly = true)
    public List<OrderStatusDto> findByIdsAndStatus(Collection<Long> orderIds, Collection<OrderStatus> orderStatuses) {
        return orderRepository.findByIdInAndStatusIn(orderIds, orderStatuses);
    }

    public void updateErrorWithMessageIfNecessary(Long orderId, @Nullable String msg) {
        Order order = orderRepository.findSimpleById(orderId);

        order.setStatus(OrderStatus.ERROR);
        order.setMessage(msg);

        orderRepository.save(order);
    }

    /**
     * Available actions to work with an order
     */
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
