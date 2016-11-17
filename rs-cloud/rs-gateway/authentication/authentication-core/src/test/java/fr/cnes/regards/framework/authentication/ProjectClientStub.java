/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

@Component
@Primary
public class ProjectClientStub implements IProjectsClient {

    private static List<Project> projects = new ArrayList<>();

    private static long idCount = 0;

    @Override
    public ResponseEntity<List<Resource<Project>>> retrieveProjectList() {
        return new ResponseEntity<List<Resource<Project>>>(HateoasUtils.wrapList(projects), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<Project>> createProject(final Project pNewProject) throws EntityException {
        pNewProject.setId(idCount++);
        projects.add(pNewProject);
        return new ResponseEntity<Resource<Project>>(HateoasUtils.wrap(pNewProject), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<Project>> retrieveProject(final String pProjectName) throws EntityException {
        Project result = null;
        for (final Project project : projects) {
            if (project.getName().equals(pProjectName)) {
                result = project;
                break;
            }
        }
        if (result == null) {
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        return new ResponseEntity<Resource<Project>>(HateoasUtils.wrap(result), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<Project>> updateProject(final String pProjectName, final Project pProjectToUpdate)
            throws EntityException {
        Project result = null;
        for (final Project project : projects) {
            if (project.getName().equals(pProjectName)) {
                project.setDescription(pProjectToUpdate.getDescription());
                result = project;
                break;
            }
        }
        if (result == null) {
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        return new ResponseEntity<Resource<Project>>(HateoasUtils.wrap(result), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteProject(final String pProjectName) throws EntityException {
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

}
