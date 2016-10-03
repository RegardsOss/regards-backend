/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.provider;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import fr.cnes.regards.modules.project.client.ProjectsClient;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * @author svissier
 *
 */
@Component
public class ProjectsProvider implements IProjectsProvider {

    @Autowired
    private EurekaClient discoveryClient_;

    @Autowired
    private JWTService jwtService_;

    @Override
    public List<String> retrieveProjectList() {
        InstanceInfo instance = discoveryClient_.getNextServerFromEureka("rs-admin", false);
        String adminUrl = instance.getHomePageUrl();
        String jwt = jwtService_.generateToken("", "", "", "ADMIN");
        List<String> projects = ProjectsClient.getClient(adminUrl, jwt).retrieveProjectList();
        return projects;
    }

}
