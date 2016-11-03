/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Test HATEOAS link generation
 *
 * @author msordi
 *
 */
public class DefaultResourceServiceTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultResourceServiceTest.class);

    /**
     * Tenant
     */
    private static final String TENANT = "tenant";

    /**
     * Role
     */
    private static final String ROLE = "ROLE_USER";

    /**
     * Single method name
     */
    private static final String GET_METHOD_NAME = "getPojo";

    /**
     * Update method name
     */
    private static final String UPDATE_METHOD_NAME = "updatePojo";

    /**
     * Mocket authorization service
     */
    private MethodAuthorizationService authServiceMock;

    /**
     * Mocked resource service
     */
    private IResourceService resourceServiceMock;

    /**
     * JWT authentication
     */
    private JWTAuthentication jwtAuth;

    /**
     * Init test
     */
    @Before
    public void init() {
        // Mock authentication
        jwtAuth = new JWTAuthentication("foo");
        final UserDetails details = new UserDetails();
        details.setTenant(TENANT);
        details.setName("name");
        jwtAuth.setUser(details);
        jwtAuth.setRole(ROLE);
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

        // Mock service
        authServiceMock = Mockito.mock(MethodAuthorizationService.class);
        resourceServiceMock = new MockDefaultResourceService(authServiceMock);
    }

    /**
     * Test authorized link creation
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Test authorized link creation regarding security restriction.")
    public void testAuthorizedLinkCreation() {

        setMock(GET_METHOD_NAME, Long.class, Boolean.TRUE);
        setMock(UPDATE_METHOD_NAME, Pojo.class, Boolean.TRUE);

        final PojoController pojoController = new PojoController(resourceServiceMock);
        final List<Resource<Pojo>> pojos = pojoController.getPojos();
        Assert.assertEquals(2, pojos.size());
        Assert.assertEquals(2, pojos.get(0).getLinks().size());
    }

    /**
     * Test not authorized link creation
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Test not authorized link creation regarding security restriction.")
    public void testNotAuthorizedLinkCreation() {

        setMock(GET_METHOD_NAME, Long.class, Boolean.FALSE);
        setMock(UPDATE_METHOD_NAME, Pojo.class, Boolean.FALSE);

        final PojoController pojoController = new PojoController(resourceServiceMock);
        final List<Resource<Pojo>> pojos = pojoController.getPojos();
        Assert.assertEquals(2, pojos.size());
        Assert.assertEquals(0, pojos.get(0).getLinks().size());
    }

    private <T> void setMock(String pMethodName, Class<T> pParameterType, Boolean pIsAuthorized) {
        final Method single;
        try {
            single = PojoController.class.getMethod(pMethodName, pParameterType);
            Mockito.when(authServiceMock.hasAccess(jwtAuth, single)).thenReturn(pIsAuthorized);
        } catch (NoSuchMethodException | SecurityException e) {
            LOG.error("Cannot retrieve method", e);
            Assert.fail();
        }
    }

    /**
     * Sample pojo
     *
     * @author msordi
     *
     */
    static class Pojo {

        /**
         * Pojo identifier
         */
        private Long id;

        /**
         * Sample content
         */
        private String content;

        public Pojo(Long pId, String pContent) {
            this.id = pId;
            this.content = pContent;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String pContent) {
            content = pContent;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long pId) {
            id = pId;
        }
    }

    /**
     * Pojo controller
     *
     * @author msordi
     *
     */
    @RestController
    @RequestMapping("/pojos")
    static class PojoController implements IResourceController<Pojo> {

        /**
         * Resource service
         */
        private final IResourceService resourceService;

        public PojoController(IResourceService pResourceService) {
            this.resourceService = pResourceService;
        }

        @ResourceAccess(description = "Get all pojos")
        @RequestMapping(method = RequestMethod.GET)
        public List<Resource<Pojo>> getPojos() {
            final List<Pojo> pojos = new ArrayList<>();
            pojos.add(new Pojo(1L, "first"));
            pojos.add(new Pojo(2L, "second"));
            return toResources(pojos);
        }

        @ResourceAccess(description = "Get single pojo")
        @RequestMapping(method = RequestMethod.GET, value = "/{pPojoId}")
        public Resource<Pojo> getPojo(Long pPojoId) {
            final Pojo pojo = new Pojo(3L, "third");
            return toResource(pojo);
        }

        @ResourceAccess(description = "Update pojo")
        @RequestMapping(method = RequestMethod.PUT)
        public Resource<Pojo> updatePojo(@Valid @RequestBody Pojo pPojo) {
            final Pojo pojo = new Pojo(4L, "fourth");
            return toResource(pojo);
        }

        @Override
        public Resource<Pojo> toResource(Pojo pElement) {
            final Resource<Pojo> resource = resourceService.toResource(pElement);
            resourceService.addLink(resource, PojoController.class, GET_METHOD_NAME, LinkRels.SELF,
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, PojoController.class, UPDATE_METHOD_NAME, LinkRels.UPDATE,
                                    MethodParamFactory.build(Pojo.class));
            return resource;
        }
    }
}
