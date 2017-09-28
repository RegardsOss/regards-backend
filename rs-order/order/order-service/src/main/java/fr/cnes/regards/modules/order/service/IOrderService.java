package fr.cnes.regards.modules.order.service;

import javax.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import fr.cnes.regards.framework.module.rest.exception.CannotResumeOrderException;
import fr.cnes.regards.framework.module.rest.exception.NotYetAvailableException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;

/**
 * Order service
 * @author oroussel
 */
public interface IOrderService {

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
     * Pause an order (async task)
     */
    void pause(Long id);

    /**
     * Resume a paused order (async task too)
     */
    void resume(Long id) throws CannotResumeOrderException;

    /**
     * Find all orders sorted by descending date.
     * Orders are simple loaded
     */
    Page<Order> findAll(Pageable pageRequest);

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

    void downloadOrderCurrentZip(Long orderId, HttpServletResponse response)
            throws NotYetAvailableException;

    /**
     * Scheduled method to update all current running orders completions values into database
     */
    void updateCurrentOrdersCompletions();

    void updateTenantCurrentOrdersCompletions();
}
