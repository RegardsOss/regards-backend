/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.ITenantConnectionResolver;
import fr.cnes.regards.microservices.administration.LocalTenantConnectionResolver;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectService;

/**
 *
 * Class JpaTenantConnectionConfiguration
 *
 * Test configuratiob class
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@ComponentScan("fr.cnes.regards.modules")
@PropertySource("classpath:dao.properties")
@ImportResource({ "classpath*:defaultRoles.xml" })
public class LocalTenantConnectionConfiguration {

    /**
     * Test project name
     */
    public static final String PROJECT_NAME = "new-test-project";

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * JPA Respository stub
     */
    @Autowired
    private IProjectRepository projectRepo;

    /**
     * JPA Respository stub
     */
    @Autowired
    private IProjectConnectionRepository projectConnRepo;

    /**
     * Service to access projects entities
     */
    @Autowired
    private IProjectService projectService;

    /**
     *
     * Initialize to add a project and a project connection associated.
     *
     * @return ITenantConnectionResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public ITenantConnectionResolver resolver() {
        Project project = new Project(0L, "desc", "icon", true, PROJECT_NAME);
        project = projectRepo.save(project);

        final ProjectConnection conn = new ProjectConnection(0L, project, microserviceName, "user", "password",
                "driver", "url");
        projectConnRepo.save(conn);

        return new LocalTenantConnectionResolver(microserviceName, projectService);
    }

}
