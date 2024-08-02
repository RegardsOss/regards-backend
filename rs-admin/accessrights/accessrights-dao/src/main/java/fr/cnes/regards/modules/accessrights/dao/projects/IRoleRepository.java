/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.dao.projects;

import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link Role}s.<br>
 * Embed paging/sorting abilities by extending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
public interface IRoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find the unique {@link Role} where <code>default</code> equal to passed boolean.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pIsDefault <code>True</code> or <code>False</code>
     * @return The found {@link Role}
     */
    @EntityGraph(value = "graph.role.permissions", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Role> findOneByIsDefault(boolean pIsDefault);

    /**
     * Find the unique {@link Role}s where <code>name</code> equal to passed name.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pName The <code>name</code>
     * @return The found {@link Role}
     */
    @EntityGraph(value = "graph.role.permissions", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Role> findOneByName(String pName);

    /**
     * Same method as @link {@link #findOneByName(String)} loading permissions, parent role and parent permissions.
     *
     * @param name role name
     * @return role
     */
    @EntityGraph(value = "graph.role.parent", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Role> findByName(String name);

    /**
     * Find the role with its permissions
     *
     * @param pId role identifier
     */
    @EntityGraph(value = "graph.role.permissions", type = EntityGraph.EntityGraphType.LOAD)
    Role findOneById(Long pId);

    /**
     * Find the all {@link Role}s where <code>name</code> is in passed collection.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pNames The {@link Collection} of <code>name</code>
     * @return The {@link List} of found {@link Role}s
     */
    List<Role> findByNameIn(Collection<String> pNames);

    /**
     * Find all roles which parent role is the given name.
     *
     * @param pName name of the parent role
     * @return a {@link List} of {@link Role}
     */
    @Query("select distinct r from Role r left join fetch r.permissions where r.parentRole.name=:pName")
    Set<Role> findByParentRoleName(@Param("pName") final String pName);

    /**
     * Find all {@link Role} all load the permissions attributes.
     *
     * @return a {@link List} of {@link Role}
     */
    @Query("select distinct r from Role r left join fetch r.permissions")
    Set<Role> findAllDistinctLazy();

    /**
     * Find all roles associated to given ResourceAccess Id
     *
     * @return a {@link Set} of {@link Role}
     */
    Set<Role> findByPermissionsId(Long pPermissionId);

}
