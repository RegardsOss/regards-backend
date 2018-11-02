/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.endpoint;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * Authorities cache that provide granted authorities per tenant and per resource.<br/>
     * Map<Tenant, Map<Resource, List<GrantedAuthority>>>
     */
    private final Map<String, Map<String, ArrayList<GrantedAuthority>>> grantedAuthoritiesByTenant = new ConcurrentHashMap<>();

    /**
     * Roles allowed ip addresses cache
     */
    private final Map<String, List<RoleAuthority>> rolesByTenant = new ConcurrentHashMap<>();

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
                manageTenant(tenant);
            }
        } catch (SecurityException e) {
            LOGGER.error("Cannot initialize role authorities, no access set", e);
        }
    }

    /**
     * Manage tenant initialization
     * @param tenant tenant to initialize
     * @throws SecurityException if error occurs!
     */
    public void manageTenant(String tenant) throws SecurityException {
        // Register microservice resources for current tenant
        getAuthoritiesProvider().registerEndpoints(microserviceName, tenant, getResources());
        // Collect available roles and authorities for current tenant
        collectRolesAndAuthorities(tenant);
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
                                           def.getBeanClassName()), e);
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
                LOGGER.debug(
                        "Skipping resource management for  method {} as there is no @ResourceAccess annotation on it",
                        pMethod.getName());
            }
        } catch (final ResourceMappingException e) {
            // Skip inconsistent resource management
            LOGGER.warn(e.getMessage(), e);
            LOGGER.warn("Skipping resource management for method \"{}\" on class \"{}\".",
                        pMethod.getName(),
                        pMethod.getDeclaringClass().getCanonicalName());
        }
        return mappings;
    }

    /**
     *
     * Retrieve all Role authorities of the given tenant from the administration service
     *
     * @param tenant
     *            tenant
     * @throws SecurityException
     *             if error occurs
     * @since 1.0-SNAPSHOT
     */
    public void collectRolesAndAuthorities(String tenant) throws SecurityException {

        // Register authorized roles by tenant (so you can manage IP filtering)
        List<RoleAuthority> roles = getAuthoritiesProvider().getRoleAuthorities(microserviceName, tenant);
        rolesByTenant.put(tenant, roles);

        // Manage tenant authorities
        buildAuthorities(tenant, roles);
    }

    /**
     * Build authorities cache for current tenant based on available roles
     * @param tenant the tenant
     * @param roles related roles
     */
    private void buildAuthorities(String tenant, List<RoleAuthority> roles) {
        // Remove authorities cache for current tenant
        if (grantedAuthoritiesByTenant.get(tenant) != null) {
            // Clear existing authorities then rebuild cache
            grantedAuthoritiesByTenant.get(tenant).clear();
        }

        for (RoleAuthority authority : roles) {
            Set<ResourceMapping> configuredResources = getAuthoritiesProvider()
                    .getResourceMappings(microserviceName, tenant, authority.getRoleName());
            configuredResources.forEach(resource -> setAuthorities(tenant, resource));
        }
    }

    /**
     *
     * Add resources authorization
     *
     * @param tenant
     *            tenant name
     * @param resourceMapping
     *            resource to add
     * @since 1.0-SNAPSHOT
     */
    private void setAuthorities(String tenant, ResourceMapping resourceMapping) {

        // Get resource authority cache
        Map<String, ArrayList<GrantedAuthority>> grantedAuthoritiesByResource = grantedAuthoritiesByTenant.get(tenant);
        if (grantedAuthoritiesByResource == null) {
            grantedAuthoritiesByResource = new HashMap<>();
            grantedAuthoritiesByTenant.put(tenant, grantedAuthoritiesByResource);
        }

        if ((resourceMapping != null) && (resourceMapping.getAutorizedRoles() != null)) {
            String resourceId = resourceMapping.getResourceMappingId();
            ArrayList<GrantedAuthority> newAuthorities;
            if (grantedAuthoritiesByResource.containsKey(resourceId)) {
                // we already have some authorities(roles) in the system, so lets get them and add the new ones
                final Set<GrantedAuthority> grantedAuthorities = new LinkedHashSet<>(grantedAuthoritiesByResource
                                                                                             .get(resourceId));
                for (final GrantedAuthority granted : resourceMapping.getAutorizedRoles()) {
                    grantedAuthorities.add(granted);
                }
                newAuthorities = new ArrayList<>(grantedAuthorities);
            } else {
                // we do not have any authorities(roles) for this resource, so lets just take the new ones
                newAuthorities = new ArrayList<>();
                newAuthorities.addAll(resourceMapping.getAutorizedRoles());
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

    private void unsetAuthorities(String tenant, String roleName) {
        RoleAuthority authority = new RoleAuthority(roleName);

        // Get resource authority cache
        Map<String, ArrayList<GrantedAuthority>> grantedAuthoritiesByResource = grantedAuthoritiesByTenant.get(tenant);
        if (grantedAuthoritiesByResource != null) {
            for (ArrayList<GrantedAuthority> resAuthorities : grantedAuthoritiesByResource.values()) {
                resAuthorities.remove(authority);
            }
        }
    }

    public void updateAuthoritiesFor(String tenant, String roleName) {
        unsetAuthorities(tenant, roleName);
        Set<ResourceMapping> newMappings = getAuthoritiesProvider()
                .getResourceMappings(microserviceName, tenant, roleName);
        newMappings.forEach(mapping -> setAuthorities(tenant, mapping));
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
                final String logMessage = String.format("Access %s to resource %s for user %s.",
                                                        decision,
                                                        mapping.getResourceMappingId(),
                                                        pJWTAuthentication.getName());
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
        final List<RoleAuthority> roles = rolesByTenant.get(pTenant);
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
    public void setApplicationContext(ApplicationContext pApplicationContext) {
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
}
