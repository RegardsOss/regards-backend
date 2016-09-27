/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao.stubs;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;

@Repository
@Profile("test")
@Primary
public class ProjectRepositoryStub extends RepositoryStub<Project> implements IProjectRepository {

    public ProjectRepositoryStub() {
        entities_.add(new Project(0L, "desc", "icon", true, "name"));
    }

    @Override
    public Project findOneByName(String pName) {
        return entities_.stream().filter(e -> e.getName().equals(pName)).findFirst().get();
    }
}
