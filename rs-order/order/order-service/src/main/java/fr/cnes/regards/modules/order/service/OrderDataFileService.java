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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import feign.Response;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.order.dao.IFilesTasksRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class OrderDataFileService implements IOrderDataFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDataFileService.class);

    @Autowired
    private IOrderDataFileRepository repos;

    @Autowired
    private IOrderJobService orderJobService;

    @Autowired
    private IOrderDataFileService self;

    @Autowired
    private IFilesTasksRepository filesTasksRepository;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IStorageRestClient storageClient;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IProcessingEventSender processingEventSender;

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.port:#{null}}")
    private Integer proxyPort;

    @Value("${http.proxy.noproxy:#{null}}")
    private String noProxyHostsString;

    private Proxy proxy;

    private final Set<String> noProxyHosts = Sets.newHashSet();

    @PostConstruct
    public void init() {
        proxy = Strings.isNullOrEmpty(proxyHost) ? Proxy.NO_PROXY
                : new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        if (noProxyHostsString != null) {
            Collections.addAll(noProxyHosts, noProxyHostsString.split("\\s*,\\s*"));
        }
    }

    @Override
    public Iterable<OrderDataFile> create(Iterable<OrderDataFile> dataFiles) {
        return repos.saveAll(dataFiles);
    }

    @Override
    public OrderDataFile save(OrderDataFile dataFile) {
        dataFile = repos.save(dataFile);
        // Look at FilesTask if it is ended (no more file to download)...
        FilesTask filesTask = filesTasksRepository.findDistinctByFilesContaining(dataFile);
        // In case FilesTask does not yet exist
        if (filesTask != null) {
            if (filesTask.getFiles().stream()
                    .allMatch(f -> (f.getState() == FileState.DOWNLOADED) || (f.getState() == FileState.ERROR)
                            || (f.getState() == FileState.DOWNLOAD_ERROR)
                            || (f.getState() == FileState.PROCESSING_ERROR))) {
                filesTask.setEnded(true);
            }
            // ...and if it is waiting for user
            filesTask.computeWaitingForUser();
            filesTasksRepository.save(filesTask);
            // Update then associated information to order
            Order order = orderRepository.findSimpleById(filesTask.getOrderId());
            order.setWaitingForUser(filesTasksRepository.findByOrderId(filesTask.getOrderId())
                    .anyMatch(t -> t.isWaitingForUser()));
            orderRepository.save(order);
        }
        return dataFile;
    }

    @Override
    public Iterable<OrderDataFile> save(Iterable<OrderDataFile> inDataFiles) {
        List<OrderDataFile> dataFiles = repos.saveAll(inDataFiles);
        launchNextFilesTasks(dataFiles);
        return dataFiles;
    }

    @Override
    public void launchNextFilesTasks(Iterable<OrderDataFile> dataFiles) {
        // Look at FilesTasks if they are ended (no more file to download)...
        List<FilesTask> filesTasks = filesTasksRepository
                .findDistinctByFilesIn(io.vavr.collection.List.ofAll(dataFiles).toJavaList());
        Long orderId = null;
        // Update all these FileTasks
        for (FilesTask filesTask : filesTasks) {
            if (filesTask.getFiles().stream()
                    .allMatch(f -> (f.getState() == FileState.DOWNLOADED) || (f.getState() == FileState.ERROR)
                            || (f.getState() == FileState.DOWNLOAD_ERROR)
                            || (f.getState() == FileState.PROCESSING_ERROR))) {
                filesTask.setEnded(true);
            }
            // Save order id for later
            orderId = filesTask.getOrderId();
            // ...and if it is waiting for user
            filesTask.computeWaitingForUser();
            filesTasksRepository.save(filesTask);
        }
        // All files come from same order
        if (orderId != null) {
            // Update then associated information to order
            Order order = orderRepository.findSimpleById(orderId);
            order.setWaitingForUser(filesTasksRepository.findByOrderId(orderId).anyMatch(t -> t.isWaitingForUser()));
            orderRepository.save(order);
        }
    }

    @Override
    public OrderDataFile load(Long dataFileId) throws NoSuchElementException {
        Optional<OrderDataFile> dataFile = repos.findById(dataFileId);
        return dataFile.orElseThrow(() -> new NoSuchElementException(
                String.format("Data file with id: %d doesn't exist.", dataFileId)));
    }

    @Override
    public OrderDataFile find(Long orderId, UniformResourceName aipId, String checksum) throws NoSuchElementException {
        Optional<OrderDataFile> dataFileOpt = repos.findFirstByChecksumAndIpIdAndOrderId(checksum, aipId, orderId);
        if (!dataFileOpt.isPresent()) {
            throw new NoSuchElementException();
        }
        return dataFileOpt.get();
    }

    @Override
    public List<OrderDataFile> findAllAvailables(Long orderId) {
        return repos.findAllAvailables(orderId);
    }

    @Override
    public List<OrderDataFile> findAll(Long orderId) {
        return repos.findAllByOrderId(orderId);
    }

    @Override
    public void downloadFile(OrderDataFile dataFile, Optional<String> asUser, OutputStream os) throws IOException {
        Response response = null;
        dataFile.setDownloadError(null);
        boolean error = false;
        String errorMessage = null;
        int timeout = 10_000;
        if (dataFile.isReference()) {
            try (InputStream is = DownloadUtils.getInputStreamThroughProxy(new URL(dataFile.getUrl()), proxy,
                                                                           noProxyHosts, timeout)) {
                ByteStreams.copy(is, os);
                os.flush();
            } catch (IOException e) {
                LOGGER.error("Error while downloading file", e);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                dataFile.setDownloadError("Error while downloading file\n" + sw.toString());
            }
        } else {
            try {
                // To download through storage client we must be authenticate as user in order to
                // impact the download quotas, but we upgrade the privileges so that the request passes.
                FeignSecurityManager.asUser(asUser.orElse(authResolver.getUser()), DefaultRole.PROJECT_ADMIN.name());
                response = storageClient.downloadFile(dataFile.getChecksum());
            } catch (RuntimeException e) {
                LOGGER.error("Error while downloading file from Archival Storage", e);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                errorMessage = "Error while downloading file from Archival Storage\n" + sw.toString();
            } finally {
                FeignSecurityManager.reset();
            }
            error = (response == null) || (response.status() != HttpStatus.OK.value());
            if (!error) {
                try (InputStream is = response.body().asInputStream()) {
                    long copiedBytes = ByteStreams.copy(is, os);
                    // File has not completly been copied
                    if (copiedBytes != dataFile.getFilesize()) {
                        error = true;
                        errorMessage = String
                                .format("Cannot completely retrieve data file from storage, only %s/%s bytes",
                                        copiedBytes, dataFile.getFilesize());
                    }
                }
            } else if (response != null) {
                errorMessage = String.format("Error while downloading file from Archival Storage. Cause : %s (Code=%d)",
                                             response.reason(), response.status());
            }
            if (response != null) {
                response.close();
            }
            if (response != null) {
                response.close();
            }
        }
        // Update OrderDataFile state
        if (error) { // set State as DOWNLOAD_ERROR ONLY IF file wasn't previously DOWLOADED (ie. AVAILABLE)
            if (dataFile.getState() == FileState.AVAILABLE) {
                dataFile.setState(FileState.DOWNLOAD_ERROR);
            } else {
                LOGGER.warn("File download error. File status not set  to DOWNLOAD_ERROR as current status is {}",
                            dataFile.getState().toString());
            }
            dataFile.setDownloadError(errorMessage);
            LOGGER.error("Error downloading file as user {}", asUser.orElse(authResolver.getUser()));
            LOGGER.error(errorMessage);
        } else { // Set State as DOWNLOADED, even if it is online
            dataFile.setState(FileState.DOWNLOADED);
            processingEventSender.sendDownloadedFilesNotification(Collections.singleton(dataFile));
        }
        dataFile = self.save(dataFile);
        Order order = orderRepository.findSimpleById(dataFile.getOrderId());
        orderJobService.manageUserOrderStorageFilesJobInfos(order.getOwner());
    }

    @Override
    public Set<Order> updateCurrentOrdersComputedValues() {
        OffsetDateTime now = OffsetDateTime.now();
        // find not yet finished orders and their sum of data files sizes
        List<Object[]> totalOrderFiles = repos.findSumSizesByOrderId(now);
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
        Map<Long, Long> treatedSizeMap = repos
                .selectSumSizesByOrderIdAndStates(now, FileState.AVAILABLE, FileState.DOWNLOADED,
                                                  FileState.DOWNLOAD_ERROR, FileState.PROCESSING_ERROR, FileState.ERROR)
                .stream().collect(Collectors.toMap(getOrderIdFct, getValueFct));
        // Map { order_id -> files in error count } Files with status DOWNLOAD_ERROR are not taken into account
        // because they are not considered as errors (available from storage)
        Map<Long, Long> errorCountMap = repos.selectCountFilesByOrderIdAndStates(now, FileState.ERROR).stream()
                .collect(Collectors.toMap(getOrderIdFct, getValueFct));
        Map<Long, Long> processErrorCountMap = repos
                .selectCountFilesByOrderIdAndStates4AllOrders(now, FileState.PROCESSING_ERROR).stream()
                .collect(Collectors.toMap(getOrderIdFct, getValueFct));
        // Map {order_id -> available files count }
        Map<Long, Long> availableCountMap = repos.selectCountFilesByOrderIdAndStates4AllOrders(now, FileState.AVAILABLE)
                .stream().collect(Collectors.toMap(getOrderIdFct, getValueFct));

        // Update all orders completion values
        for (Order order : orders) {
            long totalSize = totalSizeMap.get(order.getId());
            long treatedSize = treatedSizeMap.containsKey(order.getId()) ? treatedSizeMap.get(order.getId()) : 0l;
            order.setPercentCompleted((int) Math.floorDiv(100l * treatedSize, totalSize));
            long errorCount = (errorCountMap.containsKey(order.getId()) ? errorCountMap.get(order.getId()) : 0l)
                    + (processErrorCountMap.containsKey(order.getId()) ? processErrorCountMap.get(order.getId()) : 0l);
            order.setFilesInErrorCount((int) errorCount);
            long availableCount = availableCountMap.containsKey(order.getId()) ? availableCountMap.get(order.getId())
                    : 0l;
            // If number of available files has changed...
            if (order.getAvailableFilesCount() != availableCount) {
                order.setAvailableFilesCount((int) availableCount);
            }
            updateOrderIfFinished(order, errorCount);
        }
        return orders;
    }

    /**
     * Update finished order status and clean associated FileTasks not in error
     */
    private void updateOrderIfFinished(Order order, long errorCount) {
        // Update order status if percent_complete has reached 100%
        if (order.getPercentCompleted() == 100) {
            // If no files in error = DONE
            if (errorCount == 0) {
                order.setStatus(OrderStatus.DONE);
            } else if (errorCount == order.getDatasetTasks().stream().mapToLong(DatasetTask::getFilesCount).sum()) {
                // If all files in error => FAILED
                order.setStatus(OrderStatus.FAILED);
            } else { // DONE_WITH_WARNING
                order.setStatus(OrderStatus.DONE_WITH_WARNING);
            }
        }
    }

    @Override
    public void removeAll(Long orderId) {
        repos.deleteByOrderId(orderId);
    }
}
