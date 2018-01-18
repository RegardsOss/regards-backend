package fr.cnes.regards.modules.order.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.IFilesTasksRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.storage.client.IAipClient;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class OrderDataFileService implements IOrderDataFileService {

    @Autowired
    private IOrderDataFileRepository repos;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private IOrderJobService orderJobService;

    @Autowired
    private IOrderDataFileService self;

    @Autowired
    private IFilesTasksRepository filesTasksRepository;

    @Autowired
    private IOrderRepository orderRepository;

    @Override
    public OrderDataFile save(OrderDataFile dataFile) {
        dataFile = repos.save(dataFile);
        // Look at FilesTask if it is ended (no more file to download)...
        FilesTask filesTask = filesTasksRepository.findDistinctByFilesIn(dataFile);
        // In case FilesTask does not yet exist
        if (filesTask != null) {
            if (filesTask.getFiles().stream()
                    .allMatch(f -> (f.getState() == FileState.DOWNLOADED) || (f.getState() == FileState.ERROR))) {
                filesTask.setEnded(true);
            }
            // ...and if it is waiting for user
            filesTask.computeWaitingForUser();
            filesTasksRepository.save(filesTask);
            // Update then associated information to order
            Order order = orderRepository.findSimpleById(filesTask.getOrderId());
            order.setWaitingForUser(
                    filesTasksRepository.findByOrderId(filesTask.getOrderId()).anyMatch(t -> t.isWaitingForUser()));
            orderRepository.save(order);
        }
        return dataFile;
    }

    @Override
    public Iterable<OrderDataFile> save(Iterable<OrderDataFile> inDataFiles) {
        List<OrderDataFile> dataFiles = repos.save(inDataFiles);
        // Look at FilesTasks if they are ended (no more file to download)...
        List<FilesTask> filesTasks = filesTasksRepository.findDistinctByFilesIn(dataFiles);
        Long orderId = null;
        // Update all these FileTasks
        for (FilesTask filesTask : filesTasks) {
            if (filesTask.getFiles().stream()
                    .allMatch(f -> (f.getState() == FileState.DOWNLOADED) || (f.getState() == FileState.ERROR))) {
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
        return dataFiles;
    }

    @Override
    public OrderDataFile load(Long dataFileId) throws NoSuchElementException {
        OrderDataFile dataFile = repos.findOne(dataFileId);
        if (dataFile == null) {
            throw new NoSuchElementException();
        }
        return dataFile;
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
    public void downloadFile(OrderDataFile dataFile, OutputStream os) throws IOException {
        try (InputStream is = aipClient.downloadFile(dataFile.getIpId().toString(), dataFile.getChecksum()).body()
                .asInputStream()) {
            ByteStreams.copy(is, os);
            os.close();
        }
        // Update OrderDataFile (set State as DOWNLOADED, even if it is online)
        dataFile.setState(FileState.DOWNLOADED);
        dataFile = self.save(dataFile);
        Order order = orderRepository.findSimpleById(dataFile.getOrderId());
        orderJobService.manageUserOrderJobInfos(order.getOwner());
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
        // Set or orders not yet finished
        Set<Order> orders = totalOrderFiles.stream().map(array -> (Order) array[0]).collect(Collectors.toSet());
        // Map { order_id -> total files size }
        Map<Long, Long> totalSizeMap = totalOrderFiles.stream().collect(Collectors.toMap(getOrderIdFct, getValueFct));

        // Map { order_id -> treated files size  }
        Map<Long, Long> treatedSizeMap = repos
                .selectSumSizesByOrderIdAndStates(now, FileState.ONLINE, FileState.AVAILABLE, FileState.DOWNLOADED,
                                                  FileState.ERROR).stream()
                .collect(Collectors.toMap(getOrderIdFct, getValueFct));
        // Map { order_id -> files in error count }
        Map<Long, Long> errorCountMap = repos.selectCountFilesByOrderIdAndStates(now, FileState.ERROR).stream()
                .collect(Collectors.toMap(getOrderIdFct, getValueFct));

        // Map {order_id -> available files count }
        Map<Long, Long> availableCountMap = repos
                .selectCountFilesByOrderIdAndStates4AllOrders(now, FileState.AVAILABLE, FileState.ONLINE).stream()
                .collect(Collectors.toMap(getOrderIdFct, getValueFct));

        // Update all orders completion values
        for (Order order : orders) {
            long totalSize = totalSizeMap.get(order.getId());
            long treatedSize = treatedSizeMap.containsKey(order.getId()) ? treatedSizeMap.get(order.getId()) : 0l;
            order.setPercentCompleted(Math.floorDiv(100 * (int) treatedSize, (int) totalSize));
            long errorCount = errorCountMap.containsKey(order.getId()) ? errorCountMap.get(order.getId()) : 0l;
            order.setFilesInErrorCount((int) errorCount);
            long availableCount = availableCountMap.containsKey(order.getId()) ?
                    availableCountMap.get(order.getId()) :
                    0l;
            // If number of available files has changed...
            if (order.getAvailableFilesCount() != availableCount) {
                order.setAvailableFilesCount((int) availableCount);
            }
        }
        return orders;
    }

    @Override
    public void removeAll(Long orderId) {
        repos.deleteByOrderId(orderId);
    }
}
