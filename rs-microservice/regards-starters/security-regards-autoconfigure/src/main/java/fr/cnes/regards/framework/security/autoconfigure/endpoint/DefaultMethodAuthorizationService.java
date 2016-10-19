/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure.endpoint;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.endpoint.annotation.ResourceAccess;

/**
 * Service MethodAutorizationServiceImpl<br/>
 * Allow to set/get the REST resource method access authorizations.<br/>
 * An authorization is defined by a endpoint, a HTTP Verb and a list of authorized user ROLES
 *
 * @author CS SI
 *
 */
public class DefaultMethodAuthorizationService implements IMethodAuthorizationService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMethodAuthorizationService.class);

    /**
     * List of configurated authorities
     */
    @Value("${regards.security.authorities:#{null}}")
    private String[] authorities;

    /**
     * Plugin resource manager. To handle plugins endpoints specific resources.
     */
    private final IPluginResourceManager pluginResourceManager;

    /**
     * Authorities cache that provide granted authorities per resource
     */
    private final Map<String, ArrayList<GrantedAuthority>> grantedAuthoritiesByResource;

    public DefaultMethodAuthorizationService(final IPluginResourceManager pPluginResourceManager) {
        grantedAuthoritiesByResource = new HashMap<>();
        pluginResourceManager = pPluginResourceManager;
    }

    /**
     * After bean contruction, read configurated authorities
     */
    @PostConstruct
    public void init() {
        if (authorities != null) {
            LOG.debug("Initializing granted authorities from property file");
            for (final String auth : authorities) {
                final String[] urlVerbRoles = auth.split("\\|");
                if (urlVerbRoles.length > 1) {
                    final String[] urlVerb = urlVerbRoles[0].split("@");
                    if (urlVerb.length == 2) {
                        // Url path
                        final String url = urlVerb[0];
                        // HTTP method
                        final String verb = urlVerb[1];

                        try {
                            final RequestMethod httpVerb = RequestMethod.valueOf(verb);

                            // Roles
                            final String[] roles = new String[urlVerbRoles.length - 1];
                            for (int i = 1; i < urlVerbRoles.length; i++) {
                                roles[i - 1] = urlVerbRoles[i];
                            }

                            setAuthorities(url, httpVerb, roles);
                        } catch (final IllegalArgumentException pIAE) {
                            LOG.error("Cannot retrieve HTTP method from {}", verb);
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<ResourceMapping> getResources() {

        final List<ResourceMapping> resources = new ArrayList<>();
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false);

        // Check for all RestController classes
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        for (final BeanDefinition def : scanner.findCandidateComponents("fr.cnes.regards")) {
            try {
                final Class<?> classe = Class.forName(def.getBeanClassName());
                // For each method get the method annotated with both @ResourceAccess and @RequestMapping
                for (final Method method : classe.getMethods()) {
                    resources.addAll(manageMethodResource(classe, method));
                }
            } catch (final ClassNotFoundException e) {
                LOG.error(String.format("Error getting resources from RestController classe : %s",
                                        def.getBeanClassName()));
            }
        }
        return resources;

    }

    /**
     *
     * Create the resources associated to a Rest controller endpoint
     *
     * @param pClass
     *            class of the rest controller
     * @param pMethod
     *            method of the endpoint
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    private List<ResourceMapping> manageMethodResource(final Class<?> pClass, final Method pMethod) {

        final List<ResourceMapping> mappings = new ArrayList<>();
        // Get inital resource path if the class is annotated with @RequestMapping
        String path = "";
        final RequestMapping classRequestMapping = AnnotationUtils.findAnnotation(pClass, RequestMapping.class);
        if (classRequestMapping != null) {
            path = classRequestMapping.value()[0];
        }

        final ResourceAccess resourceAccess = AnnotationUtils.findAnnotation(pMethod, ResourceAccess.class);
        final RequestMapping requestMapping = AnnotationUtils.findAnnotation(pMethod, RequestMapping.class);

        if ((resourceAccess != null) && (requestMapping != null)) {
            if ((!resourceAccess.plugin().equals(void.class)) && (pluginResourceManager != null)) {
                // Manage specific plugin endpoint
                mappings.addAll(pluginResourceManager.manageMethodResource(path, resourceAccess, requestMapping));
            } else {
                // Manage standard endpoint
                final RequestMethod reqMethod = requestMapping.method()[0];
                path += requestMapping.value()[0];
                mappings.add(new ResourceMapping(resourceAccess, Optional.ofNullable(path), reqMethod));
            }
        }
        return mappings;
    }

    /**
     * Add a resource authorization
     */
    @Override
    public void setAuthorities(final ResourceMapping pResourceMapping, final GrantedAuthority... pAuthorities) {
        if ((pResourceMapping != null) && (pAuthorities != null)) {
            final String resourceId = pResourceMapping.getResourceMappingId();
            final ArrayList<GrantedAuthority> newAuthorities;
            if (grantedAuthoritiesByResource.containsKey(resourceId)) {
                final Set<GrantedAuthority> set = new LinkedHashSet<>(grantedAuthoritiesByResource.get(resourceId));
                for (final GrantedAuthority grant : pAuthorities) {
                    set.add(grant);
                }
                newAuthorities = new ArrayList<>(set);
            } else {
                newAuthorities = new ArrayList<>();
                newAuthorities.addAll(Arrays.asList(pAuthorities));
            }
            grantedAuthoritiesByResource.put(resourceId, newAuthorities);
        }
    }

    @Override
    public Optional<List<GrantedAuthority>> getAuthorities(final ResourceMapping pResourceMapping) {
        return Optional.ofNullable(grantedAuthoritiesByResource.get(pResourceMapping.getResourceMappingId()));
    }

    @Override
    public void setAuthorities(final String pUrlPath, final RequestMethod pMethod, final String... pRoleNames) {
        // Validate
        Assert.notNull(pUrlPath, "Path to resource cannot be null.");
        Assert.notNull(pMethod, "HTTP method cannot be null.");
        Assert.notNull(pRoleNames, "At least one role is required.");

        // Build granted authorities
        final List<GrantedAuthority> newAuthorities = new ArrayList<>();
        for (final String role : pRoleNames) {
            newAuthorities.add(new RoleAuthority(role));
        }

        setAuthorities(new ResourceMapping(Optional.of(pUrlPath), pMethod),
                       newAuthorities.toArray(new GrantedAuthority[0]));
    }
}
