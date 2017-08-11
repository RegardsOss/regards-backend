package fr.cnes.regards.modules.order.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.order.domain.basket.Basket;

/**
 * @author oroussel
 */
public interface IBasketRepository extends JpaRepository<Basket, Long> {

    /**
     * Load a basket with all its relations
     */
/*    @EntityGraph(attributePaths = { "datasetSelections", "datasetSelections.itemsSelections" },
            type = EntityGraph.EntityGraphType.LOAD)*/
    @EntityGraph("graph.basket")
    Basket findOneById(Long id);

    /**
     * Load a basket with all its relations
     */
    @EntityGraph("graph.basket")
    Basket findByEmail(String email);

}
