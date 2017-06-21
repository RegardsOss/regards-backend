/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao.stub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.JpaRepositoryStub;
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
public class ProjectRepositoryStub extends JpaRepositoryStub<Project> implements IProjectRepository {

    public ProjectRepositoryStub() {
        this.entities.add(new Project(0L, "desc", "icon", true, "name"));
    }

    @Override
    public Project findOneByNameIgnoreCase(final String pName) {
        Project result = null;
        final Optional<Project> project = this.entities.stream().filter(e -> e.getName().equals(pName)).findFirst();
        if (project.isPresent()) {
            result = project.get();
        }
        return result;
    }

    @Override
    public Page<Project> findByIsPublicTrue(final Pageable pPageable) {
        final List<Project> publicProjects = new ArrayList<>();
        publicProjects
                .addAll(this.entities.stream().filter(project -> project.isPublic()).collect(Collectors.toList()));
        return new PageImpl<>(publicProjects);
    }
}
