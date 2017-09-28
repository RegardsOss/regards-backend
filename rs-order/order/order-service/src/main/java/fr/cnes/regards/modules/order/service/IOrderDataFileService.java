package fr.cnes.regards.modules.order.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;

/**
 * OrderDataFile specific service (OrderDataFiles are detached entities from Order, DatasetTasks and FilesTasks)
 * @author oroussel
 */
public interface IOrderDataFileService {

    OrderDataFile save(OrderDataFile dataFile);

    Iterable<OrderDataFile> save(Iterable<OrderDataFile> dataFiles);

    /**
     * Find all OrderDataFile with state AVAILABLE associated to given order
     * @param orderId id of order
     */
    List<OrderDataFile> findAllAvailables(Long orderId);

    /**
     * Copy asked file from storage to HttpServletResponse
     */
    void downloadFile(Long orderId, UniformResourceName aipId, String checksum, HttpServletResponse response)
            throws IOException;

    /**
     * Search all current orders (ie not finished), compute and update completion values (percentCompleted and files in
     * error count).
     * THIS METHOD DON'T UPDATE ANYTHING INTO DATABASE (it concerns Orders so it is the responsibility of OrderService)
     * @return updated orders
     */
    Set<Order> updateCurrentOrdersCompletionValues();
}
