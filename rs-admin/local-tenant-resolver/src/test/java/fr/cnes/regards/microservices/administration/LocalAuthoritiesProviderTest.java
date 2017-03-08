/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;

/**
 *
 * Class LocalAuthoritiesProviderTest
 *
 * Test for administration local AuthoritiesProvider
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(classes = { AuthoritiesTestConfiguration.class })
public class LocalAuthoritiesProviderTest {

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Authorities provider to test
     */
    @Autowired
    private IAuthoritiesProvider provider;

    /**
     * Authorities provider to test
     */
    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private IRoleRepository roleRepository;

    @Before
    public void init() throws JwtException {
        jwtService.injectToken(AuthoritiesTestConfiguration.PROJECT_NAME,
                               AuthoritiesTestConfiguration.CORS_ROLE_NAME_GRANTED, "");
        final List<String> addresses = new ArrayList<>();
        addresses.add("127.0.0.1");
        addresses.add("127.0.0.2");
        addresses.add("127.0.0.3");
        final RoleFactory roleFactory = new RoleFactory();

        roleFactory.withId(0L).withAuthorizedAddresses(addresses).withCorsRequestsAuthorized(true).withDefault(false)
                .withNative(true);

        final Role publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString())
                .orElseGet(() -> roleRepository.save(roleFactory.createPublic()));

        roleFactory.withParentRole(publicRole);

        roleRepository.findOneByName(AuthoritiesTestConfiguration.CORS_ROLE_NAME_GRANTED)
                .ifPresent(role -> roleRepository.delete(role.getId()));
        roleRepository.save(roleFactory.withName(AuthoritiesTestConfiguration.CORS_ROLE_NAME_GRANTED)
                .withCorsRequestsAuthorizationEndDate(LocalDateTime.now().plusDays(5L)).create());

        roleRepository.findOneByName(AuthoritiesTestConfiguration.CORS_ROLE_NAME_INVALID_1)
                .ifPresent(role -> roleRepository.delete(role.getId()));
        roleRepository.save(roleFactory.withName(AuthoritiesTestConfiguration.CORS_ROLE_NAME_INVALID_1)
                .withCorsRequestsAuthorizationEndDate(LocalDateTime.now().minusDays(5L)).create());

        roleRepository.findOneByName(AuthoritiesTestConfiguration.CORS_ROLE_NAME_INVALID_2)
                .ifPresent(role -> roleRepository.delete(role.getId()));
        roleRepository.save(roleFactory.withName(AuthoritiesTestConfiguration.CORS_ROLE_NAME_INVALID_2)
                .withCorsRequestsAuthorized(false).withCorsRequestsAuthorizationEndDate(null).create());

        resourcesAccessRepository.deleteAll();
        resourcesAccessRepository.save(new ResourcesAccess("description", microserviceName, "/resource", HttpVerb.GET));
        resourcesAccessRepository.save(new ResourcesAccess("description", microserviceName, "/resource", HttpVerb.PUT));
        resourcesAccessRepository
                .save(new ResourcesAccess("description", microserviceName, "/resource", HttpVerb.POST));
        resourcesAccessRepository
                .save(new ResourcesAccess("description", microserviceName, "/resource", HttpVerb.DELETE));
    }

    /**
     *
     * Check cors requests access by role with date limitation
     *
     * @throws SecurityException
     *             when no role with passed name could be found
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_030")
    @Requirement("REGARDS_DSL_SYS_ARC_040")
    @Purpose("Check cors requests access by role with date limitation")
    @Test
    public void checkCorsRequestsAccessByRole() throws SecurityException {
        final List<RoleAuthority> roles = provider.getRoleAuthorities();
        Assert.assertEquals(roles.size(), roleRepository.findAll().size());
        for (final RoleAuthority role : roles) {
            switch (RoleAuthority.getRoleName(role.getAuthority())) {
                case AuthoritiesTestConfiguration.CORS_ROLE_NAME_GRANTED:
                    Assert.assertTrue(role.getCorsAccess());
                    break;
                case AuthoritiesTestConfiguration.CORS_ROLE_NAME_INVALID_1:
                case AuthoritiesTestConfiguration.CORS_ROLE_NAME_INVALID_2:
                    Assert.assertFalse(role.getCorsAccess());
                    break;
                default:
                    // Nothing to do
                    break;
            }
        }
    }

}
