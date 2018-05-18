package fr.cnes.regards.modules.order.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.order.domain.basket.Basket;

/**
 * Basket repository
 * @author oroussel
 */
@Repository
public interface IBasketRepository extends JpaRepository<Basket, Long> {

    /**
     * Load a basket with all its relations
     */
    @EntityGraph("graph.basket")
    Basket findOneById(Long id);

    /**
     * Load a basket with all its relations
     */
    @EntityGraph("graph.basket")
    Basket findByOwner(String owner);

}
