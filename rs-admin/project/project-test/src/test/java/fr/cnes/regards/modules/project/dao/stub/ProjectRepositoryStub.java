/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao.stub;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.RepositoryStub;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class ProjectRepositoryStub
 *
 * Stub for JPA Repository
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Repository
@Primary
public class ProjectRepositoryStub extends RepositoryStub<Project> implements IProjectRepository {

    public ProjectRepositoryStub() {
        this.entities.add(new Project(0L, "desc", "icon", true, "name"));
    }

    @Override
    public Project findOneByName(final String pName) {
        Project result = null;
        final Optional<Project> project = this.entities.stream().filter(e -> e.getName().equals(pName)).findFirst();
        if (project.isPresent()) {
            result = project.get();
        }
        return result;
    }
}
