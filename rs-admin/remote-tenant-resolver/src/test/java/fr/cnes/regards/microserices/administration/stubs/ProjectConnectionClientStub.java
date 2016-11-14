/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration.stubs;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectClientStub
 *
 * Stub class for administration service client.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class ProjectConnectionClientStub implements IProjectConnectionClient {

    @Override
    public ResponseEntity<Resource<ProjectConnection>> retrieveProjectConnection(final String pProjectName,
            final String pMicroService) {

        final ProjectConnection connection = new ProjectConnection(0L, ProjectClientStub.PROJECT, pMicroService, "", "",
                "", "");
        final Resource<ProjectConnection> resource = new Resource<ProjectConnection>(connection);
        return new ResponseEntity<Resource<ProjectConnection>>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<ProjectConnection>> createProjectConnection(
            final ProjectConnection pProjectConnection) {
        return null;
    }

    @Override
    public ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(
            final ProjectConnection pProjectConnection) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteProjectConnection(final String pProjectName, final String pMicroservice) {
        return null;
    }

}
