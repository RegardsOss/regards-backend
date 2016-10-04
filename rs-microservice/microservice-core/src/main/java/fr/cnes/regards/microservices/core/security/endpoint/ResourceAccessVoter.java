/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.endpoint;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.core.MappingDiscoverer;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.microservices.core.security.configuration.MethodSecurityConfiguration;
import fr.cnes.regards.security.utils.endpoint.annotation.ResourceAccess;

/**
 * REGARDS endpoint security voter to manage resource access dynamically at method level.
 *
 * @See {@link MethodSecurityConfiguration}
 * @author msordi
 *
 */
public class ResourceAccessVoter implements AccessDecisionVoter<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAccessVoter.class);

    private final MethodAuthorizationService methodAuthService_;

    public ResourceAccessVoter(MethodAuthorizationService pMethodAuthService) {
        this.methodAuthService_ = pMethodAuthService;
    }

    @Override
    public boolean supports(ConfigAttribute pAttribute) {
        return true;
    }

    /**
     * This implementation supports any type of class, because it does not query the presented secure object.
     *
     * @param pClazz
     *            the secure object
     *
     * @return always <code>true</code>
     */
    @Override
    public boolean supports(Class<?> pClazz) {
        return true;
    }

    @Override
    public int vote(Authentication pAuthentication, Object pObject, Collection<ConfigAttribute> pAttributes) {

        // If authentication do not contains authority, deny access
        if ((pAuthentication.getAuthorities() == null) || pAuthentication.getAuthorities().isEmpty()) {
            return ACCESS_DENIED;
        }

        if (!(pObject instanceof MethodInvocation)) {
            return ACCESS_DENIED;
        }

        MethodInvocation mi = (MethodInvocation) pObject;

        // Retrieve resource mapping configuration
        ResourceMapping mapping;
        try {
            mapping = buildResourceMapping(mi.getMethod());
        }
        catch (ResourceMappingException e) {
            // If error occurs, deny access
            return ACCESS_DENIED;
        }

        // Retrieve granted authorities
        Optional<List<GrantedAuthority>> options = methodAuthService_.getAuthorities(mapping);
        if (!options.isPresent()) {
            return ACCESS_DENIED;
        }

        return checkAuthorities(options.get(), pAuthentication.getAuthorities());
    }

    /**
     * Introspect code to retrieve resource mapping configuration.<br/>
     * Normalize annotations {@link RequestMapping}, {@link GetMapping}, {@link PostMapping}, {@link PutMapping},
     * {@link DeleteMapping} and {@link PatchMapping}.
     *
     * @param pMethod
     *            the called method
     * @return {@link ResourceMapping}
     * @throws ResourceMappingException
     */
    public static ResourceMapping buildResourceMapping(Method pMethod) throws ResourceMappingException {
        // Retrieve resource access annotation
        ResourceAccess access = AnnotationUtils.findAnnotation(pMethod, ResourceAccess.class);
        if (access == null) {
            // Throw exception if resource is not annotated with resource access
            LOG.error("Missing annotation \"{}\" on method {}", ResourceAccess.class.getName(), pMethod.getName());
            throw new ResourceMappingException("No resource access annotation");
        }

        // Retrieve resource access annotation
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(pMethod, RequestMapping.class);
        if ((requestMapping.value() != null) && (requestMapping.path() != null)
                && !requestMapping.value()[0].equals(requestMapping.path()[0])) {
            throw new AnnotationConfigurationException("Provided value and mapping are different!");
        }

        // Retrieve the assembled path (at class and method level) from RequestMapping annotations
        // (and inherited ones : GetMapping, PutMapping...)
        MappingDiscoverer discoverer = new AnnotatedElementMappingDiscoverer(RequestMapping.class);
        String path = discoverer.getMapping(pMethod);

        return new ResourceMapping(access, Optional.of(path),
                getSingleMethod(requestMapping.method(), pMethod.getName()));

    }

    /**
     * Retrieve single HTTP method
     *
     * @param pMethods
     *            HTTP methods
     * @param pMethodName
     *            method name
     * @return HTTP method
     * @throws ResourceMappingException
     */
    private static RequestMethod getSingleMethod(RequestMethod[] pMethods, String pMethodName)
            throws ResourceMappingException {
        if (pMethods.length == 1) {
            return pMethods[0];
        }
        else
            if (pMethods.length == 0) {
                String errorMessage = MessageFormat
                        .format("A single method is required in request mapping for method {0}", pMethodName);
                LOG.error(errorMessage);
                throw new ResourceMappingException(errorMessage);
            }
            else {
                String errorMessage = MessageFormat
                        .format("Only single method is accepted in request mapping for method {0}", pMethodName);
                LOG.error(errorMessage);
                throw new ResourceMappingException(errorMessage);
            }
    }

    /**
     *
     * Check if the user authorities contains on of the method authorities
     *
     * @param pMethodAutorities
     *            list of granted authorities
     * @param pUserAutorities
     *            user authorities represented by a single role
     * @return granted or denied access (default)
     */
    private int checkAuthorities(List<GrantedAuthority> pMethodAutorities,
            Collection<? extends GrantedAuthority> pUserAutorities) {

        for (GrantedAuthority userAuthority : pUserAutorities) {
            for (GrantedAuthority resourceAuthority : pMethodAutorities) {
                if (userAuthority.getAuthority().equals(resourceAuthority.getAuthority())) {
                    return ACCESS_GRANTED;
                }
            }
        }
        return ACCESS_DENIED;
    }
}
