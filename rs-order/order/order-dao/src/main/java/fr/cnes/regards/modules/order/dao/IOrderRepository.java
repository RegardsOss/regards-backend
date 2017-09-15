package fr.cnes.regards.modules.order.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.order.domain.Order;

/**
 * Order repository
 * @author oroussel
 */
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
    Page<Order> findAllByEmailOrderByCreationDateDesc(String email, Pageable pageRequest);
}
