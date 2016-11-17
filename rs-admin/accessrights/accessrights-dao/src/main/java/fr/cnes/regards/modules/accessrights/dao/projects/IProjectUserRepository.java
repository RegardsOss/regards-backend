/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.projects;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link ProjectUser}s.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author CS SI
 */
public interface IProjectUserRepository extends JpaRepository<ProjectUser, Long> {

    /**
     * Find the single {@link ProjectUser} with passed <code>email</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pEmail
     *            The {@link ProjectUser}'s <code>email</code>
     * @return The single {@link ProjectUser} with passed <code>email</code>
     */
    ProjectUser findOneByEmail(String pEmail);

    /**
     * Find all {@link ProjectUser}s with passed <code>status</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pStatus
     *            The {@link ProjectUser}'s <code>status</code>
     * @return The {@link List} of {@link ProjectUser}s with passed <code>status</code>
     */
    List<ProjectUser> findByStatus(UserStatus pStatus);

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
}
