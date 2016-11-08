/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration.stubs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class ProjectClientStub
 *
 * Stub to get a test implementation for IProjectsClient
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class ProjectClientStub implements IProjectsClient {

    /**
     * Project name return by this stub
     */
    public static final String PROJECT_NAME = "stub-project";

    /**
     * Project managed by this stub
     */
    public static final Project PROJECT = new Project(0L, "", "", true, PROJECT_NAME);

    @Override
    public ResponseEntity<List<Resource<Project>>> retrieveProjectList() {
        final List<Resource<Project>> resouces = new ArrayList<>();
        resouces.add(new Resource<Project>(PROJECT));
        return new ResponseEntity<>(resouces, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<Project>> createProject(final Project pNewProject) throws EntityException {
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Resource<Project>> retrieveProject(final String pProjectName) throws EntityException {
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Resource<Project>> updateProject(final String pProjectName, final Project pProjectToUpdate)
            throws EntityException {
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Void> deleteProject(final String pProjectName) throws EntityException {
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
