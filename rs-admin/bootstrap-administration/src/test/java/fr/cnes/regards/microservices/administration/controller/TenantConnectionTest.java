/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration.controller;

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

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class TenantConnectionTest
 *
 * Test for tenant resolver from administration microservice
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(classes = { TenantConnectionConfiguration.class })
public class TenantConnectionTest {

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Resolver to test
     */
    @Autowired
    private ITenantConnectionResolver resolver;

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
     *
     * Initialize to add a project and a project connection associated.
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() {
        final Project project = new Project(0L, "desc", "icon", true, "test-1");
        projectRepo.save(project);

        final ProjectConnection conn = new ProjectConnection(0L, project, microserviceName, "user", "password",
                "driver", "url");
        projectConnRepo.save(conn);
    }

    /**
     *
     * Check for multitenant resolver from administration microservice.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Check for multitenant resolver from administration microservice.")
    @Test
    public void test() {
        final List<TenantConnection> tenants = resolver.getTenantConnections();
        Assert.assertTrue(!tenants.isEmpty());
    }

}
