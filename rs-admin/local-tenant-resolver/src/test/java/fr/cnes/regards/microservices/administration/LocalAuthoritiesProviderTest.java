/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
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
@MultitenantTransactional
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
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Authorities provider to test
     */
    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant("test-project");
    }

    /**
     * @throws JwtException
     *             if the token is wrong
     */
    @Before
    public void setUp() {
        resourcesAccessRepository.deleteAll();
        roleRepository.deleteAll();

        final List<String> addresses = new ArrayList<>();
        addresses.add("127.0.0.1");
        addresses.add("127.0.0.2");
        addresses.add("127.0.0.3");
        final RoleFactory roleFactory = new RoleFactory();

        roleFactory.withId(0L).withAuthorizedAddresses(addresses).withDefault(false).withNative(true);

        final Role publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString())
                .orElseGet(() -> roleRepository.save(roleFactory.createPublic()));

        roleFactory.withParentRole(publicRole);

        roleRepository.findOneByName(AuthoritiesTestConfiguration.ROLE_NAME)
                .ifPresent(role -> roleRepository.delete(role.getId()));
        roleRepository.save(roleFactory.withName(AuthoritiesTestConfiguration.ROLE_NAME).create());

        resourcesAccessRepository.save(new ResourcesAccess(0L, "description", microserviceName, "/resource",
                "Controller", RequestMethod.GET, DefaultRole.ADMIN));
        resourcesAccessRepository.save(new ResourcesAccess(0L, "description", microserviceName, "/resource",
                "Controller", RequestMethod.PUT, DefaultRole.ADMIN));
        resourcesAccessRepository.save(new ResourcesAccess(0L, "description", microserviceName, "/resource",
                "Controller", RequestMethod.POST, DefaultRole.ADMIN));
        resourcesAccessRepository.save(new ResourcesAccess(0L, "description", microserviceName, "/resource",
                "Controller", RequestMethod.DELETE, DefaultRole.ADMIN));
    }

    @Test
    public void init() throws JwtException {
        // TODO
    }
}
