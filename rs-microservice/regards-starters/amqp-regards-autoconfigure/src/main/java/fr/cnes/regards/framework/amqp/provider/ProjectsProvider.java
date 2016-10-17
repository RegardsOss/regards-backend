/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.project.client.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * @author svissier
 *
 */
@Component
public class ProjectsProvider implements IProjectsProvider {

    /**
     * Feign client to retrieve all projects from the project module in the microservice administration by default
     */
    @Autowired
    private IProjectsClient projectsClient;

    @Override
    public List<String> retrieveProjectList() {
        final HttpEntity<List<Resource<Project>>> httpProjects = projectsClient.retrieveProjectList();
        return httpProjects.getBody().stream().map(r -> r.getContent().getName()).collect(Collectors.toList());
    }

}
