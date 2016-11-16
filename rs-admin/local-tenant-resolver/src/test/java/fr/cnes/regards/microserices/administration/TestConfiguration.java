/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.microservices.administration.LocalAuthoritiesProvider;
import fr.cnes.regards.microservices.administration.LocalTenantConnectionResolver;
import fr.cnes.regards.microservices.administration.LocalTenantConnectionResolverAutoConfigure;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;
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
@PropertySource("classpath:dao.properties")
@EnableAutoConfiguration(exclude = LocalTenantConnectionResolverAutoConfigure.class)
@ImportResource({ "classpath*:defaultRoles.xml" })
public class TestConfiguration {

    /**
     * Test project name
     */
    public static final String PROJECT_NAME = "new-test-project";

    /**
     * Role name with access granted to CORS requests.
     */
    public static final String CORS_ROLE_NAME_GRANTED = "USER_CORS_OK";

    /**
     * Role name with access denied to CORS requests.
     */
    public static final String CORS_ROLE_NAME_INVALID_1 = "USER_CORS_NOK_1";

    /**
     * Role name with access denied to CORS requests.
     */
    public static final String CORS_ROLE_NAME_INVALID_2 = "USER_CORS_NOK_2";

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

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

    @Bean
    public IAuthoritiesProvider authoritiesProvider(final JWTService pJwtService, final IRoleRepository pRoleRpo,
            final IResourcesAccessRepository pResourceAccessRepo) throws JwtException {

        pJwtService.injectToken(PROJECT_NAME, CORS_ROLE_NAME_GRANTED);
        final List<String> addresses = new ArrayList<>();
        addresses.add("127.0.0.1");
        addresses.add("127.0.0.2");
        addresses.add("127.0.0.3");
        pRoleRpo.deleteAll();

        final LocalDateTime endDate = LocalDateTime.now().plusDays(5L);

        final RoleFactory roleFactory = new RoleFactory();
        roleFactory.withId(0L).withAuthorizedAddresses(addresses).withCorsRequestsAuthorized(true)
                .withCorsRequestsAuthorizationEndDate(endDate).withDefault(false).withNative(true)
                .withCorsRequestsAuthorized(true);

        final Role publicRole = pRoleRpo.save(roleFactory.createPublic());

        pRoleRpo.save(roleFactory.withName(CORS_ROLE_NAME_GRANTED).withParentRole(publicRole).create());

        pRoleRpo.save(roleFactory.withName(CORS_ROLE_NAME_INVALID_1)
                .withCorsRequestsAuthorizationEndDate(LocalDateTime.now().minusDays(5L)).create());

        pRoleRpo.save(roleFactory.withName(CORS_ROLE_NAME_INVALID_2).withCorsRequestsAuthorized(false)
                .withCorsRequestsAuthorizationEndDate(null).create());

        pResourceAccessRepo.deleteAll();
        pResourceAccessRepo.save(new ResourcesAccess(0L, "description", microserviceName, "/resource", HttpVerb.GET));
        pResourceAccessRepo.save(new ResourcesAccess(0L, "description", microserviceName, "/resource", HttpVerb.PUT));
        pResourceAccessRepo.save(new ResourcesAccess(0L, "description", microserviceName, "/resource", HttpVerb.POST));
        pResourceAccessRepo
                .save(new ResourcesAccess(0L, "description", microserviceName, "/resource", HttpVerb.DELETE));

        return new LocalAuthoritiesProvider();
    }

}
