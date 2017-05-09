/*
 * LICENSE_PLACEHOLDER
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
public interface IProjectUserRepository extends JpaRepository<ProjectUser, Long> {

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
    Page<ProjectUser> findByStatus(UserStatus pStatus, Pageable pPageable);

    /**
     * Find all {@link ProjectUser}s where <code>email</code> is in passed collection.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pEmail
     *            The {@link Collection} of <code>email</code>
     * @return The {@link List} of found {@link ProjectUser}s
     */
    List<ProjectUser> findByEmailIn(Collection<String> pEmail);

    /**
     * Find all project users whose role name equals param.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pName
     *            The role name
     * @return all project users with this role
     */
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
    Page<ProjectUser> findByRoleNameIn(Set<String> pNames, Pageable pPageable);
}
