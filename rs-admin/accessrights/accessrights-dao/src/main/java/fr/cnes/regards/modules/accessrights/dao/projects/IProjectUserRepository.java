/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link ProjectUser}s.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
public interface IProjectUserRepository extends JpaRepository<ProjectUser, Long>, JpaSpecificationExecutor<ProjectUser> {

    /**
     * Find the single {@link ProjectUser} with passed <code>email</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pEmail The {@link ProjectUser}'s <code>email</code>
     * @return The optional {@link ProjectUser} with passed <code>email</code>
     */
    @EntityGraph(value = "graph.user.metadata", type = EntityGraph.EntityGraphType.LOAD)
    Optional<ProjectUser> findOneByEmail(String pEmail);

    @EntityGraph(value = "graph.user.metadata", type = EntityGraph.EntityGraphType.LOAD)
    Optional<ProjectUser> findById(Long id);

    /**
     * Find all {@link ProjectUser}s with passed <code>status</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param status
     *            The {@link ProjectUser}'s <code>status</code>
     * @param pageable
     *            the pagination information
     * @return The {@link List} of {@link ProjectUser}s with passed <code>status</code>
     */
    default Page<ProjectUser> findByStatus(UserStatus status, Pageable pageable) {
        Page<Long> idPage = findIdPageByStatus(status, pageable);
        List<ProjectUser> projectUsers = findAllById(idPage.getContent());
        return new PageImpl<>(projectUsers, idPage.getPageable(), idPage.getTotalElements());
    }

    @Override
    @EntityGraph(value = "graph.user.metadata", type = EntityGraph.EntityGraphType.LOAD)
    List<ProjectUser> findAllById(Iterable<Long> ids);

    @Query(value = "select pu.id from ProjectUser pu where pu.status=:status")
    Page<Long> findIdPageByStatus(@Param("status") UserStatus status, Pageable pageable);

    /**
     * Find all {@link ProjectUser}s where <code>email</code> is in passed collection.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pEmail
     *            The {@link Collection} of <code>email</code>
     * @return The {@link List} of found {@link ProjectUser}s
     */
    @EntityGraph(value = "graph.user.metadata", type = EntityGraph.EntityGraphType.LOAD)
    List<ProjectUser> findByEmailIn(Collection<String> pEmail);

    /**
     * Find all project users whose role name equals param.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pName
     *            The role name
     * @return all project users with this role
     */
    @EntityGraph(value = "graph.user.metadata", type = EntityGraph.EntityGraphType.LOAD)
    List<ProjectUser> findByRoleName(String pName);

    /**
     * Find all project users whose role name equals param.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param names
     *            a set of role name
     * @param pageable
     *            the pagination information
     * @return all project users with this role
     */
    default Page<ProjectUser> findByRoleNameIn(Set<String> names, Pageable pageable) {
        Page<Long> idPage = findIdPageByRoleNameIn(names, pageable);
        List<ProjectUser> projectUsers = findAllById(idPage.getContent());
        return new PageImpl<>(projectUsers, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query(value = "select pu.id from ProjectUser pu where pu.role.name in :names")
    Page<Long> findIdPageByRoleNameIn(@Param("names") Set<String> names, Pageable pageable);

    /**
     * Find all project users Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pageable
     *            the pagination information
     * @return all project users with this role
     */
    @Override
    @EntityGraph(value = "graph.user.metadata", type = EntityGraph.EntityGraphType.LOAD)
    default Page<ProjectUser> findAll(Pageable pageable) {
        Page<Long> idPage = findIdPage(pageable);
        List<ProjectUser> projectUsers = findAllById(idPage.getContent());
        return new PageImpl<>(projectUsers, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("select pu.id from ProjectUser pu")
    Page<Long> findIdPage(Pageable pageable);

    /**
     * Find all project users according to the given specification
     *
     * @param spec     project user specification
     * @param pageable the pagination information
     * @return all project users with this role
     */
    @Override
    @EntityGraph(value = "graph.user.metadata", type = EntityGraph.EntityGraphType.LOAD)
    Page<ProjectUser> findAll(Specification<ProjectUser> spec, Pageable pageable);


    @Query(nativeQuery = true, value = "SELECT access_group, count(*) FROM {h-schema}ta_project_user_access_group GROUP BY access_group")
    List<Object[]> getCountByAccessGroup();

    default Map<String, Long> getUserCountByAccessGroup() {
        return getCountByAccessGroup().stream().collect(Collectors.toMap(count -> (String) count[0], count -> ((BigInteger) count[1]).longValue()));
    }

}
