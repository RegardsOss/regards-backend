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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.ResourceMappingException;
import fr.cnes.regards.framework.security.event.UpdateAuthoritiesEvent;
import fr.cnes.regards.framework.security.event.handler.UpdateAuthoritiesEventHandler;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * Service MethodAutorizationServiceImpl<br/>
 * Allow to set/get the REST resource method access authorizations.<br/>
 * An authorization is defined by a endpoint, a HTTP Verb and a list of authorized user ROLES
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
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
     * AMQP Object to send event on queue.
     */
    @Autowired
    private ISubscriber eventListener;

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
     * After bean construction
     */
    @PostConstruct
    public void init() {
        refreshAuthorities();
        try {
            // Listen for every update authorities message
            // Update authorities event must be provided by administration service when the authorities configuration
            // are updated like resourceAccess or Roles configurations.
            eventListener.subscribeTo(UpdateAuthoritiesEvent.class, new UpdateAuthoritiesEventHandler(this),
                                      AmqpCommunicationMode.ONE_TO_MANY, AmqpCommunicationTarget.EXTERNAL);
        } catch (final RabbitMQVhostException e) {
            LOG.error("Error during security module initialization. {}", e.getMessage(), e);
            throw new ApplicationContextException(e.getMessage());
        }
    }

    /**
     *
     * Refresh all authorities configuration information for all tenants of the current microservice
     *
     * @since 1.0-SNAPSHOT
     */
    public void refreshAuthorities() {
        try {
            jwtService.injectToken("instance", RoleAuthority.getSysRole(microserviceName));
            tenantResolver.getAllTenants().forEach(this::refreshTenantAuthorities);
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     *
     * Refresh all authorities configuration information for given tenant of the current microservice
     *
     * @param pTenant
     *            tenant to refresh
     * @since 1.0-SNAPSHOT
     */
    private void refreshTenantAuthorities(final String pTenant) {
        try {
            jwtService.injectToken(pTenant, RoleAuthority.getSysRole(microserviceName));
            registerMethodResourcesAccessByTenant(pTenant);
            collectRolesByTenant(pTenant);
        } catch (final JwtException e) {
            LOG.error(String.format("Error during resources access initialization for tenant %s", pTenant));
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     *
     * Register all the current microservice endpoints to the administration service.
     *
     * @param pTenant
     *            tenant
     * @since 1.0-SNAPSHOT
     */
    private void registerMethodResourcesAccessByTenant(final String pTenant) {
        final List<ResourceMapping> resources = authoritiesProvider.registerEndpoints(getResources());
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
     * @since 1.0-SNAPSHOT
     */
    private void collectRolesByTenant(final String pTenant) {
        final List<RoleAuthority> roleAuthorities = authoritiesProvider.getRoleAuthorities();
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
        for (final BeanDefinition def : scanner.findCandidateComponents("fr.cnes.regards")) {
            try {
                final Class<?> controller = Class.forName(def.getBeanClassName());
                // For each method get the method annotated with both @ResourceAccess and @RequestMapping
                for (final Method method : controller.getMethods()) {
                    resources.addAll(manageMethodResource(method));
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

                if (!mapping.getResourceAccess().plugin().equals(Void.class) && (pluginResourceManager != null)) {
                    // Manage specific plugin endpoints
                    mappings.addAll(pluginResourceManager.manageMethodResource(mapping));
                } else {
                    mappings.add(mapping);
                }
            } else {
                LOG.debug("Skipping resource management for  method {} as there is no @ResourceAccess annotation on it",
                          pMethod.getName());
            }
        } catch (final ResourceMappingException e) {
            // Skip inconsistent resource management
            LOG.warn(e.getMessage(), e);
            LOG.warn("Skipping resource management for method \"{}\" on class \"{}\".", pMethod.getName(),
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
                if (access) {
                    LOG.debug(logMessage);
                } else {
                    LOG.info(logMessage);
                }

            } catch (final ResourceMappingException e) {
                LOG.debug(e.getMessage(), e);
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
}
