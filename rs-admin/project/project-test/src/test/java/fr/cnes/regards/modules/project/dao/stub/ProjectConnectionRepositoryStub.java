/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao.stub;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.JpaRepositoryStub;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectConnectionRepositoryStub
 *
 * STUB for JPA ProjectConnection Repository.
 *
 * @author CS
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@Repository
@Primary
public class ProjectConnectionRepositoryStub extends JpaRepositoryStub<ProjectConnection>
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

    @Override
    public Page<ProjectConnection> findByProjectName(final String pProjectName, final Pageable pPageable) {
        final List<ProjectConnection> list = entities.stream()
                .filter(e -> e.getProject().getName().equals(pProjectName)).collect(Collectors.toList());
        return new PageImpl<>(list);
    }

    @Override
    public List<ProjectConnection> findByMicroservice(String pMicroservice) {
        List<ProjectConnection> list = entities.stream().filter(e -> e.getMicroservice().equals(pMicroservice))
                .collect(Collectors.toList());
        return list;
    }

}
