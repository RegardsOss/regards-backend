package fr.cnes.regards.microserices.administration.stubs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.project.client.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Resource<Project>> retrieveProject(final String pProjectName) throws EntityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Resource<Project>> updateProject(final String pProjectName, final Project pProjectToUpdate)
            throws EntityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteProject(final String pProjectName) throws EntityException {
        // TODO Auto-generated method stub
        return null;
    }

}
