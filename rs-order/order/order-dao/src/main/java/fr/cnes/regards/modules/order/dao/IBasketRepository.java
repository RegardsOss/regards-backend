/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.order.domain.basket.Basket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Basket repository
 *
 * @author oroussel
 */
@Repository
public interface IBasketRepository extends JpaRepository<Basket, Long> {

    /**
     * Load a basket with all its relations
     */
    @EntityGraph(value = "graph.basket", type = EntityGraph.EntityGraphType.LOAD)
    Basket findOneById(Long id);

    /**
     * Load a basket with all its relations
     */
    @EntityGraph(value = "graph.basket", type = EntityGraph.EntityGraphType.LOAD)
    Basket findByOwner(String owner);

}
