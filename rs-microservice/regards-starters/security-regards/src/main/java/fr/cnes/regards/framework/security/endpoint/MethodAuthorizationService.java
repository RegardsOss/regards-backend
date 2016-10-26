/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * Service MethodAutorizationServiceImpl<br/>
 * Allow to set/get the REST resource method access authorizations.<br/>
 * An authorization is defined by a endpoint, a HTTP Verb and a list of authorized user ROLES
 *
 * @author CS SI
 *
 */
public class MethodAuthorizationService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MethodAuthorizationService.class);

    /**
     * Plugin resource manager. To handle plugins endpoints specific resources.
     */
    @Autowired
    private IPluginResourceManager pluginResourceManager;

    /**
     * Provider for authorities
     */
    @Autowired
    private IAuthoritiesProvider authoritiesProvider;

    /**
     * Tenant resolver
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * JWT Security service
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Authorities cache that provide granted authorities per tenant and per resource.<br/>
     * Map<Tenant, Map<Resource, List<GrantedAuthority>>>
     */
    private final Map<String, Map<String, ArrayList<GrantedAuthority>>> grantedAuthoritiesByTenant;

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    public MethodAuthorizationService() {
        grantedAuthoritiesByTenant = new HashMap<>();
    }

    /**
     * After bean contruction
     */
    @PostConstruct
    public void init() {
        refreshAuthorities();
    }

    /**
     *
     * Refresh all resources access authortities for all tenants.
     *
     * @since 1.0-SNAPSHOT
     */
    public void refreshAuthorities() {
        grantedAuthoritiesByTenant.clear();
        for (final String tenant : tenantResolver.getAllTenants()) {
            try {
                jwtService.injectToken(tenant, RootResourceAccessVoter.ROOT_ADMIN_AUHTORITY);
                final List<ResourceMapping> resources = authoritiesProvider.getResourcesAccessConfiguration();
                for (final ResourceMapping resource : resources) {
                    setAuthorities(tenant, resource);
                }
            } catch (final JwtException e) {
                LOG.error(String.format("Error during resources access initialization for tenant %s", tenant));
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     *
     * Retrieve the resources of the current microservice
     *
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
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
                                        def.getBeanClassName()),
                          e);
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
        final RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(pMethod, RequestMapping.class);

        if ((resourceAccess != null) && (requestMapping != null)) {
            if ((!resourceAccess.plugin().equals(void.class)) && (pluginResourceManager != null)) {
                // Manage specific plugin endpoint
                mappings.addAll(pluginResourceManager.manageMethodResource(path, resourceAccess, requestMapping));
            } else {
                // Manage standard endpoint
                RequestMethod reqMethod = RequestMethod.GET;
                if (requestMapping.method().length > 0) {
                    reqMethod = requestMapping.method()[0];
                }
                if (requestMapping.value().length > 0) {
                    path += requestMapping.value()[0];
                } else
                    if (requestMapping.path().length > 0) {
                        path += requestMapping.path()[0];
                    }
                mappings.add(new ResourceMapping(resourceAccess, path, reqMethod));
            }
        }
        return mappings;
    }

    /**
     *
     * Add resources authorization
     *
     * @param pTenant
     *            tenant name
     * @param pResourceMapping
     *            resource to add
     * @since 1.0-SNAPSHOT
     */
    public void setAuthorities(final String pTenant, final ResourceMapping pResourceMapping) {
        Map<String, ArrayList<GrantedAuthority>> grantedAuthoritiesByResource = grantedAuthoritiesByTenant.get(pTenant);
        if (grantedAuthoritiesByResource == null) {
            grantedAuthoritiesByResource = new HashMap<>();
            grantedAuthoritiesByTenant.put(pTenant, grantedAuthoritiesByResource);
        }
        if ((pResourceMapping != null) && (pResourceMapping.getAutorizedRoles() != null)) {
            final String resourceId = pResourceMapping.getResourceMappingId();
            final ArrayList<GrantedAuthority> newAuthorities;
            if (grantedAuthoritiesByResource.containsKey(resourceId)) {
                final Set<GrantedAuthority> set = new LinkedHashSet<>(grantedAuthoritiesByResource.get(resourceId));
                for (final GrantedAuthority grant : pResourceMapping.getAutorizedRoles()) {
                    set.add(grant);
                }
                newAuthorities = new ArrayList<>(set);
            } else {
                newAuthorities = new ArrayList<>();
                newAuthorities.addAll(pResourceMapping.getAutorizedRoles());
            }
            grantedAuthoritiesByResource.put(resourceId, newAuthorities);
        }
    }

    /**
     *
     * Add resources authorization
     *
     * @param pTenant
     *            tenant name
     * @param pUrlPath
     *            resource path
     * @param pMethod
     *            resource Method
     * @param pRoleNames
     *            resource role names
     * @since 1.0-SNAPSHOT
     */
    public void setAuthorities(final String pTenant, final String pUrlPath, final RequestMethod pMethod,
            final String... pRoleNames) {
        // Validate
        Assert.notNull(pUrlPath, "Path to resource cannot be null.");
        Assert.notNull(pMethod, "HTTP method cannot be null.");
        Assert.notNull(pRoleNames, "At least one role is required.");

        // Build granted authorities
        final List<RoleAuthority> newAuthorities = new ArrayList<>();
        for (final String role : pRoleNames) {
            newAuthorities.add(new RoleAuthority(role));
        }

        final ResourceMapping resource = new ResourceMapping(pUrlPath, pMethod);
        resource.setAutorizedRoles(newAuthorities);

        setAuthorities(pTenant, resource);
    }

    /**
     *
     * Get authorities for the given resource and the current tenant.
     *
     * @param pTenant
     *            tenant name
     * @param pResourceMapping
     *            resource to retrieve
     * @return List<GrantedAuthority>>
     * @since 1.0-SNAPSHOT
     */
    public Optional<List<GrantedAuthority>> getAuthorities(final String pTenant,
            final ResourceMapping pResourceMapping) {
        List<GrantedAuthority> result = null;
        final Map<String, ArrayList<GrantedAuthority>> grantedAuthoritiesByResource = grantedAuthoritiesByTenant
                .get(pTenant);
        if (grantedAuthoritiesByResource != null) {
            result = grantedAuthoritiesByResource.get(pResourceMapping.getResourceMappingId());
        }
        return Optional.ofNullable(result);
    }

    /**
     *
     * Retrieve all authority resources for the given tenant
     *
     * @param pTenant
     *            tenant name
     * @return Map<String, ArrayList<GrantedAuthority>>
     * @since 1.0-SNAPSHOT
     */
    public Map<String, ArrayList<GrantedAuthority>> getTenantAuthorities(final String pTenant) {
        return grantedAuthoritiesByTenant.get(pTenant);
    }
}
