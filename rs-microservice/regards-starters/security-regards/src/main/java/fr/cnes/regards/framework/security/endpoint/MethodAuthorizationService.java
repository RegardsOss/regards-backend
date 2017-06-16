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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.ResourceMappingException;
import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * This service allows to set/get the REST resource method access authorizations.<br/>
 * An authorization is defined by a endpoint, a HTTP Verb and a list of authorized user ROLES
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 *
 */
public class MethodAuthorizationService implements ApplicationContextAware, ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodAuthorizationService.class);

    /**
     * Plugin resource manager. To handle plugins endpoints specific resources.
     */
    private IPluginResourceManager pluginResourceManager;

    /**
     * Tenant resolver
     */
    private ITenantResolver tenantResolver;

    /**
     * Role and resource access manager
     */
    private IAuthoritiesProvider authoritiesProvider;

    /**
     * Curent microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Authorities cache that provide granted authorities per tenant and per resource.<br/>
     * Map<Tenant, Map<Resource, List<GrantedAuthority>>>
     */
    private final Map<String, Map<String, ArrayList<GrantedAuthority>>> grantedAuthoritiesByTenant = new HashMap<>();

    /**
     * Roles allowed ip addresses cache
     */
    private final Map<String, List<RoleAuthority>> grantedRolesIpAddressesByTenant = new HashMap<>();

    /**
     * Spring application context
     */
    private ApplicationContext applicationContext;

    /**
     * Initialize security cache as soon as the application is ready.
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        try {
            // Init all authorities
            for (String tenant : getTenantResolver().getAllActiveTenants()) {
                // Register microservice resources for all configured tenant
                registerMethodResourcesAccessByTenant(tenant);
                // Collect role authorities
                collectRolesByTenant(tenant);
            }
        } catch (SecurityException e) {
            LOGGER.error("Cannot initialize role authorities, no access set", e);
        }
    }

    /**
     *
     * Register all the current microservice endpoints to the administration service.
     *
     * @param pTenant
     *            tenant
     * @throws SecurityException
     *             if error occurs
     * @since 1.0-SNAPSHOT
     */
    public void registerMethodResourcesAccessByTenant(String pTenant) throws SecurityException {
        final List<ResourceMapping> resources = getAuthoritiesProvider().registerEndpoints(microserviceName, pTenant,
                                                                                           getResources());
        if (grantedAuthoritiesByTenant.get(pTenant) != null) {
            grantedAuthoritiesByTenant.get(pTenant).clear();
        }
        for (final ResourceMapping resource : resources) {
            setAuthorities(pTenant, resource);
        }
    }

    /**
     *
     * Retrieve all Role authorities of the given tenant from the administration service
     *
     * @param pTenant
     *            tenant
     * @throws SecurityException
     *             if error occurs
     * @since 1.0-SNAPSHOT
     */
    public void collectRolesByTenant(String pTenant) throws SecurityException {
        final List<RoleAuthority> roleAuthorities = getAuthoritiesProvider().getRoleAuthorities(microserviceName,
                                                                                                pTenant);
        if (grantedRolesIpAddressesByTenant.get(pTenant) != null) {
            grantedRolesIpAddressesByTenant.get(pTenant).clear();
        }
        grantedRolesIpAddressesByTenant.put(pTenant, roleAuthorities);
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
        // FIXME externalize base package / enable multiple base packages
        for (final BeanDefinition def : scanner.findCandidateComponents("fr.cnes.regards")) {
            try {
                final Class<?> controller = Class.forName(def.getBeanClassName());
                // For each method get the method annotated with both @ResourceAccess and @RequestMapping
                for (final Method method : controller.getMethods()) {
                    resources.addAll(manageMethodResource(method));
                }
            } catch (final ClassNotFoundException e) {
                LOGGER.error(String.format("Error getting resources from RestController classe : %s",
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
     * @param pMethod
     *            method of the endpoint
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    private List<ResourceMapping> manageMethodResource(final Method pMethod) {

        final List<ResourceMapping> mappings = new ArrayList<>();

        try {
            if (AnnotationUtils.findAnnotation(pMethod, ResourceAccess.class) != null) {
                final ResourceMapping mapping = MethodAuthorizationUtils.buildResourceMapping(pMethod);

                if (!mapping.getResourceAccess().plugin().equals(Void.class) && (getPluginResourceManager() != null)) {
                    // Manage specific plugin endpoints
                    mappings.addAll(getPluginResourceManager().manageMethodResource(mapping));
                } else {
                    mappings.add(mapping);
                }
            } else {
                LOGGER.debug("Skipping resource management for  method {} as there is no @ResourceAccess annotation on it",
                             pMethod.getName());
            }
        } catch (final ResourceMappingException e) {
            // Skip inconsistent resource management
            LOGGER.warn(e.getMessage(), e);
            LOGGER.warn("Skipping resource management for method \"{}\" on class \"{}\".", pMethod.getName(),
                        pMethod.getDeclaringClass().getCanonicalName());
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
                // we already have some authorities(roles) in the system, so lets get them and add the new ones
                final Set<GrantedAuthority> grantedAuthorities = new LinkedHashSet<>(
                        grantedAuthoritiesByResource.get(resourceId));
                for (final GrantedAuthority granted : pResourceMapping.getAutorizedRoles()) {
                    grantedAuthorities.add(granted);
                }
                newAuthorities = new ArrayList<>(grantedAuthorities);
            } else {
                // we do not have any authorities(roles) for this resource, so lets just take the new ones
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
     * @param pControllerSimpleName
     *            controller simple name
     * @param pMethod
     *            resource Method
     * @param pRoleNames
     *            resource role names
     * @since 1.0-SNAPSHOT
     */
    public void setAuthorities(final String pTenant, final String pUrlPath, final String pControllerSimpleName,
            final RequestMethod pMethod, final String... pRoleNames) {
        // Validate
        Assert.notNull(pUrlPath, "Path to resource cannot be null.");
        Assert.notNull(pMethod, "HTTP method cannot be null.");
        Assert.notNull(pRoleNames, "At least one role is required.");

        // Build granted authorities
        final List<RoleAuthority> newAuthorities = new ArrayList<>();
        for (final String role : pRoleNames) {
            newAuthorities.add(new RoleAuthority(role));
        }

        final ResourceMapping resource = new ResourceMapping(pUrlPath, pControllerSimpleName, pMethod);
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
     * Check if a user can access an annotated method
     *
     * @param pJWTAuthentication
     *            the user authentication object
     * @param pMethod
     *            the method to access
     * @return {@link Boolean#TRUE} if user can access the method
     */
    public Boolean hasAccess(final JWTAuthentication pJWTAuthentication, final Method pMethod) {
        Boolean access = Boolean.FALSE;

        // If authentication do not contains authority, deny access
        if ((pJWTAuthentication.getAuthorities() != null) && !pJWTAuthentication.getAuthorities().isEmpty()) {

            try {
                // Retrieve resource mapping configuration
                final ResourceMapping mapping = MethodAuthorizationUtils.buildResourceMapping(pMethod);
                // Retrieve granted authorities
                final Optional<List<GrantedAuthority>> options = getAuthorities(pJWTAuthentication.getTenant(),
                                                                                mapping);
                if (options.isPresent()) {
                    access = MethodAuthorizationUtils.hasAccess(options.get(), pJWTAuthentication.getAuthorities());
                }

                // CHECKSTYLE:OFF
                final String decision = access ? "granted" : "denied";
                // CHECKSTYLE:ON
                final String logMessage = String.format("Access %s to resource %s for user %s.", decision,
                                                        mapping.getResourceMappingId(), pJWTAuthentication.getName());
                LOGGER.debug(logMessage);

            } catch (final ResourceMappingException e) {
                LOGGER.debug(e.getMessage(), e);
                // Nothing to do : access will be denied
            }
        }
        return access;
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

    /**
     *
     * Return the role authority configuration for the given tenant
     *
     * @param pRoleAuthorityName
     *            Role name
     * @param pTenant
     *            tenant
     * @return authorized addresses
     * @since 1.0-SNAPSHOT
     */
    public Optional<RoleAuthority> getRoleAuthority(final String pRoleAuthorityName, final String pTenant) {
        final List<RoleAuthority> roles = grantedRolesIpAddressesByTenant.get(pTenant);
        if (roles != null) {
            for (final RoleAuthority role : roles) {
                if (role.getAuthority().equals(pRoleAuthorityName)) {
                    return Optional.of(role);
                }
            }
        }
        return Optional.empty();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.
     * ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext pApplicationContext) throws BeansException {
        this.applicationContext = pApplicationContext;
    }

    public IAuthoritiesProvider getAuthoritiesProvider() {
        if (authoritiesProvider == null) {
            authoritiesProvider = applicationContext.getBean(IAuthoritiesProvider.class);
        }
        return authoritiesProvider;
    }

    public ITenantResolver getTenantResolver() {
        if (tenantResolver == null) {
            tenantResolver = applicationContext.getBean(ITenantResolver.class);
        }
        return tenantResolver;
    }

    public IPluginResourceManager getPluginResourceManager() {
        if (pluginResourceManager == null) {
            pluginResourceManager = applicationContext.getBean(IPluginResourceManager.class);
        }
        return pluginResourceManager;
    }

    public void updateAuthoritiesFor(String tenant, String roleName) {
        Set<ResourceMapping> newMappings = getAuthoritiesProvider().getResourceMappings(microserviceName, tenant,
                                                                                        roleName);
        newMappings.forEach(mapping -> setAuthorities(tenant, mapping));
    }
}
