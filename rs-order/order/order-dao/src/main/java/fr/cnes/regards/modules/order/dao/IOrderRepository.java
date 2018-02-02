package fr.cnes.regards.modules.order.dao;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;

/**
 * Order repository
 * @author oroussel
 */
@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {

    /**
     * Load Order with all lazy relations
     */
    @EntityGraph("graph.order.complete")
    Order findCompleteById(Long id);

    /**
     * Load Order one level lazy relations (ie. only dataTasks)
     */
    @EntityGraph("graph.order.simple")
    Order findSimpleById(Long id);

    @EntityGraph("graph.order.simple")
    Page<Order> findAllByOrderByCreationDateDesc(Pageable pageRequest);

    @EntityGraph("graph.order.simple")
    Page<Order> findAllByOwnerOrderByCreationDateDesc(String owner, Pageable pageRequest);

    @EntityGraph("graph.order.simple")
    Page<Order> findAllByOwnerAndStatusNotInOrderByCreationDateDesc(String owner, OrderStatus[] excludeStatuses,
            Pageable pageRequest);

    @EntityGraph("graph.order.simple")
    List<Order> findAllByWaitingForUserAndAvailableFilesCountGreaterThanAndStatusIn(boolean waitingForUser,
            int minAvailableCount, OrderStatus... statuses);

    @EntityGraph("graph.order.simple")
    List<Order> findByAvailableFilesCountGreaterThanAndAvailableUpdateDateLessThanOrderByOwner(int count,
            OffsetDateTime date);

    @EntityGraph("graph.order.simple")
    Optional<Order> findOneByExpirationDateLessThanAndStatusIn(OffsetDateTime date, OrderStatus... statuses);

    /**
     * Find all orders considered as "aside" ie whom no associated data files have been downloaded since specified
     * days count
     * Orders are sorted by owner
     */
    default List<Order> findAsideOrders(int daysBeforeConsideringAside) {
        return findByAvailableFilesCountGreaterThanAndAvailableUpdateDateLessThanOrderByOwner(0, OffsetDateTime.now()
                .minus(daysBeforeConsideringAside, ChronoUnit.DAYS));
    }

    /**
     * Find one expired order.
     */
    default Optional<Order> findOneExpiredOrder() {
        return findOneByExpirationDateLessThanAndStatusIn(OffsetDateTime.now(), OrderStatus.PENDING,
                                                          OrderStatus.RUNNING, OrderStatus.PAUSED);
    }

    /**
     * Find all finished orders not waiting for user with availableCount > 0
     */
    default List<Order> findFinishedOrdersToUpdate() {
        return findAllByWaitingForUserAndAvailableFilesCountGreaterThanAndStatusIn(false, 0, OrderStatus.EXPIRED,
                                                                                   OrderStatus.DONE,
                                                                                   OrderStatus.DONE_WITH_WARNING,
                                                                                   OrderStatus.FAILED);
    }
}
