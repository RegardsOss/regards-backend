package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.utils.OrderCounts;

import java.util.Set;
import java.util.UUID;

public interface IOrderRetryService {

    void asyncCompleteRetry(long orderId, String role, int subOrderDuration, String tenant);

    void retry(long orderId, String role, int subOrderDuration);

    void retryDatasetTask(Long datasetTaskId,
                          OrderCounts orderCounts,
                          long orderId,
                          String owner,
                          int subOrderDuration,
                          int priority,
                          String role);

    UUID createStorageSubOrder(DatasetTask datasetTask,
                               Set<OrderDataFile> orderDataFiles,
                               long orderId,
                               String owner,
                               int subOrderDuration,
                               String role,
                               int priority);

    void createExternalSubOrder(DatasetTask datasetTask, Set<OrderDataFile> orderDataFiles, long orderId, String owner);

}
