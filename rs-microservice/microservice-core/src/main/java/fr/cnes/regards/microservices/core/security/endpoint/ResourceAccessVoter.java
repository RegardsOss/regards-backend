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
import org.springframework.core.annotation.AnnotationUtils;
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
        } catch (ResourceMappingException e) {
            // If error occurs, deny access
            LOG.error(e.getMessage(), e);
            return ACCESS_DENIED;
        }

        // Retrieve granted authorities
        Optional<List<GrantedAuthority>> options = methodAuthService_.getAuthorities(mapping);
        if (!options.isPresent()) {
            LOG.error("Access denied to resource " + mi.getMethod().toGenericString() + " for user role");
            return ACCESS_DENIED;
        }

        int result = checkAuthorities(options.get(), pAuthentication.getAuthorities());
        if (result == ACCESS_DENIED) {
            LOG.error("Access denied to resource " + mi.getMethod().toGenericString() + " for user role");
        }
        return result;
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
    private ResourceMapping buildResourceMapping(Method pMethod) throws ResourceMappingException {
        ResourceMapping mapping = null;

        // Retrieve resource access annotation
        ResourceAccess access = AnnotationUtils.findAnnotation(pMethod, ResourceAccess.class);
        if (access == null) {
            // Throw exception if resource is not annotated with resource access
            LOG.error("Missing annotation \"{}\" on method {}", ResourceAccess.class.getName(), pMethod.getName());
            throw new ResourceMappingException("No resource access annotation");
        }

        // Retrieve base path at class level
        Optional<String> classPath = null;
        String className = pMethod.getDeclaringClass().getName();
        RequestMapping classMapping = AnnotationUtils.findAnnotation(pMethod.getDeclaringClass(), RequestMapping.class);
        if (classMapping != null) {
            classPath = getSingleMethodPath(classMapping.path(), classMapping.value(), pMethod.getName(), className);
        }

        // Retrieve resource path and HTTP methods at method level

        // - Manage GET mapping
        GetMapping get = AnnotationUtils.findAnnotation(pMethod, GetMapping.class);
        if (get != null) {
            return new ResourceMapping(access, classPath,
                    getSingleMethodPath(get.path(), get.value(), pMethod.getName(), className), RequestMethod.GET);
        }

        // - Manage REQUEST mapping
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(pMethod, RequestMapping.class);
        if (requestMapping != null) {
            return new ResourceMapping(access, classPath,
                    getSingleMethodPath(requestMapping.path(), requestMapping.value(), pMethod.getName(), className),
                    getSingleMethod(requestMapping.method(), pMethod.getName()));
        }

        // - Manage POST mapping
        PostMapping post = AnnotationUtils.findAnnotation(pMethod, PostMapping.class);
        if (post != null) {
            return new ResourceMapping(access, classPath,
                    getSingleMethodPath(post.path(), post.value(), pMethod.getName(), className), RequestMethod.POST);
        }

        // - Manage PUT mapping
        PutMapping put = AnnotationUtils.findAnnotation(pMethod, PutMapping.class);
        if (put != null) {
            return new ResourceMapping(access, classPath,
                    getSingleMethodPath(put.path(), put.value(), pMethod.getName(), className), RequestMethod.PUT);
        }

        // - Manage DELETE mapping
        DeleteMapping delete = AnnotationUtils.findAnnotation(pMethod, DeleteMapping.class);
        if (delete != null) {
            return new ResourceMapping(access, classPath,
                    getSingleMethodPath(delete.path(), delete.value(), pMethod.getName(), className),
                    RequestMethod.DELETE);
        }

        // - Manage PATCH mapping
        PatchMapping patch = AnnotationUtils.findAnnotation(pMethod, PatchMapping.class);
        if (patch != null) {
            return new ResourceMapping(access, classPath,
                    getSingleMethodPath(patch.path(), patch.value(), pMethod.getName(), className),
                    RequestMethod.PATCH);
        }

        return mapping;
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
    private RequestMethod getSingleMethod(RequestMethod[] pMethods, String pMethodName)
            throws ResourceMappingException {
        if (pMethods.length == 1) {
            return pMethods[0];
        } else
            if (pMethods.length == 0) {
                String errorMessage = MessageFormat
                        .format("A single method is required in request mapping for method {0}", pMethodName);
                LOG.error(errorMessage);
                throw new ResourceMappingException(errorMessage);
            } else {
                String errorMessage = MessageFormat
                        .format("Only single method is accepted in request mapping for method {0}", pMethodName);
                LOG.error(errorMessage);
                throw new ResourceMappingException(errorMessage);
            }
    }

    /**
     * Retrieve single method path
     *
     * @param pPaths
     *            Array of path (alias for values)
     * @param pValues
     *            Array of path (alias for path)
     * @param pMethodName
     *            method name
     * @param pClassName
     *            class name
     * @return single path
     * @throws ResourceMappingException
     *             if multiple paths set
     */
    private Optional<String> getSingleMethodPath(String[] pPaths, String[] pValues, String pMethodName,
            String pClassName) throws ResourceMappingException {

        Optional<String> pathFromPaths = getSingleMethodPath(pPaths, pMethodName, pClassName);
        Optional<String> pathFromValues = getSingleMethodPath(pValues, pMethodName, pClassName);
        if (pathFromPaths.isPresent() && pathFromValues.isPresent()
                && !pathFromPaths.get().equals(pathFromValues.get())) {
            // Only path or value must be set
            String errorMessage = MessageFormat.format("Conflict between path and value attributes in method ",
                                                       pMethodName);
            LOG.error(errorMessage);
            throw new ResourceMappingException(errorMessage);
        }

        if (pathFromValues.isPresent()) {
            return pathFromValues;
        } else {
            return pathFromPaths;
        }

    }

    /**
     * Retrieve single method path
     *
     * @param pPaths
     *            Array of path
     * @param pMethodName
     *            method name
     * @param pClassName
     *            class name
     * @return single path
     * @throws ResourceMappingException
     *             if multiple paths set
     */
    private Optional<String> getSingleMethodPath(String[] pPaths, String pMethodName, String pClassName)
            throws ResourceMappingException {
        Optional<String> path = Optional.empty();

        if (pPaths.length == 1) {
            path = Optional.of(pPaths[0]);
        } else
            if (pPaths.length == 0) {
                // Nothing to do
            } else {
                // Only single path is accepted
                String errorMessage = MessageFormat.format(
                                                           "Only single path is accepted in request mapping for method {0} or class {1}",
                                                           pMethodName, pClassName);
                LOG.error(errorMessage);
                throw new ResourceMappingException(errorMessage);
            }

        return path;
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
