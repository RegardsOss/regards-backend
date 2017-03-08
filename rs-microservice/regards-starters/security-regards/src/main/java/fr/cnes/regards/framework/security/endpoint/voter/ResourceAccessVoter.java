/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint.voter;

import java.util.Collection;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * REGARDS endpoint security voter to manage resource access dynamically at method level.
 *
 * {@link MethodSecurityAutoConfiguration}
 *
 * @author msordi
 *
 */
public class ResourceAccessVoter implements AccessDecisionVoter<Object> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceAccessVoter.class);

    /**
     * Method authorization service
     */
    private final MethodAuthorizationService methodAuthService;

    /**
     * Constructor
     *
     * @param pMethodAuthService
     *            the method authoization service
     */
    public ResourceAccessVoter(final MethodAuthorizationService pMethodAuthService) {
        this.methodAuthService = pMethodAuthService;
    }

    @Override
    public boolean supports(final ConfigAttribute pAttribute) {
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
    public boolean supports(final Class<?> pClazz) {
        return true;
    }

    @Override
    public int vote(final Authentication pAuthentication, final Object pObject,
            final Collection<ConfigAttribute> pAttributes) {

        // Default behavior : deny access
        int access = ACCESS_DENIED;

        final JWTAuthentication jwtAuth = (JWTAuthentication) pAuthentication;

        if (pObject instanceof MethodInvocation) {

            final MethodInvocation mi = (MethodInvocation) pObject;
            if (methodAuthService.hasAccess(jwtAuth, mi.getMethod())) {
                access = ACCESS_GRANTED;
            } else {
                access = ACCESS_DENIED;
            }
        }

        // CHECKSTYLE:OFF
        final String decision = access == ACCESS_GRANTED ? "granted" : "denied";
        // CHECKSTYLE:ON
        LOG.debug("Access {} for user {}.", decision, jwtAuth.getName());

        return access;
    }
}
