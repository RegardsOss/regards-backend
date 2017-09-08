package fr.cnes.regards.modules.order.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.order.domain.Order;

/**
 * Order repository
 * @author oroussel
 */
public interface IOrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph("graph.order")
    Order findOneById(Long id);
}
