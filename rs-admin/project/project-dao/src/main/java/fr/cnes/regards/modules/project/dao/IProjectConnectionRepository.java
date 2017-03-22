/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class IProjectConnectionRepository
 *
 * JPA Repository to access ProjectConnection entities.
 *
 * @author CS
 * @author Xavier-Alexandre Brochard
 * @author Marc Sordi
 * @since 1.0-SNAPSHOT
 */
@InstanceEntity
public interface IProjectConnectionRepository extends JpaRepository<ProjectConnection, Long> {

    /**
     * Retrieve all tenant connections for a specified microservice
     * 
     * @param microservice
     *            microservice name
     * @return all tenant connections
     */
    List<ProjectConnection> findByMicroservice(String microservice);

    ProjectConnection findOneByProjectNameAndMicroservice(final String pProjectName, final String pMicroService);

    /**
     * Find all {@link ProjectConnection}s whose project has given <code>name</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pProjectName
     *            The {@link ProjectConnection}'s {@link Projects}'s <code>name</code>
     * @return A {@link Page} of found {@link ProjectConnection}s
     */
    Page<ProjectConnection> findByProjectName(String pProjectName, Pageable pPageable);
}
