/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link ProjectUser}s.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
public interface IProjectUserRepository extends JpaRepository<ProjectUser, Long>,
        JpaSpecificationExecutor<ProjectUser> {

    /**
     * Find the single {@link ProjectUser} with passed <code>email</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pEmail
     *            The {@link ProjectUser}'s <code>email</code>
     * @return The optional {@link ProjectUser} with passed <code>email</code>
     */
    @EntityGraph(value = "graph.user.metadata")
    Optional<ProjectUser> findOneByEmail(String pEmail);

    /**
     * Find all {@link ProjectUser}s with passed <code>status</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pStatus
     *            The {@link ProjectUser}'s <code>status</code>
     * @param pPageable
     *            the pagination information
     * @return The {@link List} of {@link ProjectUser}s with passed <code>status</code>
     */
    @EntityGraph(value = "graph.user.metadata")
    Page<ProjectUser> findByStatus(UserStatus pStatus, Pageable pPageable);

    /**
     * Find all {@link ProjectUser}s where <code>email</code> is in passed collection.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pEmail
     *            The {@link Collection} of <code>email</code>
     * @return The {@link List} of found {@link ProjectUser}s
     */
    @EntityGraph(value = "graph.user.metadata")
    List<ProjectUser> findByEmailIn(Collection<String> pEmail);

    /**
     * Find all project users whose role name equals param.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pName
     *            The role name
     * @return all project users with this role
     */
    @EntityGraph(value = "graph.user.metadata")
    List<ProjectUser> findByRoleName(String pName);

    /**
     * Find all project users whose role name equals param.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pNames
     *            a set of role name
     * @param pPageable
     *            the pagination information
     * @return all project users with this role
     */
    @EntityGraph(value = "graph.user.metadata")
    Page<ProjectUser> findByRoleNameIn(Set<String> pNames, Pageable pPageable);

    /**
     * Find all project users Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pPageable
     *            the pagination information
     * @return all project users with this role
     */
    @Override
    @EntityGraph(value = "graph.user.metadata")
    Page<ProjectUser> findAll(Pageable pPageable);

}
