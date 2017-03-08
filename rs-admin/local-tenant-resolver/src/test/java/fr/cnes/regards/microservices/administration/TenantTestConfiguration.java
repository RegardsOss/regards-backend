/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.microservices.administration.LocalTenantConnectionResolver;
import fr.cnes.regards.microservices.administration.LocalTenantConnectionResolverAutoConfiguration;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.service.IProjectService;

/**
 *
 * Class JpaTenantConnectionConfiguration
 *
 * Test configuration class
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@ComponentScan("fr.cnes.regards.modules")
@PropertySource("classpath:application-test.properties")
@EnableAutoConfiguration(exclude = LocalTenantConnectionResolverAutoConfiguration.class)
@ImportResource({ "classpath*:defaultRoles.xml" })
public class TenantTestConfiguration {

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
     *
     * Initialize a Mock for AMQP Publisher
     *
     * @return IPublisher
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IPublisher mockPublisher() {
        return Mockito.mock(IPublisher.class);
    }

    /**
     *
     * Initialize a Mock for AMQP Subsriber
     *
     * @return ISubscriber
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public ISubscriber mockSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    /**
     *
     * Initialize to add a project and a project connection associated.
     *
     * @param pProjectRepo
     *            JPA repository
     * @param pProjectConnRepo
     *            JPA repository
     * @param pProjectService
     *            JPA repository
     * @return ITenantConnectionResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public ITenantConnectionResolver resolver(final IProjectRepository pProjectRepo,
            final IProjectConnectionRepository pProjectConnRepo, final IProjectService pProjectService,
            final IProjectConnectionService pProjectConnectionService) {
        pProjectConnRepo.deleteAll();
        pProjectRepo.deleteAll();
        Project project = new Project(0L, "desc", "icon", true, PROJECT_NAME);
        project = pProjectRepo.save(project);

        final ProjectConnection conn = new ProjectConnection(0L, project, microserviceName, "user", "password",
                "driver", "url");
        pProjectConnRepo.save(conn);

        return new LocalTenantConnectionResolver(microserviceName, pProjectService, pProjectConnectionService);
    }

}
