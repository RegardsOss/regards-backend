package fr.cnes.regards.modules.order.service;

import javax.servlet.http.HttpServletResponse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.exception.CannotDeleteOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotRemoveOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.domain.exception.NotYetAvailableException;

/**
 * Order service
 * @author oroussel
 */
public interface IOrderService {
    String ORDER_TOKEN = "orderToken";

    String ORDER_ID_KEY = "ORDER_ID";

    /**
     * Create an order
     * @param basket basket from which order is created
     * @return copletely loaded order
     */
    Order createOrder(Basket basket);

    /**
     * Load an order.
     * Order is simple loaded
     * @param id order id
     */
    Order loadSimple(Long id);

    /**
     * Load an order.
     * Order is completely loaded
     * @param id order id
     */
    Order loadComplete(Long id);

    /**
     * Pause an order (status is immediately updated but it's an async task)
     */
    void pause(Long id);

    /**
     * Resume a paused order.
     * All associated jobs must be compatible with a PAUSED status (not running nor planned to be run)
     */
    void resume(Long id) throws CannotResumeOrderException;

    /**
     * Delete an order. Order must be PAUSED and effectiveley paused (ie all associated jobs must be compatible with a
     * PAUSED status (not running nor planned to be run))
     * Only associated data files are removed from database (stats are still available)
     */
    void delete(Long id) throws CannotDeleteOrderException;

    /**
     * Remove completely an order. Current order status must be DELETED, All associated jobs must be compatible with a
     * PAUSED status (not running nor planned to be run)
     */
    void remove(Long id) throws CannotRemoveOrderException;

    /**
     * Find all orders sorted by descending date.
     * Orders are simple loaded
     */
    Page<Order> findAll(Pageable pageRequest);

    /**
     * Write all orders in CSV format
     */
    void writeAllOrdersInCsv(BufferedWriter writer) throws IOException;

    default Page<Order> findAll(int pageSize) {
        return findAll(new PageRequest(0, pageSize));
    }

    /**
     * Find all user orders sorted by descending date
     * Orders are simple loaded
     * @param user user 
     */
    Page<Order> findAll(String user, Pageable pageRequest);

    default Page<Order> findAll(String user, int pageSize) {
        return findAll(user, new PageRequest(0, pageSize));
    }

    /**
     * Create a ZIP containing all currently available files. Once a file has been part of ZIP file, it will not be
     * part of another again.
     * @param inDataFiles concerned order data files
     * @throws NotYetAvailableException if no files are available yet
     */
    void downloadOrderCurrentZip(List<OrderDataFile> inDataFiles, OutputStream os) throws IOException;

    /**
     * Create a metalink file with all files.
     * @param orderId concerned order id
     */
    void downloadOrderMetalink(Long orderId, OutputStream os);

    /**
     * Scheduled method to update all current running orders completions values into database
     */
    void updateCurrentOrdersCompletions();

    /**
     * Same method as previous one but for one tenant (ence transactionnal)
     */
    void updateTenantCurrentOrdersCompletions();
}
