package fr.cnes.regards.modules.order.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.NoSuchElementException;
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

    OrderDataFile find(Long orderId, UniformResourceName aipId, String checksum) throws NoSuchElementException;

    /**
     * Find all OrderDataFile with state AVAILABLE associated to given order
     * @param orderId id of order
     */
    List<OrderDataFile> findAllAvailables(Long orderId);

    /**
     * Find all OrderDataFile of given order
     * @param orderId if of order
     */
    List<OrderDataFile> findAll(Long orderId);

    /**
     * Copy asked file from storage to HttpServletResponse
     */
    void downloadFile(OrderDataFile dataFile, UniformResourceName aipId, String checksum, OutputStream os)
            throws IOException;

    /**
     * Search all current orders (ie not finished), compute and update completion values (percentCompleted and files in
     * error count).
     * Search all orders (eventually finished), compute available files count and update values.
     * THIS METHOD DON'T UPDATE ANYTHING INTO DATABASE (it concerns Orders so it is the responsibility of OrderService,
     * @see IOrderService#updateTenantOrdersComputations )
     * @return updated orders
     */
    Set<Order> updateCurrentOrdersComputedValues();

    /**
     * Remove all data files from an order
     */
    void removeAll(Long orderId);
}
