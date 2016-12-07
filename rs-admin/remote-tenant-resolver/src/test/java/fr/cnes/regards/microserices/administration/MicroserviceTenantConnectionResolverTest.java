package fr.cnes.regards.microserices.administration;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.microserices.administration.stubs.ProjectClientStub;
import fr.cnes.regards.microserices.administration.stubs.ProjectConnectionClientStub;
import fr.cnes.regards.microservices.administration.MicroserviceTenantConnectionResolver;
import fr.cnes.regards.microservices.administration.RemoteTenantResolver;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 *
 * Class MicroserviceMultitenantResolverTest
 *
 * MultitenantResolver test
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class MicroserviceTenantConnectionResolverTest {

    /**
     * Tenant resolver to test
     */
    private ITenantConnectionResolver resovler;

    @Before
    public void init() {
        final JWTService jwtService = new JWTService();
        final IProjectsClient projectsClient = new ProjectClientStub();
        jwtService.setSecret("123456789");
        resovler = new MicroserviceTenantConnectionResolver("test-microservice", jwtService, projectsClient,
                new ProjectConnectionClientStub(),
                new RemoteTenantResolver(projectsClient, jwtService, "test-microservice"));
    }

    /**
     *
     * Check for multitenant resolver throught administration microservice client
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Check for multitenant resolver throught administration microservice client")
    @Test
    public void test() {

        final List<TenantConnection> results = resovler.getTenantConnections();
        Assert.assertTrue("Error geting tenants", results.size() == 1);
        Assert.assertTrue("", results.get(0).getName().equals(ProjectClientStub.PROJECT_NAME));

    }

}
