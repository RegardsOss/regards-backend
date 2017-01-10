/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration.stubs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.domain.Project;
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
    public ResponseEntity<PagedResources<Resource<ProjectConnection>>> retrieveProjectsConnections(
            final String pProjectName, final String pMicroService) {
        final List<Resource<ProjectConnection>> resources = new ArrayList<>();
        final ProjectConnection connection = new ProjectConnection(0L, ProjectClientStub.PROJECT, pMicroService, "", "",
                "", "");
        resources.add(new Resource<ProjectConnection>(connection));

        final PagedResources<Resource<ProjectConnection>> page = new PagedResources<>(resources,
                new PageMetadata(1, 1, 1), new ArrayList<>());
        return new ResponseEntity<>(page, HttpStatus.OK);
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
