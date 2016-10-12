/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao.stub;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.microservices.core.test.repository.RepositoryStub;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectConnectionRepositoryStub
 *
 * STUB for JPA ProjectConnection Repository.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Repository
@Profile("test")
@Primary
public class ProjectConnectionRepositoryStub extends RepositoryStub<ProjectConnection>
        implements IProjectConnectionRepository {

    @Override
    public ProjectConnection findOneByProjectNameAndMicroservice(final String pProjectName,
            final String pMicroService) {
        ProjectConnection result = null;
        for (final ProjectConnection conn : this.entities) {
            if ((conn.getProject() != null) && conn.getProject().getName().equals(pProjectName)
                    && conn.getMicroservice().equals(pMicroService)) {
                result = conn;
                break;
            }
        }

        return result;
    }

}
