/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.ResourceMappingException;

/**
 * Authorization utilities
 *
 * @author msordi
 *
 */
public final class MethodAuthorizationUtils {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MethodAuthorizationUtils.class);

    /**
     * Pattern to detect multiple slash
     */
    private static final Pattern MULTIPLE_SLASHES = Pattern.compile("\\/{2,}");

    /**
     * Single slash for building URL
     */
    private static final String SINGLE_SLASH = "/";

    private MethodAuthorizationUtils() {
    }

    /**
     * Introspect code to retrieve resource mapping configuration.<br/>
     * Following annotations are supported : {@link RequestMapping}, {@link GetMapping}, {@link PostMapping},
     * {@link PutMapping}, {@link DeleteMapping} and {@link PatchMapping}.<br/>
     *
     * @param pMethod
     *            the called method
     * @return {@link ResourceMapping}
     * @throws ResourceMappingException
     *             if resource mapping cannot be built
     */
    public static ResourceMapping buildResourceMapping(final Method pMethod) throws ResourceMappingException {
        // Retrieve resource access annotation
        final ResourceAccess access = AnnotationUtils.findAnnotation(pMethod, ResourceAccess.class);
        if (access == null) {
            // Throw exception if resource is not annotated with resource access
            LOG.error("Missing annotation \"{}\" on method {}", ResourceAccess.class.getName(), pMethod.getName());
            throw new ResourceMappingException("No resource access annotation");
        }

        // Retrieve request mapping annotation
        // - from class
        final String classMapping = getMapping(pMethod.getDeclaringClass(), pMethod.getDeclaringClass().getName());
        // - from method
        final String methodMapping = getMapping(pMethod, getMethodFullPath(pMethod));

        // Validate mapping
        if ((classMapping == null) && (methodMapping == null)) {
            // Throw exception if all path are null
            final String pattern = "At least one path (on class or method) must be set for method {0}.";
            final String message = MessageFormat.format(pattern, getMethodFullPath(pMethod));
            LOG.error(message);
            throw new ResourceMappingException(message);
        }

        // Join path mapping
        final String path = join(classMapping, methodMapping);

        return new ResourceMapping(access, path, getSingleMethod(pMethod));
    }

    /**
     * Retrieve single mapping path of the annotated element
     *
     * @param pElement
     *            {@link RequestMapping} annotated element
     * @param pElementName
     *            element name (for logging)
     * @return single path mapping
     * @throws ResourceMappingException
     *             if mapping contains more than one path
     */
    private static String getMapping(final AnnotatedElement pElement, final String pElementName)
            throws ResourceMappingException {
        String mapping = null;
        final RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(pElement,
                                                                                         RequestMapping.class);
        if (requestMapping != null) {
            final String[] paths = requestMapping.value();
            if (paths != null) {
                if (paths.length == 1) {
                    mapping = paths[0];// Nothing to do
                } else
                    if (paths.length == 0) {
                        // Nothing to do
                        LOG.debug("No path definition for {}", pElementName);
                    } else {
                        // Throw exception if resource maps to more than one path
                        final String message = MessageFormat
                                .format("Only single path is authorized in annotated element {0}.", pElementName);
                        LOG.error(message);
                        throw new ResourceMappingException(message);
                    }
            }
        }
        return mapping;
    }

    /**
     * Retrieve single HTTP method
     *
     * @param pMethod
     *            method
     * @return HTTP method
     * @throws ResourceMappingException
     *             if no single method detected
     */
    private static RequestMethod getSingleMethod(final Method pMethod) throws ResourceMappingException {

        final RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(pMethod, RequestMapping.class);

        if (requestMapping == null) {
            // Throw exception if request mapping not found
            final String message = MessageFormat.format("Request mapping is required for method {0}.",
                                                        getMethodFullPath(pMethod));
            LOG.error(message);
            throw new ResourceMappingException(message);
        }

        final RequestMethod[] methods = requestMapping.method();
        if (methods.length == 1) {
            return methods[0];
        } else
            if (methods.length == 0) {
                final String errorMessage = MessageFormat.format("One HTTP method is required for method {0}",
                                                                 getMethodFullPath(pMethod));
                LOG.error(errorMessage);
                throw new ResourceMappingException(errorMessage);
            } else {
                final String errorMessage = MessageFormat.format("Only one HTTP method is required for method {0}",
                                                                 getMethodFullPath(pMethod));
                LOG.error(errorMessage);
                throw new ResourceMappingException(errorMessage);
            }
    }

    /**
     *
     * @param pMethod
     *            method
     * @return full method path
     */
    private static String getMethodFullPath(final Method pMethod) {
        return pMethod.getDeclaringClass().getName() + "#" + pMethod.getName();
    }

    /**
     * Join class and method mapping to retrieve full path
     *
     * @param pClassMapping
     *            class mapping
     * @param pMethodMapping
     *            method mapping
     * @return full path to method
     */
    private static String join(final String pClassMapping, final String pMethodMapping) {
        final StringBuffer fullPath = new StringBuffer();
        if (pClassMapping != null) {
            fullPath.append(pClassMapping);
        }
        if (pMethodMapping != null) {
            fullPath.append(SINGLE_SLASH);
            fullPath.append(pMethodMapping);
        }
        return MULTIPLE_SLASHES.matcher(fullPath.toString()).replaceAll(SINGLE_SLASH);
    }

    // CHECKSTYLE:OFF
    public static Boolean hasAccess(final List<GrantedAuthority> pMethodAutorities,
            final Collection<? extends GrantedAuthority> pUserAutorities) {
        for (final GrantedAuthority userAuthority : pUserAutorities) {
            for (final GrantedAuthority resourceAuthority : pMethodAutorities) {
                if (userAuthority.getAuthority().equals(resourceAuthority.getAuthority())) {
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }
    // CHECKSTYLE:ON

}
