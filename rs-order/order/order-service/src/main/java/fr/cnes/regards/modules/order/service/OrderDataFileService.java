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

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import feign.Response;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.ResponseStreamProxy;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.order.dao.IFilesTasksRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.*;
import fr.cnes.regards.modules.order.dto.OrderControllerEndpointConfiguration;
import fr.cnes.regards.modules.order.dto.dto.OrderDataFileDTO;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class OrderDataFileService implements IOrderDataFileService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDataFileService.class);

    public static final String CONTENT_LENGTH_HEADER = "content-length";

    private final Set<String> noProxyHosts = Sets.newHashSet();

    private final IOrderDataFileRepository orderDataFileRepository;

    private final IOrderJobService orderJobService;

    private final IOrderDataFileService self;

    private final IFilesTasksRepository filesTasksRepository;

    private final IOrderRepository orderRepository;

    private final IStorageRestClient storageClient;

    private final IAuthenticationResolver authResolver;

    private final IProcessingEventSender processingEventSender;

    private final IPublisher publisher;

    private final OrderResponseService orderResponseService;

    private final OrderDownloadService orderDownloadService;

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.port:#{null}}")
    private Integer proxyPort;

    @Value("${http.proxy.noproxy:#{null}}")
    private String noProxyHostsString;

    private Proxy proxy;

    @Value("${prefix.path}")
    private String prefixPath;

    @Value("${spring.application.name}")
    private String applicationName;

    public OrderDataFileService(IOrderDataFileRepository orderDataFileRepository,
                                IOrderJobService orderJobService,
                                IOrderDataFileService orderDataFileService,
                                IFilesTasksRepository filesTasksRepository,
                                IOrderRepository orderRepository,
                                IStorageRestClient storageClient,
                                IAuthenticationResolver authResolver,
                                IProcessingEventSender processingEventSender,
                                IPublisher publisher,
                                OrderResponseService orderResponseService,
                                OrderDownloadService orderDownloadService) {
        this.orderDataFileRepository = orderDataFileRepository;
        this.orderJobService = orderJobService;
        this.self = orderDataFileService;
        this.filesTasksRepository = filesTasksRepository;
        this.orderRepository = orderRepository;
        this.storageClient = storageClient;
        this.authResolver = authResolver;
        this.processingEventSender = processingEventSender;
        this.publisher = publisher;
        this.orderResponseService = orderResponseService;
        this.orderDownloadService = orderDownloadService;
    }

    private static MediaType asMediaType(MimeType mimeType) {
        if (mimeType instanceof MediaType mediaType) {
            return mediaType;
        }
        return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
    }

    @Override
    public void afterPropertiesSet() {
        proxy = Strings.isNullOrEmpty(proxyHost) ?
            Proxy.NO_PROXY :
            new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        if (noProxyHostsString != null) {
            Collections.addAll(noProxyHosts, noProxyHostsString.split("\\s*,\\s*"));
        }
    }

    @Override
    public Iterable<OrderDataFile> create(Iterable<OrderDataFile> dataFiles) {
        return orderDataFileRepository.saveAll(dataFiles);
    }

    @Override
    public OrderDataFile save(OrderDataFile dataFile) {
        dataFile = orderDataFileRepository.save(dataFile);
        // Look at FilesTask if it is ended (no more file to download)...
        FilesTask filesTask = filesTasksRepository.findDistinctByFilesContaining(dataFile);
        // In case FilesTask does not yet exist
        if (filesTask != null) {
            filesTask.computeTaskEnded();
            if (filesTask.isEnded()) {
                LOGGER.trace("File task {} on order {} has ended (no more file to download)",
                             filesTask.getId(),
                             filesTask.getOrderId());
            }
            // ...and if it is waiting for user
            filesTask.computeWaitingForUser();
            filesTasksRepository.save(filesTask);
            // Update then associated information to order
            Order order = orderRepository.findSimpleById(filesTask.getOrderId());
            boolean wasWaitingForUser = order.isWaitingForUser();
            boolean isNowWaitingForUser = filesTasksRepository.findByOrderId(filesTask.getOrderId())
                                                              .anyMatch(FilesTask::isWaitingForUser);
            order.setWaitingForUser(isNowWaitingForUser);
            if (wasWaitingForUser != isNowWaitingForUser) {
                LOGGER.trace("Order {} no longer waits for user download", filesTask.getOrderId());
            }
            orderRepository.save(order);
        }
        return dataFile;
    }

    @Override
    public Iterable<OrderDataFile> save(Iterable<OrderDataFile> inDataFiles) {
        List<OrderDataFile> dataFiles = orderDataFileRepository.saveAll(inDataFiles);
        launchNextFilesTasks(dataFiles);
        return dataFiles;
    }

    @Override
    public void launchNextFilesTasks(Iterable<OrderDataFile> dataFiles) {
        // Look at FilesTasks if they are ended (no more file to download)...
        List<FilesTask> filesTasks = filesTasksRepository.findDistinctByFilesIn(io.vavr.collection.List.ofAll(dataFiles)
                                                                                                       .toJavaList());
        Long orderId = null;
        // Update all these FileTasks
        for (FilesTask filesTask : filesTasks) {
            computeFilesTaskStates(filesTask);
            // Save order id for later
            orderId = filesTask.getOrderId();
            filesTasksRepository.save(filesTask);
        }
        // All files come from same order
        if (orderId != null) {
            // Update then associated information to order
            Order order = orderRepository.findSimpleById(orderId);
            boolean wasWaitingForUser = order.isWaitingForUser();
            order.setWaitingForUser(filesTasksRepository.findByOrderId(orderId).anyMatch(FilesTask::isWaitingForUser));
            LOGGER.debug("Update order {} | WaitingForUser from {} to {} - status {} - available files count {}",
                         order.getId(),
                         wasWaitingForUser,
                         order.isWaitingForUser(),
                         order.getStatus(),
                         order.getAvailableFilesCount());
            orderRepository.save(order);
        }
    }

    /**
     * Compute states for a sub-order. Notify a sub-order done notification.
     */
    private void computeFilesTaskStates(FilesTask filesTask) {
        filesTask.computeTaskEnded();
        boolean wasWaitingForUser = filesTask.isWaitingForUser();
        filesTask.computeWaitingForUser();
        if (!wasWaitingForUser && filesTask.isWaitingForUser()) {
            // notification must be sent once
            orderResponseService.notifySuborderDone(filesTask);
        }
    }

    @Override
    public OrderDataFile load(Long dataFileId) throws NoSuchElementException {
        Optional<OrderDataFile> dataFile = orderDataFileRepository.findById(dataFileId);
        return dataFile.orElseThrow(() -> new NoSuchElementException(String.format(
            "Data file with id: %d doesn't exist.",
            dataFileId)));
    }

    @Override
    public OrderDataFile find(Long orderId, UniformResourceName aipId, String checksum) throws NoSuchElementException {
        Optional<OrderDataFile> dataFileOpt = orderDataFileRepository.findFirstByChecksumAndIpIdAndOrderId(checksum,
                                                                                                           aipId,
                                                                                                           orderId);
        if (!dataFileOpt.isPresent()) {
            throw new NoSuchElementException();
        }
        return dataFileOpt.get();
    }

    @Override
    public List<OrderDataFile> findAllAvailables(Long orderId) {
        return orderDataFileRepository.findAllAvailables(orderId);
    }

    @Override
    public List<OrderDataFile> findAll(Long orderId) {
        return orderDataFileRepository.findAllByOrderId(orderId);
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadFile(final OrderDataFile dataFile, Optional<String> asUser) {
        ResponseEntity<InputStreamResource> response;
        if (Boolean.TRUE.equals(dataFile.isReference())) {
            response = downloadReferenceFile(dataFile);
        } else {
            response = downloadStoredFile(dataFile, asUser.orElse(null));
        }
        self.save(dataFile);
        Order order = orderRepository.findSimpleById(dataFile.getOrderId());
        orderJobService.manageUserOrderStorageFilesJobInfos(order.getOwner());
        return response;
    }

    @Override
    public Page<OrderDataFileDTO> findAvailableDataFiles(Long orderId, @Nullable Long filesTaskId, Pageable page) {
        Page<OrderDataFile> availableByOrderId = orderDataFileRepository.findAvailableDataFiles(orderId,
                                                                                                filesTaskId,
                                                                                                page);
        return new PageImpl<>(availableByOrderId.stream()
                                                .map(OrderDataFile::toOrderDataFileDto)
                                                .peek(orderDataFileDTO -> orderDataFileDTO.setDownloadUrl(
                                                    computeDownloadLink(orderDataFileDTO.getId()))) //we need to
                                                // compute an
                                                // effective download link of the order data file
                                                .toList(),
                              availableByOrderId.getPageable(),
                              availableByOrderId.getTotalElements());
    }

    @Override
    public boolean hasAvailableFiles(Long orderId) {
        return orderDataFileRepository.existsByStateAndOrderId(FileState.AVAILABLE, orderId);
    }

    /**
     * Download a file not stored with storage microservice.
     *
     * @return {@link InputStreamResource} of the file
     */
    private ResponseEntity<InputStreamResource> downloadReferenceFile(OrderDataFile dataFile) {
        HttpHeaders headers = new HttpHeaders();
        String filename = dataFile.getFilename() != null ?
            dataFile.getFilename() :
            dataFile.getUrl().substring(dataFile.getUrl().lastIndexOf('/') + 1);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
        if (dataFile.getFilesize() != null) {
            headers.setContentLength(dataFile.getFilesize());
        }
        if (dataFile.getMimeType() != null) {
            headers.setContentType(asMediaType(dataFile.getMimeType()));
        }
        InputStream stream;
        try {
            stream = DownloadUtils.getInputStreamThroughProxy(new URL(dataFile.getUrl()),
                                                              proxy,
                                                              noProxyHosts,
                                                              10_000,
                                                              Collections.emptyList());
            return new ResponseEntity<>(new InputStreamResource(stream), headers, HttpStatus.OK);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Download a file stored on storage microservice
     *
     * @return {@link InputStreamResource} of the file
     */
    private ResponseEntity<InputStreamResource> downloadStoredFile(OrderDataFile dataFile, @Nullable String asUser) {
        try {
            InputStreamResource isr = null;
            Optional<Response> responseOpt = orderDownloadService.downloadDataFile(dataFile,
                                                                                   "Error while downloading file "
                                                                                   + dataFile.getFilename(),
                                                                                   asUser);
            if (responseOpt.isEmpty()) {
                dataFile.setState(FileState.DOWNLOAD_ERROR);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            Response response = responseOpt.get();
            String reason = response.reason();
            int status = response.status();
            if (status != HttpStatus.OK.value()) {
                LOGGER.error("Error downloading file {} from storage or dam : {} : {}",
                             dataFile.getChecksum(),
                             status,
                             reason);
                dataFile.setState(FileState.DOWNLOAD_ERROR);
                dataFile.setDownloadError(reason);
                if (response.body() != null) {
                    isr = new InputStreamResource(new ResponseStreamProxy(response));
                }
            } else {
                Function<ResponseStreamProxy, Void> beforeClose = (ResponseStreamProxy stream) -> {
                    LOGGER.info("Download of file {} succeeded with {}bytes",
                                dataFile.getFilename(),
                                stream.getStreamReadCount());
                    int contentLength = Integer.parseInt(response.headers()
                                                                 .get(CONTENT_LENGTH_HEADER)
                                                                 .iterator()
                                                                 .next());
                    if (stream.getStreamReadCount() >= contentLength) {
                        dataFile.setState(FileState.DOWNLOADED);
                        processingEventSender.sendDownloadedFilesNotification(Collections.singleton(dataFile));
                    } else {
                        String message = "Cannot completely retrieve data file from storage, only "
                                         + stream.getStreamReadCount()
                                         + "/"
                                         + dataFile.getFilesize()
                                         + " bytes";
                        dataFile.setState(FileState.DOWNLOAD_ERROR);
                        dataFile.setDownloadError(message);
                        LOGGER.error(message);
                    }
                    self.save(dataFile);
                    Order order = orderRepository.findSimpleById(dataFile.getOrderId());
                    orderJobService.manageUserOrderStorageFilesJobInfos(order.getOwner());
                    return null;
                };

                isr = new InputStreamResource(new ResponseStreamProxy(response, beforeClose));
            }
            HttpHeaders headers = new HttpHeaders();
            for (Entry<String, Collection<String>> h : response.headers().entrySet()) {
                h.getValue().forEach(v -> headers.add(h.getKey(), v));
            }
            return new ResponseEntity<>(isr, headers, HttpStatus.valueOf(status));
        } catch (HttpServerErrorException | HttpClientErrorException | IOException e) {
            LOGGER.error(e.getMessage(), e);
            dataFile.setState(FileState.DOWNLOAD_ERROR);
            dataFile.setDownloadError(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Set<Order> updateCurrentOrdersComputedValues() {
        Timestamp now = Timestamp.valueOf(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        // find not yet finished orders and their sum of data files sizes
        List<Object[]> totalOrderFiles = orderDataFileRepository.findSumSizesByOrderId(now);
        if (totalOrderFiles.isEmpty()) {
            return Collections.emptySet();
        }
        // All following methods returns a Collection of Object[] whom first elt is an Order
        Function<Object[], Long> getOrderIdFct = array -> ((Order) array[0]).getId();
        // All following methods returns a Collection of Object[] whom second elt is a Long (a sum or a count)
        Function<Object[], Long> getValueFct = array -> ((Long) array[1]);
        // Set of orders not yet finished
        Set<Order> orders = totalOrderFiles.stream().map(array -> (Order) array[0]).collect(Collectors.toSet());
        // Map { order_id -> total files size }
        Map<Long, Long> totalSizeMap = totalOrderFiles.stream().collect(Collectors.toMap(getOrderIdFct, getValueFct));

        // Map { order_id -> treated files size  }
        Map<Long, Long> treatedSizeMap = orderDataFileRepository.selectSumSizesByOrderIdAndStates(now,
                                                                                                  FileState.AVAILABLE,
                                                                                                  FileState.DOWNLOADED,
                                                                                                  FileState.DOWNLOAD_ERROR,
                                                                                                  FileState.PROCESSING_ERROR,
                                                                                                  FileState.ERROR)
                                                                .stream()
                                                                .collect(Collectors.toMap(getOrderIdFct, getValueFct));
        // Map { order_id -> files in error count } Files with status DOWNLOAD_ERROR are not taken into account
        // because they are not considered as errors (available from storage)
        Map<Long, Long> errorCountMap = orderDataFileRepository.selectCountFilesByOrderIdAndStates(now, FileState.ERROR)
                                                               .stream()
                                                               .collect(Collectors.toMap(getOrderIdFct, getValueFct));
        Map<Long, Long> processErrorCountMap = orderDataFileRepository.selectCountFilesByOrderIdAndStates4AllOrders(now,
                                                                                                                    FileState.PROCESSING_ERROR)
                                                                      .stream()
                                                                      .collect(Collectors.toMap(getOrderIdFct,
                                                                                                getValueFct));
        // Map {order_id -> available files count }
        Map<Long, Long> availableCountMap = orderDataFileRepository.selectCountFilesByOrderIdAndStates4AllOrders(now,
                                                                                                                 FileState.AVAILABLE)
                                                                   .stream()
                                                                   .collect(Collectors.toMap(getOrderIdFct,
                                                                                             getValueFct));

        // Update all orders completion values
        for (Order order : orders) {
            long totalSize = totalSizeMap.get(order.getId());
            long treatedSize = treatedSizeMap.getOrDefault(order.getId(), 0L);
            int previousPercentCompleted = order.getPercentCompleted();
            LOGGER.debug("Updating computed value for order {} ; treated size : {} ; total size : {}",
                         order.getId(),
                         treatedSize,
                         totalSize);
            if (totalSize > 0) {
                order.setPercentCompleted((int) Math.floorDiv(100L * treatedSize, totalSize));
            } else {
                order.setPercentCompleted(100);
            }
            long errorCount = errorCountMap.getOrDefault(order.getId(), 0L)
                              + processErrorCountMap.getOrDefault(order.getId(), 0L);
            order.setFilesInErrorCount((int) errorCount);
            long availableCount = availableCountMap.getOrDefault(order.getId(), 0L);
            int previousAvailableFilesCount = order.getAvailableFilesCount();
            // If number of available files has changed...
            if (order.getAvailableFilesCount() != availableCount) {
                order.setAvailableFilesCount((int) availableCount);
            }
            LOGGER.debug("Update order {} | AvailableFilesCount from {} to {} | state {} | is waiting for user {} "
                         + "| PercentCompleted from {} to {}",
                         order.getId(),
                         previousAvailableFilesCount,
                         order.getAvailableFilesCount(),
                         order.getStatus(),
                         order.isWaitingForUser(),
                         previousPercentCompleted,
                         order.getPercentCompleted());
            updateOrderIfFinished(order, errorCount);
        }
        return orders;
    }

    /**
     * Update finished order status and clean associated FileTasks not in error
     * If status updated, send an amqp notification
     */
    private void updateOrderIfFinished(Order order, long errorCount) {
        // Update order status if percent_complete has reached 100%
        LOGGER.debug("Completion of order {} : {}%", order.getId(), order.getPercentCompleted());
        if (order.getPercentCompleted() == 100) {
            // update only once the order status
            if (!order.getStatus()
                      .isOneOfStatuses(OrderStatus.DONE, OrderStatus.FAILED, OrderStatus.DONE_WITH_WARNING)) {
                // If no files in error = DONE
                if (errorCount == 0) {
                    order.setStatus(OrderStatus.DONE);
                } else if (errorCount == order.getDatasetTasks().stream().mapToLong(DatasetTask::getFilesCount).sum()) {
                    // If all files in error => FAILED
                    order.setStatus(OrderStatus.FAILED);
                } else { // DONE_WITH_WARNING
                    order.setStatus(OrderStatus.DONE_WITH_WARNING);
                }
                orderResponseService.notifyFinishedOrder(order);
            }
        }
    }

    @Override
    public void removeAll(Long orderId) {
        orderDataFileRepository.deleteByOrderId(orderId);
    }

    @Override
    public boolean hasDownloadErrors(Long orderId) {
        return orderDataFileRepository.countByOrderIdAndStateIn(orderId, FileState.DOWNLOAD_ERROR) > 0L;
    }

    public String computeDownloadLink(Long orderDatafileId) {
        return orderResponseService.getProjectHost()
               + prefixPath
               + File.separator
               + applicationName
               + File.separator
               + OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID.replace("{dataFileId}",
                                                                                        orderDatafileId.toString());
    }

}
