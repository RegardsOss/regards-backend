/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.test;

import java.lang.reflect.Method;
import java.net.URI;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.core.AnnotationMappingDiscoverer;
import org.springframework.hateoas.core.MappingDiscoverer;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriTemplate;

import fr.cnes.regards.framework.hateoas.DefaultResourceService;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;

/**
 *
 * Mock default resource service to avoid bad servlet context issue *
 *
 * @author msordi
 *
 */
public class MockDefaultResourceService extends DefaultResourceService {

    /**
     * Mapping discoverer
     */
    private static final MappingDiscoverer DISCOVERER = new AnnotationMappingDiscoverer(RequestMapping.class);

    public MockDefaultResourceService(final MethodAuthorizationService pMethodAuthorizationService) {
        super(pMethodAuthorizationService);
    }

    @Override
    protected Link buildLink(final Method pMethod, final String pRel, final Object... pParameterValues) {

        Assert.notNull(pMethod, "Method must not be null!");

        final UriTemplate template = new UriTemplate(DISCOVERER.getMapping(pMethod.getDeclaringClass(), pMethod));
        final URI uri = template.expand(pParameterValues);

        return new Link(uri.toString(), pRel);
    }
}
