/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.projects;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.accessrights.domain.projects.Role;

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
     * @param pIsDefault
     *            <code>True</code> or <code>False</code>
     * @return The found {@link Role}
     */
    @EntityGraph(value = "graph.role.permissions")
    Optional<Role> findOneByIsDefault(boolean pIsDefault);

    /**
     * Find the unique {@link Role}s where <code>name</code> equal to passed name.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pName
     *            The <code>name</code>
     * @return The found {@link Role}
     */
    @EntityGraph(value = "graph.role.permissions")
    Optional<Role> findOneByName(String pName);

    /**
     * Find the all {@link Role}s where <code>name</code> is in passed collection.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pNames
     *            The {@link Collection} of <code>name</code>
     * @return The {@link List} of found {@link Role}s
     */
    List<Role> findByNameIn(Collection<String> pNames);

    /**
     *
     * Find all roles which parent role is the given name.
     *
     * @param pName
     *            name of the parent role
     * @return a {@link List} of {@link Role}
     * @since 1.0-SNAPSHOT
     */
    List<Role> findByParentRoleName(final String pName);

    /**
     * Find all {@link Role} all load the permissions attributes.
     * 
     * @return a {@link List} of {@link Role}
     */
    @Query("select distinct r from Role r left join fetch r.permissions")
    List<Role> findAllDistinctLazy();

}
