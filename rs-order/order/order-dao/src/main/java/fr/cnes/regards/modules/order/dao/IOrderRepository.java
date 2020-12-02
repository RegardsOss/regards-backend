/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.dao;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;

/**
 * Order repository
 * @author oroussel
 */
@Repository
public interface IOrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

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

    /**
     * Find by label and owner (allows checking unicity before inserting)
     * @param label
     * @param owner
     * @return
     */
    Optional<Order> findByLabelAndOwner(String label, String owner);

    default Page<Order> findAllByOrderByCreationDateDesc(Pageable pageRequest) {
        // pagination and entity graph do not cohexist well so we need to handle it by hand
        // 1. get a page of Ids concerned
        Page<OrderIdOnly> idPage = findIdPageByOrderByCreationDateDesc(pageRequest);
        // 2. query Orders from these ids
        List<Order> pageContent = findAllByIdInOrderByCreationDateDesc(idPage.map(OrderIdOnly::getId).getContent());
        // 3. Recreate the page from this
        return new PageImpl<>(pageContent, pageRequest, idPage.getTotalElements());
    }

    @Query(value = "select o.id as id from Order o order by o.creationDate desc",
            countQuery = "select count(o.id) from Order o order by o.creationDate desc")
    Page<OrderIdOnly> findIdPageByOrderByCreationDateDesc(Pageable pageRequest);

    default Page<Order> findAllByOwnerOrderByCreationDateDesc(String owner, Pageable pageRequest) {
        // pagination and entity graph do not cohexist well so we need to handle it by hand
        // 1. get a page of Ids concerned
        Page<OrderIdOnly> idPage = findAllIdsByOwnerOrderByCreationDateDesc(owner, pageRequest);
        // 2. query Orders from these ids
        List<Order> pageContent = findAllByIdInOrderByCreationDateDesc(idPage.map(OrderIdOnly::getId).getContent());
        // 3. Recreate the page from this
        return new PageImpl<>(pageContent, pageRequest, idPage.getTotalElements());
    }

    @Query(value = "select o.id as id from Order o where o.owner = :owner order by o.creationDate desc",
            countQuery = "select count(o.id) from Order o where o.owner = :owner order by o.creationDate desc")
    Page<OrderIdOnly> findAllIdsByOwnerOrderByCreationDateDesc(@Param("owner") String owner, Pageable pageRequest);

    default Page<Order> findAllByOwnerAndStatusNotInOrderByCreationDateDesc(String owner, OrderStatus[] excludeStatuses,
            Pageable pageRequest) {
        // pagination and entity graph do not cohexist well so we need to handle it by hand
        // 1. get a page of Ids concerned
        Page<OrderIdOnly> idPage = findAllIdsByOwnerAndStatusNotInOrderByCreationDateDesc(owner,
                                                                                          excludeStatuses,
                                                                                          pageRequest);
        // 2. query Orders from these ids
        List<Order> pageContent = findAllByIdInOrderByCreationDateDesc(idPage.map(OrderIdOnly::getId).getContent());
        // 3. Recreate the page from this
        return new PageImpl<>(pageContent, pageRequest, idPage.getTotalElements());
    }

    @Query(value = "select o.id as id from Order o where o.owner = :owner and o.status not in :excludeStatuses order by o.creationDate desc",
            countQuery = "select count(o.id) from Order o where o.owner = :owner and o.status not in :excludeStatuses order by o.creationDate desc")
    Page<OrderIdOnly> findAllIdsByOwnerAndStatusNotInOrderByCreationDateDesc(@Param("owner") String owner,
            @Param("excludeStatuses") OrderStatus[] excludeStatuses, Pageable pageRequest);

    @EntityGraph("graph.order.simple")
    List<Order> findAllByIdInOrderByCreationDateDesc(Collection<Long> ids);

    @EntityGraph("graph.order.simple")
    List<Order> findAllByWaitingForUserAndAvailableFilesCountGreaterThanAndStatusIn(boolean waitingForUser,
            int minAvailableCount, OrderStatus... statuses);

    @EntityGraph("graph.order.simple")
    List<Order> findByAvailableFilesCountGreaterThanAndAvailableUpdateDateLessThanAndStatusNotInOrderByOwner(int count,
            OffsetDateTime date, OrderStatus... statuses);

    @EntityGraph("graph.order.simple")
    Optional<Order> findOneByExpirationDateLessThanAndStatusIn(OffsetDateTime date, OrderStatus... statuses);

    /**
     * Find all orders considered as "aside" ie whom no associated data files have been downloaded since specified
     * days count
     * Orders are sorted by owner
     */
    default List<Order> findAsideOrders(int daysBeforeConsideringAside) {
        OffsetDateTime date = OffsetDateTime.now().minus(daysBeforeConsideringAside, ChronoUnit.DAYS);
        return findByAvailableFilesCountGreaterThanAndAvailableUpdateDateLessThanAndStatusNotInOrderByOwner(0,
                                                                                                            date,
                                                                                                            OrderStatus.PENDING,
                                                                                                            OrderStatus.DELETED,
                                                                                                            OrderStatus.FAILED,
                                                                                                            OrderStatus.EXPIRED);
    }

    /**
     * Find one expired order.
     */
    default Optional<Order> findOneExpiredOrder() {
        return findOneByExpirationDateLessThanAndStatusIn(OffsetDateTime.now(),
                                                          OrderStatus.PENDING,
                                                          OrderStatus.RUNNING,
                                                          OrderStatus.PAUSED);
    }

    /**
     * Find all finished orders not waiting for user with availableCount > 0
     */
    default List<Order> findFinishedOrdersToUpdate() {
        return findAllByWaitingForUserAndAvailableFilesCountGreaterThanAndStatusIn(false,
                                                                                   0,
                                                                                   OrderStatus.EXPIRED,
                                                                                   OrderStatus.DONE,
                                                                                   OrderStatus.DONE_WITH_WARNING,
                                                                                   OrderStatus.FAILED);
    }

    interface OrderIdOnly {

        Long getId();
    }
}
