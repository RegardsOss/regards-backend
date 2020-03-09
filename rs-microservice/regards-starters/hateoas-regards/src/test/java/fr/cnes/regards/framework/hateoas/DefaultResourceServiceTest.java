/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.hateoas;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.Resource;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Test HATEOAS link generation
 * @author msordi
 */
public class DefaultResourceServiceTest {

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
    private AccessDecisionManager accessDecisionManager;

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
        jwtAuth.setUser(new UserDetails(TENANT, "name", "name", ROLE));
        jwtAuth.setRole(ROLE);
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

        // Mock service
        accessDecisionManager = Mockito.mock(AccessDecisionManager.class);
        resourceServiceMock = new MockDefaultResourceService(accessDecisionManager);
    }

    /**
     * Test authorized link creation
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Test authorized link creation regarding security restriction.")
    public void testAuthorizedLinkCreation() {

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

        // Deny all acess
        Mockito.doThrow(new AccessDeniedException("Mock")).when(accessDecisionManager)
                .decide(Mockito.eq(jwtAuth), Mockito.any(), Mockito.eq(null));

        final PojoController pojoController = new PojoController(resourceServiceMock);
        final List<Resource<Pojo>> pojos = pojoController.getPojos();
        Assert.assertEquals(2, pojos.size());
        Assert.assertEquals(0, pojos.get(0).getLinks().size());
    }

    /**
     * Sample pojo
     * @author msordi
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
     * @author msordi
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
        public Resource<Pojo> toResource(Pojo element, Object... extras) {
            final Resource<Pojo> resource = resourceService.toResource(element);
            resourceService.addLink(resource, PojoController.class, GET_METHOD_NAME, LinkRels.SELF,
                                    MethodParamFactory.build(Long.class, element.getId()));
            resourceService.addLink(resource, PojoController.class, UPDATE_METHOD_NAME, LinkRels.UPDATE,
                                    MethodParamFactory.build(Pojo.class));
            return resource;
        }
    }
}
