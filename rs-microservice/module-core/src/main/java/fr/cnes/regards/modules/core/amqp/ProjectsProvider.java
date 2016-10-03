/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * @author svissier
 *
 */
public class ProjectsProvider implements IProjectsProvider {

    @Autowired
    private EurekaClient discoveryClient_;

    @Autowired
    private JWTService jwtService_;

    /**
     * TODO: remove comment on projects to get via feign client
     */
    @Override
    public List<String> retrieveProjectList() {
        InstanceInfo instance = discoveryClient_.getNextServerFromEureka("rs-admin", false);
        String adminUrl = instance.getHomePageUrl();
        String jwt = jwtService_.generateToken("", "", "", "ADMIN");
        // List<String> projects = IProjectSignature.getClient(adminUrl, jwt).retrieveProjectList();
        List<String> projects = new ArrayList<>();
        return projects;
    }

}
