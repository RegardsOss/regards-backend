/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.project.client.ProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * @author svissier
 *
 */
@Component
public class ProjectsProvider implements IProjectsProvider {

    @Autowired
    private ProjectsClient projectsClient_;

    @Autowired
    private JWTService jwtService_;

    @Override
    public List<String> retrieveProjectList() {
        HttpEntity<List<Resource<Project>>> httpProjects = projectsClient_.retrieveProjectList();
        return httpProjects.getBody().stream().map(r -> r.getContent().getName()).collect(Collectors.toList());
    }

}
